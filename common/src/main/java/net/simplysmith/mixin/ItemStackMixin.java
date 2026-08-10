package net.simplysmith.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
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

    /* 永恒不只阻止本次耐久扣除，同时将已有损伤归零，确保物品始终满耐久。 */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void simplysmith$keepEternalItemPristine(int amount, RandomSource random, ServerPlayer player,
                                                      CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (FunctionalAffixEffects.has(stack, Affixes.ETERNAL)) {
            stack.setDamageValue(0);
            cir.setReturnValue(false);
        }
    }

    /*
    把词条的属性加成并入物品自身的属性修饰符

    这里是物品属性的唯一出口，挂在这里的好处是修饰符的添加与移除完全由原版接管，
    随装备变化自动生效与撤销，不需要我方管理生命周期，也不会残留。

    只在物品的自然槽位追加，与原版对物品自身属性的处理一致。否则原版 Tooltip 会
    逐槽位查询并把六个槽位各打印一段，玩家也能把盔甲塞进副手多吃一份词条。
    */
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

        /*
        原版走 Item.getDefaultAttributeModifiers 时返回的是不可变 Multimap，
        必须先拷成可变的再追加，直接改会抛异常。
        */
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create(cir.getReturnValue());
        for (Affix affix : affixes) {
            if (affix.isAttribute()) {
                modifiers.put(affix.attribute(), affix.createModifier(slot, quality, level));
            }
        }
        cir.setReturnValue(modifiers);
    }

    /*
    让原版能够表达的物品稀有度跟随品质

    前四档直接复用原版取色。传奇以上在原版侧回落为 EPIC，实际显示色由
    ItemStackClientMixin 接管；这样无需扩展 Fabric 不支持的 Rarity 枚举。

    未盖章时返回原值，所以首次盖章读到的仍是物品固有稀有度，不会自我循环。
    */
    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void simplysmith$overrideRarity(CallbackInfoReturnable<Rarity> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (SmithData.isStamped(stack)) {
            cir.setReturnValue(SmithData.quality(stack).vanillaRarity());
        }
    }
}
