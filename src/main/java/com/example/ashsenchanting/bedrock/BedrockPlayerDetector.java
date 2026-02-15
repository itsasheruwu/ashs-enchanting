package com.example.ashsenchanting.bedrock;

import org.bukkit.entity.Player;

public interface BedrockPlayerDetector {
    boolean isBedrockPlayer(Player player);

    String detectorName();
}
