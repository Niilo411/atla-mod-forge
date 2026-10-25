package com.minecraft.atlamod.network;

import com.minecraft.atlamod.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.minecraft.atlamod.platform.net.StreamCodec;
import com.minecraft.atlamod.platform.net.ByteBufCodecs;
import io.netty.buffer.ByteBuf;

public record SwitchElementPacket(String newElement) implements CustomPacketPayload {
    public static final Type<SwitchElementPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("atlamod", "switch_element"));
    public static final StreamCodec<ByteBuf, SwitchElementPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SwitchElementPacket::newElement, SwitchElementPacket::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
