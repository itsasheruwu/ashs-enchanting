package com.example.ashsenchanting.bedrock;

import org.bukkit.entity.Player;

public final class NoopBedrockDetector implements BedrockPlayerDetector {
    private final String detectorName;

    public NoopBedrockDetector(String detectorName) {
        this.detectorName = detectorName;
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        return false;
    }

    @Override
    public String detectorName() {
        return detectorName;
    }
}
