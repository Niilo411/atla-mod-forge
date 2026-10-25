package com.minecraft.atlamod.network;

import com.minecraft.atlamod.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.minecraft.atlamod.platform.net.StreamCodec;
import com.minecraft.atlamod.platform.net.ByteBufCodecs;
import io.netty.buffer.ByteBuf;

public record MeditatePacket(boolean isStarting) implements CustomPacketPayload {
    public static final Type<MeditatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("atlamod", "meditate"));

    public static final StreamCodec<ByteBuf, MeditatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MeditatePacket::isStarting, MeditatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
