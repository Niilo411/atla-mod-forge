package io.github.niilo411.elementalpaths.network;

import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import io.github.niilo411.elementalpaths.platform.net.ByteBufCodecs;
import io.netty.buffer.ByteBuf;

public record SwitchElementPacket(String newElement) implements CustomPacketPayload {
    public static final Type<SwitchElementPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("elemental_paths", "switch_element"));
    public static final StreamCodec<ByteBuf, SwitchElementPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SwitchElementPacket::newElement, SwitchElementPacket::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
