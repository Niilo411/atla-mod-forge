package com.minecraft.atlamod.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only setup, called from the mod constructor through DistExecutor so a dedicated
 * server never loads it. (NeoForge allowed a second, client-only @Mod class; Forge 1.20.1
 * does not.)
 */
public final class AtlamodClient {
    private AtlamodClient() {
    }

    public static void init(ModLoadingContext context) {
        // What the Config button on the Mods screen opens. OUR screen rather than a
        // generic one — see AtlaSettingsScreen for why a hundred and six ability switches
        // and a set of generation rates are worth a screen of their own. The same screen
        // is reachable from the bending menu, so there is one settings UI rather than two
        // that could disagree. The second argument is the mod list, which Done goes back to.
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, modListScreen) -> new AtlaSettingsScreen(modListScreen)));
    }
}
