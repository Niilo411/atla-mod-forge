package com.minecraft.atlamod.platform.net;

import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft 1.20.2's CustomPacketPayload, cut down to what the mod's packets use: every
 * packet names its Type, and the Type's id is what PayloadRegistrar keys the channel on.
 */
public interface CustomPacketPayload {

    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
