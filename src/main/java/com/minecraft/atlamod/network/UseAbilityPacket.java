package com.minecraft.atlamod.network;

import com.minecraft.atlamod.BendingData;
import com.minecraft.atlamod.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import com.minecraft.atlamod.platform.net.StreamCodec;
import com.minecraft.atlamod.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.minecraft.atlamod.platform.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

public record UseAbilityPacket(int slot) implements CustomPacketPayload {
    public static final Type<UseAbilityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("atlamod", "use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> STREAM_CODEC = StreamCodec.composite(
            com.minecraft.atlamod.platform.net.ByteBufCodecs.INT, UseAbilityPacket::slot,
            UseAbilityPacket::new
    );

    @Override
    public Type<? extends UseAbilityPacket> type() {
        return TYPE;
    }

    public static void handle(UseAbilityPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BendingData data = com.minecraft.atlamod.ModAttachments.get(player);
                String ability = data.getEquippedAbility(payload.slot());

                // Send the ability straight to the brain!
                com.minecraft.atlamod.AbilityHandler.executeAbility(player, data, ability);
            }
        });
    }
}