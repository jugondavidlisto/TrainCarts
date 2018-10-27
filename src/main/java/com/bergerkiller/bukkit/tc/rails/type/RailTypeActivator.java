package com.bergerkiller.bukkit.tc.rails.type;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.material.Rails;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.tc.utils.PoweredTrackLogic;

public class RailTypeActivator extends RailTypeRegular {
    private final boolean isPowered;

    protected RailTypeActivator(boolean isPowered) {
        this.isPowered = isPowered;
    }

    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public void onBlockPlaced(Block railsBlock) {
        super.onBlockPlaced(railsBlock);

        // Also apply physics on the blocks adjacent for power to spread correctly
        Rails rails = BlockUtil.getRails(railsBlock);
        if (rails != null && isUpsideDown(railsBlock)) {
            BlockUtil.applyPhysics(railsBlock.getRelative(rails.getDirection()), Material.ACTIVATOR_RAIL);
            BlockUtil.applyPhysics(railsBlock.getRelative(rails.getDirection().getOppositeFace()), Material.ACTIVATOR_RAIL);
        }
    }

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
        super.onBlockPhysics(event);
        if (this.isUpsideDown(event.getBlock())) {
            PoweredTrackLogic logic = new PoweredTrackLogic(Material.ACTIVATOR_RAIL);
            logic.updateRedstone(event.getBlock());
        }
    }

    @Override
    public boolean isRail(BlockData blockData) {
        return blockData.isType(Material.ACTIVATOR_RAIL) && ((blockData.getRawData() & 0x8) == 0x8) == isPowered;
    }
}
