package net.simplysmith.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.smith.SmithingRecipes;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
铁砧中的强化与突破主逻辑
在 createResult 开头接管：识别出是我方配方就自行产出结果并取消原版逻辑，
否则原样放行，原版的修复与附魔完全不受影响。
材料消耗刻意复用原版通路，把 repairItemCountCost 设为 1，原版 onTake 就会在玩家取件时精确扣掉 1 个石头。
槽位一律经 getSlot 存取。inputSlots 与 resultSlots 声明在父类 ItemCombinerMenu 上，
而 @Shadow 只在目标类本身查找、不会向上找父类；getSlot 与槽位常量都是公开的，用它们既绕开该限制，也不必为此引入访问转换配置。
*/
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    // 原版此字段为 private，Forge 侧才被访问转换器放宽为 public，这里按原版声明
    @Shadow
    private int repairItemCountCost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplysmith$createSmithingResult(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;

        ItemStack gear = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack material = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();

        SmithingRecipes.Kind kind = SmithingRecipes.identify(gear, material);
        if (kind == null) {
            return;
        }

        ItemStack result = SmithingRecipes.assemble(gear, kind, RandomSource.create());
        if (result.isEmpty()) {
            // 例如对已到最高品质的装备使用突破石，不给结果
            self.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            cost.set(0);
            repairItemCountCost = 0;
            ci.cancel();
            return;
        }

        self.getSlot(AnvilMenu.RESULT_SLOT).set(result);
        cost.set(0);
        repairItemCountCost = 1;
        ci.cancel();
    }

    /*
    允许零经验取件

    原版 mayPickup 末尾有 cost > 0 的硬条件，cost 为 0 时玩家根本取不走结果。
    强化与突破不消耗经验等级，所以这里在识别出是我方配方时放行。
    */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void simplysmith$allowFreePickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (cost.get() == 0 && repairItemCountCost == 1
                && !self.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
