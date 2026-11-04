package su.nightexpress.excellentcrates.keys.balance.command;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.key.command.KeyCommand;
import su.nightexpress.excellentcrates.keys.balance.UnifiedRedeemService;
import su.nightexpress.excellentcrates.keys.lang.KeyLang;
import su.nightexpress.excellentcrates.keys.permission.KeyPerms;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.tree.ExecutableNode;

@NullMarked
public class KeyRedeemCommand implements KeyCommand {

    private final UnifiedRedeemService redeemService;

    public KeyRedeemCommand(UnifiedRedeemService redeemService) {
        this.redeemService = redeemService;
    }

    @Override
    public ExecutableNode createCommand() {
        return Commands.literal("redeem", builder -> builder
            .playerOnly()
            .description(KeyLang.COMMAND_REDEEM_DESCRIPTION)
            .permission(KeyPerms.COMMAND_REDEEM)
            .executes((context, arguments) -> this.redeemService.redeemAll(context.getPlayerOrThrow()))
        );
    }
}
