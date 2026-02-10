package com.example.ashsenchanting.util;

import com.example.ashsenchanting.config.PluginSettings;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public static int resolveCompatRepairCost(
            ItemStack left,
            ItemStack right,
            AnvilView view,
            boolean customCompatApplied,
            int vanillaRepairCost
    ) {
        if (!customCompatApplied) {
            return Math.max(0, vanillaRepairCost);
        }

        if (vanillaRepairCost > 0) {
            return vanillaRepairCost;
        }

        int computed = calculateVanillaLikeCompatCost(left, right, view.getRenameText());
        if (computed > 0) {
            return computed;
        }

        return 1;
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

    private static int calculateVanillaLikeCompatCost(ItemStack left, ItemStack right, String renameText) {
        if (left == null || right == null || left.getType() == Material.AIR || right.getType() == Material.AIR) {
            return 0;
        }

        boolean rightIsBook = right.getType() == Material.ENCHANTED_BOOK;
        int cost = getRepairCost(left) + getRepairCost(right);
        int operationCost = 0;

        if (!rightIsBook
                && left.getType() == right.getType()
                && left.getType().getMaxDurability() > 0
                && isDamaged(left)) {
            operationCost += 2;
        }

        Map<Enchantment, Integer> merged = new HashMap<>(left.getEnchantments());
        Map<Enchantment, Integer> rightEnchants = getAllEnchantLevels(right);
        boolean foundCompatible = false;

        for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int offeredLevel = Math.max(0, entry.getValue());
            if (offeredLevel <= 0) {
                continue;
            }

            int currentLevel = merged.getOrDefault(enchantment, 0);
            int mergedLevel = currentLevel == offeredLevel
                    ? offeredLevel + 1
                    : Math.max(currentLevel, offeredLevel);
            mergedLevel = Math.min(mergedLevel, enchantment.getMaxLevel());

            if (!canApplyCompatAware(enchantment, left, merged.keySet(), rightIsBook)) {
                operationCost += countIncompatible(enchantment, merged.keySet());
                continue;
            }

            foundCompatible = true;
            if (mergedLevel > currentLevel) {
                merged.put(enchantment, mergedLevel);
            }

            int enchantCost = getEnchantmentWeight(enchantment);
            if (rightIsBook) {
                enchantCost = Math.max(1, enchantCost / 2);
            }
            operationCost += enchantCost * mergedLevel;

            if (left.getAmount() > 1) {
                operationCost = 40;
                break;
            }
        }

        operationCost += calculateRenameCost(left, renameText);

        if (operationCost <= 0 && !foundCompatible) {
            return 0;
        }

        long total = (long) cost + operationCost;
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    private static Map<Enchantment, Integer> getAllEnchantLevels(ItemStack item) {
        Map<Enchantment, Integer> levels = new HashMap<>(item.getEnchantments());
        if (item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                levels.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }
        return levels;
    }

    private static boolean canApplyCompatAware(
            Enchantment candidate,
            ItemStack target,
            Set<Enchantment> currentEnchants,
            boolean rightIsBook
    ) {
        if (!rightIsBook && !candidate.canEnchantItem(target)) {
            return false;
        }

        for (Enchantment existing : currentEnchants) {
            if (existing.equals(candidate)) {
                continue;
            }
            if (!candidate.conflictsWith(existing)) {
                continue;
            }
            if (isInfinityMendingPair(candidate, existing)) {
                continue;
            }
            return false;
        }

        return true;
    }

    private static int countIncompatible(Enchantment candidate, Set<Enchantment> currentEnchants) {
        int count = 0;
        for (Enchantment existing : currentEnchants) {
            if (existing.equals(candidate)) {
                continue;
            }
            if (!candidate.conflictsWith(existing)) {
                continue;
            }
            if (isInfinityMendingPair(candidate, existing)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static boolean isInfinityMendingPair(Enchantment first, Enchantment second) {
        return (first.equals(Enchantment.INFINITY) && second.equals(Enchantment.MENDING))
                || (first.equals(Enchantment.MENDING) && second.equals(Enchantment.INFINITY));
    }

    private static int calculateRenameCost(ItemStack left, String renameText) {
        if (renameText == null) {
            return 0;
        }

        String existingName = null;
        ItemMeta leftMeta = left.getItemMeta();
        if (leftMeta != null && leftMeta.hasDisplayName()) {
            existingName = leftMeta.getDisplayName();
        }

        if (renameText.isBlank()) {
            return existingName == null ? 0 : 1;
        }

        return renameText.equals(existingName) ? 0 : 1;
    }

    private static boolean isDamaged(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            return damageable.getDamage() > 0;
        }
        return false;
    }

    private static int getEnchantmentWeight(Enchantment enchantment) {
        if (enchantment.equals(Enchantment.PROTECTION)
                || enchantment.equals(Enchantment.SHARPNESS)
                || enchantment.equals(Enchantment.EFFICIENCY)
                || enchantment.equals(Enchantment.POWER)
                || enchantment.equals(Enchantment.LOYALTY)
                || enchantment.equals(Enchantment.PIERCING)
                || enchantment.equals(Enchantment.DENSITY)) {
            return 1;
        }

        if (enchantment.equals(Enchantment.FIRE_PROTECTION)
                || enchantment.equals(Enchantment.FEATHER_FALLING)
                || enchantment.equals(Enchantment.BLAST_PROTECTION)
                || enchantment.equals(Enchantment.PROJECTILE_PROTECTION)
                || enchantment.equals(Enchantment.SMITE)
                || enchantment.equals(Enchantment.BANE_OF_ARTHROPODS)
                || enchantment.equals(Enchantment.KNOCKBACK)
                || enchantment.equals(Enchantment.UNBREAKING)
                || enchantment.equals(Enchantment.QUICK_CHARGE)
                || enchantment.equals(Enchantment.FEATHER_FALLING)) {
            return 2;
        }

        if (enchantment.equals(Enchantment.THORNS)
                || enchantment.equals(Enchantment.SILK_TOUCH)
                || enchantment.equals(Enchantment.INFINITY)
                || enchantment.equals(Enchantment.BINDING_CURSE)
                || enchantment.equals(Enchantment.VANISHING_CURSE)
                || enchantment.equals(Enchantment.CHANNELING)
                || enchantment.equals(Enchantment.SOUL_SPEED)
                || enchantment.equals(Enchantment.SWIFT_SNEAK)
                || enchantment.equals(Enchantment.WIND_BURST)) {
            return 8;
        }

        if (enchantment.equals(Enchantment.BLAST_PROTECTION)
                || enchantment.equals(Enchantment.RESPIRATION)
                || enchantment.equals(Enchantment.DEPTH_STRIDER)
                || enchantment.equals(Enchantment.AQUA_AFFINITY)
                || enchantment.equals(Enchantment.FROST_WALKER)
                || enchantment.equals(Enchantment.FIRE_ASPECT)
                || enchantment.equals(Enchantment.LOOTING)
                || enchantment.equals(Enchantment.SWEEPING_EDGE)
                || enchantment.equals(Enchantment.FORTUNE)
                || enchantment.equals(Enchantment.PUNCH)
                || enchantment.equals(Enchantment.FLAME)
                || enchantment.equals(Enchantment.LUCK_OF_THE_SEA)
                || enchantment.equals(Enchantment.LURE)
                || enchantment.equals(Enchantment.MULTISHOT)
                || enchantment.equals(Enchantment.MENDING)
                || enchantment.equals(Enchantment.IMPALING)
                || enchantment.equals(Enchantment.RIPTIDE)
                || enchantment.equals(Enchantment.BREACH)) {
            return 4;
        }

        return 1;
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
