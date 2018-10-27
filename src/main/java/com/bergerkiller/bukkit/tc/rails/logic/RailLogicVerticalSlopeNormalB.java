package com.bergerkiller.bukkit.tc.rails.logic;

import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.tc.controller.components.RailPath;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * Handles the rail logic of a sloped rail with a vertical rail below it.<br>
 * <br>
 * <img src="./doc-files/sloped_vertical_rail_logics.png" />
 */
public class RailLogicVerticalSlopeNormalB extends RailLogicVerticalSlopeBase {
    private static final RailLogicVerticalSlopeNormalB[] values = new RailLogicVerticalSlopeNormalB[4];

    static {
        for (int i = 0; i < 4; i++) {
            values[i] = new RailLogicVerticalSlopeNormalB(FaceUtil.notchToFace(i << 1));
        }
    }

    private RailLogicVerticalSlopeNormalB(BlockFace direction) {
        super(direction, false);
    }

    /**
     * Gets the sloped-vertical rail logic for the the sloped track leading upwards facing the direction specified
     *
     * @param direction of the sloped rail
     * @return Rail Logic
     */
    public static RailLogicVerticalSlopeNormalB get(BlockFace direction) {
        return values[FaceUtil.faceToNotch(direction) >> 1];
    }

    @Override
    protected RailPath createPath() {
        // Initialize the rail path, making use of getFixedPosition for each node
        // This type of logic has a path consisting of two line segments
        // One segment is vertical, and leads to somewhere in the middle
        // The other segment is sloped from the middle to the other end
        // The x/z coordinates are asserted from the y-coordinate
        double dx = 0.5 + RailLogicVertical.XZ_POS_OFFSET * this.getDirection().getModX();
        double dz = 0.5 + RailLogicVertical.XZ_POS_OFFSET * this.getDirection().getModZ();
        Vector p1 = new Vector(dx, 0.0, dz);
        Vector p2 = new Vector(dx, 1.0, dz);
        Vector p3 = new Vector(dx, 1.0 + Y_POS_OFFSET, dz);

        if (this.alongZ) {
            p3.setZ(0.5 + 0.5 * (double) this.getDirection().getModZ());
        } else if (this.alongX) {
            p3.setX(0.5 + 0.5 * (double) this.getDirection().getModX());
        }

        return new RailPath.Builder()
                .add(p1, this.getDirection().getOppositeFace())
                .add(p2, this.getDirection().getOppositeFace())
                .add(p3, BlockFace.UP).build();
    }

}
