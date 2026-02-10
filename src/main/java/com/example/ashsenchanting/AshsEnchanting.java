package com.example.ashsenchanting;

import com.example.ashsenchanting.config.PluginSettings;
import com.example.ashsenchanting.command.AshTestCommand;
import com.example.ashsenchanting.listener.AnvilClickListener;
import com.example.ashsenchanting.listener.AnvilPrepareListener;
import com.example.ashsenchanting.listener.AnvilSessionListener;
import com.example.ashsenchanting.model.AnvilSessionState;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AshsEnchanting extends JavaPlugin {
    private final Map<UUID, AnvilSessionState> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();
    private PluginSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginSettings();

        getServer().getPluginManager().registerEvents(new AnvilPrepareListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilClickListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilSessionListener(this), this);
        registerCommands();

        logInfo("Enabled Ash's Enchanting v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        sessions.clear();
        processingPlayers.clear();
    }

    public void reloadPluginSettings() {
        reloadConfig();
        settings = PluginSettings.fromConfig(getConfig());
    }

    public PluginSettings getPluginSettings() {
        return settings;
    }

    public void putSession(Player player, AnvilSessionState state) {
        sessions.put(player.getUniqueId(), state);
    }

    public AnvilSessionState getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void clearSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean beginProcessing(Player player) {
        return processingPlayers.add(player.getUniqueId());
    }

    public void endProcessing(Player player) {
        processingPlayers.remove(player.getUniqueId());
    }

    public boolean isProcessing(Player player) {
        return processingPlayers.contains(player.getUniqueId());
    }

    public void clearProcessing(Player player) {
        processingPlayers.remove(player.getUniqueId());
    }

    public void logInfo(String message) {
        if (settings != null && settings.useLogger()) {
            getLogger().info(message);
        }
    }

    private void registerCommands() {
        PluginCommand ashTestCommand = getCommand("ashtetest");
        if (ashTestCommand == null) {
            getLogger().warning("Command registration failed: /ashtetest not found in plugin.yml");
            return;
        }
        ashTestCommand.setExecutor(new AshTestCommand());
    }
}
