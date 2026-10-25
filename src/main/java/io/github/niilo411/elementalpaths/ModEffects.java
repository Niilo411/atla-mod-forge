package io.github.niilo411.elementalpaths;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The mod's own status effects.
 *
 * First one is Disorientation (Air pull), and there is no vanilla effect that comes
 * close — nothing in the game reverses a player's controls — so it has to be a real
 * registered MobEffect rather than a repurposed vanilla one. Registering it properly
 * also buys the whole status-effect UI for free: the inventory icon, the timer, the
 * particles, and automatic syncing to the affected player's client, which is exactly
 * where the reversal has to happen.
 */
public final class ModEffects {

    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ElementalPaths.MODID);

    /** Reverses the victim's movement keys. See DisorientationEffect. */
    public static final RegistryObject<MobEffect> DISORIENTATION =
            EFFECTS.register("disorientation", DisorientationEffect::new);

    /** Stops the victim hearing anything. See DeafenedEffect. */
    public static final RegistryObject<MobEffect> DEAFENED =
            EFFECTS.register("deafened", DeafenedEffect::new);

    /** Stops the victim moving outright. See StunnedEffect. */
    public static final RegistryObject<MobEffect> STUNNED =
            EFFECTS.register("stunned", StunnedEffect::new);

    /**
     * Ten points of armor, worn as stone. The bonus is declared here rather than in
     * the effect class because vanilla wants it as an attribute modifier on the
     * MobEffect itself — it then applies and removes it in step with the effect, which
     * is a great deal safer than adding one by hand and hoping to remember to take it
     * off again.
     */
    public static final RegistryObject<MobEffect> EARTH_ARMOR =
            EFFECTS.register("earth_armor", () -> new EarthArmorEffect().addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                    modifierId("earth_armor"),
                    10.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    /**
     * A suit of iron: 15 points of armor, exactly a full iron set.
     *
     * TWO separate effects rather than one with two amplifiers, and the reason is
     * arithmetic: vanilla scales an ADD_VALUE modifier by (amplifier + 1), so a single
     * registration can only ever produce a base and its double. Iron's 15 and
     * diamond's 20 are not in that relationship, and there is no base that gives both.
     * Two registrations give each of them its exact figure.
     */
    public static final RegistryObject<MobEffect> METAL_ARMOR =
            EFFECTS.register("metal_armor", () -> new MetalArmorEffect().addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                    modifierId("metal_armor"),
                    15.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    /** The same suit in diamond: 20 points, once Diamond Plating is bought. */
    public static final RegistryObject<MobEffect> METAL_ARMOR_DIAMOND =
            EFFECTS.register("metal_armor_diamond", () -> new MetalArmorEffect().addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                    modifierId("metal_armor_diamond"),
                    20.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    /**
     * An attribute modifier is identified by UUID on 1.20.1, not by a resource id. Derived
     * from the id the NeoForge build used, so it is the same on every install and the
     * modifier vanilla saves with a player is recognised as ours when it comes back.
     */
    private static String modifierId(String name) {
        return java.util.UUID.nameUUIDFromBytes((ElementalPaths.MODID + ":" + name)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
