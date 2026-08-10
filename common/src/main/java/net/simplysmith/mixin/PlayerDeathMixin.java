package net.simplysmith.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 服务端玩家覆写了 Player#die；必须混入这个实际出口，才能在丢失物品前取消死亡。
@Mixin(ServerPlayer.class)
public abstract class PlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void simplysmith$preventDeath(DamageSource source, CallbackInfo ci) {
        if (FunctionalAffixEffects.tryImmortal((ServerPlayer) (Object) this)) {
            ci.cancel();
        }
    }
}
