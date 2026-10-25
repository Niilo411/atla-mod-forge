package com.minecraft.atlamod.mixin;

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
    void atlamod$setLitTime(int ticks);

    @Accessor("litDuration")
    void atlamod$setLitDuration(int ticks);
}
