package su.nightexpress.excellentcrates.keys.balance;

import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.engine.action.ActionResult;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.dispatcher.CrateMessageDispatcher;
import su.nightexpress.excellentcrates.api.crate.registry.CrateRegistry;
import su.nightexpress.excellentcrates.api.key.CrateKey;
import su.nightexpress.excellentcrates.api.key.dispatcher.KeyMessageDispatcher;
import su.nightexpress.excellentcrates.api.key.registry.KeyRegistry;
import su.nightexpress.excellentcrates.crates.item.CratePackageService;
import su.nightexpress.excellentcrates.keys.lang.KeyLang;

@NullMarked
public class UnifiedRedeemService {

    private final KeyRegistry                       keyRegistry;
    private final KeyBalanceService                 balanceService;
    private final KeyMessageDispatcher              keyDispatcher;
    private final @Nullable Supplier<CrateRegistry> crateRegistrySupplier;
    private final @Nullable Supplier<CratePackageService> packageServiceSupplier;
    private final @Nullable Supplier<CrateMessageDispatcher> crateDispatcherSupplier;

    public UnifiedRedeemService(KeyRegistry keyRegistry,
                                KeyBalanceService balanceService,
                                KeyMessageDispatcher keyDispatcher,
                                @Nullable Supplier<CrateRegistry> crateRegistrySupplier,
                                @Nullable Supplier<CratePackageService> packageServiceSupplier,
                                @Nullable Supplier<CrateMessageDispatcher> crateDispatcherSupplier) {
        this.keyRegistry = keyRegistry;
        this.balanceService = balanceService;
        this.keyDispatcher = keyDispatcher;
        this.crateRegistrySupplier = crateRegistrySupplier;
        this.packageServiceSupplier = packageServiceSupplier;
        this.crateDispatcherSupplier = crateDispatcherSupplier;
    }

    public boolean redeemAll(Player player) {
        return this.redeemAll(player, true);
    }

    public boolean redeemAll(Player player, boolean notifyIfEmpty) {
        boolean anyRedeemed = false;
        boolean hadUnclaimed = false;

        // 1. Redeem unclaimed keys
        for (CrateKey key : this.keyRegistry.values()) {
            if (this.balanceService.countUnclaimedKeys(player, key) > 0) {
                hadUnclaimed = true;
                ActionResult result = this.balanceService.redeemKey(player, key);
                if (result.success()) {
                    anyRedeemed = true;
                }
                result.handleFeedback((locale, actionContext) -> {
                    this.keyDispatcher.sendBase(player, key, locale, actionContext);
                });
            }
        }

        // 2. Redeem unclaimed crate packages
        CrateRegistry crateRegistry = this.crateRegistrySupplier != null ? this.crateRegistrySupplier.get() : null;
        CratePackageService packageService = this.packageServiceSupplier != null ? this.packageServiceSupplier.get() : null;
        CrateMessageDispatcher crateDispatcher = this.crateDispatcherSupplier != null ? this.crateDispatcherSupplier.get() : null;

        if (crateRegistry != null && packageService != null && crateDispatcher != null) {
            for (Crate crate : crateRegistry.values()) {
                if (packageService.countUnclaimedPackages(player, crate) > 0) {
                    hadUnclaimed = true;
                    ActionResult result = packageService.redeemPackage(player, crate);
                    if (result.success()) {
                        anyRedeemed = true;
                    }
                    result.handleFeedback((locale, actionContext) -> {
                        crateDispatcher.sendBase(player, crate, locale, actionContext);
                    });
                }
            }
        }

        if (!hadUnclaimed && notifyIfEmpty) {
            this.keyDispatcher.send(player, KeyLang.KEY_REDEEM_NO_KEYS);
            return false;
        }

        return anyRedeemed;
    }
}
