package com.example.ashsenchanting.packet;

import org.bukkit.entity.Player;

public interface ClientAbilitySpoofer {
    boolean isSupported();

    void setInstantBuild(Player player, boolean instantBuild);
}
