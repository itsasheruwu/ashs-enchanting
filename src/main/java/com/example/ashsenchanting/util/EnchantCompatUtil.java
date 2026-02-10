package com.example.ashsenchanting.util;

import com.example.ashsenchanting.config.PluginSettings;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.view.AnvilView;

public final class EnchantCompatUtil {
    private EnchantCompatUtil() {
    }

    public static PatchResult patchInfinityMending(
            PluginSettings settings,
            ItemStack left,
            ItemStack right,
            ItemStack vanillaResult,
            AnvilView view
    ) {
        if (left == null || right == null) {
            return PatchResult.noChange(vanillaResult);
        }

        if (!isTargetItem(settings, left.getType())) {
            return PatchResult.noChange(vanillaResult);
        }

        if (!isValidMergeInput(left, right)) {
            return PatchResult.noChange(vanillaResult);
        }

        int sourceInfinity = getEnchantLevel(right, Enchantment.INFINITY);
        int sourceMending = getEnchantLevel(right, Enchantment.MENDING);

        if (sourceInfinity <= 0 && sourceMending <= 0) {
            return PatchResult.noChange(vanillaResult);
        }

        ItemStack workingResult = vanillaResult != null ? vanillaResult.clone() : left.clone();
        applyRenameIfPresent(workingResult, view.getRenameText());

        int beforeInfinity = workingResult.getEnchantmentLevel(Enchantment.INFINITY);
        int beforeMending = workingResult.getEnchantmentLevel(Enchantment.MENDING);

        boolean changed = false;

        if (sourceInfinity > 0) {
            int mergedInfinity = Math.max(beforeInfinity, sourceInfinity);
            if (mergedInfinity > beforeInfinity) {
                workingResult.addUnsafeEnchantment(Enchantment.INFINITY, mergedInfinity);
                changed = true;
            }
        }

        if (sourceMending > 0) {
            int mergedMending = Math.max(beforeMending, sourceMending);
            if (mergedMending > beforeMending) {
                workingResult.addUnsafeEnchantment(Enchantment.MENDING, mergedMending);
                changed = true;
            }
        }

        if (!changed) {
            return PatchResult.noChange(vanillaResult);
        }

        if (vanillaResult == null) {
            applyIncreasedRepairCost(workingResult, left, right);
        }

        return new PatchResult(
                workingResult,
                true,
                vanillaResult == null
                        || beforeInfinity != workingResult.getEnchantmentLevel(Enchantment.INFINITY)
                        || beforeMending != workingResult.getEnchantmentLevel(Enchantment.MENDING)
        );
    }

    public static boolean shouldTakeOverForCompat(AnvilSessionDecision decision) {
        return decision.customCompatApplied() && (decision.vanillaResultMissing() || decision.vanillaCostIsZero());
    }

    public static int resolveRepairItemConsumption(int vanillaCount, boolean customCompatApplied, ItemStack rightSlot) {
        if (vanillaCount > 0) {
            return vanillaCount;
        }
        if (customCompatApplied && rightSlot != null && rightSlot.getType() != Material.AIR) {
            return 1;
        }
        return 0;
    }

    private static boolean isTargetItem(PluginSettings settings, Material material) {
        if (material == Material.BOW) {
            return settings.allowInfinityMendingOnBows();
        }
        return material == Material.CROSSBOW && settings.alsoAllowOnCrossbows();
    }

    private static boolean isValidMergeInput(ItemStack left, ItemStack right) {
        return right.getType() == Material.ENCHANTED_BOOK || right.getType() == left.getType();
    }

    private static int getEnchantLevel(ItemStack item, Enchantment enchantment) {
        int itemLevel = item.getEnchantmentLevel(enchantment);
        if (item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            int storedLevel = storageMeta.getStoredEnchantLevel(enchantment);
            return Math.max(itemLevel, storedLevel);
        }
        return itemLevel;
    }

    private static void applyRenameIfPresent(ItemStack item, String renameText) {
        if (renameText == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (renameText.isBlank()) {
            meta.setDisplayName(null);
        } else {
            meta.setDisplayName(renameText);
        }

        item.setItemMeta(meta);
    }

    private static void applyIncreasedRepairCost(ItemStack result, ItemStack left, ItemStack right) {
        ItemMeta resultMeta = result.getItemMeta();
        if (!(resultMeta instanceof Repairable repairableMeta)) {
            return;
        }

        int leftRepairCost = getRepairCost(left);
        int rightRepairCost = getRepairCost(right);
        int base = Math.max(leftRepairCost, rightRepairCost);
        int increased = base * 2 + 1;

        repairableMeta.setRepairCost(increased);
        result.setItemMeta((ItemMeta) repairableMeta);
    }

    private static int getRepairCost(ItemStack item) {
        if (item == null) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Repairable repairable && repairable.hasRepairCost()) {
            return repairable.getRepairCost();
        }

        return 0;
    }

    public record PatchResult(ItemStack result, boolean customCompatApplied, boolean anyChange) {
        public static PatchResult noChange(ItemStack vanillaResult) {
            return new PatchResult(vanillaResult == null ? null : vanillaResult.clone(), false, false);
        }
    }

    public record AnvilSessionDecision(boolean customCompatApplied, boolean vanillaResultMissing, boolean vanillaCostIsZero) {
    }
}
