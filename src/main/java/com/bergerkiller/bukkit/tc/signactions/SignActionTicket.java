package com.bergerkiller.bukkit.tc.signactions;

import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.TCConfig;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
/*
import com.earth2me.essentials.register.payment.Method;
import com.earth2me.essentials.register.payment.Method.MethodAccount;
import com.earth2me.essentials.register.payment.Methods;
*/

/**
 * @author reeZZer
 *         Me (Bergerkiller) gives a big thank-you to him for writing the economics for this feature :)
 */
public class SignActionTicket extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return TCConfig.EssentialsEnabled && info.isType("ticket");
    }

    @Override
    public void execute(SignActionEvent info) {
        throw new RuntimeException("BROKEN");
        /*
        final boolean isTrain;
        if (info.isCartSign() && info.isAction(SignActionType.MEMBER_ENTER, SignActionType.REDSTONE_ON)) {
            isTrain = false;
        } else if (info.isTrainSign() && info.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON)) {
            isTrain = true;
        } else {
            return;
        }
        if ((info.hasMember() && info.isPowered())) {
            Method method = Methods.hasMethod() ? Methods.getMethod() : null;
            if (method != null) {
                double money;
                if (info.getLine(3).isEmpty()) {
                    money = 30.0;
                } else {
                    money = ParseUtil.parseDouble(info.getLine(3), 0.0);
                }
                final String mode = info.getLine(2);
                List<MinecartMember<?>> members;
                if (isTrain) {
                    members = info.getGroup();
                } else {
                    members = new ArrayList<>(1);
                    members.add(info.getMember());
                }
                for (MinecartMember<?> member : members) {
                    if (!member.getEntity().hasPlayerPassenger()) {
                        continue;
                    }
                    Player player = member.getEntity().getPlayerPassenger();
                    if (!method.hasAccount(player.getName())) {
                        continue;
                    }
                    //actually perform something with it here
                    MethodAccount account = method.getAccount(player.getName());

                    if (mode.equalsIgnoreCase("add")) {
                        account.add(money);
                        Localization.TICKET_ADD.message(player, getCurrencyText(money));
                    } else if (mode.equalsIgnoreCase("check")) {
                        Localization.TICKET_CHECK.message(player, getCurrencyText(account.balance()));
                    } else if (mode.equalsIgnoreCase("buy")) {
                        if (account.hasUnder(money)) {
                            Localization.TICKET_BUYFAIL.message(player, getCurrencyText(money));
                            member.eject();
                        } else {
                            account.subtract(money);
                            Localization.TICKET_BUY.message(player, getCurrencyText(money));
                        }
                    }
                }
            }
        }
        */
    }

    @Override
    public boolean build(SignChangeActionEvent event) {
        return handleBuild(event, Permission.BUILD_TICKET, "ticket system", "charges the passengers of a train");
    }
}