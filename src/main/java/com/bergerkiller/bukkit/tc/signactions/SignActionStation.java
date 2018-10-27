package com.bergerkiller.bukkit.tc.signactions;

import com.bergerkiller.bukkit.tc.Direction;
import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.Station;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import org.bukkit.block.BlockFace;

public class SignActionStation extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("station") && info.getMode() != SignActionMode.NONE;
    }

    @Override
    public void execute(SignActionEvent info) {
        if (!info.isAction(SignActionType.REDSTONE_CHANGE, SignActionType.GROUP_ENTER, SignActionType.GROUP_LEAVE)) {
            return;
        }
        if (info.isAction(SignActionType.GROUP_LEAVE)) {
            if (info.getGroup().getActions().isWaitAction()) {
                info.getGroup().getActions().clear();
            }
            info.setLevers(false);
            return;
        }
        if (!info.hasRails() || !info.hasGroup() || info.getGroup().isEmpty()) {
            return;
        }
        //Check if not already targeting
        MinecartGroup group = info.getGroup();
        Station station = new Station(info);
        if (!station.isValid()) {
            return;
        }

        //What do we do?
        if (station.getInstruction() == null) {
            // Clear actions, but only if requested to do so because of a redstone change
            if (info.isAction(SignActionType.REDSTONE_CHANGE)) {
                if (info.getGroup().getActions().isCurrentActionTag(station.getTag())) {
                    info.getGroup().getActions().clear();
                }
            }
        } else if (station.getInstruction() == BlockFace.SELF) {
            MinecartMember<?> centerMember = station.getCenterCart();
            // Do not allow redstone changes to center a launching train
            if (info.isAction(SignActionType.REDSTONE_CHANGE) && (centerMember.isMovementControlled() || info.getGroup().isMoving())) {
                return;
            }

            //Brake
            //TODO: ADD CHECK?!
            group.getActions().launchReset();
            BlockFace trainDirection = station.getNextDirectionFace();
            if (station.getNextDirection() != Direction.NONE) {
                // Actual launching here
                if (station.hasDelay()) {
                    station.centerTrain();
                    station.waitTrain(station.getDelay());
                } else if (!info.getMember().isDirectionTo(trainDirection)) {
                    // Order the train to center prior to launching again
                    station.centerTrain();
                }
                station.launchTo(trainDirection);
            } else {
                station.centerTrain();
                station.waitTrain(Long.MAX_VALUE);
            }
        } else {
            //Launch
            group.getActions().launchReset();

            if (station.hasDelay() || (group.head().isMoving() && !info.getMember().isDirectionTo(station.getInstruction()))) {
                //Reversing or has delay, need to center it in the middle first
                station.centerTrain();
            }
            if (station.hasDelay()) {
                station.waitTrain(station.getDelay());
            }
            station.launchTo(station.getInstruction());
        }
    }

    @Override
    public boolean build(SignChangeActionEvent event) {
        return handleBuild(event, Permission.BUILD_STATION, "station", "stop, wait and launch trains");
    }

    @Override
    public boolean overrideFacing() {
        return true;
    }
}
