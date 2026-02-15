package com.example.ashsenchanting.bedrock;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GeyserBedrockDetector implements BedrockPlayerDetector {
    private final Object geyserApi;
    private final Method isBedrockPlayerMethod;
    private final Method connectionByUuidMethod;

    private GeyserBedrockDetector(Object geyserApi, Method isBedrockPlayerMethod, Method connectionByUuidMethod) {
        this.geyserApi = geyserApi;
        this.isBedrockPlayerMethod = isBedrockPlayerMethod;
        this.connectionByUuidMethod = connectionByUuidMethod;
    }

    public static BedrockPlayerDetector createIfAvailable() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Method apiMethod = apiClass.getMethod("api");
            Object apiInstance = apiMethod.invoke(null);
            if (apiInstance == null) {
                return null;
            }

            Method isBedrockPlayer = null;
            Method connectionByUuid = null;

            try {
                isBedrockPlayer = apiClass.getMethod("isBedrockPlayer", UUID.class);
            } catch (NoSuchMethodException ignored) {
                // Fallback to connection lookup for API variants that do not expose isBedrockPlayer(UUID).
            }

            if (isBedrockPlayer == null) {
                try {
                    connectionByUuid = apiClass.getMethod("connectionByUuid", UUID.class);
                } catch (NoSuchMethodException ignored) {
                    // No compatible lookup method available.
                }
            }

            if (isBedrockPlayer == null && connectionByUuid == null) {
                return null;
            }

            return new GeyserBedrockDetector(apiInstance, isBedrockPlayer, connectionByUuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        try {
            if (isBedrockPlayerMethod != null) {
                Object result = isBedrockPlayerMethod.invoke(geyserApi, playerId);
                return result instanceof Boolean bedrock && bedrock;
            }

            if (connectionByUuidMethod != null) {
                return connectionByUuidMethod.invoke(geyserApi, playerId) != null;
            }
        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    @Override
    public String detectorName() {
        return "geyser";
    }
}
