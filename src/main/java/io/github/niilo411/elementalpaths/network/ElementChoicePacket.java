package io.github.niilo411.elementalpaths.network;

import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import io.github.niilo411.elementalpaths.platform.net.ByteBufCodecs;
import io.netty.buffer.ByteBuf;

public record ElementChoicePacket(String element) implements CustomPacketPayload {
    public static final Type<ElementChoicePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("elemental_paths", "element_choice"));

    // This encodes and decodes the string data for transmission
    public static final StreamCodec<ByteBuf, ElementChoicePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ElementChoicePacket::element,
            ElementChoicePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
