package com.example.ashsenchanting.listener;

import com.example.ashsenchanting.AshsEnchanting;
import com.example.ashsenchanting.config.PluginSettings;
import com.example.ashsenchanting.model.AnvilSessionState;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.view.AnvilView;

import java.util.concurrent.ThreadLocalRandom;

public final class AnvilClickListener implements Listener {
    private static final float VANILLA_ANVIL_BREAK_CHANCE = 0.12F;
    private static final String TECHNICAL_NOTE_URL =
            "https://github.com/itsasheruwu/ashs-enchanting/blob/main/docs/anvil-cost-behavior.md";

    private final AshsEnchanting plugin;

    public AnvilClickListener(AshsEnchanting plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilResultTake(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView() instanceof AnvilView anvilView)) {
            return;
        }

        if (event.getRawSlot() != 2) {
            return;
        }

        AnvilSessionState state = plugin.getSession(player);
        if (state == null || state.topInventory() != event.getView().getTopInventory()) {
            return;
        }

        boolean shouldTakeOver = state.tooExpensiveBypassNeeded() || state.customCompatApplied();
        if (!shouldTakeOver) {
            return;
        }

        event.setCancelled(true);
        boolean actionSupported = isSupportedResultTakeAction(event.getAction());
        boolean clickFallbackSupported = isSupportedFallbackClick(event.getClick());
        if (!actionSupported && !clickFallbackSupported) {
            logUnsupportedResultTakeAction(player, event);
            forceSync(player);
            return;
        }
        if (!actionSupported && clickFallbackSupported) {
            logFallbackTakeAction(player, event);
        }

        if (!plugin.beginProcessing(player)) {
            forceSync(player);
            return;
        }

        try {
            processManualTake(event, player, anvilView, state);
        } finally {
            plugin.endProcessing(player);
        }
    }

    private void processManualTake(InventoryClickEvent event, Player player, AnvilView view, AnvilSessionState state) {
        AnvilInventory anvil = view.getTopInventory();
        ItemStack result = copyIfPresent(anvil.getItem(2));

        if (isAir(result)
                && state.customCompatApplied()
                && inputsStillMatchState(anvil, state)) {
            result = copyIfPresent(state.preparedResult());
        }

        if (isAir(result)) {
            forceSync(player);
            return;
        }

        PluginSettings settings = plugin.getPluginSettings();
        int cost = resolveOperationCost(view, state);

        if (mustChargePlayer(player, settings) && player.getLevel() < cost) {
            forceSync(player);
            return;
        }

        boolean delivered = deliverResult(player, event, result);
        if (!delivered) {
            forceSync(player);
            return;
        }

        if (mustChargePlayer(player, settings) && cost > 0) {
            player.giveExpLevels(-cost);
        }

        consumeInputs(anvil, state);
        anvil.setItem(2, null);
        maybeDamageOrBreakAnvil(view);
        if (shouldSendPrivateCostMessage(state, settings)) {
            sendPrivateCostMessage(player, cost);
        }

        forceSync(player);
    }

    private boolean deliverResult(Player player, InventoryClickEvent event, ItemStack result) {
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            PlayerInventory playerInventory = player.getInventory();
            if (!canFit(playerInventory, result)) {
                return false;
            }
            playerInventory.addItem(result.clone());
            return true;
        }

        ItemStack cursor = event.getCursor();
        if (isAir(cursor)) {
            event.setCursor(result.clone());
            return true;
        }

        if (!cursor.isSimilar(result)) {
            return false;
        }

        int merged = cursor.getAmount() + result.getAmount();
        if (merged > cursor.getMaxStackSize()) {
            return false;
        }

        cursor.setAmount(merged);
        event.setCursor(cursor);
        return true;
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

    private boolean mustChargePlayer(Player player, PluginSettings settings) {
        return settings.chargeCreative() || player.getGameMode() != GameMode.CREATIVE;
    }

    private boolean isSupportedResultTakeAction(InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, MOVE_TO_OTHER_INVENTORY -> true;
            default -> false;
        };
    }

    private boolean isSupportedFallbackClick(ClickType clickType) {
        return switch (clickType) {
            case LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, UNKNOWN -> true;
            default -> false;
        };
    }

    private void logFallbackTakeAction(Player player, InventoryClickEvent event) {
        PluginSettings settings = plugin.getPluginSettings();
        if (settings == null || !settings.useLogger()) {
            return;
        }

        String clientType = plugin.isBedrockPlayer(player) ? "bedrock" : "java";
        plugin.logInfo("Fallback anvil result handling: player=" + player.getName()
                + " (" + player.getUniqueId() + ")"
                + ", client=" + clientType
                + ", click=" + event.getClick()
                + ", action=" + event.getAction()
                + ", rawSlot=" + event.getRawSlot());
    }

    private void logUnsupportedResultTakeAction(Player player, InventoryClickEvent event) {
        PluginSettings settings = plugin.getPluginSettings();
        if (settings == null || !settings.useLogger()) {
            return;
        }

        String clientType = plugin.isBedrockPlayer(player) ? "bedrock" : "java";
        plugin.logInfo("Blocked unsupported anvil result action: player=" + player.getName()
                + " (" + player.getUniqueId() + ")"
                + ", client=" + clientType
                + ", click=" + event.getClick()
                + ", action=" + event.getAction()
                + ", rawSlot=" + event.getRawSlot());
    }

    private void forceSync(Player player) {
        player.updateInventory();
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    private boolean shouldSendPrivateCostMessage(AnvilSessionState state, PluginSettings settings) {
        if (!state.tooExpensiveBypassNeeded()) {
            return false;
        }

        return settings.showTrueCostChatMessage().shouldSend(state.trueCostDisplayedInUi());
    }

    private void sendPrivateCostMessage(Player player, int trueCost) {
        TextComponent prefix = new TextComponent("[Ash's Enchanting] ");
        prefix.setColor(ChatColor.GOLD);

        TextComponent body = new TextComponent("True anvil cost: " + trueCost + " levels. ");
        body.setColor(ChatColor.YELLOW);

        TextComponent link = new TextComponent("Click here");
        link.setBold(true);
        link.setUnderlined(true);
        link.setColor(ChatColor.AQUA);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, TECHNICAL_NOTE_URL));
        link.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Open the technical cost note on GitHub.").color(ChatColor.GRAY).create()
        ));

        player.spigot().sendMessage(prefix, body, link);
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
