package com.example.ashsenchanting.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.ArrayList;
import java.util.List;

public final class AshTestCommand implements CommandExecutor {
    private static final int TEST_BOW_REPAIR_COST = 120;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Only operators can use this command.");
            return true;
        }

        ItemStack testBow = buildTooExpensiveTestBow();
        player.getInventory().addItem(testBow);
        player.sendMessage(ChatColor.GOLD + "[Ash's Enchanting] " + ChatColor.YELLOW
                + "Given test bow with maxed enchants and high prior-work penalty for anvil testing.");
        return true;
    }

    private ItemStack buildTooExpensiveTestBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta == null) {
            return bow;
        }

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Ash Test Bow");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Includes all available enchantments.");
        lore.add(ChatColor.GRAY + "Prior work penalty is intentionally high.");
        lore.add(ChatColor.GRAY + "Use in anvil rename/combine to test bypass.");
        meta.setLore(lore);

        if (meta instanceof Repairable repairable) {
            repairable.setRepairCost(TEST_BOW_REPAIR_COST);
        }

        bow.setItemMeta(meta);

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            bow.addUnsafeEnchantment(enchantment, enchantment.getMaxLevel());
        }

        return bow;
    }
}
