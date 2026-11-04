package su.nightexpress.excellentcrates.reward.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.engine.id.Identifier;
import su.nightexpress.engine.id.IdentifierParser;
import su.nightexpress.excellentcrates.api.cost.pipeline.CostPipelineComponent;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.batch.BatchPipelineComponent;
import su.nightexpress.excellentcrates.api.crate.component.CrateComponentKeys;
import su.nightexpress.excellentcrates.api.crate.item.CrateItemAPI;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineChain;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineStage;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.CrateRewardsPipelineComponent;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.CrateSourcePipelineComponent;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.PipelineComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;
import su.nightexpress.excellentcrates.api.crate.pipeline.reward.PipelineReward;
import su.nightexpress.excellentcrates.api.key.CrateKey;
import su.nightexpress.excellentcrates.api.key.KeysAPI;
import su.nightexpress.excellentcrates.api.key.crate.KeyRequirementComponent;
import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.api.reward.component.RewardComponentKeys;
import su.nightexpress.excellentcrates.api.reward.dispatcher.RewardMessageDispatcher;
import su.nightexpress.excellentcrates.api.reward.items.RewardItemsComponent;
import su.nightexpress.excellentcrates.reward.lang.RewardsLang;
import su.nightexpress.excellentcrates.util.InventoryUtils;
import su.nightexpress.nightcore.bridge.item.AdaptedItem;

@NullMarked
public class RewardInventoryValidationStage implements PipelineStage {

    private final RewardMessageDispatcher               dispatcher;
    private final CrateItemAPI                          crateItemAPI;
    private final @Nullable Supplier<@Nullable KeysAPI> keysApiSupplier;

    public RewardInventoryValidationStage(RewardMessageDispatcher dispatcher,
                                          CrateItemAPI crateItemAPI,
                                          @Nullable Supplier<@Nullable KeysAPI> keysApiSupplier) {
        this.dispatcher = dispatcher;
        this.crateItemAPI = crateItemAPI;
        this.keysApiSupplier = keysApiSupplier;
    }

    @Override
    public void intercept(Player player, Crate crate, PipelineContext context, PipelineChain chain) {
        CrateRewardsPipelineComponent rewardsComponent = context.getComponentOrNull(PipelineComponentKeys.REWARDS);
        if (rewardsComponent == null || rewardsComponent.getTargetRewards().isEmpty()) {
            chain.proceed(player, context);
            return;
        }

        List<ItemStack> items = new ArrayList<>();
        for (PipelineReward reward : rewardsComponent.getTargetRewards()) {
            Reward granted = reward.getGranted();
            RewardItemsComponent component = granted.getComponentOrNull(RewardComponentKeys.ITEMS);
            if (component == null || component.isEmpty()) continue;

            for (AdaptedItem provider : component.getItems()) {
                ItemStack is = provider.getItemStack();
                if (is != null && !is.getType().isAir()) {
                    items.add(is.clone());
                }
            }
        }

        if (items.isEmpty()) {
            chain.proceed(player, context);
            return;
        }

        ItemStack[] original = player.getInventory().getStorageContents();
        ItemStack[] storage = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                storage[i] = original[i].clone();
            }
        }

        // Simulate crate item consumption if opened from item
        CrateSourcePipelineComponent source = context.getComponentOrNull(PipelineComponentKeys.CRATE_SOURCE);
        if (source != null && source.getItemStack() != null) {
            BatchPipelineComponent batchComponent = context.getComponentOrNull(PipelineComponentKeys.BATCH);
            int amount = batchComponent != null ? batchComponent.getSelectedAmount() : 1;
            InventoryUtils.simulateRemoval(storage, itemStack -> this.crateItemAPI.isCrateItem(itemStack, crate), amount);
        }

        // Simulate physical key consumption if opened using keys
        CostPipelineComponent costComp = context.getComponentOrNull(PipelineComponentKeys.COST);
        if (costComp != null && this.keysApiSupplier != null) {
            KeysAPI keysAPI = this.keysApiSupplier.get();
            if (keysAPI != null) {
                Identifier keyId = IdentifierParser.parse(costComp.getCostOptionId()).orElse(null);
                if (keyId != null) {
                    CrateKey key = keysAPI.getRegistry().get(keyId);
                    if (key != null && !key.getBase().isVirtual()) {
                        KeyRequirementComponent keyReq = crate.getComponentOrNull(CrateComponentKeys.KEY_REQUIREMENT);
                        int requiredPerOpen = (keyReq != null && keyReq.getKeyEntry(key.getId()) != null)
                            ? keyReq.getKeyEntry(key.getId()).getAmount() : 1;
                        BatchPipelineComponent batchComponent = context.getComponentOrNull(PipelineComponentKeys.BATCH);
                        int batchAmount = batchComponent != null ? batchComponent.getSelectedAmount() : 1;
                        int totalKeys = requiredPerOpen * batchAmount;
                        InventoryUtils.simulateRemoval(storage, itemStack -> key.getId().equals(keysAPI.getItemAPI().getKeyIdFromItem(itemStack)), totalKeys);
                    }
                }
            }
        }

        if (!InventoryUtils.canFitItems(storage, player.getInventory().getMaxStackSize(), items)) {
            this.dispatcher.sendBase(player, crate, RewardsLang.GRANT_ERROR_INVENTORY_FULL);
            chain.abort();
            return;
        }

        chain.proceed(player, context);
    }
}
