package net.simplysmith.smith;

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

    private Affixes() {
    }

    private static Affix register(String id, Attribute attribute,
                                  AttributeModifier.Operation operation, double defaultBaseValue) {
        Affix affix = new Affix(id, attribute, operation, defaultBaseValue);
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
