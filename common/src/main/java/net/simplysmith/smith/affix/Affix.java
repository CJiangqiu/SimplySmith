package net.simplysmith.smith.affix;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
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
一条词条的定义

内置词条在 Affixes 里静态注册，外部词条由 AffixDataLoader 从数据包读入，
两者用的是同一个类型，除了「品质倍率覆盖」只有数据包会填之外没有区别。
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

    private final ResourceLocation id;
    private final Kind kind;
    private final AffixCategory category;
    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double defaultBaseValue;

    /*
    该词条自己的品质倍率，优先于全局配置

    数据包可以只写其中几档，没写的档在取值时回落全局配置，所以这张表允许残缺。
    内置词条恒为空表，全部走全局配置。
    */
    private final Map<Quality, Double> qualityMultipliers;

    /*
    服务端下发的实际数值，只有连远程服务器的客户端才会被赋值

    Tooltip 的数值本来读的是本地配置，服务端调过而客户端没调时显示出来的数就是错的
    （只影响显示，实际属性一直是服务端算的）。连上服务器后一律以下发值为准。

    单人游戏与局域网主机不会走到这里：两端同一个 JVM、同一份配置，
    真去覆盖反而会让游戏内配置页改完不生效。
    */
    private volatile Double serverBaseValue;
    private volatile Map<Quality, Double> serverQualityMultipliers;

    /*
    修饰符 UUID 必须按槽位区分，不能所有槽位共用一个。
    AttributeInstance 以 UUID 作主键，
    而 AttributeMap.addTransientAttributeModifiers 在添加前会先移除同 UUID 的旧值，
    所以重复 UUID 不报错、而是后者顶掉前者，五件装备的同名词条最终只有一件生效，且没有任何异常或日志。
    原版对盔甲也是按部位各配一个 UUID 来规避（ArmorItem.ARMOR_MODIFIER_UUID_PER_TYPE）
    */
    private final Map<EquipmentSlot, UUID> modifierIds = new EnumMap<>(EquipmentSlot.class);

    Affix(String path, AffixCategory category, Attribute attribute,
          AttributeModifier.Operation operation, double defaultBaseValue) {
        this(new ResourceLocation(SimplySmith.MOD_ID, path), Kind.ATTRIBUTE, category,
                attribute, operation, defaultBaseValue, Map.of());
    }

    Affix(String path, Kind kind, AffixCategory category, double defaultBaseValue) {
        this(new ResourceLocation(SimplySmith.MOD_ID, path), kind, category,
                null, null, defaultBaseValue, Map.of());
    }

    // 数据包定义与服务端下发的词条都走这个入口
    public Affix(ResourceLocation id, AffixCategory category, Attribute attribute,
                 AttributeModifier.Operation operation, double defaultBaseValue,
                 Map<Quality, Double> qualityMultipliers) {
        this(id, Kind.ATTRIBUTE, category, attribute, operation, defaultBaseValue, qualityMultipliers);
    }

    private Affix(ResourceLocation id, Kind kind, AffixCategory category, Attribute attribute,
                  AttributeModifier.Operation operation, double defaultBaseValue,
                  Map<Quality, Double> qualityMultipliers) {
        this.id = id;
        this.kind = kind;
        this.category = category;
        this.attribute = attribute;
        this.operation = operation;
        this.defaultBaseValue = defaultBaseValue;
        this.qualityMultipliers = qualityMultipliers.isEmpty()
                ? Map.of()
                : new EnumMap<>(qualityMultipliers);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // 以「词条完整 id : 槽位名」确定性派生，稳定且互不冲突，不用硬编码 UUID 常量
            String seed = id + ":" + slot.getName();
            modifierIds.put(slot, UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
        }
    }

    // NBT 与配置共用的稳定标识
    public ResourceLocation id() {
        return id;
    }

    /*
    配置文件里的键名

    内置词条沿用不带命名空间的短键，老配置文件无需迁移；
    外部词条写完整 id，写出时会被加引号，因为 TOML 的裸键不允许冒号。
    */
    public String configKey() {
        return SimplySmith.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    public Kind kind() {
        return kind;
    }

    public AffixCategory category() {
        return category;
    }

    /*
    该词条是否落在这件装备的偏向池里

    普通是中立分类，并进每一件装备的偏向池；因此普通物品的偏向池就只有普通词条。
    */
    public boolean isFavoredBy(AffixCategory itemCategory) {
        return category == itemCategory || category == AffixCategory.GENERIC;
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
        Double fromServer = serverBaseValue;
        return fromServer != null
                ? fromServer
                : SimplySmithConfig.get().affixBaseValue(configKey(), defaultBaseValue);
    }

    // 该词条在指定品质下的倍率：服务端下发值优先，其次自带覆盖，最后回落全局配置
    public double qualityMultiplier(Quality quality) {
        Map<Quality, Double> fromServer = serverQualityMultipliers;
        if (fromServer != null) {
            Double synced = fromServer.get(quality);
            if (synced != null) {
                return synced;
            }
        }

        Double own = qualityMultipliers.get(quality);
        return own != null ? own : SimplySmithConfig.get().qualityMultiplier(quality);
    }

    // 服务端已经把配置与自带覆盖都算完了，下发的是最终值，客户端不再叠加任何本地来源
    public void applyServerValues(double baseValue, Map<Quality, Double> multipliers) {
        serverBaseValue = baseValue;
        serverQualityMultipliers = multipliers.isEmpty() ? null : new EnumMap<>(multipliers);
    }

    // 断开连接后恢复读本地配置
    public void clearServerValues() {
        serverBaseValue = null;
        serverQualityMultipliers = null;
    }

    /*
    实际数值 = 基础值 × 品质倍率 × (1 + 强化等级)

    每级的增量恰好等于 +0 级时的数值，所以「每级涨幅」与品质倍率同源：
    普通每级 +100% 基础值、不凡 +150%、稀有 +200%、史诗 +300%。
    */
    public double valueFor(Quality quality, int level) {
        return baseValue() * qualityMultiplier(quality) * (1 + level);
    }

    public UUID modifierId(EquipmentSlot slot) {
        return modifierIds.get(slot);
    }

    public AttributeModifier createModifier(EquipmentSlot slot, Quality quality, int level) {
        return new AttributeModifier(modifierId(slot), id.toString(), valueFor(quality, level), operation);
    }

    /*
    词条名的翻译键，形如 affix.<命名空间>.<词条名>

    用原版给物品、方块、附魔生成翻译键的同一个工具，别的 Mod 把词条名写进自己的
    语言文件即可，不需要我方转发文本。
    */
    public String translationKey() {
        return Util.makeDescriptionId("affix", id);
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
