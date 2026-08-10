package net.simplysmith.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    @Unique
    private Entity simplysmith$attackTarget;

    @Inject(method = "attack", at = @At("HEAD"))
    private void simplysmith$rememberAttackTarget(Entity target, CallbackInfo ci) {
        simplysmith$attackTarget = target;
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void simplysmith$forgetAttackTarget(Entity target, CallbackInfo ci) {
        simplysmith$attackTarget = null;
    }

    @ModifyArg(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1
    )
    private float simplysmith$applyAttackAffixes(float damage) {
        if (simplysmith$attackTarget instanceof LivingEntity livingTarget) {
            return FunctionalAffixEffects.applyAttackAffixes((Player) (Object) this, livingTarget, damage);
        }
        return damage;
    }
}
