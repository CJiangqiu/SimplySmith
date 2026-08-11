package net.simplysmith.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 真实隐身的目标过滤
@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void simplysmith$rejectTrulyInvisibleTarget(LivingEntity target, CallbackInfo ci) {
        if (target != null && FunctionalAffixEffects.isTrulyInvisible(target)) {
            ci.cancel();
        }
    }
}
