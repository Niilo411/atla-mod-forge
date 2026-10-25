package com.minecraft.atlamod;

import com.minecraft.atlamod.avatar.AvatarState;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where the mod keeps its data on Forge 1.20.1, which predates NeoForge's data
 * attachments.
 *
 * A player's BendingData is a CAPABILITY, saved with the player through the same
 * BendingData.CODEC the NeoForge build used, so the stored format is unchanged. Read it
 * with {@link #get}, which is what NeoForge's {@code getData} was; the object is mutated in
 * place everywhere, exactly as before.
 *
 * The Avatar cycle is a SavedData on the overworld rather than a level attachment.
 */
public final class ModAttachments {
    private ModAttachments() {
    }

    /**
     * The capability itself is a small mutable HOLDER rather than the BendingData, so
     * {@link #set} can swap the object inside without the capability having to change.
     */
    public static final Capability<Holder> BENDING_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final class Holder {
        private BendingData data = new BendingData();
    }

    private static final ResourceLocation BENDING_DATA_ID = ResourceLocation.fromNamespaceAndPath(Atlamod.MODID, "bending_data");

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(Holder.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModAttachments::attach);
        // HIGHEST, so the copy is in place before the mod's own clone handler (which runs
        // at LOWEST and copies its fields by hand) — see copyOnDeath.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ModAttachments::copyOnDeath);
    }

    /** The player's bending data. Never null: an entity with none gets a blank, unsaved one. */
    public static BendingData get(Entity entity) {
        return entity.getCapability(BENDING_DATA).map(holder -> holder.data).orElseGet(BendingData::new);
    }

    /** Replaces the player's bending data outright. */
    public static void set(Entity entity, BendingData data) {
        entity.getCapability(BENDING_DATA).ifPresent(holder -> holder.data = data);
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(BENDING_DATA_ID, new Provider());
        }
    }

    /**
     * NeoForge's copyOnDeath. The new body gets a codec round-trip of the old one's data —
     * exactly what NeoForge did — so everything saved comes across and every transient "is
     * X running" flag arrives false, which the clone handler in ServerEvents relies on.
     *
     * Forge invalidates a dead player's capabilities, so they are revived just long
     * enough to be read.
     */
    private static void copyOnDeath(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        BendingData old = get(original);
        set(event.getEntity(), roundTrip(old));
        original.invalidateCaps();
    }

    private static BendingData roundTrip(BendingData data) {
        return BendingData.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .flatMap(tag -> BendingData.CODEC.parse(NbtOps.INSTANCE, tag))
                .resultOrPartial(error -> Atlamod.LOGGER.error("Could not copy bending data: {}", error))
                .orElseGet(BendingData::new);
    }

    /** Holds one player's BendingData and saves it with them. */
    private static final class Provider implements ICapabilitySerializable<Tag> {
        private final Holder holder = new Holder();
        private final LazyOptional<Holder> optional = LazyOptional.of(() -> holder);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BENDING_DATA.orEmpty(cap, optional);
        }

        @Override
        public Tag serializeNBT() {
            return BendingData.CODEC.encodeStart(NbtOps.INSTANCE, holder.data)
                    .resultOrPartial(error -> Atlamod.LOGGER.error("Could not save bending data: {}", error))
                    .orElseGet(CompoundTag::new);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            holder.data = BendingData.CODEC.parse(NbtOps.INSTANCE, tag)
                    .resultOrPartial(error -> Atlamod.LOGGER.error("Could not load bending data: {}", error))
                    .orElseGet(BendingData::new);
        }
    }

    // ------------------------------------------------------------------
    // The Avatar cycle, saved with the overworld
    // ------------------------------------------------------------------

    /**
     * The Avatar cycle. Held by the OVERWORLD, not by a player — whose turn it is and who
     * currently holds the title are facts about the world, and have to outlive any one
     * player being logged in.
     */
    public static AvatarState avatarState(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                AvatarSaved::load, AvatarSaved::new, Atlamod.MODID + "_avatar").state;
    }

    private static final class AvatarSaved extends SavedData {
        private final AvatarState state;

        AvatarSaved() {
            this(new AvatarState());
        }

        private AvatarSaved(AvatarState state) {
            this.state = state;
        }

        static AvatarSaved load(CompoundTag tag) {
            return new AvatarSaved(AvatarState.CODEC.parse(NbtOps.INSTANCE, tag.get("state"))
                    .resultOrPartial(error -> Atlamod.LOGGER.error("Could not load the Avatar cycle: {}", error))
                    .orElseGet(AvatarState::new));
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            AvatarState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().ifPresent(t -> tag.put("state", t));
            return tag;
        }

        /**
         * Always dirty. The state is mutated in place all over the Avatar code and nobody
         * calls setDirty, which matches NeoForge's level attachments: they were saved with
         * the level every time regardless.
         */
        @Override
        public boolean isDirty() {
            return true;
        }
    }
}
