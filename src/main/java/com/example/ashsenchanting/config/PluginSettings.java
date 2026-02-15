package com.example.ashsenchanting.config;

import org.bukkit.configuration.file.FileConfiguration;

public record PluginSettings(
        boolean disableTooExpensive,
        boolean allowInfinityMendingOnBows,
        boolean alsoAllowOnCrossbows,
        boolean allowAllProtectionsOnArmor,
        boolean chargeCreative,
        boolean useLogger,
        boolean bedrockCompatAutoApplyRequiresCommandConfirm,
        boolean bedrockCompatAutoApplyRequiresSneak,
        boolean showTrueCostAbove40InAnvilUi,
        TrueCostChatMessageMode showTrueCostChatMessage
) {
    public static PluginSettings fromConfig(FileConfiguration config) {
        return new PluginSettings(
                config.getBoolean("disableTooExpensive", true),
                config.getBoolean("allowInfinityMendingOnBows", true),
                config.getBoolean("alsoAllowOnCrossbows", false),
                config.getBoolean("allowAllProtectionsOnArmor", false),
                config.getBoolean("chargeCreative", false),
                config.getBoolean("useLogger", true),
                config.getBoolean("bedrockCompatAutoApplyRequiresCommandConfirm", true),
                config.getBoolean("bedrockCompatAutoApplyRequiresSneak", false),
                config.getBoolean("showTrueCostAbove40InAnvilUi", true),
                TrueCostChatMessageMode.fromConfig(config.getString("showTrueCostChatMessage", "fallback-only"))
        );
    }
}
