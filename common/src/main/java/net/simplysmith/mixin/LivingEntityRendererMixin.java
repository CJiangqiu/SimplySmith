package net.simplysmith.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 隐身词条的整体隐藏，变色龙与夜莺共用
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void simplysmith$hideChameleon(LivingEntity entity, float entityYaw, float partialTick,
                                           PoseStack poseStack, MultiBufferSource buffers, int light,
                                           CallbackInfo ci) {
        if (entity.isInvisible() && FunctionalAffixEffects.hasInvisibilityAffix(entity)) {
            ci.cancel();
        }
    }
}
