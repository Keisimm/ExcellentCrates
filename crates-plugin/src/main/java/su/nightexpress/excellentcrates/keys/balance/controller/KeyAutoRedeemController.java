package su.nightexpress.excellentcrates.keys.balance.controller;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.component.BaseController;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.keys.balance.UnifiedRedeemService;
import su.nightexpress.excellentcrates.keys.permission.KeyPerms;

@NullMarked
public class KeyAutoRedeemController extends BaseController {

    private final UnifiedRedeemService redeemService;

    public KeyAutoRedeemController(CratesPlugin plugin,
                                   UnifiedRedeemService redeemService) {
        super(plugin);
        this.redeemService = redeemService;
    }

    @Override
    protected void onControllerReload() {

    }

    @Override
    protected void onControllerShutdown() {

    }

    @Override
    protected void onControllerStart() {

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(KeyPerms.AUTO_REDEEM)) {
            this.redeemService.redeemAll(player);
        }
    }
}
