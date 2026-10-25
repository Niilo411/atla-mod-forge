package io.github.niilo411.elementalpaths.network;

import io.github.niilo411.elementalpaths.ChannelingData;
import io.github.niilo411.elementalpaths.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import io.github.niilo411.elementalpaths.platform.net.ByteBufCodecs;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import io.github.niilo411.elementalpaths.platform.PacketDistributor;
import io.github.niilo411.elementalpaths.platform.IPayloadContext;

public record UnlockAbilityPacket(String abilityName, int cost) implements CustomPacketPayload {
    public static final Type<UnlockAbilityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("elemental_paths", "unlock_ability"));

    public static final StreamCodec<FriendlyByteBuf, UnlockAbilityPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UnlockAbilityPacket::abilityName,
            ByteBufCodecs.VAR_INT,
            UnlockAbilityPacket::cost,
            UnlockAbilityPacket::new
    );

    @Override
    public Type<? extends UnlockAbilityPacket> type() {
        return TYPE;
    }

    public static void handle(UnlockAbilityPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ChannelingData data = io.github.niilo411.elementalpaths.ModAttachments.get(player);

                // Nothing the settings have switched off can be bought. Checked before the
                // levels are, so a refused purchase costs nothing — and re-checked here
                // rather than trusted from the menu, which greys the node out but is only
                // ever asking. Paying levels for something that then refuses to cast would
                // be the worst outcome of the three.
                if (!io.github.niilo411.elementalpaths.PathsConfig.abilityEnabled(payload.abilityName())) return;

                // Check if player has enough level/XP and doesn't already have it
                if (data.getLevel() >= payload.cost() && !data.getUnlockedAbilities().contains(payload.abilityName())) {

                    // --- SUBTRACT THE COST FROM THEIR LEVEL ---
                    data.setLevel(data.getLevel() - payload.cost());

                    data.unlockAbility(payload.abilityName());
                    io.github.niilo411.elementalpaths.ModAttachments.set(player, data);

                    // Sync updated data back to client INCLUDING EQUIPPED ABILITIES so they don't reset!
                    PacketDistributor.sendToPlayer(player, new SyncChannelingDataPacket(
                            data.getMainElement(),
                            data.getActiveElement(),
                            data.getUnlockedElements(),
                            data.hasChosenElement(),
                            data.getUnlockedAbilities(),
                            data.getEquippedAbilities()
                    ));

                    PacketDistributor.sendToPlayer(player, SyncStatsPacket.of(data));
                }
            }
        });
    }
}