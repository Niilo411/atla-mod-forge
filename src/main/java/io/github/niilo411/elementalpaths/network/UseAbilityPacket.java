package io.github.niilo411.elementalpaths.network;

import io.github.niilo411.elementalpaths.ChannelingData;
import io.github.niilo411.elementalpaths.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.niilo411.elementalpaths.platform.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

public record UseAbilityPacket(int slot) implements CustomPacketPayload {
    public static final Type<UseAbilityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("elemental_paths", "use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> STREAM_CODEC = StreamCodec.composite(
            io.github.niilo411.elementalpaths.platform.net.ByteBufCodecs.INT, UseAbilityPacket::slot,
            UseAbilityPacket::new
    );

    @Override
    public Type<? extends UseAbilityPacket> type() {
        return TYPE;
    }

    public static void handle(UseAbilityPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ChannelingData data = io.github.niilo411.elementalpaths.ModAttachments.get(player);
                String ability = data.getEquippedAbility(payload.slot());

                // Send the ability straight to the brain!
                io.github.niilo411.elementalpaths.AbilityHandler.executeAbility(player, data, ability);
            }
        });
    }
}