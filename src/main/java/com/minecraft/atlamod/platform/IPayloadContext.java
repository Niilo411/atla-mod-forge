package com.minecraft.atlamod.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * The slice of NeoForge's IPayloadContext the packet handlers use, so every handler kept
 * its shape through the port.
 *
 * Handlers are already called on the main thread (PayloadRegistrar registers them with
 * consumerMainThread), so enqueueWork has nothing left to defer and simply runs.
 */
public interface IPayloadContext {
    Player player();

    default void enqueueWork(Runnable work) {
        work.run();
    }

    /**
     * The player a packet is about: whoever SENT it, for a packet to the server, or the
     * local player, for one to the client. The client half is reached through
     * DistExecutor so a dedicated server never loads a client class.
     */
    static IPayloadContext of(NetworkEvent.Context context) {
        Player sender = context.getSender();
        if (sender != null) return () -> sender;
        return () -> DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> com.minecraft.atlamod.client.ClientAccess::player);
    }
}
