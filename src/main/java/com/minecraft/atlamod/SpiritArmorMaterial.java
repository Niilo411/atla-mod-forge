package com.minecraft.atlamod;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Iron's protection, diamond's durability — see SpiritArmor for why.
 *
 * On 1.20.1 an armor material is an interface the game asks, not a registry entry, so this
 * is a class of its own. The figures are the NeoForge build's exactly:
 *   defence     2 / 6 / 5 / 2 (iron's)
 *   durability  diamond's factor of 33, so each piece lasts exactly as long as diamond's
 *   toughness   0, knockback resistance 0, enchantability 9
 *
 * The NAME is what the game builds the layer textures from:
 * atlamod:textures/models/armor/spirit_armor_layer_1.png and _layer_2.png — recoloured from
 * CHAINMAIL's, so the set reads as teal mail rather than teal plate.
 */
public final class SpiritArmorMaterial implements ArmorMaterial {
    public static final SpiritArmorMaterial INSTANCE = new SpiritArmorMaterial();

    /** Diamond's own durability factor. */
    private static final int DURABILITY_FACTOR = 33;

    private SpiritArmorMaterial() {
    }

    /** Vanilla's per-slot base health, which every armor material multiplies by its factor. */
    private static int baseHealth(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
    }

    /** 363 / 528 / 495 / 429 — diamond's exactly. */
    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return baseHealth(type) * DURABILITY_FACTOR;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 2;
            case CHESTPLATE -> 6;
            case LEGGINGS -> 5;
            case BOOTS -> 2;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 9;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    /** The shard, so an anvil takes the material the armor is actually made of. */
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Atlamod.SPIRIT_SHARD.get());
    }

    @Override
    public String getName() {
        return Atlamod.MODID + ":spirit_armor";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
