package net.simplysmith.smith.affix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.simplysmith.SimplySmith;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.simplysmith.mixin.LivingEntityAccessor;
import net.simplysmith.smith.SmithData;

// 功能型词条的统一结算。
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

    // 该词条是否出现在任意一个装备槽上，只问有无、不看数值
    public static boolean hasEquipped(LivingEntity entity, Affix affix) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (has(entity.getItemBySlot(slot), affix)) {
                return true;
            }
        }
        return false;
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

        // 夜莺的偷袭伤害
        if (isNightingaleHidden(player)) {
            damage *= (float) (1.0D + value(weapon, Affixes.NIGHTINGALE));
            player.getCooldowns().addCooldown(weapon.getItem(), NIGHTINGALE_COOLDOWN_TICKS);
        }

        if (player.getHealth() > 1.0F && has(weapon, Affixes.BLOODRAGE)) {
            player.setHealth(player.getHealth() - 1.0F);
            damage += (float) value(weapon, Affixes.BLOODRAGE);
        }

        if (has(weapon, Affixes.BREAK_ARMY) && isVanillaCritical(player, target)) {
            damage *= (float) (1.0D + value(weapon, Affixes.BREAK_ARMY));
        }

        if (has(weapon, Affixes.BACKSTAB) && isBehind(player, target)) {
            damage *= (float) (1.0D + value(weapon, Affixes.BACKSTAB));
        }

        return damage;
    }

    // 六条「附加」词条的统一结算
    private static boolean applyingOnHit;

    public static void applyOnHitAffixes(LivingEntity target, DamageSource source) {
        if (applyingOnHit || target.level().isClientSide) {
            return;
        }
        if (!(source.getEntity() instanceof Player player) || player == target) {
            return;
        }

        applyingOnHit = true;
        try {
            applyFlame(player, target);
            applyFrost(player, target);
            applyMobEffect(player, target, Affixes.POISON, MobEffects.POISON);
            applyMobEffect(player, target, Affixes.WITHER, MobEffects.WITHER);
            applyLightning(player, target);
            applyTrueDamage(player, target);
        } finally {
            applyingOnHit = false;
        }
    }

    private static void applyFlame(Player player, LivingEntity target) {
        int seconds = Mth.floor(mainHandValue(player, Affixes.FLAME));
        if (seconds > 0) {
            target.setSecondsOnFire(seconds);
        }
    }

    // 完全冻结需要 ticksFrozen 达到 140，而离开细雪后每 tick 掉 2 点
    private static void applyFrost(Player player, LivingEntity target) {
        int seconds = Mth.floor(mainHandValue(player, Affixes.FROST));
        if (seconds > 0) {
            target.setTicksFrozen(target.getTicksRequiredToFreeze() + seconds * TICKS_PER_SECOND * 2);
        }
    }

    private static void applyMobEffect(Player player, LivingEntity target, Affix affix, MobEffect effect) {
        int seconds = Mth.floor(mainHandValue(player, affix));
        if (seconds > 0) {
            target.addEffect(new MobEffectInstance(effect, seconds * TICKS_PER_SECOND, ON_HIT_EFFECT_AMPLIFIER));
        }
    }

    // 闪电只做视觉，伤害另外结算
    private static void applyLightning(Player player, LivingEntity target) {
        double damage = mainHandValue(player, Affixes.LIGHTNING);
        if (damage <= 0.0D || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        target.invulnerableTime = 0;
        target.hurt(level.damageSources().lightningBolt(), (float) damage);
    }

    // 真伤使用绕过全部减免的自有伤害类型
    private static void applyTrueDamage(Player player, LivingEntity target) {
        double damage = mainHandValue(player, Affixes.TRUE_DAMAGE);
        if (damage <= 0.0D) {
            return;
        }

        target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(TRUE_DAMAGE_TYPE)
                .ifPresent(type -> {
                    DamageSource source = new DamageSource(type, player);
                    float amount = (float) damage;
                    float expectedHealth = Math.max(0.0F, target.getHealth() - amount);

                    target.hurt(source, amount);
                    if (target.getHealth() != expectedHealth) {
                        target.getEntityData().set(LivingEntityAccessor.simplysmith$getHealthDataId(), expectedHealth);
                        if (expectedHealth <= 0.0F) {
                            target.die(source);
                        }
                    }
                });
    }

    // 神射手：按攻击者与目标的距离放大伤害
    public static float applySharpshooter(LivingEntity target, DamageSource source, float damage) {
        if (!(source.getEntity() instanceof Player player) || player == target) {
            return damage;
        }

        double perBlock = mainHandValue(player, Affixes.SHARPSHOOTER);
        if (perBlock <= 0.0D) {
            return damage;
        }

        double blocks = player.distanceTo(target) - SHARPSHOOTER_MIN_DISTANCE;
        if (blocks <= 0.0D) {
            return damage;
        }

        return (float) (damage * (1.0D + blocks * perBlock));
    }

    // 攻击者是否站在目标的背面 90 度扇形内
    private static boolean isBehind(Player attacker, LivingEntity target) {
        Vec3 toAttacker = attacker.position().subtract(target.position());

        // 只比水平方向，骑在头顶或站在脚下不该算背面
        Vec3 horizontal = new Vec3(toAttacker.x, 0.0D, toAttacker.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return false;
        }

        return Vec3.directionFromRotation(0.0F, target.yBodyRot).dot(horizontal.normalize()) < BACKSTAB_DOT;
    }

    // 判断原版暴击条件
    private static boolean isVanillaCritical(Player player, LivingEntity target) {
        return player.getAttackStrengthScale(0.5F) > 0.9F
                && player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSprinting();
    }

    // 变色龙：站定即隐身，一旦位移立即现形
    private static final double STILL_EPSILON = 1.0E-6D;

    // 夜莺的攻击冷却，固定不随词条数值变化
    private static final int NIGHTINGALE_COOLDOWN_TICKS = 20 * 6;

    // 真实隐身每 tick 清扫的范围，边长 8 格、以玩家为中心
    private static final double AGGRO_BREAK_SIZE = 8.0D;

    // 背刺的判定阈值，cos(135°)，即目标正后方 ±45 度
    private static final double BACKSTAB_DOT = -0.7071067811865476D;

    // 神射手从这个距离开始计数，之内没有加成
    private static final double SHARPSHOOTER_MIN_DISTANCE = 2.0D;

    // 毒素与凋零附加的效果等级，0 对应显示的 I，故 2 即显示为 III
    private static final int ON_HIT_EFFECT_AMPLIFIER = 2;

    // 真伤附加的伤害类型
    private static final ResourceKey<DamageType> TRUE_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation(SimplySmith.MOD_ID, "true_damage"));

    // 变色龙：站定即隐身
    private static boolean isChameleonHidden(Player player) {
        return player.distanceToSqr(player.xo, player.yo, player.zo) <= STILL_EPSILON
                && hasEquipped(player, Affixes.CHAMELEON);
    }

    // 夜莺：潜行即真实隐身
    private static boolean isNightingaleHidden(Player player) {
        ItemStack weapon = player.getMainHandItem();
        return has(weapon, Affixes.NIGHTINGALE)
                && player.isShiftKeyDown()
                && !player.getCooldowns().isOnCooldown(weapon.getItem());
    }

    // 真实隐身：除了看不见，还要让敌人无法把它设为目标
    public static boolean isTrulyInvisible(LivingEntity entity) {
        return entity instanceof Player player && isNightingaleHidden(player);
    }

    // 客户端据此整体取消渲染，两条隐身词条都算
    public static boolean hasInvisibilityAffix(LivingEntity entity) {
        return hasEquipped(entity, Affixes.CHAMELEON) || hasEquipped(entity, Affixes.NIGHTINGALE);
    }

    public static void tickInvisibility(Player player) {
        if (isChameleonHidden(player) || isNightingaleHidden(player)) {
            player.setInvisible(true);
            if (isTrulyInvisible(player)) {
                breakNearbyAggro(player);
            }
        } else if (player.isInvisible() && !player.hasEffect(MobEffects.INVISIBILITY)) {
            player.setInvisible(false);
        }
    }

    // 让附近已经锁定自己的敌人立刻脱战
    private static void breakNearbyAggro(Player player) {
        AABB box = AABB.ofSize(player.position(), AGGRO_BREAK_SIZE, AGGRO_BREAK_SIZE, AGGRO_BREAK_SIZE);

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box)) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }

            Brain<?> brain = mob.getBrain();
            if (brain.getMemory(MemoryModuleType.ATTACK_TARGET).filter(held -> held == player).isPresent()) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
    }

    // 结算经验修补与永恒
    public static void tickEquipment(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        tickInvisibility(player);

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

    // 词条能否把这件工具的挖掘等级抬到够得着这个方块
    public static boolean liftsMiningTier(ItemStack stack, BlockState state) {
        int bonus = Mth.floor(value(stack, Affixes.REFINED_DRILL));
        if (bonus <= 0) {
            return false;
        }

        int required = requiredMiningTier(state);
        if (required < 0 || !(stack.getItem() instanceof TieredItem tiered)) {
            return false;
        }

        // 同时校验工具类型
        if (stack.getItem().getDestroySpeed(stack, state) <= 1.0F) {
            return false;
        }

        return tiered.getTier().getLevel() + bonus >= required;
    }

    // 方块要求的挖掘等级；不在原版三个标签里返回 -1 表示无从判断
    private static int requiredMiningTier(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return 1;
        }
        return -1;
    }

    public static float miningSpeedMultiplier(ItemStack stack) {
        return (float) (1.0D + value(stack, Affixes.EXCAVATOR));
    }

    // 点石成金：挖主世界基岩类方块时概率掉金粒
    public static void tryMidasTouch(Level level, BlockPos pos, BlockState state, ItemStack tool) {
        if (level.isClientSide || !state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return;
        }

        double chance = Math.min(1.0D, value(tool, Affixes.MIDAS_TOUCH));
        if (chance > 0.0D && level.getRandom().nextDouble() < chance) {
            Block.popResource(level, pos, new ItemStack(Items.GOLD_NUGGET));
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
