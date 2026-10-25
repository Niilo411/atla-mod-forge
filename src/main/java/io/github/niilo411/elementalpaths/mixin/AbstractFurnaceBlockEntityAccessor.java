package io.github.niilo411.elementalpaths.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The furnace's burn-time fields, for the Ignite ability. They are package-private and
 * there is no public "burn for N ticks" API. An accessor rather than an access
 * transformer: on Forge 1.20.1 a transformer has to name the obfuscated field, where a
 * mixin is remapped for us.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
    @Accessor("litTime")
    void elemental_paths$setLitTime(int ticks);

    @Accessor("litDuration")
    void elemental_paths$setLitDuration(int ticks);
}
