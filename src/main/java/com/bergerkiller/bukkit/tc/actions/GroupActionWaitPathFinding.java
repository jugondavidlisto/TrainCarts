package com.bergerkiller.bukkit.tc.actions;

import com.bergerkiller.bukkit.tc.Localization;
import com.bergerkiller.bukkit.tc.controller.components.RailJunction;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.pathfinding.PathConnection;
import com.bergerkiller.bukkit.tc.pathfinding.PathNode;
import com.bergerkiller.bukkit.tc.pathfinding.PathProvider;

public class GroupActionWaitPathFinding extends GroupActionWaitForever {
    private final SignActionEvent info;
    private final PathNode from;
    private final String destination;
    private final RailJunction fromJunction;
    private int failCounter = 0;

    public GroupActionWaitPathFinding(SignActionEvent info, PathNode from, String destination) {
        this.info = info;
        this.from = from;
        this.destination = destination;
        this.fromJunction = info.getEnterJunction();
    }

    @Override
    public boolean update() {
        if (PathProvider.isProcessing()) {
            if (this.failCounter++ == 20) {
                Localization.PATHING_BUSY.broadcast(this.getGroup());
            }
            return super.update();
        } else {
            // Switch the rails to the right direction
            PathConnection conn = this.from.findConnection(this.destination);
            if (conn != null) {
                this.info.setRailsFromTo(this.fromJunction, conn.junctionName);
            } else {
                Localization.PATHING_FAILED.broadcast(this.getGroup(), this.destination);
            }
            return true;
        }
    }
}
