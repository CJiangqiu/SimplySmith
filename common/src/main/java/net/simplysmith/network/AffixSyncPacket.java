package net.simplysmith.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.Affixes;
import net.simplysmith.smith.quality.Quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
词条表同步

客户端手上没有数据包，Tooltip 又要在客户端渲染，不同步的话数据包词条那一行压根
渲染不出来——SmithData 读到客户端认不出的 id 会直接跳过。

顺带解决另一件事：数值一律由服务端算完再下发。Tooltip 原本读的是本地配置，
服务端调过而客户端没调时显示的数就是错的，所以内置词条也一并下发。

载荷是纯数值：词条名与描述走各自 Mod 的语言文件，不需要在这里传文本。
*/
public final class AffixSyncPacket {

    public static final ResourceLocation CHANNEL = new ResourceLocation(SimplySmith.MOD_ID, "affix_sync");

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplySmith.MOD_ID);

    private AffixSyncPacket() {
    }

    /*
    单条词条的下发内容

    attribute 与 operation 只有属性型词条才有；功能型词条的行为写在 Java 里，
    两端各自都有，这里只需要把数值对齐。
    */
    public record Entry(ResourceLocation id, Affix.Kind kind, ResourceLocation attribute,
                        AttributeModifier.Operation operation, double baseValue,
                        Map<Quality, Double> qualityMultipliers) {
    }

    /*
    在服务端把当前整张词条表拍成快照

    下发的是配置与自带覆盖都算完的最终值，客户端拿到就用，不再叠加任何本地来源。
    */
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

            entries.add(new Entry(affix.id(), affix.kind(), attribute, affix.operation(),
                    affix.baseValue(), multipliers));
        }
        return entries;
    }

    public static void encode(List<Entry> entries, FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());

        for (Entry entry : entries) {
            buf.writeResourceLocation(entry.id());
            buf.writeEnum(entry.kind());

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

    /*
    解码与应用分开

    缓冲区必须无条件读完，不能因为要跳过应用就提前返回——网络层会把没读干净的
    缓冲区当成错误。
    */
    public static List<Entry> decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Affix.Kind kind = buf.readEnum(Affix.Kind.class);

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

            entries.add(new Entry(id, kind, attribute, operation, baseValue, multipliers));
        }
        return entries;
    }

    /*
    应用到客户端的词条表

    内置词条两端都有，只覆盖数值；其余的按下发内容重建。
    整批替换而不是逐条增删，避免上一次连接残留的词条留在池子里。
    */
    public static void apply(List<Entry> entries) {
        Map<ResourceLocation, Affix> rebuilt = new LinkedHashMap<>();

        for (Entry entry : entries) {
            if (Affixes.isBuiltIn(entry.id())) {
                Affixes.byId(entry.id()).applyServerValues(entry.baseValue(), entry.qualityMultipliers());
                continue;
            }

            /*
            功能型词条的行为写在 Java 里，只能是内置的。
            走到这里说明服务端装的版本比本端新，本端没有对应实现，显示出来也是空壳。
            */
            if (entry.kind() != Affix.Kind.ATTRIBUTE || entry.attribute() == null) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                        + "Affix '{}' from the server is not attribute-based and has no implementation here, skipped",
                        entry.id());
                continue;
            }

            /*
            属性解析不了就跳过这一条

            正常情况下两端的 Mod 一致，走不到这里；真遇上了也只是少显示一行，
            不该让整个包处理失败。
            */
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.getOptional(entry.attribute()).orElse(null);
            if (attribute == null) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                        + "Affix '{}' from the server uses attribute '{}' which does not exist on this client, skipped",
                        entry.id(), entry.attribute());
                continue;
            }

            Affix affix = new Affix(entry.id(), attribute, entry.operation(),
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
