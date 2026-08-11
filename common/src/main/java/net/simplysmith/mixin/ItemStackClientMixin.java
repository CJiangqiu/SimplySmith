package net.simplysmith.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.AffixText;
import net.simplysmith.smith.quality.Quality;
import net.simplysmith.smith.quality.QualityText;
import net.simplysmith.smith.SmithData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// 物品显示的客户端接管
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    // 词条数值行相对词条名的缩进
    private static final String SIMPLYSMITH$INDENT = "  ";

    // 传奇以上没有对应的原版 Rarity，在显示名出口补上自有品质样式。
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

    // 在 Tooltip 里追加品质、强化等级与词条，插在物品名之后
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void simplysmith$appendTooltip(Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!SmithData.isStamped(stack)) {
            return;
        }

        List<Component> lines = cir.getReturnValue();
        Quality quality = SmithData.quality(stack);

        int index = Math.min(1, lines.size());

        MutableComponent qualityLine = Component.translatable("tooltip." + SimplySmith.MOD_ID + ".quality")
                .withStyle(ChatFormatting.GRAY)
                .append(QualityText.apply(quality,
                        Component.translatable("quality." + SimplySmith.MOD_ID + "." + quality.id())));
        lines.add(index, qualityLine);

        int level = SmithData.level(stack);
        if (level > 0) {
            index++;
            lines.add(index, Component.translatable("tooltip." + SimplySmith.MOD_ID + ".level", level)
                    .withStyle(ChatFormatting.GOLD));
        }

        List<Affix> affixes = SmithData.affixes(stack);
        boolean detailed = Screen.hasShiftDown();

        for (Affix affix : affixes) {
            index++;
            lines.add(index, Component.translatable(affix.translationKey()).withStyle(ChatFormatting.BLUE));
            if (detailed) {
                index++;
                lines.add(index, Component.literal(SIMPLYSMITH$INDENT)
                        .append(Component.translatable(affix.descriptionKey(),
                                AffixText.value(affix, quality, level)))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        if (!detailed && !affixes.isEmpty()) {
            index++;
            lines.add(index, Component.translatable("tooltip." + SimplySmith.MOD_ID + ".shift_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
