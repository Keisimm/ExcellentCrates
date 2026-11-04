package su.nightexpress.excellentcrates.crates.item;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.engine.action.ActionResult;
import su.nightexpress.engine.id.Identifier;
import su.nightexpress.engine.id.IdentifierParser;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.item.CrateItemAPI;
import su.nightexpress.excellentcrates.api.key.balance.KeyBalanceAPI;
import su.nightexpress.excellentcrates.crates.item.lang.CrateItemLang;
import su.nightexpress.excellentcrates.keys.lang.KeyLang;
import su.nightexpress.excellentcrates.util.InventoryUtils;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;

@NullMarked
public class CratePackageService {

    public static final String PACKAGE_PREFIX = "crate_package_";

    private final CratesPlugin  plugin;
    private final CrateItemAPI  itemAPI;
    private final KeyBalanceAPI balanceAPI;

    public CratePackageService(CratesPlugin plugin,
                               CrateItemAPI itemAPI,
                               KeyBalanceAPI balanceAPI) {
        this.plugin = plugin;
        this.itemAPI = itemAPI;
        this.balanceAPI = balanceAPI;
    }

    public static Identifier toPackageId(Crate crate) {
        return IdentifierParser.parse(PACKAGE_PREFIX + crate.idString())
            .orElseGet(() -> new Identifier(PACKAGE_PREFIX + crate.idString()));
    }

    public static @Nullable String extractCrateId(Identifier identifier) {
        if (identifier.value().startsWith(PACKAGE_PREFIX)) {
            return identifier.value().substring(PACKAGE_PREFIX.length());
        }
        return null;
    }

    public int countUnclaimedPackages(Player player, Crate crate) {
        return this.balanceAPI.countUnclaimedKeys(player, toPackageId(crate));
    }

    public CompletableFuture<ActionResult> givePackageAsync(UUID playerId, Crate crate, int amount) {
        Identifier packageId = toPackageId(crate);
        int absAmount = Math.abs(amount);

        ActionResult success = ActionResult.ok(CrateItemLang.COMMAND_GIVE_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
        );

        return CompletableFuture.supplyAsync(() -> {
            Player player = Players.getPlayer(playerId);
            if (player != null) {
                ItemStack itemStack = this.itemAPI.createTaggedItem(crate);
                int space = InventoryUtils.countInventorySpace(player, itemStack);

                if (space >= absAmount) {
                    itemStack.setAmount(absAmount);
                    Players.addItem(player, itemStack);
                    return CompletableFuture.completedFuture(success);
                }

                int toGive = Math.max(0, space);
                int toUnclaimed = absAmount - toGive;

                if (toGive > 0) {
                    itemStack.setAmount(toGive);
                    Players.addItem(player, itemStack);
                }

                return this.balanceAPI.addUnclaimedAsync(playerId, packageId, toUnclaimed)
                    .thenApply(res -> success);
            }

            return this.balanceAPI.addUnclaimedAsync(playerId, packageId, absAmount)
                .thenApply(res -> success);
        }, this.plugin::runTask).thenCompose(result -> result);
    }

    public ActionResult redeemPackage(Player player, Crate crate) {
        Identifier packageId = toPackageId(crate);
        int unclaimed = this.balanceAPI.countUnclaimedKeys(player, packageId);
        if (unclaimed <= 0) {
            return ActionResult.fail(KeyLang.KEY_REDEEM_NO_UNCLAIMED);
        }

        ItemStack itemStack = this.itemAPI.createTaggedItem(crate);
        int space = InventoryUtils.countInventorySpace(player, itemStack);
        if (space <= 0) {
            return ActionResult.fail(KeyLang.ERROR_INVENTORY_SPACE);
        }

        int toRedeem = Math.min(space, unclaimed);
        int taken = this.balanceAPI.takeUnclaimed(player, packageId, toRedeem);
        if (taken <= 0) {
            return ActionResult.fail(KeyLang.KEY_REDEEM_NO_UNCLAIMED);
        }

        itemStack.setAmount(taken);
        Players.addItem(player, itemStack);

        return ActionResult.ok(CrateItemLang.COMMAND_REDEEM_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(taken))
        );
    }
}
