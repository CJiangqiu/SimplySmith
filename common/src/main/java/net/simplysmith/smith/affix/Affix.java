package net.simplysmith.smith.affix;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.simplysmith.SimplySmith;
import net.simplysmith.config.SimplySmithConfig;
import net.simplysmith.smith.quality.Quality;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/*
一条属性型词条的定义
*/
public final class Affix {

    public enum Kind {
        ATTRIBUTE,
        LIFESTEAL,
        BLOODRAGE,
        EXPERIENCE_MENDING,
        ETERNAL,
        BREAK_ARMY,
        IMMORTAL,
        DODGE
    }

    private final String id;
    private final Kind kind;
    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double defaultBaseValue;

    /*
    修饰符 UUID 必须按槽位区分，不能所有槽位共用一个。
    AttributeInstance 以 UUID 作主键，
    而 AttributeMap.addTransientAttributeModifiers 在添加前会先移除同 UUID 的旧值，
    所以重复 UUID 不报错、而是后者顶掉前者，五件装备的同名词条最终只有一件生效，且没有任何异常或日志。
    原版对盔甲也是按部位各配一个 UUID 来规避（ArmorItem.ARMOR_MODIFIER_UUID_PER_TYPE）
    */
    private final Map<EquipmentSlot, UUID> modifierIds = new EnumMap<>(EquipmentSlot.class);

    Affix(String id, Attribute attribute, AttributeModifier.Operation operation, double defaultBaseValue) {
        this(id, Kind.ATTRIBUTE, attribute, operation, defaultBaseValue);
    }

    Affix(String id, Kind kind, double defaultBaseValue) {
        this(id, kind, null, null, defaultBaseValue);
    }

    private Affix(String id, Kind kind, Attribute attribute, AttributeModifier.Operation operation, double defaultBaseValue) {
        this.id = id;
        this.kind = kind;
        this.attribute = attribute;
        this.operation = operation;
        this.defaultBaseValue = defaultBaseValue;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // 以「modid:词条id:槽位名」确定性派生，稳定且互不冲突，不用硬编码 UUID 常量
            String seed = SimplySmith.MOD_ID + ":" + id + ":" + slot.getName();
            modifierIds.put(slot, UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
        }
    }

    // NBT、配置键、语言文件键共用的稳定标识
    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isAttribute() {
        return kind == Kind.ATTRIBUTE;
    }

    public Attribute attribute() {
        return attribute;
    }

    public AttributeModifier.Operation operation() {
        return operation;
    }

    // 内置默认值，只在配置缺失该项时用
    public double defaultBaseValue() {
        return defaultBaseValue;
    }

    // 当前生效的基础值，即普通品质下的数值
    public double baseValue() {
        return SimplySmithConfig.get().affixBaseValue(id, defaultBaseValue);
    }

    /*
    实际数值 = 基础值 × 品质倍率 × (1 + 强化等级)

    每级的增量恰好等于 +0 级时的数值，所以「每级涨幅」与品质倍率同源：
    普通每级 +100% 基础值、不凡 +150%、稀有 +200%、史诗 +300%。
    */
    public double valueFor(Quality quality, int level) {
        return baseValue() * SimplySmithConfig.get().qualityMultiplier(quality) * (1 + level);
    }

    public UUID modifierId(EquipmentSlot slot) {
        return modifierIds.get(slot);
    }

    public AttributeModifier createModifier(EquipmentSlot slot, Quality quality, int level) {
        return new AttributeModifier(modifierId(slot), SimplySmith.MOD_ID + ":" + id,
                valueFor(quality, level), operation);
    }

    public String translationKey() {
        return "affix." + SimplySmith.MOD_ID + "." + id;
    }

    /*
    效果描述的翻译键，按住 Shift 时显示在词条名下方

    对应的语言文本用 %s 接收实时数值，例如「增加 %s 点攻击伤害」。
    描述由每条词条各自撰写，功能型词条可以在这里讲清自己的机制；
    没有数值可讲的词条把 %s 省掉即可，多余的参数不会被展开。
    */
    public String descriptionKey() {
        return translationKey() + ".desc";
    }
}
