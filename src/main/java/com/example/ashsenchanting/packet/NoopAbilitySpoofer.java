package com.example.ashsenchanting.packet;

import org.bukkit.entity.Player;

public final class NoopAbilitySpoofer implements ClientAbilitySpoofer {
    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void setInstantBuild(Player player, boolean instantBuild) {
        // Intentionally no-op when ProtocolLib is unavailable.
    }
}
