package io.github.niilo411.elementalpaths.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** The local player, for common code that must only ever reach it through DistExecutor. */
public final class ClientAccess {
    private ClientAccess() {
    }

    public static Player player() {
        return Minecraft.getInstance().player;
    }
}
