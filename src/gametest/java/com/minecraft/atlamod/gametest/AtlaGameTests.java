package com.minecraft.atlamod.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.minecraft.atlamod.AbilityHandler;
import com.minecraft.atlamod.Atlamod;
import com.minecraft.atlamod.BendingData;
import com.minecraft.atlamod.ModAttachments;
import com.minecraft.atlamod.ServerEvents;
import com.minecraft.atlamod.abilities.Ability;
import com.minecraft.atlamod.abilities.AbilityRegistry;
import com.minecraft.atlamod.abilities.ChargedAbility;
import com.minecraft.atlamod.abilities.PassiveAbility;
import com.minecraft.atlamod.abilities.TwoPhaseAbility;
import com.minecraft.atlamod.spirit.island.IslandStyle;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated checks run by `gradlew runGameTestServer` on a headless server.
 *
 * Smoke tests, not balance tests: they prove the mod's data loads, and that every ability
 * can be pressed, held, released, clicked and pressed again without throwing — the kind of
 * failure that would otherwise only surface as a crash the first time somebody happened
 * to use that one ability in game.
 *
 * THE ABILITY TEST DRIVES TIME ITSELF. 1.20.1 has no /tick sprint (that is 1.20.3), so
 * waiting real game ticks for 123 abilities with ten-second charges would take a quarter
 * of an hour. Instead each simulated tick runs the player's own tick and the mod's server
 * tick handler directly, which is every path the mod's code takes; what it skips is
 * vanilla ticking the rest of the world, which is not what is being tested.
 */
@GameTestHolder(Atlamod.MODID)
@PrefixGameTestTemplate(false)
public class AtlaGameTests {

    /** Every registry entry the Spirit World depends on is present and parses. */
    @GameTest(template = "empty")
    public static void spiritWorldDataLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var access = server.registryAccess();

        helper.assertTrue(access.registryOrThrow(Registries.DIMENSION_TYPE)
                        .containsKey(ResourceLocation.fromNamespaceAndPath(Atlamod.MODID, "spirit_world")),
                "the spirit_world dimension type is missing");

        var biomes = access.registryOrThrow(Registries.BIOME);
        for (IslandStyle style : IslandStyle.values()) {
            helper.assertTrue(biomes.containsKey(style.biome()), "biome missing: " + style.biome());
        }
        helper.assertTrue(biomes.containsKey(ResourceLocation.fromNamespaceAndPath(Atlamod.MODID, "spirit_void")),
                "spirit_void biome missing");

        var structures = access.registryOrThrow(Registries.STRUCTURE);
        helper.assertTrue(structures.containsKey(ResourceLocation.fromNamespaceAndPath(Atlamod.MODID, "spirit_temple")),
                "spirit_temple structure missing");
        helper.assertTrue(structures.containsKey(ResourceLocation.fromNamespaceAndPath(Atlamod.MODID, "spirit_shrine")),
                "spirit_shrine structure missing");

