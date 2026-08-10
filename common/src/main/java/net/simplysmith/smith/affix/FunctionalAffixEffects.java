package net.simplysmith.smith.affix;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.simplysmith.smith.SmithData;

/*
功能型词条的统一结算。

属性型词条由 ItemStack#getAttributeModifiers 接管；这里保留所有需要事件时机的效果，
避免把伤害、耐久和死亡逻辑散落在多个 Mixin 中。
*/
public final class FunctionalAffixEffects {

    private static final int IMMORTAL_BASE_COOLDOWN_TICKS = 20 * 60;
    private static final int TICKS_PER_SECOND = 20;

    private FunctionalAffixEffects() {
    }

    public static boolean has(ItemStack stack, Affix affix) {
        return SmithData.affixes(stack).contains(affix);
    }

    public static double value(ItemStack stack, Affix affix) {
        if (!has(stack, affix)) {
            return 0.0D;
        }
        return affix.valueFor(SmithData.quality(stack), SmithData.level(stack));
    }

    // 攻击词条只读取攻击瞬间的主手物品。
    public static double mainHandValue(Player player, Affix affix) {
        return value(player.getMainHandItem(), affix);
    }

    // 闪避等防御词条统计主手、副手和全部盔甲；数值按用户设计直接相加。
    public static double equippedValue(LivingEntity entity, Affix affix) {
        double total = 0.0D;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            total += value(entity.getItemBySlot(slot), affix);
        }
        return total;
    }

    public static boolean tryDodge(LivingEntity entity) {
        if (entity.level().isClientSide || !(entity instanceof Player)) {
            return false;
        }

        // 设计指定的唯一概率上限：99%。
        double chance = Math.min(0.99D, equippedValue(entity, Affixes.DODGE));
        return chance > 0.0D && entity.getRandom().nextDouble() < chance;
    }

    public static void keepEternal(ItemStack stack) {
        if (has(stack, Affixes.ETERNAL)) {
            stack.setDamageValue(0);
        }
    }

    public static void applyLifesteal(Player player, float actualHealthDamage) {
        if (player.level().isClientSide || actualHealthDamage <= 0.0F) {
            return;
        }

        double ratio = mainHandValue(player, Affixes.LIFESTEAL);
        if (ratio <= 0.0D) {
            return;
        }

        float recovery = (float) (actualHealthDamage * ratio);
        float missingHealth = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
        float healing = Math.min(recovery, missingHealth);
        if (healing > 0.0F) {
            player.heal(healing);
        }

        // 故意不设上限：满血时的治疗溢出会不断叠加为伤害吸收。
        float overflow = recovery - healing;
        if (overflow > 0.0F) {
            player.setAbsorptionAmount(player.getAbsorptionAmount() + overflow);
        }
    }

    public static float applyAttackAffixes(Player player, LivingEntity target, float damage) {
        ItemStack weapon = player.getMainHandItem();

        if (player.getHealth() > 1.0F && has(weapon, Affixes.BLOODRAGE)) {
            player.setHealth(player.getHealth() - 1.0F);
            damage += (float) value(weapon, Affixes.BLOODRAGE);
        }

        if (has(weapon, Affixes.BREAK_ARMY) && isVanillaCritical(player, target)) {
            damage *= (float) (1.0D + value(weapon, Affixes.BREAK_ARMY));
        }

        return damage;
    }

    /* 与 Player#attack 内的原版暴击条件一致；此时仍处于命中前，冲刺状态尚未被原版清除。 */
    private static boolean isVanillaCritical(Player player, LivingEntity target) {
        return player.getAttackStrengthScale(0.5F) > 0.9F
                && player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSprinting();
    }

    // 每个装备栏中的经验修补各自每 tick 消耗 1 点经验；永恒同时把已有损伤清零。
    public static void tickEquipment(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            keepEternal(stack);

            if (has(stack, Affixes.EXPERIENCE_MENDING) && stack.isDamaged() && player.totalExperience > 0) {
                player.giveExperiencePoints(-1);
                int repaired = Math.max(0, Mth.floor(value(stack, Affixes.EXPERIENCE_MENDING)));
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - repaired));
            }
        }
    }

    public static boolean tryImmortal(Player player) {
        if (player.level().isClientSide) {
            return false;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!has(stack, Affixes.IMMORTAL) || player.getCooldowns().isOnCooldown(stack.getItem())) {
                continue;
            }

            int reductionSeconds = Mth.floor(value(stack, Affixes.IMMORTAL));
            int cooldownTicks = Math.max(TICKS_PER_SECOND,
                    IMMORTAL_BASE_COOLDOWN_TICKS - reductionSeconds * TICKS_PER_SECOND);
            player.setHealth(1.0F);
            player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);
            return true;
        }
        return false;
    }
}
