package com.example.ashsenchanting.listener;

import com.example.ashsenchanting.AshsEnchanting;
import com.example.ashsenchanting.config.PluginSettings;
import com.example.ashsenchanting.model.AnvilSessionState;
import com.example.ashsenchanting.util.EnchantCompatUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.concurrent.ThreadLocalRandom;

public final class AnvilPrepareListener implements Listener {
    private static final int VANILLA_TOO_EXPENSIVE_THRESHOLD = 40;
    private static final int HIGHEST_DISPLAYABLE_COST = VANILLA_TOO_EXPENSIVE_THRESHOLD - 1;
    private static final float VANILLA_ANVIL_BREAK_CHANCE = 0.12F;

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

        if (isBedrock && patch.customCompatApplied() && finalResult != null && finalResult.getType() != Material.AIR) {
            scheduleBedrockCompatAutoApply(player);
        }
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

    private void scheduleBedrockCompatAutoApply(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> tryBedrockCompatAutoApply(player));
    }

    private void tryBedrockCompatAutoApply(Player player) {
        if (!player.isOnline() || !plugin.isBedrockPlayer(player)) {
            return;
        }

        if (!(player.getOpenInventory() instanceof AnvilView view)) {
            return;
        }

        AnvilSessionState state = plugin.getSession(player);
        if (state == null || state.topInventory() != view.getTopInventory() || !state.customCompatApplied()) {
            return;
        }

        PluginSettings settings = plugin.getPluginSettings();
        if (settings == null) {
            return;
        }

        AnvilInventory anvil = view.getTopInventory();
        if (!inputsStillMatchState(anvil, state)) {
            return;
        }

        ItemStack result = copyIfPresent(anvil.getItem(2));
        if (isAir(result)) {
            result = copyIfPresent(state.preparedResult());
        }
        if (isAir(result)) {
            return;
        }

        if (!plugin.beginProcessing(player)) {
            return;
        }

        try {
            applyBedrockCompatMerge(player, view, anvil, state, result);
        } finally {
            plugin.endProcessing(player);
        }
    }

    private void applyBedrockCompatMerge(
            Player player,
            AnvilView view,
            AnvilInventory anvil,
            AnvilSessionState state,
            ItemStack result
    ) {
        PluginSettings settings = plugin.getPluginSettings();
        int cost = resolveOperationCost(view, state);

        if (mustChargePlayer(player, settings) && player.getLevel() < cost) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        if (!canFit(inventory, result)) {
            forceSync(player);
            return;
        }

        inventory.addItem(result.clone());
        if (mustChargePlayer(player, settings) && cost > 0) {
            player.giveExpLevels(-cost);
        }

        consumeInputs(anvil, state);
        anvil.setItem(2, null);
        maybeDamageOrBreakAnvil(view);
        plugin.logInfo("Bedrock prepare auto-apply: player=" + player.getName()
                + " (" + player.getUniqueId() + "), result=" + result.getType()
                + ", amount=" + result.getAmount()
                + ", cost=" + cost);
        forceSync(player);
    }

    private int resolveOperationCost(AnvilView view, AnvilSessionState state) {
        if (state.tooExpensiveBypassNeeded()) {
            return Math.max(0, state.repairCost());
        }

        int liveCost = Math.max(0, view.getRepairCost());
        if (liveCost > 0) {
            return liveCost;
        }

        int cachedCost = Math.max(0, state.repairCost());
        if (cachedCost > 0) {
            return cachedCost;
        }

        if (state.customCompatApplied()) {
            return 1;
        }

        return 0;
    }

    private boolean mustChargePlayer(Player player, PluginSettings settings) {
        return settings.chargeCreative() || player.getGameMode() != GameMode.CREATIVE;
    }

    private boolean canFit(PlayerInventory inventory, ItemStack stack) {
        int remaining = stack.getAmount();

        for (ItemStack slot : inventory.getStorageContents()) {
            if (remaining <= 0) {
                return true;
            }

            if (slot == null || slot.getType() == Material.AIR) {
                remaining -= stack.getMaxStackSize();
                continue;
            }

            if (!slot.isSimilar(stack)) {
                continue;
            }

            remaining -= Math.max(0, slot.getMaxStackSize() - slot.getAmount());
        }

        return remaining <= 0;
    }

    private void consumeInputs(AnvilInventory anvil, AnvilSessionState state) {
        anvil.setItem(0, null);

        ItemStack right = anvil.getItem(1);
        if (isAir(right)) {
            return;
        }

        int consume = Math.max(0, state.repairItemCountCost());
        if (consume <= 0) {
            consume = 1;
        }

        if (right.getAmount() <= consume) {
            anvil.setItem(1, null);
            return;
        }

        right.setAmount(right.getAmount() - consume);
        anvil.setItem(1, right);
    }

    private boolean inputsStillMatchState(AnvilInventory anvil, AnvilSessionState state) {
        return sameItem(anvil.getItem(0), state.leftInput()) && sameItem(anvil.getItem(1), state.rightInput());
    }

    private boolean sameItem(ItemStack current, ItemStack expected) {
        if (isAir(current) && isAir(expected)) {
            return true;
        }
        if (isAir(current) || isAir(expected)) {
            return false;
        }
        return current.isSimilar(expected) && current.getAmount() == expected.getAmount();
    }

    private void maybeDamageOrBreakAnvil(AnvilView view) {
        Inventory top = view.getTopInventory();
        Location location = top.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        if (ThreadLocalRandom.current().nextFloat() >= VANILLA_ANVIL_BREAK_CHANCE) {
            return;
        }

        Block block = location.getBlock();
        Material current = block.getType();
        if (current != Material.ANVIL && current != Material.CHIPPED_ANVIL && current != Material.DAMAGED_ANVIL) {
            return;
        }

        Material next;
        Sound sound;
        if (current == Material.ANVIL) {
            next = Material.CHIPPED_ANVIL;
            sound = Sound.BLOCK_ANVIL_HIT;
        } else if (current == Material.CHIPPED_ANVIL) {
            next = Material.DAMAGED_ANVIL;
            sound = Sound.BLOCK_ANVIL_HIT;
        } else {
            next = Material.AIR;
            sound = Sound.BLOCK_ANVIL_DESTROY;
        }

        if (next == Material.AIR) {
            block.setType(Material.AIR, true);
        } else {
            BlockData oldData = block.getBlockData();
            BlockData nextData = Bukkit.createBlockData(next);
            if (oldData instanceof Directional oldDirectional && nextData instanceof Directional nextDirectional) {
                nextDirectional.setFacing(oldDirectional.getFacing());
            }
            block.setBlockData(nextData, true);
        }

        location.getWorld().playSound(location, sound, 1.0F, 1.0F);
    }

    private void forceSync(Player player) {
        player.updateInventory();
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    private static ItemStack copyIfPresent(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        return stack.clone();
    }

    private static boolean isAir(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR;
    }
}
