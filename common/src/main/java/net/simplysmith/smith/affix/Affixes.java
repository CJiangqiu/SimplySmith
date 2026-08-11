package net.simplysmith.smith.affix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.simplysmith.SimplySmith;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 词条注册表
public final class Affixes {

    private static final Map<ResourceLocation, Affix> BUILT_IN = new LinkedHashMap<>();

    // 数据包词条与合并后的池子
    private static volatile Map<ResourceLocation, Affix> dataDriven = Map.of();
    private static volatile List<Affix> pool = List.of();

    // 以上三个字段必须声明在词条常量之前

    // 强壮：最大生命值提升
    public static final Affix VITALITY = register("vitality", AffixCategory.ARMOR, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 2.0D);

    // 磐石：击退抗性提升
    public static final Affix STEADFAST = register("steadfast", AffixCategory.ARMOR, Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION, 0.05D);

    // 坚固：护甲提升
    public static final Affix STURDY = register("sturdy", AffixCategory.ARMOR, Attributes.ARMOR, AttributeModifier.Operation.ADDITION, 2.0D);

    // 坚韧：护甲韧性提升
    public static final Affix TENACITY = register("tenacity", AffixCategory.ARMOR, Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADDITION, 2.0D);

    //轻盈：速度提升
    public static final Affix NIMBLE = register("nimble", AffixCategory.GENERIC, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE, 0.10D);

    // 力量：攻击力提升
    public static final Affix STRENGTH = register("strength", AffixCategory.WEAPON, Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 1.0D);

    // 狂暴：攻击速度提升
    public static final Affix FRENZY = register("frenzy", AffixCategory.WEAPON, Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADDITION, 0.1D);

    // 冲击：击退强度提升
    public static final Affix IMPACT = register("impact", AffixCategory.WEAPON, Attributes.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADDITION, 0.1D);

    // 幸运：幸运提升
    public static final Affix FORTUNE = register("fortune", AffixCategory.GENERIC, Attributes.LUCK, AttributeModifier.Operation.ADDITION, 1.0D);

    // 吸血：按实际造成的伤害恢复生命，满血时转为伤害吸收
    public static final Affix LIFESTEAL = registerEffect("lifesteal", Affix.Kind.LIFESTEAL, AffixCategory.WEAPON, 0.10D);

    // 血怒：生命高于 1 点时消耗 1 点生命，增加本次近战伤害
    public static final Affix BLOODRAGE = registerEffect("bloodrage", Affix.Kind.BLOODRAGE, AffixCategory.WEAPON, 2.0D);

    // 经验修补：消耗经验点修复装备耐久
    public static final Affix EXPERIENCE_MENDING = registerEffect("experience_mending", Affix.Kind.EXPERIENCE_MENDING, AffixCategory.GENERIC, 2.0D);

    // 永恒：耐久始终保持满值
    public static final Affix ETERNAL = registerEffect("eternal", Affix.Kind.ETERNAL, AffixCategory.TOOL, 0.0D);

    // 破军：提升原版暴击后的伤害
    public static final Affix BREAK_ARMY = registerEffect("break_army", Affix.Kind.BREAK_ARMY, AffixCategory.WEAPON, 1.0D);

    // 不朽：死亡时保留 1 点生命，基础数值为冷却减免秒数
    public static final Affix IMMORTAL = registerEffect("immortal", Affix.Kind.IMMORTAL, AffixCategory.ARMOR, 1.0D);

    // 闪避：手持与穿戴装备的概率直接叠加
    public static final Affix DODGE = registerEffect("dodge", Affix.Kind.DODGE, AffixCategory.GENERIC, 0.10D);

    // 神射手：距离越远伤害越高，基础数值为每格的增伤比例
    public static final Affix SHARPSHOOTER = registerEffect("sharpshooter", Affix.Kind.SHARPSHOOTER, AffixCategory.WEAPON, 0.10D);

    // 背刺：从敌人身后近战时提升伤害
    public static final Affix BACKSTAB = registerEffect("backstab", Affix.Kind.BACKSTAB, AffixCategory.WEAPON, 2.0D);

    // 夜莺：潜行时真实隐身，攻击时解除隐身并造成额外伤害
    public static final Affix NIGHTINGALE = registerEffect("nightingale", Affix.Kind.NIGHTINGALE, AffixCategory.WEAPON, 3.0D);

    // 以下六条「附加」都挂在伤害入口上：只要由玩家造成伤害就触发，不限近战或弹射物

