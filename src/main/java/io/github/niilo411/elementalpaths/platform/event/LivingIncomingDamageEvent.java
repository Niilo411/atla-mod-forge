package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Damage about to be TRIED on something, fired near the head of LivingEntity#hurt (LivingEntityMixin)
 * before invulnerability frames, armour or absorption have had a say. Cancel it and
 * nothing lands at all, knockback included; change the amount and everything after
 * works from the new figure.
 */
public final class LivingIncomingDamageEvent extends CancellableEvent {
    private final LivingEntity entity;
    private final DamageSource source;
    private float amount;

    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        this.entity = entity;
        this.source = source;
        this.amount = amount;
    }

    public LivingEntity getEntity() { return entity; }
    public DamageSource getSource() { return source; }
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }
}
