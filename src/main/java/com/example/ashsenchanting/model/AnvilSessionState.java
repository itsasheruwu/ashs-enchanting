package com.example.ashsenchanting.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public record AnvilSessionState(
        Inventory topInventory,
        ItemStack leftInput,
        ItemStack rightInput,
        ItemStack preparedResult,
        int repairCost,
        int repairItemCountCost,
        int maximumRepairCost,
        boolean tooExpensiveBypassNeeded,
        boolean customCompatApplied
) {
}