    // 火焰附加：使敌人着火，基础数值为秒数
    public static final Affix FLAME = registerEffect("flame", Affix.Kind.FLAME, AffixCategory.GENERIC, 3.0D);

    // 冰冻附加：使敌人完全冻结，基础数值为秒数
    public static final Affix FROST = registerEffect("frost", Affix.Kind.FROST, AffixCategory.GENERIC, 3.0D);

    // 雷电附加：召唤仅视觉效果的闪电并造成雷电伤害，基础数值为伤害点数
    public static final Affix LIGHTNING = registerEffect("lightning", Affix.Kind.LIGHTNING, AffixCategory.GENERIC, 2.0D);

    // 毒素附加：使敌人中毒，基础数值为秒数
    public static final Affix POISON = registerEffect("poison", Affix.Kind.POISON, AffixCategory.GENERIC, 3.0D);

    // 凋零附加：使敌人凋零，基础数值为秒数
    public static final Affix WITHER = registerEffect("wither", Affix.Kind.WITHER, AffixCategory.GENERIC, 3.0D);

    // 真伤附加：造成无视一切减免的伤害，基础数值为伤害点数
    public static final Affix TRUE_DAMAGE = registerEffect("true_damage", Affix.Kind.TRUE_DAMAGE, AffixCategory.GENERIC, 2.0D);

    // 变色龙：站定不动时隐身，一旦位移立即现形
    public static final Affix CHAMELEON = registerEffect("chameleon", Affix.Kind.CHAMELEON, AffixCategory.ARMOR, 0.0D);

    // 精炼钻头：挖掘等级提升，基础数值为提升的级数
    public static final Affix REFINED_DRILL = registerEffect("refined_drill", Affix.Kind.MINING_LEVEL, AffixCategory.TOOL, 1.0D);

    // 挖掘机：挖掘速度提升
    public static final Affix EXCAVATOR = registerEffect("excavator", Affix.Kind.MINING_SPEED, AffixCategory.TOOL, 0.50D);

    // 点石成金：挖掘主世界基岩类方块时概率掉落金粒
    public static final Affix MIDAS_TOUCH = registerEffect("midas_touch", Affix.Kind.MIDAS_TOUCH, AffixCategory.TOOL, 0.10D);

    private Affixes() {
    }

    private static Affix register(String path, AffixCategory category, Attribute attribute,
                                  AttributeModifier.Operation operation, double defaultBaseValue) {
        return put(new Affix(path, category, attribute, operation, defaultBaseValue));
    }

    private static Affix registerEffect(String path, Affix.Kind kind, AffixCategory category,
                                        double defaultBaseValue) {
        return put(new Affix(path, kind, category, defaultBaseValue));
    }

    private static Affix put(Affix affix) {
        BUILT_IN.put(affix.id(), affix);
        rebuildPool();
        return affix;
    }

    // 整批替换数据包词条
    public static void replaceDataDriven(Map<ResourceLocation, Affix> loaded) {
        dataDriven = Map.copyOf(loaded);
        rebuildPool();
    }

    // 合并顺序必须确定
    private static void rebuildPool() {
        List<Affix> merged = new ArrayList<>(BUILT_IN.values());

        List<Affix> external = new ArrayList<>(dataDriven.values());
        external.sort(Comparator.comparing(affix -> affix.id().toString()));
        merged.addAll(external);

        pool = List.copyOf(merged);
    }

    // 顺序稳定，决定配置文件中的书写顺序
    public static List<Affix> all() {
        return pool;
    }

    // 池中可用词条总数，抽取数量超过它时才会触发兜底重复
    public static int size() {
        return pool.size();
    }

    public static boolean isBuiltIn(ResourceLocation id) {
        return BUILT_IN.containsKey(id);
    }

    // 未知 id 返回 null，例如读到了旧版本、已删除的词条，或数据包已被移除
    public static Affix byId(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Affix affix = BUILT_IN.get(id);
        return affix != null ? affix : dataDriven.get(id);
    }

    public static Affix byId(String raw) {
        return byId(parseId(raw));
    }

    // 解析 NBT 里的词条 id，非法返回 null
    public static ResourceLocation parseId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (raw.indexOf(':') < 0) {
            return ResourceLocation.tryParse(SimplySmith.MOD_ID + ":" + raw);
        }
        return ResourceLocation.tryParse(raw);
    }

    // 触发类加载，确保静态注册在使用前完成
    public static void bootstrap() {
    }
}
