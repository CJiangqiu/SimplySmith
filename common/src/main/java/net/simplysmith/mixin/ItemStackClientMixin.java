package net.simplysmith.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.smith.Quality;
import net.simplysmith.smith.QualityText;
import net.simplysmith.smith.SmithData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
传奇以上没有对应的原版 Rarity，客户端在显示名出口补上自有品质样式。
只放在 client mixin 列表，避免服务端把可翻译物品名提前展开成固定语言文本。
*/
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void simplysmith$applyCustomQualityStyle(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!SmithData.isStamped(stack)) {
            return;
        }

        Quality quality = SmithData.quality(stack);
        if (quality.hasCustomVisual()) {
            cir.setReturnValue(QualityText.apply(quality, cir.getReturnValue()));
        }
    }
}
