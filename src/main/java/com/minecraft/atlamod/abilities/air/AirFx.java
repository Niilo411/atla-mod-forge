package com.minecraft.atlamod.abilities.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Airbending's sounds and particles, for Minecraft 1.20.1.
 *
 * The NeoForge build (1.21.1) drew air from the Breeze and the Wind Charge — gust
 * particles and the Breeze's whirls, whooshes and bursts — and those arrived in 1.21, so
 * none of them exist here. Each keeps its 1.21 NAME and maps onto the nearest thing
 * 1.20.1 has. Changing how air sounds or looks on this version is a change to this file
 * alone, and the ability code reads the same as in the other builds.
 *
 * The two whirl sounds map onto the elytra's wind, which is a long loop: play them
 * through AirSounds, which cuts them a second after the ability stops.
 */
public final class AirFx {
    private AirFx() {
    }

    // --- particles ---

    /** A small puff of moving air. */
    public static final SimpleParticleType SMALL_GUST = ParticleTypes.CLOUD;
    /** A larger gust. */
    public static final SimpleParticleType GUST = ParticleTypes.POOF;
    /** A burst of gusts — a splinter or a small blast landing. */
    public static final SimpleParticleType GUST_EMITTER_SMALL = ParticleTypes.POOF;
    /** A big burst — an air cannon round going off. */
    public static final SimpleParticleType GUST_EMITTER_LARGE = ParticleTypes.EXPLOSION;

    // --- sounds ---

    /** A blast of air going off. The dragon's wingbeat is the deepest whoosh 1.20.1 has. */
    public static final SoundEvent WIND_CHARGE_BURST = SoundEvents.ENDER_DRAGON_FLAP;
    public static final SoundEvent BREEZE_WIND_CHARGE_BURST = SoundEvents.ENDER_DRAGON_FLAP;
    /** Something thrown on the wind. */
    public static final SoundEvent BREEZE_SHOOT = SoundEvents.PLAYER_ATTACK_SWEEP;
    /** Air spinning — tornadoes, spouts, the aura. */
    public static final SoundEvent BREEZE_WHIRL = SoundEvents.ELYTRA_FLYING;
    public static final SoundEvent BREEZE_IDLE_AIR = SoundEvents.ELYTRA_FLYING;
    /** Drawing the air in — breathless, and the start of a charge. */
    public static final SoundEvent BREEZE_INHALE = SoundEvents.PLAYER_BREATH;
    /** Gathering the air for a blast. */
    public static final SoundEvent BREEZE_CHARGE = SoundEvents.PHANTOM_FLAP;
    /** Something turned aside by a wall of wind. */
    public static final SoundEvent BREEZE_DEFLECT = SoundEvents.SHIELD_BLOCK;
    /** Coming down on a cushion of air. */
    public static final SoundEvent BREEZE_LAND = SoundEvents.PHANTOM_FLAP;
}
