package net.simplysmith.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
闪避必须同时处理 hurt 与 actuallyHurt：通常伤害走 hurt，再由它调用 actuallyHurt；
少数实体会直接调用 actuallyHurt。hurtDepth 用来防止同一段标准伤害链掷两次闪避骰子。
*/
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private int simplysmith$hurtDepth;

    @Unique
    private float simplysmith$healthBeforeDamage;

    @Unique
    private float simplysmith$absorptionBeforeDamage;

    @Unique
    private DamageSource simplysmith$damageSource;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void simplysmith$tryDodgeAtHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        simplysmith$hurtDepth++;
        if (FunctionalAffixEffects.tryDodge((LivingEntity) (Object) this)) {
            simplysmith$hurtDepth = Math.max(0, simplysmith$hurtDepth - 1);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void simplysmith$leaveHurt(CallbackInfoReturnable<Boolean> cir) {
        simplysmith$hurtDepth = Math.max(0, simplysmith$hurtDepth - 1);
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void simplysmith$tryDodgeAtActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
        if (simplysmith$hurtDepth == 0 && FunctionalAffixEffects.tryDodge((LivingEntity) (Object) this)) {
            ci.cancel();
            return;
        }

        simplysmith$healthBeforeDamage = ((LivingEntity) (Object) this).getHealth();
        simplysmith$absorptionBeforeDamage = ((LivingEntity) (Object) this).getAbsorptionAmount();
        simplysmith$damageSource = source;
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void simplysmith$applyLifesteal(CallbackInfo ci) {
        if (simplysmith$damageSource != null && simplysmith$damageSource.getEntity() instanceof Player player) {
            LivingEntity target = (LivingEntity) (Object) this;
            float dealt = simplysmith$healthBeforeDamage - target.getHealth()
                    + simplysmith$absorptionBeforeDamage - target.getAbsorptionAmount();
            FunctionalAffixEffects.applyLifesteal(player, dealt);
        }
        simplysmith$damageSource = null;
    }
}
