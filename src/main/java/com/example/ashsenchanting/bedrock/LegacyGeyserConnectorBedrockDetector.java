package com.example.ashsenchanting.bedrock;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class LegacyGeyserConnectorBedrockDetector implements BedrockPlayerDetector {
    private final Object connectorInstance;
    private final Method getPlayerByUuidMethod;

    private LegacyGeyserConnectorBedrockDetector(Object connectorInstance, Method getPlayerByUuidMethod) {
        this.connectorInstance = connectorInstance;
        this.getPlayerByUuidMethod = getPlayerByUuidMethod;
    }

    public static BedrockPlayerDetector createIfAvailable() {
        try {
            Class<?> connectorClass = Class.forName("org.geysermc.connector.GeyserConnector");
            Method getInstanceMethod = connectorClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            if (instance == null) {
                return null;
            }

            Method getPlayerByUuid = connectorClass.getMethod("getPlayerByUuid", UUID.class);
            return new LegacyGeyserConnectorBedrockDetector(instance, getPlayerByUuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        try {
            return getPlayerByUuidMethod.invoke(connectorInstance, player.getUniqueId()) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String detectorName() {
        return "geyser-legacy";
    }
}
