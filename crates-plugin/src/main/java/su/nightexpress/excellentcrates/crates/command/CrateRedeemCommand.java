package su.nightexpress.excellentcrates.crates.command;

import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.crate.command.CrateCommand;
import su.nightexpress.excellentcrates.core.crate.permission.CratePerms;
import su.nightexpress.excellentcrates.keys.lang.KeyLang;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.tree.ExecutableNode;

@NullMarked
public class CrateRedeemCommand implements CrateCommand {

    private final Consumer<Player> redeemAction;

    public CrateRedeemCommand(Consumer<Player> redeemAction) {
        this.redeemAction = redeemAction;
    }

    @Override
    public ExecutableNode createCommand() {
        return Commands.literal("redeem", builder -> builder
            .playerOnly()
            .description(KeyLang.COMMAND_REDEEM_DESCRIPTION)
            .permission(CratePerms.COMMAND_REDEEM)
            .executes((context, arguments) -> {
                this.redeemAction.accept(context.getPlayerOrThrow());
                return true;
            })
        );
    }
}
