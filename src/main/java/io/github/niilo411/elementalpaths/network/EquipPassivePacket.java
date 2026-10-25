package io.github.niilo411.elementalpaths.network;

import io.github.niilo411.elementalpaths.ChannelingData;
import io.github.niilo411.elementalpaths.ModAttachments;
import io.netty.buffer.ByteBuf;
import io.github.niilo411.elementalpaths.platform.net.ByteBufCodecs;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import io.github.niilo411.elementalpaths.platform.IPayloadContext;

/** Client -> server. Puts a passive into one of the four slots, or clears it. */
public record EquipPassivePacket(int slot, String passive) implements CustomPacketPayload {

    public static final Type<EquipPassivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("elemental_paths", "equip_passive"));

    public static final StreamCodec<ByteBuf, EquipPassivePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, EquipPassivePacket::slot,
            ByteBufCodecs.STRING_UTF8, EquipPassivePacket::passive,
            EquipPassivePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EquipPassivePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ChannelingData data = io.github.niilo411.elementalpaths.ModAttachments.get(player);

            // Only something the player has actually unlocked may be slotted, and only
            // something this world's settings still allow. Both let an EMPTY name past,
            // since that is the player clearing the slot — and a slot holding a passive
            // that was disabled after it went in has to be emptiable.
            String passive = payload.passive();
            if (passive != null && !passive.isEmpty()
                    && !data.getUnlockedAbilities().contains(passive)) {
                return;
            }
            if (!io.github.niilo411.elementalpaths.PathsConfig.abilityEnabled(passive)) return;

            data.setEquippedPassive(payload.slot(), passive);
            io.github.niilo411.elementalpaths.ModAttachments.set(player, data);
        });
    }
}
