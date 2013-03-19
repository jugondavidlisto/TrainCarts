package com.bergerkiller.bukkit.tc.controller.type;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.entity.CommonMinecartFurnace;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.Util;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.MemberCoalUsedEvent;
import com.bergerkiller.bukkit.tc.utils.PoweredCartSoundLoop;

public class MinecartMemberFurnace extends MinecartMember<CommonMinecartFurnace> {
	private BlockFace pushDirection;
	private int fuelCheckCounter = 0;

	@Override
	public void onAttached(CommonMinecartFurnace entity) {
		super.onAttached(entity);
		this.soundLoop = new PoweredCartSoundLoop(this);
		double pushX = entity.getPushX();
		double pushZ = entity.getPushZ();
		if (MathUtil.lengthSquared(pushX, pushZ) < 0.001) {
			this.pushDirection = BlockFace.SELF;
		} else {
			this.pushDirection = FaceUtil.getDirection(pushX, pushZ, true);
		}
	}

	@Override
	public boolean onInteractBy(HumanEntity human) {
		ItemStack itemstack = human.getItemInHand();
		if (itemstack != null && itemstack.getTypeId() == Material.COAL.getId()) {
			ItemUtil.subtractAmount(itemstack, 1);
			human.setItemInHand(itemstack);
			addFuelTicks(CommonMinecartFurnace.COAL_FUEL);
		}
		if (this.isOnVertical()) {
			this.pushDirection = Util.getVerticalFace((entity.getLocY() - EntityUtil.getLocY(human)) > 0.0);
		} else {
			BlockFace dir = FaceUtil.getRailsCartDirection(this.getRailDirection());
			if (MathUtil.isHeadingTo(dir, new Vector(entity.getLocX() - EntityUtil.getLocX(human), 0.0, entity.getLocZ() - EntityUtil.getLocZ(human)))) {
				this.pushDirection = dir;
			} else {
				this.pushDirection = dir.getOppositeFace();
			}
		}
		if (this.getGroup().isMoving()) {
			if (this.pushDirection == this.getDirection().getOppositeFace()) {
				this.getGroup().reverse();
				// Prevent push direction being inverted
				this.pushDirection = this.pushDirection.getOppositeFace();
			}
		}
		return true;
	}

	public void addFuelTicks(int fuelTicks) {
		int newFuelTicks = entity.getFuelTicks() + fuelTicks;
		if (newFuelTicks <= 0) {
			newFuelTicks = 0;
			this.pushDirection = BlockFace.SELF;
		} else if (this.pushDirection == BlockFace.SELF) {
			this.pushDirection = this.getDirection();
		}
		entity.setFuelTicks(newFuelTicks);
	}

	/**
	 * Checks if new coal can be used
	 * 
	 * @return True if new coal can be put into the powered minecart, False if not
	 */
	public boolean onCoalUsed() {
		MemberCoalUsedEvent event = MemberCoalUsedEvent.call(this);
		if (event.useCoal()) {
			return this.getCoalFromNeighbours();
		}
		return event.refill();
	}

	public boolean getCoalFromNeighbours() {
		for (MinecartMember<?> mm : this.getNeightbours()) {
			//Is it a storage minecart?
			if (mm instanceof MinecartMemberChest) {
				//has coal?
				Inventory inv = ((MinecartMemberChest) mm).getEntity().getInventory();
				for (int i = 0; i < inv.getSize(); i++) {
					ItemStack item = inv.getItem(i);
					if (LogicUtil.nullOrEmpty(item) || item.getTypeId() != Material.COAL.getId()) {
						continue;
					}
					ItemUtil.subtractAmount(item, 1);
					inv.setItem(i, item);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void reverse() {
		super.reverse();
		this.pushDirection = this.pushDirection.getOppositeFace();
	}

	@Override
	public void doPhysicsEndLogic() {
		super.doPhysicsEndLogic();
		// Fuel update routines
		if (entity.hasFuel()) {
			entity.addFuelTicks(-1);
			if (!entity.hasFuel()) {
				//TrainCarts - Actions to be done when empty
				if (this.onCoalUsed()) {
					this.addFuelTicks(CommonMinecartFurnace.COAL_FUEL); //Refill
				}
			}
		}
		// Put coal into cart if needed
		if (!entity.hasFuel()) {
			if (fuelCheckCounter++ % 20 == 0 && TrainCarts.useCoalFromStorageCart && this.getCoalFromNeighbours()) {
				this.addFuelTicks(CommonMinecartFurnace.COAL_FUEL);
			}
		} else {
			this.fuelCheckCounter = 0;
		}
		if (!entity.hasFuel()) {
			entity.setFuelTicks(0);
			this.pushDirection = BlockFace.SELF;
		}
		entity.setSmoking(entity.hasFuel());
	}

	@Override
	public void doPostMoveLogic() {
		super.doPostMoveLogic();
		// Update pushing direction
		if (this.pushDirection != BlockFace.SELF) {
			BlockFace dir = this.getDirection();
			if (this.isOnVertical()) {
				if (dir != this.pushDirection.getOppositeFace()) {
					this.pushDirection = dir;
				}
			} else {
				if (FaceUtil.isVertical(this.pushDirection) || FaceUtil.getFaceYawDifference(dir, this.pushDirection) <= 45) {
					this.pushDirection = dir;
				}
			}
		}

		// Velocity boost is applied
		if (!getGroup().isMovementControlled()) {
			if (this.pushDirection != BlockFace.SELF) {
				double boost = 0.04 + TrainCarts.poweredCartBoost;
				entity.multiplyVelocity(0.8);
				entity.addMotX(boost * -FaceUtil.cos(this.pushDirection));
				entity.addMotY((boost + 0.04) * this.pushDirection.getModY());
				entity.addMotZ(boost * -FaceUtil.sin(this.pushDirection));
			} else {
				if (this.getGroup().getProperties().isSlowingDown()) {
					entity.multiplyVelocity(0.9);
				}
			}
		}
	}
}