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

// 一条词条的定义
public final class Affix {

    public enum Kind {
        ATTRIBUTE,
        LIFESTEAL,
        BLOODRAGE,
        EXPERIENCE_MENDING,
        ETERNAL,
        BREAK_ARMY,
        IMMORTAL,
        DODGE,
        MINING_LEVEL,
        MINING_SPEED,
        MIDAS_TOUCH,
        CHAMELEON,
        NIGHTINGALE,
        BACKSTAB,
        SHARPSHOOTER,
        FLAME,
        FROST,
        LIGHTNING,
        POISON,
        WITHER,
        TRUE_DAMAGE
    }

    private final ResourceLocation id;
    private final Kind kind;
    private final AffixCategory category;
    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double defaultBaseValue;

    // 该词条自己的品质倍率，优先于全局配置
    private final Map<Quality, Double> qualityMultipliers;

    // 服务端下发的实际数值，只有连远程服务器的客户端才会被赋值
    private volatile Double serverBaseValue;
    private volatile Map<Quality, Double> serverQualityMultipliers;

    // 按槽位区分修饰符 UUID
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
            // 按完整词条 ID 与槽位派生 UUID
            String seed = id + ":" + slot.getName();
            modifierIds.put(slot, UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
        }
    }

    // NBT 与配置共用的稳定标识
    public ResourceLocation id() {
        return id;
    }

    // 配置文件里的键名
    public String configKey() {
        return SimplySmith.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    public Kind kind() {
        return kind;
    }

    public AffixCategory category() {
        return category;
    }

    // 该词条是否落在这件装备的偏向池里
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

    // 获取指定品质下的词条倍率
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

    // 应用服务端下发的最终数值
    public void applyServerValues(double baseValue, Map<Quality, Double> multipliers) {
        serverBaseValue = baseValue;
        serverQualityMultipliers = multipliers.isEmpty() ? null : new EnumMap<>(multipliers);
    }

    // 断开连接后恢复读本地配置
    public void clearServerValues() {
        serverBaseValue = null;
        serverQualityMultipliers = null;
    }

    // 实际数值 = 基础值 × 品质倍率 × (1 + 强化等级)
    public double valueFor(Quality quality, int level) {
        return baseValue() * qualityMultiplier(quality) * (1 + level);
    }

    public UUID modifierId(EquipmentSlot slot) {
        return modifierIds.get(slot);
    }

    public AttributeModifier createModifier(EquipmentSlot slot, Quality quality, int level) {
        return new AttributeModifier(modifierId(slot), id.toString(), valueFor(quality, level), operation);
    }

    // 词条名的翻译键，形如 affix.<命名空间>.<词条名>
    public String translationKey() {
        return Util.makeDescriptionId("affix", id);
    }

    // 效果描述的翻译键，按住 Shift 时显示在词条名下方
    public String descriptionKey() {
        return translationKey() + ".desc";
    }
}
