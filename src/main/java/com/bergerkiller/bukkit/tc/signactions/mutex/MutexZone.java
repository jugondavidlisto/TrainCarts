package com.bergerkiller.bukkit.tc.signactions.mutex;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartGroupStore;
import com.bergerkiller.bukkit.tc.controller.components.RailTracker.TrackedRail;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;

public class MutexZone {
    public final UUID world;
    public final IntVector3 block;
    public final IntVector3 start;
    public final IntVector3 end;
    private MinecartGroup currentGroup = null;
    private int currentGroupTimeout = 0;

    private MutexZone(UUID world, IntVector3 block, int dx, int dy, int dz) {
        this.world = world;
        this.block = block;
        this.start = new IntVector3(block.x - dx, block.y - dy, block.z - dz);
        this.end = new IntVector3(block.x + dx, block.y + dy, block.z + dz);
    }

    public boolean containsBlock(UUID world, IntVector3 block) {
        return world.equals(this.world) &&
                block.x >= start.x && block.y >= start.y && block.z >= start.z &&
                block.x <= end.x && block.y <= end.y && block.z <= end.z;
    }

    public boolean containsBlock(Block block) {
        return block.getWorld().getUID().equals(this.world) &&
                block.getX() >= start.x && block.getY() >= start.y && block.getZ() >= start.z &&
                block.getX() <= end.x && block.getY() <= end.y && block.getZ() <= end.z;
    }

    public boolean isNearby(UUID world, IntVector3 block, int radius) {
        if (!world.equals(this.world)) return false;

        return block.x>=(start.x-radius) && block.y>=(start.y-radius) && block.z>=(start.z-radius) &&
               block.x<=(end.x + radius) && block.y<=(end.y + radius) && block.z<=(end.z + radius);
    }

    public static MutexZone fromSign(SignActionEvent info) {
        // mutex dx/dy/dz
        // mutex (dx+dz)/dy
        // mutex (dx+dy+dz)
        int dx = 1;
        int dy = 2;
        int dz = 1;
        String coords = info.getLine(1);
        int firstSpace = coords.indexOf(' ');
        if (firstSpace != -1) {
            coords = coords.substring(firstSpace).trim();
            if (!coords.isEmpty()) {
                String[] parts = coords.split("/");
                if (parts.length >= 3) {
                    dx = ParseUtil.parseInt(parts[0], dx);
                    dy = ParseUtil.parseInt(parts[1], dy);
                    dz = ParseUtil.parseInt(parts[2], dz);
                } else if (parts.length >= 2) {
                    dx = dz = ParseUtil.parseInt(parts[0], dx);
                    dy = ParseUtil.parseInt(parts[1], dy);
                } else if (parts.length >= 1) {
                    dx = dy = dz = ParseUtil.parseInt(parts[0], dx);
                }
            }
        }
        return new MutexZone(info.getWorld().getUID(), getPosition(info), dx, dy, dz);
    }

    public static IntVector3 getPosition(SignActionEvent info) {
        Location middlePos = info.getCenterLocation();
        if (middlePos != null) {
            return new IntVector3(middlePos);
        } else {
            return new IntVector3(info.getBlock());
        }
    }

    public boolean tryEnter(MinecartGroup group) {
        // Check not occupied by someone else
        int serverTicks = CommonUtil.getServerTicks();
        if (this.currentGroup != null && this.currentGroup != group && MinecartGroupStore.getGroups().contains(this.currentGroup)) {
            if (serverTicks < this.currentGroupTimeout) {
                return false;
            }

            // Check whether the group is still occupying this mutex zone
            // Do so by iterating all the rails (positiosn!) of that train
            for (TrackedRail rail : this.currentGroup.getRailTracker().getRailInformation()) {
                if (this.containsBlock(rail.minecartBlock)) {
                    this.currentGroupTimeout = serverTicks + 5;
                    return false;
                }
            }
        }

        // Occupy it.
        this.currentGroup = group;
        this.currentGroupTimeout = serverTicks + 5;
        return true;
    }

}
