package com.example.ashsenchanting.config;

import org.bukkit.configuration.file.FileConfiguration;

public record PluginSettings(
        boolean disableTooExpensive,
        boolean allowInfinityMendingOnBows,
        boolean alsoAllowOnCrossbows,
        boolean chargeCreative,
        boolean useLogger
) {
    public static PluginSettings fromConfig(FileConfiguration config) {
        return new PluginSettings(
                config.getBoolean("disableTooExpensive", true),
                config.getBoolean("allowInfinityMendingOnBows", true),
                config.getBoolean("alsoAllowOnCrossbows", false),
                config.getBoolean("chargeCreative", false),
                config.getBoolean("useLogger", true)
        );
    }
}
