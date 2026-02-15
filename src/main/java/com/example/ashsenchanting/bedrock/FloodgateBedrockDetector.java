package com.example.ashsenchanting.bedrock;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FloodgateBedrockDetector implements BedrockPlayerDetector {
    private final Object floodgateApi;
    private final Method isFloodgatePlayerMethod;

    private FloodgateBedrockDetector(Object floodgateApi, Method isFloodgatePlayerMethod) {
        this.floodgateApi = floodgateApi;
        this.isFloodgatePlayerMethod = isFloodgatePlayerMethod;
    }

    public static BedrockPlayerDetector createIfAvailable() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstanceMethod = apiClass.getMethod("getInstance");
            Object apiInstance = getInstanceMethod.invoke(null);
            if (apiInstance == null) {
                return null;
            }

            Method isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            return new FloodgateBedrockDetector(apiInstance, isFloodgatePlayer);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        try {
            Object result = isFloodgatePlayerMethod.invoke(floodgateApi, player.getUniqueId());
            return result instanceof Boolean bedrock && bedrock;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String detectorName() {
        return "floodgate";
    }
}
