package com.bergerkiller.bukkit.tc.controller;

import com.bergerkiller.bukkit.common.bases.mutable.VectorAbstract;
import com.bergerkiller.bukkit.common.controller.EntityNetworkController;
import com.bergerkiller.bukkit.common.entity.type.CommonMinecart;
import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.wrappers.DataWatcher;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.parts.FakePlayer;
import com.bergerkiller.bukkit.tc.parts.VirtualEntity;
import com.bergerkiller.generated.net.minecraft.server.EntityHandle;
import com.bergerkiller.generated.net.minecraft.server.EntityLivingHandle;
import com.bergerkiller.generated.net.minecraft.server.EntityTrackerEntryHandle;
import com.bergerkiller.generated.net.minecraft.server.PacketPlayOutEntityMetadataHandle;
import com.bergerkiller.generated.net.minecraft.server.PacketPlayOutMountHandle;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class MinecartMemberNetwork extends EntityNetworkController<CommonMinecart<?>> {
    public static final float ROTATION_K = 0.55f;
    public static final int ABSOLUTE_UPDATE_INTERVAL = 200;
    public static final double VELOCITY_SOUND_RADIUS = 16;
    public static final double VELOCITY_SOUND_RADIUS_SQUARED = VELOCITY_SOUND_RADIUS * VELOCITY_SOUND_RADIUS;
    private static final Vector ZERO_VELOCITY = new Vector(0.0, 0.0, 0.0);
    private final Set<Player> velocityUpdateReceivers = new HashSet<>();
    private boolean wasUpsideDown = false; // whether passengers should be rendered upside-down
    private boolean useVirtualCamera = false; // whether a virtual camera detached from the minecart should be used by players
    private VirtualEntity fakeMount = null;
    private FakePlayer fakePlayer = null;
    private boolean disableMountHandling = false;
    private boolean disableVirtualCameraHandling = false;
    private boolean isFirstUpdate = true;
    private double lastDeltaX = 0.0;
    private double lastDeltaY = 0.0;
    private double lastDeltaZ = 0.0;

    public MinecartMemberNetwork() {
        final VectorAbstract velLiveBase = this.velLive;
        this.velLive = new VectorAbstract() {
            public double getX() {
                return convertVelocity(velLiveBase.getX());
            }

            public double getY() {
                return convertVelocity(velLiveBase.getY());
            }

            public double getZ() {
                return convertVelocity(velLiveBase.getZ());
            }

            public VectorAbstract setX(double x) {
                velLiveBase.setX(x);
                return this;
            }

            public VectorAbstract setY(double y) {
                velLiveBase.setY(y);
                return this;
            }

            public VectorAbstract setZ(double z) {
                velLiveBase.setZ(z);
                return this;
            }
        };
    }

    private static float getAngleKFactor(float angle1, float angle2) {
        float diff = angle1 - angle2;
        while (diff <= -180.0f) {
            diff += 360.0f;
        }
        while (diff > 180.0f) {
            diff -= 360.0f;
        }
        return (ROTATION_K * diff);
    }

    private double convertVelocity(double velocity) {
        return isSoundEnabled() ? MathUtil.clamp(velocity, getEntity().getMaxSpeed()) : 0.0;
    }

    private boolean isSoundEnabled() {
        MinecartMember<?> member = (MinecartMember<?>) entity.getController();
        return !(member == null || member.isUnloaded()) && member.getGroup().getProperties().isSoundEnabled();
    }

    private void updateVelocity(Player player) {
        final boolean inRange = isSoundEnabled() && getEntity().loc.distanceSquared(player) <= VELOCITY_SOUND_RADIUS_SQUARED;
        if (LogicUtil.addOrRemove(velocityUpdateReceivers, player, inRange)) {
            CommonPacket velocityPacket;
            if (inRange) {
                // Send the current velocity
                velocityPacket = getVelocityPacket(velSynched.getX(), velSynched.getY(), velSynched.getZ());
            } else {
                // Clear velocity
                velocityPacket = getVelocityPacket(0.0, 0.0, 0.0);
            }
            // Send
            PacketUtil.sendPacket(player, velocityPacket);
        }
    }

    public void sendUpsideDownUnmount(Player viewer, Entity entity) {
        // Destroy fake first-person mount
        if (viewer == entity && this.fakeMount != null) {
            this.fakeMount.destroy(viewer);
        }

        // Destroy a fake player entity - if displayed
        destroyFakeEntity(viewer, entity);
    }

    private void destroyFakeEntity(Player viewer, Entity entity) {
        // Make entity visible again and reset potential nametags by re-sending all metadata
        DataWatcher metaTmp = EntityHandle.fromBukkit(entity).getDataWatcher();
        PacketPlayOutEntityMetadataHandle metaPacket = PacketPlayOutEntityMetadataHandle.createNew(entity.getEntityId(), metaTmp, true);
        PacketUtil.sendPacket(viewer, metaPacket);

        // Destroy a fake player entity - if displayed
        if (entity instanceof Player && this.fakePlayer != null) {
            this.fakePlayer.destroy(viewer, (Player) entity);
        }
    }

    private void handlePassengerMount(Player viewer, Entity passenger) {
        boolean isVirtualMount = (this.useVirtualCamera && viewer == passenger);

        if (this.wasUpsideDown || isVirtualMount) {
            if (passenger instanceof Player) {
                Player player = (Player) passenger;

                // Create a new entity Id for a fake player, if needed
                if (this.fakePlayer == null) {
                    this.fakePlayer = new FakePlayer();
                }

                // Refresh name
                this.fakePlayer.setMode(viewer, player, this.wasUpsideDown ? 
                        FakePlayer.DisplayMode.UPSIDEDOWN : FakePlayer.DisplayMode.NORMAL);

                // Make original entity invisible using a metadata change
                DataWatcher metaTmp = new DataWatcher();
                metaTmp.set(EntityHandle.DATA_FLAGS, (byte) (EntityHandle.DATA_FLAG_INVISIBLE));
                PacketPlayOutEntityMetadataHandle metaPacket = PacketPlayOutEntityMetadataHandle.createNew(passenger.getEntityId(), metaTmp, true);
                PacketUtil.sendPacket(viewer, metaPacket);

                // Spawn the fake entity
                this.fakePlayer.spawn(viewer, player);
            } else {
                // Apply the upside-down nametag to the entity to turn him upside-down
                // Only do this when upside-down; we don't want to ever show the nametag
                if (this.wasUpsideDown) {
                    DataWatcher metaTmp = new DataWatcher();
                    metaTmp.set(EntityHandle.DATA_CUSTOM_NAME, FakePlayer.DisplayMode.UPSIDEDOWN.getPlayerName());
                    metaTmp.set(EntityHandle.DATA_CUSTOM_NAME_VISIBLE, false);
                    PacketPlayOutEntityMetadataHandle metaPacket = PacketPlayOutEntityMetadataHandle.createNew(passenger.getEntityId(), metaTmp, true);
                    PacketUtil.sendPacket(viewer, metaPacket);
                }
            }
        }

        if (isVirtualMount && this.fakeMount == null && !this.disableVirtualCameraHandling) {
            this.fakeMount = new VirtualEntity();
            this.fakeMount.setPosition(0.0, 1.0, 0.0);

            // When synchronizing passenger to himself, we put him on a fake mount to alter where the camera is at
            this.fakeMount.updatePosition(this.getTransform());
            this.fakeMount.syncPosition(Collections.emptyList(), true);
            this.fakeMount.getMetaData().set(EntityHandle.DATA_FLAGS, (byte) (EntityHandle.DATA_FLAG_INVISIBLE));
            this.fakeMount.getMetaData().set(EntityLivingHandle.DATA_HEALTH, 10.0F);
            this.fakeMount.setPassengers(new int[] {passenger.getEntityId()});
            this.fakeMount.spawn(viewer, this.lastDeltaX, this.lastDeltaY, this.lastDeltaZ);
        }
    }

    private void handlePassengerUnmount(Player viewer, Entity passenger) {
        // If this player has mounted this Minecart and virtual camera is used, unmount the fake mount
        boolean destroyFakeEntity = this.wasUpsideDown;
        if ((viewer == passenger) && this.useVirtualCamera && this.fakeMount != null && !this.disableVirtualCameraHandling) {
            this.fakeMount.destroy(viewer);
            this.fakeMount = null;

            // Also destroy the ghost player that took this person's place in the Minecart
            destroyFakeEntity = true;
        }

        // When upside-down, destroy the fake entity that is displayed
        if (destroyFakeEntity) {
            destroyFakeEntity(viewer, passenger);
        }
    }

    // update the list of entities directly attached to this Minecart as passenger
    private void sendDirectPassengers(Player viewer, Collection<Entity> passengers) {
        // Clear mounted passengers
        PacketPlayOutMountHandle mount = PacketPlayOutMountHandle.createNew(this.getEntity().getEntityId(), new int[0]);
        for (Entity passenger : passengers) {
            if ((this.wasUpsideDown || (this.useVirtualCamera && passenger == viewer)) && passenger instanceof Player && this.fakePlayer != null) {
                mount.addMountedEntityId(this.fakePlayer.entityId);
            } else {
                mount.addMountedEntityId(passenger.getEntityId());
            }
        }
        PacketUtil.sendPacket(viewer, mount);

        // Make fake player visible if needed
        if (this.fakePlayer != null && this.fakePlayer.wasInvisible) {
            this.fakePlayer.wasInvisible = false;

            DataWatcher metaTmp = new DataWatcher();
            metaTmp.watch(EntityHandle.DATA_FLAGS, (byte) 0);
            FakePlayer.setMetaVisibility(metaTmp, true);
            PacketPlayOutEntityMetadataHandle metaPacket = PacketPlayOutEntityMetadataHandle.createNew(this.fakePlayer.entityId, metaTmp, true);
            PacketUtil.sendPacket(viewer, metaPacket);
        }
    }

    @Override
    protected void onSyncPassengers(Player viewer, List<Entity> oldPassengers, List<Entity> newPassengers) {
        boolean viewerChanged = (!oldPassengers.contains(viewer) || !newPassengers.contains(viewer));
        if (!this.wasUpsideDown && (!this.useVirtualCamera || !viewerChanged)) {
            super.onSyncPassengers(viewer, oldPassengers, newPassengers);
            return;
        }

        if (!this.disableMountHandling) {
            // Mount new passengers
            for (Entity newPassenger : newPassengers) {
                if (oldPassengers.contains(newPassenger)) {
                    continue;
                }

                // Send upside-down mounted player to a viewer
                handlePassengerMount(viewer, newPassenger);
            }

            // Unmount old passengers
            for (Entity oldPassenger : oldPassengers) {
                if (newPassengers.contains(oldPassenger)) {
                    continue;
                }

                // Send upside-down unmounted player to a viewer
                handlePassengerUnmount(viewer, oldPassenger);
            }
        }

        this.sendDirectPassengers(viewer, newPassengers);
    }

    @Override
    public void makeVisible(Player viewer) {
        super.makeVisible(viewer);
        this.velocityUpdateReceivers.add(viewer);
        this.updateVelocity(viewer);

        boolean viewerIsPassenger = this.getSynchedPassengers().contains(viewer);
        if (this.wasUpsideDown || (this.useVirtualCamera && viewerIsPassenger)) {
            for (Entity passenger : this.getSynchedPassengers()) {
                handlePassengerMount(viewer, passenger);
            }
        }
        this.sendDirectPassengers(viewer, this.getSynchedPassengers());
    }

    @Override
    public void makeHidden(Player viewer, boolean instant) {
        super.makeHidden(viewer, instant);
        this.velocityUpdateReceivers.remove(viewer);
        PacketUtil.sendPacket(viewer, PacketType.OUT_ENTITY_VELOCITY.newInstance(getEntity().getEntityId(), ZERO_VELOCITY));

        boolean viewerIsPassenger = this.getSynchedPassengers().contains(viewer);
        if (this.wasUpsideDown || (this.useVirtualCamera && viewerIsPassenger)) {
            for (Entity passenger : this.getSynchedPassengers()) {
                handlePassengerUnmount(viewer, passenger);
            }
        }
    }

    @Override
    public void onSync() {
        try {
            if (entity.isDead()) {
                return;
            }
            MinecartMember<?> member = (MinecartMember<?>) entity.getController();
            if (member.isUnloaded()) {
                // Unloaded: Synchronize just this Minecart
                super.onSync();
                return;
            } else if (member.getIndex() != 0) {
                // Ignore
                return;
            }

            // Update the entire group
            int i;
            MinecartGroup group = member.getGroup();
            final int count = group.size();
            MinecartMemberNetwork[] networkControllers = new MinecartMemberNetwork[count];
            for (i = 0; i < count; i++) {
                EntityNetworkController<?> controller = group.get(i).getEntity().getNetworkController();
                if (!(controller instanceof MinecartMemberNetwork)) {
                    // This is not good, but we can fix it...but not here
                    group.networkInvalid.set();
                    return;
                }
                networkControllers[i] = (MinecartMemberNetwork) controller;
            }

            // Synchronize to the clients
            if (this.getTicksSinceLocationSync() > ABSOLUTE_UPDATE_INTERVAL) {
                // Perform absolute updates
                for (i = 0; i < count; i++) {
                    networkControllers[i].syncSelf(group.get(i), true, true, true);
                }
            } else {
                // Perform relative updates
                boolean needsSync = this.isUpdateTick();
                if (!needsSync) {
                    for (i = 0; i < count; i++) {
                        MinecartMemberNetwork controller = networkControllers[i];
                        if (controller.getEntity().isPositionChanged() || controller.getEntity().getDataWatcher().isChanged() || controller.isPassengersChanged()) {
                            needsSync = true;
                            break;
                        }
                    }
                }
                if (needsSync) {
                    boolean moved = false;
                    boolean rotated = false;

                    // Check whether changes are needed
                    for (i = 0; i < count; i++) {
                        MinecartMemberNetwork controller = networkControllers[i];
                        moved |= controller.isPositionChanged(MIN_RELATIVE_POS_CHANGE);
                        rotated |= controller.isRotationChanged(MIN_RELATIVE_ROT_CHANGE);
                    }

                    // Perform actual updates
                    for (i = 0; i < count; i++) {
                        networkControllers[i].syncSelf(group.get(i), moved, rotated, false);
                    }
                }
            }
        } catch (Throwable t) {
            TrainCarts.plugin.log(Level.SEVERE, "Failed to synchronize a network controller:");
            TrainCarts.plugin.handle(t);
        }
    }

    /**
     * Gets the position transform of this Minecart
     * 
     * @return transform
     */
    private Matrix4x4 getTransform() {
        Matrix4x4 transform = new Matrix4x4();

        // Some factor of the movement change needs to be re-predicted
        // Otherwise things stuck to this Minecart will always move ahead
        final double MOVE_FX = 0.625;
        transform.translateRotate(
                (this.locSynched.getX() - (this.lastDeltaX * MOVE_FX)),
                (this.locSynched.getY() - (this.lastDeltaY * MOVE_FX)),
                (this.locSynched.getZ() - (this.lastDeltaZ * MOVE_FX)),
                this.locSynched.getYaw(), this.locSynched.getPitch()
        );
        return transform;
    }

    public void syncSelf(MinecartMember<?> member, boolean moved, boolean rotated, boolean absolute) {
        // Read live location
        double posX = locLive.getX();
        double posY = locLive.getY();
        double posZ = locLive.getZ();
        float rotYawLive = locLive.getYaw();
        float rotPitchLive = locLive.getPitch();
        float rotYaw = rotYawLive;
        float rotPitch = rotPitchLive;

        // Synchronize location
        if (rotated && !member.isDerailed() && !isFirstUpdate) {
            // Update rotation with control system function
            // This ensures that the Client animation doesn't glitch the rotation
            rotYaw += getAngleKFactor(rotYaw, locSynched.getYaw());
            rotPitch += getAngleKFactor(rotPitch, locSynched.getPitch());
        }

        // If the pitch angle crosses a 180-degree boundary, re-spawn the minecart
        // This prevents a really ugly 360 rotation from occurring
        if (rotated) {
            int prot_a = EntityTrackerEntryHandle.getProtocolRotation(rotPitch) & 0xFF;
            int prot_b = EntityTrackerEntryHandle.getProtocolRotation(locSynched.getPitch()) & 0xFF;
            if ((prot_a <= 127 && prot_b >= 128) || (prot_b <= 127 && prot_a >= 128)) {
                rotYaw = rotYawLive;
                rotPitch = rotPitchLive;
                absolute = false;
                rotated = false;

                // Instantly set the newly requested rotation
                locSynched.setRotation(rotYaw, rotPitch);

                // Destroy and re-spawn the minecart with the new coordinates
                // Do not do any wacky passenger mounting/unmounting here
                // We only want to respawn the Minecart itself
                this.disableMountHandling = true;
                for (Player viewer : this.getViewers()) {
                    super.makeHidden(viewer, true);
                    super.makeVisible(viewer);
                }
                this.disableMountHandling = false;
            }
        }

        isFirstUpdate = false;
        getEntity().setPositionChanged(false);

        // Absolute/relative movement updates
        if (absolute) {
            syncLocationAbsolute(posX, posY, posZ, rotYaw, rotPitch);

            lastDeltaX = 0.0;
            lastDeltaY = 0.0;
            lastDeltaZ = 0.0;
        } else {
            if (moved) {
                lastDeltaX = (posX - this.locSynched.getX());
                lastDeltaY = (posY - this.locSynched.getY());
                lastDeltaZ = (posZ - this.locSynched.getZ());
            }

            syncLocation(moved, rotated, posX, posY, posZ, rotYaw, rotPitch);
        }

        // Velocity is used exclusively for controlling the minecart's audio level
        // When derailed, no audio should be made. Otherwise, the velocity speed controls volume.
        // Minecraft does not play minecart audio for the Y-axis. To make sound on vertical rails,
        // we instead apply the vector length to just the X-axis so that this works.
        Vector currVelocity;
        if (member.isDerailed()) {
            currVelocity = new Vector(0.0, 0.0, 0.0);
        } else {
            currVelocity = new Vector(velLive.length(), 0.0, 0.0);
        }
        boolean velocityChanged = (velSynched.distanceSquared(currVelocity) > (MIN_RELATIVE_VELOCITY * MIN_RELATIVE_VELOCITY)) ||
                (velSynched.lengthSquared() > 0.0 && currVelocity.lengthSquared() == 0.0);

        // Synchronize velocity
        if (absolute || getEntity().isVelocityChanged() || velocityChanged) {
            // Reset dirty velocity
            getEntity().setVelocityChanged(false);

            // Send packets to recipients
            velSynched.set(currVelocity);

            CommonPacket velocityPacket = getVelocityPacket(currVelocity.getX(), currVelocity.getY(), currVelocity.getZ());
            for (Player player : velocityUpdateReceivers) {
                PacketUtil.sendPacket(player, velocityPacket);
            }
        }

        // Update the velocity update receivers
        if (isSoundEnabled()) {
            for (Player player : getViewers()) {
                updateVelocity(player);
            }
        }

        // Synchronize meta data
        syncMetaData();

        // Handle switching between upside-down/virtual camera views
        boolean isUpsideDown = MathUtil.getAngleDifference(locLive.getPitch(), 180.0f) < 89.0f;
        boolean isVirtualCamera = isUpsideDown || (locLive.getPitch() < -46.0f) || (locLive.getPitch() > 46.0f);
        if (isUpsideDown != this.wasUpsideDown || isVirtualCamera != this.useVirtualCamera) {  
            List<Entity> old_passengers = this.getSynchedPassengers();

            if (isVirtualCamera && this.useVirtualCamera) {
                disableVirtualCameraHandling = true;
            }

            // First remove all old passengers
            for (Player viewer : this.getViewers()) {
                onSyncPassengers(viewer, old_passengers, new ArrayList<Entity>(0));
            }

            // Change modes
            this.useVirtualCamera = isVirtualCamera;
            this.wasUpsideDown = isUpsideDown;

            // Add all passengers again
            for (Player viewer : this.getViewers()) {
                onSyncPassengers(viewer, new ArrayList<Entity>(0), old_passengers);
            }

            this.disableVirtualCameraHandling = false;
        }
        this.syncPassengers();

        // Synchronized the player mount to the player that rides this Minecart, when upside-down
        // This moves the mount along with the player and simulates an alternative first-person camera view
        if (this.useVirtualCamera && (absolute || moved) && this.fakeMount != null) {
            this.fakeMount.updatePosition(this.getTransform());

            Collection<Entity> passengers = this.getSynchedPassengers();
            for (Player viewer : this.getViewers()) {
                if (passengers.contains(viewer)) {
                    this.fakeMount.syncPosition(Collections.singleton(viewer), absolute);
                    break;
                }
            }
        }

        // Synchronize the head and body rotation of the passenger(s) to the fake player
        // This makes the fake player look where the player/entity is looking
        Player fakeRealPlayer = null;
        if (this.fakePlayer != null && (this.wasUpsideDown || this.useVirtualCamera)) {
            for (Entity passenger : this.getSynchedPassengers()) {
                if (passenger instanceof Player && (this.wasUpsideDown || this.getViewers().contains(passenger))) {
                    fakeRealPlayer = (Player) passenger;
                    break;
                }
            }
        }

        if (fakeRealPlayer != null) {
            EntityHandle realPlayer = EntityHandle.fromBukkit(fakeRealPlayer);

            float yaw = realPlayer.getYaw();
            float pitch = realPlayer.getPitch();
            float headRot = realPlayer.getHeadRotation();

            // Reverse the values and correct head yaw, because the player is upside-down
            if (this.wasUpsideDown) {
                pitch = -pitch;
                headRot = -headRot + 2.0f * yaw;
            }

            // Only send to self-player when not upside-down
            Collection<Player> viewers;
            if (this.wasUpsideDown) {
                viewers = this.getViewers();
            } else {
                viewers = Collections.singleton(fakeRealPlayer);
            }
            this.fakePlayer.syncRotation(viewers, yaw, pitch, headRot);
        }
    }
}
