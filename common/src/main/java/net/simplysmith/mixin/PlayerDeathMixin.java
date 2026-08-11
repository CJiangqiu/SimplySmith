package net.simplysmith.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 在玩家掉落物品前处理不朽
@Mixin(ServerPlayer.class)
public abstract class PlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void simplysmith$preventDeath(DamageSource source, CallbackInfo ci) {
        if (FunctionalAffixEffects.tryImmortal((ServerPlayer) (Object) this)) {
            ci.cancel();
        }
    }
}
