package net.simplysmith.smith.affix;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
词条注册表
统一池：所有词条共用一张表，不按槽位划分适用范围。
因此会出现一些好玩的情况例如「靴子带攻击力」「剑带护甲」这类组合，这是有意为之的随机性，不是 bug。（别一直给我提相关的issues了QWQ）
*/
public final class Affixes {

    private static final Map<String, Affix> BY_ID = new LinkedHashMap<>();
    private static final List<Affix> ALL = new ArrayList<>();

    // 强壮：最大生命值提升
    public static final Affix VITALITY = register("vitality", Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 2.0D);

    // 磐石：击退抗性提升
    public static final Affix STEADFAST = register("steadfast", Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION, 0.05D);

    // 坚固：护甲提升
    public static final Affix STURDY = register("sturdy", Attributes.ARMOR, AttributeModifier.Operation.ADDITION, 2.0D);

    // 坚韧：护甲韧性提升
    public static final Affix TENACITY = register("tenacity", Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADDITION, 2.0D);

    //轻盈：速度提升
    public static final Affix NIMBLE = register("nimble", Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE, 0.10D);

    // 力量：攻击力提升
    public static final Affix STRENGTH = register("strength", Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 1.0D);

    // 狂暴：攻击速度提升
    public static final Affix FRENZY = register("frenzy", Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADDITION, 0.1D);

    // 冲击：击退强度提升
    public static final Affix IMPACT = register("impact", Attributes.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADDITION, 0.1D);

    // 幸运：幸运提升
    public static final Affix FORTUNE = register("fortune", Attributes.LUCK, AttributeModifier.Operation.ADDITION, 1.0D);

    // 吸血：按实际造成的伤害恢复生命，满血时转为伤害吸收
    public static final Affix LIFESTEAL = registerEffect("lifesteal", Affix.Kind.LIFESTEAL, 0.10D);

    // 血怒：生命高于 1 点时消耗 1 点生命，增加本次近战伤害
    public static final Affix BLOODRAGE = registerEffect("bloodrage", Affix.Kind.BLOODRAGE, 2.0D);

    // 经验修补：消耗经验点修复装备耐久
    public static final Affix EXPERIENCE_MENDING = registerEffect("experience_mending", Affix.Kind.EXPERIENCE_MENDING, 2.0D);

    // 永恒：耐久始终保持满值
    public static final Affix ETERNAL = registerEffect("eternal", Affix.Kind.ETERNAL, 0.0D);

    // 破军：提升原版暴击后的伤害
    public static final Affix BREAK_ARMY = registerEffect("break_army", Affix.Kind.BREAK_ARMY, 1.0D);

    // 不朽：死亡时保留 1 点生命，基础数值为冷却减免秒数
    public static final Affix IMMORTAL = registerEffect("immortal", Affix.Kind.IMMORTAL, 1.0D);

    // 闪避：手持与穿戴装备的概率直接叠加
    public static final Affix DODGE = registerEffect("dodge", Affix.Kind.DODGE, 0.10D);

    private Affixes() {
    }

    private static Affix register(String id, Attribute attribute,
                                  AttributeModifier.Operation operation, double defaultBaseValue) {
        Affix affix = new Affix(id, attribute, operation, defaultBaseValue);
        BY_ID.put(id, affix);
        ALL.add(affix);
        return affix;
    }

    private static Affix registerEffect(String id, Affix.Kind kind, double defaultBaseValue) {
        Affix affix = new Affix(id, kind, defaultBaseValue);
        BY_ID.put(id, affix);
        ALL.add(affix);
        return affix;
    }

    // 顺序稳定，决定配置文件中的书写顺序
    public static List<Affix> all() {
        return Collections.unmodifiableList(ALL);
    }

    // 池中可用词条总数，抽取数量超过它时才会触发兜底重复
    public static int size() {
        return ALL.size();
    }

    // 未知 id 返回 null，例如读到了旧版本或已删除的词条
    public static Affix byId(String id) {
        return BY_ID.get(id);
    }

    // 触发类加载，确保静态注册在使用前完成
    public static void bootstrap() {
    }
}
