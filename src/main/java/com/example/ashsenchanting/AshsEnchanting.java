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
import com.example.ashsenchanting.update.AutoUpdateService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.PluginManager;
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
    private final Set<UUID> bedrockAutoApplyHintedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bedrockAutoApplyConfirmedPlayers = ConcurrentHashMap.newKeySet();
    private ClientAbilitySpoofer abilitySpoofer = new NoopAbilitySpoofer();
    private BedrockPlayerDetector bedrockPlayerDetector = new NoopBedrockDetector("none");
    private AutoUpdateService autoUpdateService;
    private boolean protocolLibFallbackWarningLogged;
    private PluginSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeAbilitySpoofer();
        initializeBedrockDetector();
        reloadPluginSettings();
        warnIfTrueCostUiFallbackIsActive();
        initializeAutoUpdater();

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
        bedrockAutoApplyHintedPlayers.clear();
        bedrockAutoApplyConfirmedPlayers.clear();
    }

    public void reloadPluginSettings() {
        reloadConfig();
        settings = PluginSettings.fromConfig(getConfig());
        if (settings != null && !settings.showTrueCostAbove40InAnvilUi()) {
            clearAllAbilitySpoofs();
        } else {
            warnIfTrueCostUiFallbackIsActive();
        }

        if (autoUpdateService != null && settings != null) {
            autoUpdateService.checkAndDownloadUpdateAsync(settings);
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
        bedrockAutoApplyHintedPlayers.remove(player.getUniqueId());
        bedrockAutoApplyConfirmedPlayers.remove(player.getUniqueId());
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

    public boolean markBedrockAutoApplyHinted(Player player) {
        return bedrockAutoApplyHintedPlayers.add(player.getUniqueId());
    }

    public boolean confirmBedrockAutoApply(Player player) {
        return bedrockAutoApplyConfirmedPlayers.add(player.getUniqueId());
    }

    public boolean consumeBedrockAutoApplyConfirmation(Player player) {
        return bedrockAutoApplyConfirmedPlayers.remove(player.getUniqueId());
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"aeconfirm".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (!(player.getOpenInventory() instanceof AnvilView)) {
            player.sendMessage(ChatColor.RED + "[Ash's Enchanting] Open an anvil first.");
            return true;
        }

        AnvilSessionState state = getSession(player);
        if (state == null || !state.customCompatApplied()) {
            player.sendMessage(ChatColor.RED + "[Ash's Enchanting] No pending compat merge to confirm.");
            return true;
        }

        confirmBedrockAutoApply(player);
        player.sendMessage(ChatColor.GREEN + "[Ash's Enchanting] Merge confirmed. Re-tap an anvil input to apply.");
        triggerAnvilRecompute(player);
        return true;
    }

    private void triggerAnvilRecompute(Player player) {
        if (!(player.getOpenInventory() instanceof AnvilView view)) {
            return;
        }

        AnvilInventory anvil = view.getTopInventory();
        ItemStack right = anvil.getItem(1);
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!(player.getOpenInventory() instanceof AnvilView currentView) || currentView.getTopInventory() != anvil) {
                return;
            }
            anvil.setItem(1, right == null ? null : right.clone());
            player.updateInventory();
        });
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

        PluginManager pluginManager = getServer().getPluginManager();
        boolean geyserPresent = pluginManager.getPlugin("Geyser-Spigot") != null
                || pluginManager.getPlugin("Geyser") != null;
        boolean floodgatePresent = pluginManager.getPlugin("floodgate") != null;
        if ((geyserPresent || floodgatePresent) && "none".equals(bedrockPlayerDetector.detectorName())) {
            getLogger().warning(
                    "Detected Geyser/Floodgate plugin presence, but no compatible Bedrock API was found. "
                            + "Bedrock-specific safeguards may not activate."
            );
        }
    }

    private void initializeAutoUpdater() {
        autoUpdateService = new AutoUpdateService(this);
        if (settings != null) {
            autoUpdateService.checkAndDownloadUpdateAsync(settings);
        }
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
