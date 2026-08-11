package net.simplysmith.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// hurt 与 actuallyHurt 共用一次闪避判定
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

    // 原版记录的上一次伤害量，附加伤害结算前后要靠它还原无敌帧状态
    @Shadow
    protected float lastHurt;

    // 神射手按攻击者与目标的距离放大伤害
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplysmith$applySharpshooter(float amount, DamageSource source, float original) {
        return FunctionalAffixEffects.applySharpshooter((LivingEntity) (Object) this, source, amount);
    }

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

    // 着火、冰冻、雷电、毒素、凋零、真伤六条附加词条的触发点
    @Inject(method = "hurt", at = @At("HEAD"))
    private void simplysmith$applyOnHitAffixes(DamageSource source, float amount,
                                               CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 在附加伤害后恢复主伤害的无敌帧状态
        int invulnerable = self.invulnerableTime;
        float previousHurt = lastHurt;

        FunctionalAffixEffects.applyOnHitAffixes(self, source);

        self.invulnerableTime = invulnerable;
        lastHurt = previousHurt;
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