        // Decoded exactly as world creation would decode it.
        var resource = server.getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(Atlamod.MODID, "dimension/spirit_world.json"))
                .orElseThrow(() -> new AssertionError("dimension/spirit_world.json not found"));
        try (Reader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), json)
                    .getOrThrow(false, error -> {
                        throw new AssertionError("spirit_world dimension does not parse: " + error);
                    });
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        helper.succeed();
    }

    /** Ticks given to an ability after its charge (or once pressed) before it is let go. */
    private static final int HOLD_EXTRA = 30;

    /** Ticks given after release for projectiles, waves and timers to run. */
    private static final int AFTERMATH = 40;

    /** Where the player stands and what they are looking at. */
    private enum Setting {
        /** A zombie four blocks ahead, looked at. */
        AT_A_MOB,
        /** Standing in water, a pool ahead, looking along it at a zombie, with a full canteen. */
        IN_WATER,
        /** In water with a canteen, looking straight at a zombie: Drown wants both at once. */
        IN_WATER_AT_A_MOB,
        /** On stone, looking down at the stone ahead. */
        AT_THE_GROUND
    }

    /**
     * Every registered ability, driven the way the client drives it: the key press sends
     * both a cast and a hold, then the key is held long enough for any charge to complete,
     * then released, then the left mouse button is clicked once per shot (two-phase
     * abilities), then the key is pressed once more (switching toggles back off), and
     * finally the player logs out — which runs every cleanup path the mod has.
     *
     * FOUR SETTINGS, tried in turn. An ability that leaves no trace in one — spends no
     * chi, earns no xp, starts no cooldown — is tried again in the next, so the water and
     * ground code paths actually run. Whatever is still silent after all three is logged;
     * passives belong there, since they cost nothing by design.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyAbilitySurvivesAFullCycle(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();
        List<Ability> queue = new ArrayList<>(AbilityRegistry.all().values());
        int runs = 0;

        for (Setting where : Setting.values()) {
            List<Ability> silent = new ArrayList<>();
            for (Ability ability : queue) {
                runs++;
                if (run(helper, ability, where, failures)) silent.add(ability);
            }
            queue = silent;
            if (queue.isEmpty()) break;
        }

        Atlamod.LOGGER.info("Game test: {} ability runs over {} abilities; {} left no trace in any setting: {}",
                runs, AbilityRegistry.all().size(), queue.size(), queue.stream().map(Ability::getName).toList());

        if (failures.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(failures.size() + " ability step(s) threw:\n" + String.join("\n", failures));
        }
    }

    /** One full cycle of one ability. @return true if it left no trace. */
    private static boolean run(GameTestHelper helper, Ability ability, Setting where, List<String> failures) {
        String name = ability.getName();
        MinecraftServer server = helper.getLevel().getServer();

        ServerPlayer player = mockPlayer(helper);
        Mob target = prepare(helper, where, player);

        BendingData data = ModAttachments.get(player);
        data.setLevel(20);
        data.unlockAbility(name);
        if (ability instanceof PassiveAbility) {
            // A passive does its work while equipped and ticking.
            data.setAllEquippedPassives(List.of(name, "EMPTY", "EMPTY", "EMPTY"));
        }

        Runnable tick = () -> {
            player.doTick();
            ServerEvents.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
        };

        // One tick on the spot first, as a real player would have had: vanilla only works
        // out "standing in water" during a tick, and Water heal asks at the press.
        guard(name, where, "settle", tick, failures);
        data.setCurrentChi(data.getMaxChi());
        int fullChi = data.getCurrentChi();

        int hold = HOLD_EXTRA;
        if (ability instanceof ChargedAbility charged) hold += charged.getChargeTicks();
        int clicks = ability instanceof TwoPhaseAbility twoPhase ? Math.max(1, twoPhase.getShots()) : 1;

        guard(name, where, "press", () -> {
            AbilityHandler.executeAbility(player, data, name);
            AbilityHandler.executeAbilityHold(player, data, name, true);
        }, failures);
        for (int i = 0; i < hold; i++) guard(name, where, "tick", tick, failures);
        guard(name, where, "release", () -> AbilityHandler.executeAbilityHold(player, data, name, false), failures);
        for (int i = 0; i < clicks; i++) {
            guard(name, where, "left click", () -> AbilityHandler.executeLeftClickPhase(player, data), failures);
            guard(name, where, "tick", tick, failures);
        }
        for (int i = 0; i < AFTERMATH; i++) guard(name, where, "tick", tick, failures);
        guard(name, where, "second press", () -> {
            AbilityHandler.executeAbility(player, data, name);
            AbilityHandler.executeAbilityHold(player, data, name, true);
            AbilityHandler.executeAbilityHold(player, data, name, false);
        }, failures);
        for (int i = 0; i < 5; i++) guard(name, where, "tick", tick, failures);

        guard(name, where, "logout", () ->
                ServerEvents.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player)), failures);
        server.getPlayerList().remove(player);
        if (target != null) target.discard();

        return data.getCurrentChi() >= fullChi && data.getXp() == 0 && data.getLevel() == 20
                && !data.isOnCooldown(ability.getKey());
    }

    /**
     * A test player that the mod can send packets to.
     *
     * Vanilla 1.20.1's own makeMockServerPlayerInLevel gives its player a Connection with
     * no Netty channel at all, so the first packet sent to it — the mod syncs a player's
     * data the moment they log in — dies on a null channel. Newer vanilla attaches an
     * EmbeddedChannel, which simply swallows whatever is written to it; this does the same.
     * Creative, like vanilla's mock.
     */
    /**
     * Named rather than anonymous, for the reason SpiritSkyEffects.WastelandEffects gives:
     * an anonymous subclass copies the superclass constructor's parameter names, which
     * collided on CI.
     */
    private static final class MockPlayer extends ServerPlayer {
        MockPlayer(net.minecraft.server.level.ServerLevel level) {
            super(level.getServer(), level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-mock-player"));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return true;
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = new MockPlayer(level);
        net.minecraft.network.Connection connection =
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }

    /** Lays out the setting (blocks are re-set every time, since abilities reshape them). */
    private static Mob prepare(GameTestHelper helper, Setting where, ServerPlayer player) {
        // A stone floor under the whole working area, whatever the last ability did to it.
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }

        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);

        return switch (where) {
            case AT_A_MOB -> {
                Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 5));
                player.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
                yield zombie;
            }
            case IN_WATER -> {
                for (int z = 1; z <= 4; z++) helper.setBlock(new BlockPos(1, 1, z), Blocks.WATER);
                player.getInventory().add(new ItemStack(Atlamod.WATER_CANTEEN.get()));
                Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 6));
                player.lookAt(EntityAnchorArgument.Anchor.EYES,
                        Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 3))));
                yield zombie;
            }
            case IN_WATER_AT_A_MOB -> {
                for (int z = 1; z <= 2; z++) helper.setBlock(new BlockPos(1, 1, z), Blocks.WATER);
                player.getInventory().add(new ItemStack(Atlamod.WATER_CANTEEN.get()));
                Mob zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 5));
                player.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
                yield zombie;
            }
            case AT_THE_GROUND -> {
                player.lookAt(EntityAnchorArgument.Anchor.EYES,
                        Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 0, 3))));
                yield null;
            }
        };
    }

    private static void guard(String ability, Setting where, String step, Runnable action, List<String> failures) {
        try {
            action.run();
        } catch (Throwable t) {
            Atlamod.LOGGER.error("Game test: {} threw on {} ({})", ability, step, where, t);
            failures.add(ability + " (" + step + ", " + where + "): " + t);
        }
    }
}
