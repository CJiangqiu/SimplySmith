package net.simplysmith.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;

import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.Affixes;
import net.simplysmith.smith.affix.FunctionalAffixEffects;
import net.simplysmith.smith.quality.Quality;
import net.simplysmith.smith.SmithData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    // 永恒使物品始终保持满耐久
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void simplysmith$keepEternalItemPristine(int amount, RandomSource random, ServerPlayer player,
                                                      CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (FunctionalAffixEffects.has(stack, Affixes.ETERNAL)) {
            stack.setDamageValue(0);
            cir.setReturnValue(false);
        }
    }

    // 精炼钻头：让词条的挖掘等级参与「能否正常收获」的判定
    @Inject(method = "isCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
    private void simplysmith$liftMiningTier(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (FunctionalAffixEffects.liftsMiningTier((ItemStack) (Object) this, state)) {
            cir.setReturnValue(true);
        }
    }

    // 挖掘机：按倍率放大挖掘速度
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void simplysmith$boostMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        float multiplier = FunctionalAffixEffects.miningSpeedMultiplier((ItemStack) (Object) this);
        if (multiplier != 1.0F) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }

    // 把词条的属性加成并入物品自身的属性修饰符
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void simplysmith$appendAffixModifiers(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!SmithData.appliesTo(stack, slot)) {
            return;
        }

        List<Affix> affixes = SmithData.affixes(stack);
        if (affixes.isEmpty()) {
            return;
        }

        Quality quality = SmithData.quality(stack);
        int level = SmithData.level(stack);

        // 复制原版的不可变属性表后再追加
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create(cir.getReturnValue());
        for (Affix affix : affixes) {
            if (affix.isAttribute()) {
                modifiers.put(affix.attribute(), affix.createModifier(slot, quality, level));
            }
        }
        cir.setReturnValue(modifiers);
    }

    // 让原版能够表达的物品稀有度跟随品质
    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void simplysmith$overrideRarity(CallbackInfoReturnable<Rarity> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (SmithData.isStamped(stack)) {
            cir.setReturnValue(SmithData.quality(stack).vanillaRarity());
        }
    }
}
