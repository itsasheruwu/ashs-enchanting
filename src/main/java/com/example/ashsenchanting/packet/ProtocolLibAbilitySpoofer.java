package com.example.ashsenchanting.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

public final class ProtocolLibAbilitySpoofer implements ClientAbilitySpoofer {
    private final ProtocolManager protocolManager;

    public ProtocolLibAbilitySpoofer() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void setInstantBuild(Player player, boolean instantBuild) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ABILITIES);

        packet.getFloat()
                .write(0, player.getFlySpeed() / 2F)
                .write(1, player.getWalkSpeed() / 2F);

        packet.getBooleans()
                .write(0, player.isInvulnerable())
                .write(1, player.isFlying())
                .write(2, player.getAllowFlight())
                .write(3, instantBuild);

        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception ignored) {
            // Non-fatal: client will keep current vanilla ability display state.
        }
    }
}
