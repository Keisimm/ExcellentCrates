package su.nightexpress.excellentcrates.crates.item.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import su.nightexpress.engine.action.ActionResult;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.command.CrateCommand;
import su.nightexpress.excellentcrates.api.crate.dispatcher.CrateFeedbackHandler;
import su.nightexpress.excellentcrates.api.crate.dispatcher.CrateMessageDispatcher;
import su.nightexpress.excellentcrates.core.lang.Lang;
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
import su.nightexpress.nightcore.userdata.UserData;
import su.nightexpress.nightcore.userdata.UserDataManager;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;

@NullMarked
public class CrateItemGiveCommand implements CrateCommand, CrateFeedbackHandler {

    private static final String ARGUMENT_PLAYER = "player";
    private static final String ARGUMENT_CRATE  = "crate";
    private static final String ARGUMENT_AMOUNT = "amount";

    private static final Logger LOGGER = LoggerFactory.getLogger(CrateItemGiveCommand.class);

    private final CrateItemService                          itemService;
    private final @Nullable Supplier<@Nullable CratePackageService> packageServiceSupplier;
    private final UserDataManager                           userDataService;
    private final CrateMessageDispatcher                    dispatcher;

    public CrateItemGiveCommand(CrateItemService itemService,
                                @Nullable Supplier<@Nullable CratePackageService> packageServiceSupplier,
                                UserDataManager userDataService,
                                CrateMessageDispatcher dispatcher) {
        this.itemService = itemService;
        this.packageServiceSupplier = packageServiceSupplier;
        this.userDataService = userDataService;
        this.dispatcher = dispatcher;
    }

    @Override
    public CrateMessageDispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public ExecutableNode createCommand() {
        return Commands.literal("give", builder -> builder
            .description(CrateItemLang.COMMAND_GIVE_DESCRIPTION)
            .permission(CrateItemPerms.COMMAND_GIVE)
            .withArguments(
                Arguments.playerName(ARGUMENT_PLAYER),
                Arguments.argument(ARGUMENT_CRATE, Crate.class),
                Arguments.integer(ARGUMENT_AMOUNT, 1)
                    .localized(CoreLang.COMMAND_ARGUMENT_NAME_AMOUNT)
                    .suggestions((reader, context) -> Lists.newList("1", "5", "10"))
                    .optional()
            )
            .executes(this::run)
        );
    }

    private boolean run(CommandContext context, ParsedArguments arguments) {
        CommandSender sender = context.getSender();

        String playerName = arguments.getString(ARGUMENT_PLAYER);
        Crate crate = arguments.get(ARGUMENT_CRATE, Crate.class);
        int amount = arguments.getInt(ARGUMENT_AMOUNT, 1);

        this.userDataService.loadByNameAndCacheAsync(playerName).thenCompose(opt -> {
            if (opt.isEmpty()) {
                this.dispatcher.send(sender, CoreLang.ERROR_INVALID_PLAYER);
                return CompletableFuture.completedFuture(null);
            }

            UserData userData = opt.get();
            CratePackageService packageService = this.packageServiceSupplier != null ? this.packageServiceSupplier.get() : null;

            if (packageService != null) {
                return packageService.givePackageAsync(userData.getId(), crate, amount).thenAccept(result -> {
                    result.handleFeedback((locale, actionContext) -> {
                        this.dispatcher.sendBase(sender, crate, locale, builder -> builder
                            .apply(actionContext)
                            .with(CommonPlaceholders.PLAYER_NAME, userData::getName)
                        );

                        if (result.success()) {
                            Players.findById(userData.getId()).ifPresent(target -> {
                                this.dispatcher.sendBase(target, crate, CrateItemLang.COMMAND_GET_FEEDBACK, actionContext);
                            });
                        }
                    });
                });
            }

            Player player = Players.findById(userData.getId()).orElse(null);
            if (player == null) {
                this.dispatcher.send(sender, CoreLang.ERROR_INVALID_PLAYER);
                return CompletableFuture.completedFuture(null);
            }

            ItemStack itemStack = this.itemService.createTaggedItem(crate);
            itemStack.setAmount(amount);
            Players.addItem(player, itemStack);

            this.handleFeedbackBase(sender, crate, ActionResult.ok(CrateItemLang.COMMAND_GIVE_FEEDBACK, ctx -> ctx
                .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                .with(CommonPlaceholders.PLAYER_NAME, userData::getName)
            ));

            return CompletableFuture.completedFuture(null);
        }).exceptionally(exception -> {
            LOGGER.error("An error occurred while giving crate package", exception);
            this.dispatcher.send(sender, Lang.GENERIC_INTERNAL_ERROR);
            return null;
        });

        return true;
    }
}
