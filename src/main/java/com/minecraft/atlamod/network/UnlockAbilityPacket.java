package com.minecraft.atlamod.network;

import com.minecraft.atlamod.BendingData;
import com.minecraft.atlamod.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import com.minecraft.atlamod.platform.net.ByteBufCodecs;
import com.minecraft.atlamod.platform.net.StreamCodec;
import com.minecraft.atlamod.platform.net.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.minecraft.atlamod.platform.PacketDistributor;
import com.minecraft.atlamod.platform.IPayloadContext;

public record UnlockAbilityPacket(String abilityName, int cost) implements CustomPacketPayload {
    public static final Type<UnlockAbilityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("atlamod", "unlock_ability"));

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
                BendingData data = com.minecraft.atlamod.ModAttachments.get(player);

                // Nothing the settings have switched off can be bought. Checked before the
                // levels are, so a refused purchase costs nothing — and re-checked here
                // rather than trusted from the menu, which greys the node out but is only
                // ever asking. Paying levels for something that then refuses to cast would
                // be the worst outcome of the three.
                if (!com.minecraft.atlamod.AtlaConfig.abilityEnabled(payload.abilityName())) return;

                // Check if player has enough level/XP and doesn't already have it
                if (data.getLevel() >= payload.cost() && !data.getUnlockedAbilities().contains(payload.abilityName())) {

                    // --- SUBTRACT THE COST FROM THEIR LEVEL ---
                    data.setLevel(data.getLevel() - payload.cost());

                    data.unlockAbility(payload.abilityName());
                    com.minecraft.atlamod.ModAttachments.set(player, data);

                    // Sync updated data back to client INCLUDING EQUIPPED ABILITIES so they don't reset!
                    PacketDistributor.sendToPlayer(player, new SyncBendingDataPacket(
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