package io.github.niilo411.elementalpaths.network;

import io.netty.buffer.ByteBuf;
import io.github.niilo411.elementalpaths.platform.net.ByteBufCodecs;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Whites out the receiving player's screen for a moment. Lightning stun sends one
 * to its victim as the bolt lands.
 *
 * A packet because a flash has no server-side existence at all -- the same reason
 * EarthquakePacket exists for the camera shake. The server can only ask; the client
 * counts it down and draws it.
 */
public record ScreenFlashPacket(int ticks) implements CustomPacketPayload {

    public static final Type<ScreenFlashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("elemental_paths", "screen_flash"));

    public static final StreamCodec<ByteBuf, ScreenFlashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ScreenFlashPacket::ticks,
            ScreenFlashPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
