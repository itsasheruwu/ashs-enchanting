package com.example.ashsenchanting.listener;

import com.example.ashsenchanting.AshsEnchanting;
import com.example.ashsenchanting.config.PluginSettings;
import com.example.ashsenchanting.model.AnvilSessionState;
import com.example.ashsenchanting.util.EnchantCompatUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
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
        maybeLogBedrockCompatMiss(isBedrock, settings, left, right, patch.customCompatApplied());

        ItemStack finalResult = patch.result() == null ? null : patch.result().clone();
        event.setResult(finalResult);
        if (patch.customCompatApplied()) {
            // Geyser/Bedrock may fail to reflect PrepareAnvilEvent result-only updates for custom compat paths.
            inventory.setItem(2, finalResult == null ? null : finalResult.clone());
        }

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

        boolean customCompatNeedsTakeover = patch.customCompatApplied()
                && (finalResult != null && finalResult.getType() != Material.AIR);

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

    private void maybeLogBedrockCompatMiss(
            boolean isBedrock,
            PluginSettings settings,
            ItemStack left,
            ItemStack right,
            boolean customCompatApplied
    ) {
        if (!isBedrock || settings == null || !settings.useLogger() || customCompatApplied) {
            return;
        }
        if (left == null || right == null || left.getType() != Material.BOW) {
            return;
        }
        if (!left.containsEnchantment(Enchantment.MENDING)) {
            return;
        }
        if (right.getType() != Material.ENCHANTED_BOOK && right.getType() != Material.BOOK) {
            return;
        }

        String stored = "none";
        if (right.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            stored = storageMeta.getStoredEnchants().toString();
        }
        Object serializedMeta = right.serialize().get("meta");

        plugin.logInfo("Bedrock compat miss on bow+mending+book: rightType=" + right.getType()
                + ", directEnchants=" + right.getEnchantments()
                + ", storedEnchants=" + stored
                + ", itemMetaClass=" + (right.getItemMeta() == null ? "null" : right.getItemMeta().getClass().getName())
                + ", serializedMeta=" + String.valueOf(serializedMeta));
    }
}
