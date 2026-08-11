package net.simplysmith.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.AffixCategory;
import net.simplysmith.smith.affix.Affixes;
import net.simplysmith.smith.quality.Quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 词条表同步
public final class AffixSyncPacket {

    public static final ResourceLocation CHANNEL = new ResourceLocation(SimplySmith.MOD_ID, "affix_sync");

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplySmith.MOD_ID);

    private AffixSyncPacket() {
    }

    // 单条词条的下发内容
    public record Entry(ResourceLocation id, Affix.Kind kind, AffixCategory category,
                        ResourceLocation attribute, AttributeModifier.Operation operation,
                        double baseValue, Map<Quality, Double> qualityMultipliers) {
    }

    // 在服务端把当前整张词条表拍成快照
    public static List<Entry> snapshot() {
        List<Affix> pool = Affixes.all();
        List<Entry> entries = new ArrayList<>(pool.size());

        for (Affix affix : pool) {
            Map<Quality, Double> multipliers = new EnumMap<>(Quality.class);
            for (Quality quality : Quality.values()) {
                multipliers.put(quality, affix.qualityMultiplier(quality));
            }

            ResourceLocation attribute = affix.isAttribute() && affix.attribute() != null
                    ? BuiltInRegistries.ATTRIBUTE.getKey(affix.attribute())
                    : null;

            entries.add(new Entry(affix.id(), affix.kind(), affix.category(), attribute,
                    affix.operation(), affix.baseValue(), multipliers));
        }
        return entries;
    }

    public static void encode(List<Entry> entries, FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());

        for (Entry entry : entries) {
            buf.writeResourceLocation(entry.id());
            buf.writeEnum(entry.kind());
            buf.writeEnum(entry.category());

            buf.writeBoolean(entry.attribute() != null);
            if (entry.attribute() != null) {
                buf.writeResourceLocation(entry.attribute());
                buf.writeEnum(entry.operation());
            }

            // 品质倍率按 Quality 声明序写满，省掉一份档位名，也免得漏档
            buf.writeDouble(entry.baseValue());
            for (Quality quality : Quality.values()) {
                buf.writeDouble(entry.qualityMultipliers().get(quality));
            }
        }
    }

    // 解码与应用分开
    public static List<Entry> decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Affix.Kind kind = buf.readEnum(Affix.Kind.class);
            AffixCategory category = buf.readEnum(AffixCategory.class);

            ResourceLocation attribute = null;
            AttributeModifier.Operation operation = null;
            if (buf.readBoolean()) {
                attribute = buf.readResourceLocation();
                operation = buf.readEnum(AttributeModifier.Operation.class);
            }

            double baseValue = buf.readDouble();
            Map<Quality, Double> multipliers = new EnumMap<>(Quality.class);
            for (Quality quality : Quality.values()) {
                multipliers.put(quality, buf.readDouble());
            }

            entries.add(new Entry(id, kind, category, attribute, operation, baseValue, multipliers));
        }
        return entries;
    }

    // 应用到客户端的词条表
    public static void apply(List<Entry> entries) {
        Map<ResourceLocation, Affix> rebuilt = new LinkedHashMap<>();

        for (Entry entry : entries) {
            if (Affixes.isBuiltIn(entry.id())) {
                Affixes.byId(entry.id()).applyServerValues(entry.baseValue(), entry.qualityMultipliers());
                continue;
            }

            // 功能型词条的行为写在 Java 里，只能是内置的。
            if (entry.kind() != Affix.Kind.ATTRIBUTE || entry.attribute() == null) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                        + "Affix '{}' from the server is not attribute-based and has no implementation here, skipped",
                        entry.id());
                continue;
            }

            // 属性解析不了就跳过这一条
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.getOptional(entry.attribute()).orElse(null);
            if (attribute == null) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                        + "Affix '{}' from the server uses attribute '{}' which does not exist on this client, skipped",
                        entry.id(), entry.attribute());
                continue;
            }

            Affix affix = new Affix(entry.id(), entry.category(), attribute, entry.operation(),
                    entry.baseValue(), entry.qualityMultipliers());
            affix.applyServerValues(entry.baseValue(), entry.qualityMultipliers());
            rebuilt.put(entry.id(), affix);
        }

        Affixes.replaceDataDriven(rebuilt);
    }

    // 断开连接后清干净：数据包词条移除，内置词条恢复读本地配置
    public static void reset() {
        Affixes.replaceDataDriven(Map.of());
        for (Affix affix : Affixes.all()) {
            affix.clearServerValues();
        }
    }
}
