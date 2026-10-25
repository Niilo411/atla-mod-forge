package com.minecraft.atlamod.abilities.air;

import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Air sounds that last exactly as long as the ability making them, plus one second.
 *
 * WHY THIS EXISTS ON 1.20.1 ONLY. The air abilities were written for the Breeze's sounds,
 * which are a second or two long, so they simply replay the sound every so often while
 * they run. On 1.20.1 the nearest stand-in for the Breeze's whirl is the elytra's wind
 * (see AirFx), which is a loop around ten seconds long: every replay stacked on the last,
 * and the sound ran on for up to ten seconds after the ability had ended.
 *
 * So a sustained sound is KEYED to whatever is making it — a player's charge, a ride, one
 * spout — and the ability keeps that key alive every tick it runs ({@link #keepAlive}).
 * Twenty ticks after the last keepAlive the sound is stopped for everybody in earshot, by
 * the same packet the /stopsound command sends.
 *
 * A stop names a SOUND, not one playing instance of it, so it would also cut somebody
 * else's identical sound nearby. A player within earshot of another live use of the same
 * sound is therefore skipped, and hears the other one out instead.
 */
public final class AirSounds {
    private AirSounds() {
    }

    /** One second after the ability stops, the sound goes. */
    private static final int GRACE_TICKS = 20;

    /** Far enough to cover the loudest of them (vanilla hearing reach is 16 x volume). */
    private static final double EARSHOT = 64.0;

    private static final class Sustained {
        final ServerLevel level;
        final SoundEvent sound;
        Vec3 pos;
        long lastAlive;

        Sustained(ServerLevel level, SoundEvent sound, Vec3 pos, long now) {
            this.level = level;
            this.sound = sound;
            this.pos = pos;
            this.lastAlive = now;
        }
    }

    private static final Map<Object, Sustained> ACTIVE = new HashMap<>();

    /**
     * Plays a sustained sound for {@code key} and marks it alive. The key is whatever the
     * sound belongs to: a string naming the ability and the player, or the spout itself.
     */
    public static void play(ServerLevel level, Object key, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
        ACTIVE.compute(key, (k, existing) -> {
            if (existing == null || existing.level != level || existing.sound != sound) {
                return new Sustained(level, sound, pos, level.getGameTime());
            }
            existing.pos = pos;
            existing.lastAlive = level.getGameTime();
            return existing;
        });
    }

    /** The ability is still running: keep its sound going, and follow it if it moves. */
    public static void keepAlive(Object key, Vec3 pos) {
        Sustained s = ACTIVE.get(key);
        if (s == null) return;
        s.pos = pos;
        s.lastAlive = s.level.getGameTime();
    }

    /** Stops whatever has gone a second without being kept alive. Called once a server tick. */
    public static void tickAll(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;

        List<Sustained> expired = new ArrayList<>();
        ACTIVE.values().removeIf(s -> {
            if (s.level.getGameTime() - s.lastAlive <= GRACE_TICKS) return false;
            expired.add(s);
            return true;
        });

        for (Sustained s : expired) stop(s);
    }

    private static void stop(Sustained ended) {
        ClientboundStopSoundPacket packet =
                new ClientboundStopSoundPacket(ended.sound.getLocation(), SoundSource.PLAYERS);

        for (ServerPlayer player : ended.level.players()) {
            if (player.position().distanceToSqr(ended.pos) > EARSHOT * EARSHOT) continue;
            if (stillHearsAnother(player, ended)) continue;
            player.connection.send(packet);
        }
    }

    private static boolean stillHearsAnother(ServerPlayer player, Sustained ended) {
        for (Sustained other : ACTIVE.values()) {
            if (other.level == ended.level && other.sound == ended.sound
                    && player.position().distanceToSqr(other.pos) <= EARSHOT * EARSHOT) {
                return true;
            }
        }
        return false;
    }

    /** A level going away takes its sounds with it; stopping them would reach nobody. */
    public static void forgetLevel(ServerLevel level) {
        ACTIVE.values().removeIf(s -> s.level == level);
    }
}
