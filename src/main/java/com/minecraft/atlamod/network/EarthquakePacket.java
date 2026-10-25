package com.minecraft.atlamod.network;

import io.netty.buffer.ByteBuf;
import com.minecraft.atlamod.platform.net.ByteBufCodecs;
import com.minecraft.atlamod.platform.net.StreamCodec;
import com.minecraft.atlamod.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client. Shakes the receiving player's camera for a while.
 *
 * Camera shake has no server-side existence at all — where the view points is a purely
 * client concern — so an earthquake has to ask each client to do it. Sent to everyone
 * in range INCLUDING the caster, who is the one person the earthquake's effects spare
 * but its ground does not.
 */
public record EarthquakePacket(int ticks) implements CustomPacketPayload {

    public static final Type<EarthquakePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("atlamod", "earthquake"));

    public static final StreamCodec<ByteBuf, EarthquakePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EarthquakePacket::ticks,
            EarthquakePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
