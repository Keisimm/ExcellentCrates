package su.nightexpress.excellentcrates.crates.item.command;

import java.util.function.Supplier;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.engine.action.ActionResult;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.command.CrateCommand;
import su.nightexpress.excellentcrates.api.crate.dispatcher.CrateFeedbackHandler;
import su.nightexpress.excellentcrates.api.crate.dispatcher.CrateMessageDispatcher;
import su.nightexpress.excellentcrates.core.SharedPlaceholders;
import su.nightexpress.excellentcrates.crates.item.CrateItemService;
import su.nightexpress.excellentcrates.crates.item.CratePackageService;
import su.nightexpress.excellentcrates.crates.item.lang.CrateItemLang;
import su.nightexpress.excellentcrates.crates.item.permission.CrateItemPerms;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.commands.tree.ExecutableNode;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;

@NullMarked
public class CrateItemGiveAllCommand implements CrateCommand, CrateFeedbackHandler {

    private static final String ARGUMENT_CRATE  = "crate";
    private static final String ARGUMENT_AMOUNT = "amount";

    private static final String FLAG_SILENT          = "s";
    private static final String FLAG_SILENT_FEEDBACK = "sf";

    private final CrateItemService                          itemService;
    private final @Nullable Supplier<@Nullable CratePackageService> packageServiceSupplier;
    private final CrateMessageDispatcher                    dispatcher;

    public CrateItemGiveAllCommand(CrateItemService itemService,
                                   @Nullable Supplier<@Nullable CratePackageService> packageServiceSupplier,
                                   CrateMessageDispatcher dispatcher) {
        this.itemService = itemService;
        this.packageServiceSupplier = packageServiceSupplier;
        this.dispatcher = dispatcher;
    }

    @Override
    public CrateMessageDispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public ExecutableNode createCommand() {
        return Commands.literal("giveall", builder -> builder
            .description(CrateItemLang.COMMAND_GIVE_ALL_DESCRIPTION)
            .permission(CrateItemPerms.COMMAND_GIVE_ALL)
            .withArguments(
                Arguments.argument(ARGUMENT_CRATE, Crate.class),
                Arguments.integer(ARGUMENT_AMOUNT, 1)
                    .localized(CoreLang.COMMAND_ARGUMENT_NAME_AMOUNT)
                    .suggestions((reader, context) -> Lists.newList("1", "5", "10"))
                    .optional()
            )
            .withFlags(FLAG_SILENT, FLAG_SILENT_FEEDBACK)
            .executes(this::run)
        );
    }

    private boolean run(CommandContext context, ParsedArguments arguments) {
        Crate crate = arguments.get(ARGUMENT_CRATE, Crate.class);
        int amount = Math.abs(arguments.getInt(ARGUMENT_AMOUNT, 1));
        CommandSender sender = context.getSender();

        CratePackageService packageService = this.packageServiceSupplier != null ? this.packageServiceSupplier.get() : null;

        int totalGiven = 0;
        for (Player player : Players.getOnline()) {
            if (!player.hasPermission(CrateItemPerms.COMMAND_GIVE_ALL_INCLUDED)) continue;

            if (packageService != null) {
                ActionResult result = packageService.givePackage(player, crate, amount);
                if (!result.success()) {
                    result.handleFeedback((locale, actionContext) -> {
                        this.dispatcher.sendBase(sender, crate, locale, ctx -> ctx
                            .apply(actionContext)
                            .with(CommonPlaceholders.PLAYER.resolver(player))
                        );
                    });
                    continue;
                }

                if (!context.hasFlag(FLAG_SILENT)) {
                    result.handleFeedback((locale, actionContext) -> {
                        this.dispatcher.sendBase(player, crate, CrateItemLang.COMMAND_GET_FEEDBACK, actionContext);
                    });
                }
            } else {
                ItemStack itemStack = this.itemService.createTaggedItem(crate);
                itemStack.setAmount(amount);
                Players.addItem(player, itemStack);

                if (!context.hasFlag(FLAG_SILENT)) {
                    this.dispatcher.sendBase(player, crate, CrateItemLang.COMMAND_GET_FEEDBACK, ctx -> ctx
                        .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                    );
                }
            }

            totalGiven += amount;
        }

        if (!context.hasFlag(FLAG_SILENT_FEEDBACK)) {
            int finalTotalGiven = totalGiven;
            this.dispatcher.sendBase(sender, crate, CrateItemLang.CRATE_GIVE_ALL_FEEDBACK, ctx -> ctx
                .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                .with(SharedPlaceholders.TOTAL, () -> NumberUtil.format(finalTotalGiven))
            );
        }

        return true;
    }
}
