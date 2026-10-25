package com.minecraft.atlamod.network;

import io.netty.buffer.ByteBuf;
import com.minecraft.atlamod.platform.net.ByteBufCodecs;
import com.minecraft.atlamod.platform.net.StreamCodec;
import com.minecraft.atlamod.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> client. The four passive slots.
 *
 * Separate from SyncBendingDataPacket rather than a seventh field on it: that
 * record is already at six, which is as many as StreamCodec.composite takes.
 */
public record SyncPassivesPacket(List<String> equippedPassives) implements CustomPacketPayload {

    public static final Type<SyncPassivesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("atlamod", "sync_passives"));

    public static final StreamCodec<ByteBuf, SyncPassivesPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncPassivesPacket::equippedPassives,
            SyncPassivesPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
