package com.bergerkiller.bukkit.tc;

import com.bergerkiller.bukkit.common.localization.LocalizationEnum;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;

import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Localization extends LocalizationEnum {
    public static final Localization COMMAND_ABOUT = new Localization("command.about", "TrainCarts %0% - See WIKI page for more information");
    public static final Localization COMMAND_NOPERM = new Localization("command.noperm", ChatColor.RED + "You do not have permission, ask an admin to do this for you.");
    public static final Localization EDIT_NOSELECT = new Localization("edit.noselect", ChatColor.YELLOW + "You haven't selected a train to edit yet!");
    public static final Localization EDIT_NOTALLOWED = new Localization("edit.notallowed", ChatColor.RED + "You are not allowed to own trains!");
    public static final Localization EDIT_NONEFOUND = new Localization("edit.nonefound", ChatColor.RED + "You do not own any trains you can edit.");
    public static final Localization EDIT_NOTOWNED = new Localization("edit.notowned", ChatColor.RED + "You do not own this train!");
    public static final Localization SELECT_DESTINATION = new Localization("select.destination", ChatColor.YELLOW + "You have selected " + ChatColor.WHITE + "%0%" + ChatColor.YELLOW + " as your destination!");
    public static final Localization TICKET_EXPIRED = new Localization("ticket.expired", ChatColor.RED + "Your ticket for %0% is expired");
    public static final Localization TICKET_REQUIRED = new Localization("ticket.required", ChatColor.RED + "You do not own a ticket for this train!");
    public static final Localization TICKET_USED = new Localization("ticket.used", ChatColor.GREEN + "You have used your " + ChatColor.YELLOW + "%0%" + ChatColor.GREEN + " ticket!");
    public static final Localization TICKET_CONFLICT = new Localization("ticket.conflict", ChatColor.RED + "You own multiple tickets that can be used for this train. Please hold the right ticket in your hand!");
    public static final Localization TICKET_CONFLICT_OWNER = new Localization("ticket.ownerConflict", ChatColor.RED + "The train ticket %0% is not yours, it belongs to %1%!");
    public static final Localization TICKET_CONFLICT_TYPE = new Localization("ticket.typeConflict", ChatColor.RED + "The train ticket %0% can not be used for this train!");
    public static final Localization WAITER_TARGET_NOT_FOUND = new Localization("waiter.notfound", ChatColor.RED + "Didn't find a " + ChatColor.YELLOW + "%0%" + ChatColor.RED + " sign on the track!");

    // Note: these aren't really used anymore :(
    public static final Localization TICKET_ADD = new Localization("ticket.add", ChatColor.WHITE + "[Ticket System]" + ChatColor.YELLOW + " You received %0% in your bank account!");
    public static final Localization TICKET_CHECK = new Localization("ticket.check", ChatColor.WHITE + "[Ticket System]" + ChatColor.YELLOW + " You currently have %0% in your bank account!");
    public static final Localization TICKET_BUYFAIL = new Localization("ticket.buyfail", ChatColor.WHITE + "[Ticket System]" + ChatColor.RED + " You can't afford a Ticket for %0%, sorry.");
    public static final Localization TICKET_BUY = new Localization("ticket.buy", ChatColor.WHITE + "[Ticket System]" + ChatColor.YELLOW + " You bought a Ticket for %0%.");

    // pathfinding
    public static final Localization PATHING_BUSY = new Localization("pathfinding.busy", ChatColor.YELLOW + "Looking for a way to reach the destination...");
    public static final Localization PATHING_FAILED = new Localization("pathfinding.failed", ChatColor.RED + "Destination " + ChatColor.YELLOW + "%0%" + ChatColor.RED + " could not be reached from here!");

    // train storing chest
    public static final Localization CHEST_NOPERM = new Localization("chest.noperm", ChatColor.RED + "You do not have permission to use the train storage chest!");
    public static final Localization CHEST_GIVE = new Localization("chest.give", ChatColor.GREEN + "You have been given a train storage chest item. Use it to store and spawn trains");
    public static final Localization CHEST_UPDATE = new Localization("chest.update", ChatColor.GREEN + "Your train storage chest item has been updated");
    public static final Localization CHEST_LOCKED = new Localization("chest.locked", ChatColor.RED + "Your train storage chest item is locked and can not pick up the train");
    public static final Localization CHEST_PICKUP = new Localization("chest.pickup", ChatColor.GREEN + "Train picked up and stored inside the item!");
    public static final Localization CHEST_SPAWN_SUCCESS = new Localization("chest.spawn.success", ChatColor.GREEN + "Train stored inside the item has been spawned on the rails!");
    public static final Localization CHEST_SPAWN_EMPTY = new Localization("chest.spawn.empty", ChatColor.RED + "Train can not be spawned, no train is stored in the item!");
    public static final Localization CHEST_SPAWN_NORAIL = new Localization("chest.spawn.norail", ChatColor.RED + "Train can not be spawned, clicked block is not a known rail!");
    public static final Localization CHEST_SPAWN_BLOCKED = new Localization("chest.spawn.blocked", ChatColor.RED + "Train can not be spawned, no space on rails!");

    private Localization(String name, String defValue) {
        super(name, defValue);
    }

    @Override
    public String get(String... arguments) {
        return TrainCarts.plugin.getLocale(this.getName(), arguments);
    }

    public void broadcast(MinecartGroup group, String... arguments) {
        HashSet<Player> receivers = new HashSet<>();
        for (MinecartMember<?> member : group) {
            // Editing
            receivers.addAll(member.getProperties().getEditingPlayers());
            // Occupants
            if (member.getEntity().hasPlayerPassenger()) {
                receivers.add(member.getEntity().getPlayerPassenger());
            }
        }
        for (Player player : receivers) {
            this.message(player, arguments);
        }
    }
}
