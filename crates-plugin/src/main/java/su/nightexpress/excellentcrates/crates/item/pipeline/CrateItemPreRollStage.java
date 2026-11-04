package su.nightexpress.excellentcrates.crates.item.pipeline;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineChain;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineStage;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.CrateSourcePipelineComponent;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.PipelineComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;
import su.nightexpress.excellentcrates.crates.item.CrateItemService;
import su.nightexpress.nightcore.util.Players;

@NullMarked
public class CrateItemPreRollStage implements PipelineStage {

    private final CrateItemService itemService;

    public CrateItemPreRollStage(CrateItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public void intercept(Player player, Crate crate, PipelineContext context, PipelineChain chain) {
        CrateSourcePipelineComponent source = context.getComponentOrNull(PipelineComponentKeys.CRATE_SOURCE);
        if (source != null) {
            ItemStack itemStack = source.getItemStack();
            if (itemStack != null) {
                int count = Players.countItem(player, inv -> this.itemService.isCrateItem(inv, crate));
                context.getComponent(PipelineComponentKeys.BATCH).ifPresent(batchComponent -> {
                    batchComponent.limitMaxAllowed(count);
                });
            }
        }
        chain.proceed(player, context);
    }
}
