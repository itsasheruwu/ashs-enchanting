package com.example.ashsenchanting;

import com.example.ashsenchanting.bedrock.BedrockDetectorFactory;
import com.example.ashsenchanting.bedrock.BedrockPlayerDetector;
import com.example.ashsenchanting.bedrock.NoopBedrockDetector;
import com.example.ashsenchanting.config.PluginSettings;
import com.example.ashsenchanting.listener.AnvilClickListener;
import com.example.ashsenchanting.listener.AnvilPrepareListener;
import com.example.ashsenchanting.listener.AnvilSessionListener;
import com.example.ashsenchanting.model.AnvilSessionState;
import com.example.ashsenchanting.packet.ClientAbilitySpoofer;
import com.example.ashsenchanting.packet.NoopAbilitySpoofer;
import com.example.ashsenchanting.packet.ProtocolLibAbilitySpoofer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AshsEnchanting extends JavaPlugin {
    private final Map<UUID, AnvilSessionState> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spoofedPlayers = ConcurrentHashMap.newKeySet();
    private ClientAbilitySpoofer abilitySpoofer = new NoopAbilitySpoofer();
    private BedrockPlayerDetector bedrockPlayerDetector = new NoopBedrockDetector("none");
    private boolean protocolLibFallbackWarningLogged;
    private PluginSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeAbilitySpoofer();
        initializeBedrockDetector();
        reloadPluginSettings();
        warnIfTrueCostUiFallbackIsActive();

        getServer().getPluginManager().registerEvents(new AnvilPrepareListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilClickListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilSessionListener(this), this);

        logInfo("Enabled Ash's Enchanting v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        clearAllAbilitySpoofs();
        sessions.clear();
        processingPlayers.clear();
        spoofedPlayers.clear();
    }

    public void reloadPluginSettings() {
        reloadConfig();
        settings = PluginSettings.fromConfig(getConfig());
        if (settings != null && !settings.showTrueCostAbove40InAnvilUi()) {
            clearAllAbilitySpoofs();
        } else {
            warnIfTrueCostUiFallbackIsActive();
        }
    }

    public PluginSettings getPluginSettings() {
        return settings;
    }

    public ClientAbilitySpoofer getAbilitySpoofer() {
        return abilitySpoofer;
    }

    public boolean isBedrockPlayer(Player player) {
        return bedrockPlayerDetector.isBedrockPlayer(player);
    }

    public boolean canDisplayTrueCostAbove40InUi() {
        return settings != null
                && settings.showTrueCostAbove40InAnvilUi()
                && abilitySpoofer.isSupported();
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

    public boolean isAbilitySpoofActive(Player player) {
        return spoofedPlayers.contains(player.getUniqueId());
    }

    public void updateAbilitySpoof(Player player, boolean shouldSpoof) {
        UUID playerId = player.getUniqueId();
        boolean currentlySpoofed = spoofedPlayers.contains(playerId);

        if (shouldSpoof == currentlySpoofed) {
            return;
        }

        if (!abilitySpoofer.isSupported()) {
            spoofedPlayers.remove(playerId);
            return;
        }

        if (shouldSpoof) {
            abilitySpoofer.setInstantBuild(player, true);
            spoofedPlayers.add(playerId);
            return;
        }

        abilitySpoofer.setInstantBuild(player, player.getGameMode() == GameMode.CREATIVE);
        spoofedPlayers.remove(playerId);
    }

    public void clearAbilitySpoof(Player player) {
        updateAbilitySpoof(player, false);
    }

    public void clearAllAbilitySpoofs() {
        for (UUID playerId : new HashSet<>(spoofedPlayers)) {
            Player player = getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                clearAbilitySpoof(player);
            } else {
                spoofedPlayers.remove(playerId);
            }
        }
    }

    public void logInfo(String message) {
        if (settings != null && settings.useLogger()) {
            getLogger().info(message);
        }
    }

    private void initializeAbilitySpoofer() {
        abilitySpoofer = new NoopAbilitySpoofer();

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            return;
        }

        try {
            abilitySpoofer = new ProtocolLibAbilitySpoofer();
        } catch (Throwable throwable) {
            abilitySpoofer = new NoopAbilitySpoofer();
            getLogger().warning("ProtocolLib is present but ability packet init failed; falling back to 39-cost UI mode.");
        }
    }

    private void initializeBedrockDetector() {
        bedrockPlayerDetector = BedrockDetectorFactory.create();
        getLogger().info("Bedrock detector initialized: " + bedrockPlayerDetector.detectorName());
    }

    private void warnIfTrueCostUiFallbackIsActive() {
        if (protocolLibFallbackWarningLogged || settings == null) {
            return;
        }
        if (!settings.showTrueCostAbove40InAnvilUi() || abilitySpoofer.isSupported()) {
            return;
        }

        getLogger().warning(
                "showTrueCostAbove40InAnvilUi is enabled, but ProtocolLib is unavailable. "
                        + "Falling back to 39 display with true-cost private chat message."
        );
        protocolLibFallbackWarningLogged = true;
    }
}
