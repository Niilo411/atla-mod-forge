package com.minecraft.atlamod.mixin;

import com.minecraft.atlamod.ServerEvents;
import com.minecraft.atlamod.platform.event.LivingIncomingDamageEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** The amount the incoming-damage handler settled on, for the ModifyVariable below. */
    @Unique
    private float atlamod$incomingAmount;

    /**
     * NeoForge 1.21's LivingIncomingDamageEvent, which Forge 1.20.1 does not have, at the
     * place NeoForge fires it: straight after vanilla's own refusals (invulnerable, client
     * side, dying, fire into fire resistance) and before blocking, helmets, invulnerability
     * frames or armour have looked at the figure. Cancelling returns false, so nothing lands
     * at all — no knockback, no hurt flash — which Forge's own LivingHurtEvent could not
     * give: it fires after hurt() is already committed to knocking the target back.
     */
    @Inject(method = "hurt", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z", ordinal = 0))
    private void atlamod$incomingDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent((LivingEntity) (Object) this, source, amount);
        ServerEvents.onIncomingDamage(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
        }
        atlamod$incomingAmount = event.getAmount();
    }

    /**
     * Hands the handler's amount to the rest of hurt(). A separate injection because plain
     * Mixin cannot change an argument from an @Inject; placed at the noActionTime write,
     * which comes after the call above and before anything reads the amount.
     */
    @ModifyVariable(method = "hurt", argsOnly = true, ordinal = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD, ordinal = 0))
    private float atlamod$incomingAmount(float amount) {
        return atlamod$incomingAmount;
    }
}
