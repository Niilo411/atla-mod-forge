package io.github.niilo411.elementalpaths.platform;

import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * NeoForge's PacketDistributor, over Forge 1.20.1's SimpleChannel. Same method names, so
 * the fifty-odd call sites did not change. Every payload goes inside PayloadRegistrar's
 * envelope — see that class.
 */
public final class PacketDistributor {
    private PacketDistributor() {
    }

    private static PayloadRegistrar.ClientBound toClient(CustomPacketPayload payload) {
        return new PayloadRegistrar.ClientBound(new PayloadRegistrar.Envelope(payload, false));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PayloadRegistrar.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                toClient(payload));
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        PayloadRegistrar.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                toClient(payload));
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        PayloadRegistrar.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), toClient(payload));
    }

    public static void sendToAllPlayers(MinecraftServer server, CustomPacketPayload payload) {
        sendToAllPlayers(payload);
    }

    /** Client to server. Only ever called from client code. */
    public static void sendToServer(CustomPacketPayload payload) {
        PayloadRegistrar.CHANNEL.sendToServer(
                new PayloadRegistrar.ServerBound(new PayloadRegistrar.Envelope(payload, true)));
    }
}
