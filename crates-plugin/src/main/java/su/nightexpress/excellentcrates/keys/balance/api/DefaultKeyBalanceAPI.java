package su.nightexpress.excellentcrates.keys.balance.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.action.ActionResult;
import su.nightexpress.engine.id.Identifier;
import su.nightexpress.excellentcrates.api.key.CrateKey;
import su.nightexpress.excellentcrates.api.key.balance.IKeyBalance;
import su.nightexpress.excellentcrates.api.key.balance.KeyBalanceAPI;
import su.nightexpress.excellentcrates.keys.balance.KeyBalanceService;

import su.nightexpress.excellentcrates.keys.balance.UnifiedRedeemService;

@NullMarked
public class DefaultKeyBalanceAPI implements KeyBalanceAPI {

    private final KeyBalanceService    balanceService;
    private final UnifiedRedeemService redeemService;

    public DefaultKeyBalanceAPI(KeyBalanceService balanceService, UnifiedRedeemService redeemService) {
        this.balanceService = balanceService;
        this.redeemService = redeemService;
    }

    @Override
    public boolean hasKey(Player player, CrateKey key, int amount) {
        return balanceService.hasKey(player, key, amount);
    }

    @Override
    public int countKeys(Player player, CrateKey key) {
        return balanceService.countKeys(player, key);
    }

    @Override
    public int countPhysicalKeys(Player player, CrateKey key) {
        return balanceService.countPhysicalKeys(player, key);
    }

    @Override
    public int countPhysicalKeys(Player player, Identifier keyId) {
        return balanceService.countPhysicalKeys(player, keyId);
    }

    @Override
    public int countVirtualKeys(Player player, CrateKey key) {
        return balanceService.countVirtualKeys(player, key);
    }

    @Override
    public int countVirtualKeys(Player player, Identifier keyId) {
        return balanceService.countVirtualKeys(player, keyId);
    }

    @Override
    public int countUnclaimedKeys(Player player, CrateKey key) {
        return balanceService.countUnclaimedKeys(player, key);
    }

    @Override
    public int countUnclaimedKeys(Player player, Identifier keyId) {
        return balanceService.countUnclaimedKeys(player, keyId);
    }

    @Override
    public IKeyBalance getKeyBalance(Player player, CrateKey key) {
        return balanceService.getKeyBalance(player, key);
    }

    @Override
    public CompletableFuture<IKeyBalance> getKeyBalanceAsync(UUID playerId, CrateKey key) {
        return balanceService.getKeyBalanceAsync(playerId, key);
    }

    @Override
    public ActionResult redeemKey(Player player, CrateKey key) {
        return balanceService.redeemKey(player, key);
    }

    @Override
    public boolean redeemAll(Player player) {
        return this.redeemService.redeemAll(player);
    }

    @Override
    public ActionResult addKey(Player player, CrateKey key, int amount) {
        return balanceService.addKey(player, key, amount);
    }

    @Override
    public CompletableFuture<ActionResult> giveKeyAsync(UUID playerId, CrateKey key, int amount) {
        return balanceService.giveKeyAsync(playerId, key, amount);
    }

    @Override
    public ActionResult removeKey(Player player, CrateKey key, int amount) {
        return balanceService.removeKey(player, key, amount);
    }

    @Override
    public CompletableFuture<ActionResult> removeKeyAsync(UUID playerId, CrateKey key, int amount) {
        return balanceService.removeKeyAsync(playerId, key, amount);
    }

    @Override
    public CompletableFuture<ActionResult> addUnclaimedAsync(UUID playerId, Identifier id, int amount) {
        return balanceService.addUnclaimedAsync(playerId, id, amount);
    }

    @Override
    public int takeUnclaimed(Player player, Identifier id, int amount) {
        return balanceService.takeUnclaimed(player, id, amount);
    }
}
