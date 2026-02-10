package com.example.ashsenchanting.listener;

import com.example.ashsenchanting.AshsEnchanting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryType;

public final class AnvilSessionListener implements Listener {
    private final AshsEnchanting plugin;

    public AnvilSessionListener(AshsEnchanting plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        plugin.clearSession(player);
        plugin.clearProcessing(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.clearSession(player);
        plugin.clearProcessing(player);
    }
}
