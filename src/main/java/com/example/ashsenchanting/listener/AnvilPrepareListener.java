package com.example.ashsenchanting.listener;

import com.example.ashsenchanting.AshsEnchanting;
import com.example.ashsenchanting.config.PluginSettings;
import com.example.ashsenchanting.model.AnvilSessionState;
import com.example.ashsenchanting.util.EnchantCompatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

public final class AnvilPrepareListener implements Listener {
    private static final int VANILLA_TOO_EXPENSIVE_THRESHOLD = 40;
    private static final int HIGHEST_DISPLAYABLE_COST = VANILLA_TOO_EXPENSIVE_THRESHOLD - 1;

    private final AshsEnchanting plugin;

    public AnvilPrepareListener(AshsEnchanting plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        PluginSettings settings = plugin.getPluginSettings();
        AnvilInventory inventory = event.getInventory();
        AnvilView view = event.getView();
        boolean isBedrock = plugin.isBedrockPlayer(player);

        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        ItemStack vanillaResult = event.getResult();

        int vanillaRepairCost = Math.max(0, view.getRepairCost());
        int vanillaRepairItemCountCost = Math.max(0, view.getRepairItemCountCost());
        int maximumRepairCost = Math.max(0, view.getMaximumRepairCost());

        EnchantCompatUtil.PatchResult infinityPatch = EnchantCompatUtil.patchInfinityMending(
                settings,
                left,
                right,
                vanillaResult,
                view
        );
        EnchantCompatUtil.PatchResult protectionPatch = EnchantCompatUtil.patchAllProtectionsOnArmor(
                settings,
                left,
                right,
                infinityPatch.result(),
                view
        );
        EnchantCompatUtil.PatchResult patch = EnchantCompatUtil.merge(infinityPatch, protectionPatch);

        ItemStack finalResult = patch.result() == null ? null : patch.result().clone();
        event.setResult(finalResult);

        int effectiveRepairCost = EnchantCompatUtil.resolveCompatRepairCost(
                settings,
                left,
                right,
                view,
                patch.customCompatApplied(),
                vanillaRepairCost
        );

        boolean tooExpensiveBypassNeeded = settings.disableTooExpensive()
                && finalResult != null
                && effectiveRepairCost >= VANILLA_TOO_EXPENSIVE_THRESHOLD;
        boolean trueCostDisplayModeActive = tooExpensiveBypassNeeded
                && plugin.canDisplayTrueCostAbove40InUi()
                && !isBedrock;
        boolean abilitySpoofNeeded = trueCostDisplayModeActive && finalResult != null;

        if (settings.disableTooExpensive()) {
            view.setMaximumRepairCost(Integer.MAX_VALUE);
            maximumRepairCost = Integer.MAX_VALUE;
        }

        if (finalResult != null && (patch.customCompatApplied() || settings.disableTooExpensive())) {
            int displayedRepairCost = trueCostDisplayModeActive
                    ? effectiveRepairCost
                    : (tooExpensiveBypassNeeded ? HIGHEST_DISPLAYABLE_COST : effectiveRepairCost);
            view.setRepairCost(displayedRepairCost);
        }

        plugin.updateAbilitySpoof(player, abilitySpoofNeeded);

        boolean customCompatNeedsTakeover = EnchantCompatUtil.shouldTakeOverForCompat(
                new EnchantCompatUtil.AnvilSessionDecision(
                        patch.customCompatApplied(),
                        vanillaResult == null,
                        vanillaRepairCost == 0
                )
        );
        if (isBedrock && patch.customCompatApplied()) {
            // Bedrock/Geyser clients may not surface custom compat results reliably in UI.
            customCompatNeedsTakeover = true;
        }

        int effectiveRightConsume = EnchantCompatUtil.resolveRepairItemConsumption(
                vanillaRepairItemCountCost,
                patch.customCompatApplied(),
                right
        );

        plugin.putSession(player, new AnvilSessionState(
                event.getView().getTopInventory(),
                left == null ? null : left.clone(),
                right == null ? null : right.clone(),
                finalResult == null ? null : finalResult.clone(),
                effectiveRepairCost,
                effectiveRightConsume,
                maximumRepairCost,
                tooExpensiveBypassNeeded,
                customCompatNeedsTakeover,
                trueCostDisplayModeActive,
                abilitySpoofNeeded
        ));
    }
}
