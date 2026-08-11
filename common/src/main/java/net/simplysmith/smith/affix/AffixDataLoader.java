package net.simplysmith.smith.affix;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import net.simplysmith.SimplySmith;
import net.simplysmith.config.SimplySmithConfig;
import net.simplysmith.smith.quality.Quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// 从数据包读取外部词条定义
public class AffixDataLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = SimplySmith.MOD_ID + "/affixes";

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplySmith.MOD_ID);
    private static final Gson GSON = new Gson();

    public AffixDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Affix> loaded = new LinkedHashMap<>();

        files.forEach((id, element) -> {
            // 禁止数据包覆盖内置词条
            if (Affixes.isBuiltIn(id)) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                        + "Affix '{}' clashes with a built-in affix and was ignored, use the config file to retune built-ins", id);
                return;
            }

            try {
                loaded.put(id, parse(id, GsonHelper.convertToJsonObject(element, "affix")));
            } catch (Exception e) {
                LOGGER.error(SimplySmith.LOG_PREFIX + "Failed to load affix '{}', skipped", id, e);
            }
        });

        Affixes.replaceDataDriven(loaded);
        LOGGER.info(SimplySmith.LOG_PREFIX + "Loaded {} datapack affixes, pool now holds {}",
                loaded.size(), Affixes.size());

        // 池子这时才算完整，配置里的数量上限到这一步才有得比
        SimplySmithConfig.get().warnIfPoolTooSmall(Affixes.size());
    }

    private static Affix parse(ResourceLocation id, JsonObject json) {
        Attribute attribute = readAttribute(json);
        AttributeModifier.Operation operation = readOperation(json);
        AffixCategory category = readCategory(json);

        double baseValue = GsonHelper.getAsDouble(json, "base_value");
        if (!Double.isFinite(baseValue)) {
            throw new IllegalArgumentException("base_value must be a finite number");
        }

        // 属性没有挂在玩家身上时只警告不拒绝
        if (!DefaultAttributes.getSupplier(EntityType.PLAYER).hasAttribute(attribute)) {
            LOGGER.warn(SimplySmith.LOG_PREFIX
                            + "Affix '{}' uses attribute '{}' which is not present on players, it will have no effect",
                    id, BuiltInRegistries.ATTRIBUTE.getKey(attribute));
        }

        return new Affix(id, category, attribute, operation, baseValue, readQualityMultipliers(json));
    }

    // 不写就是普通，即对任何装备都算在偏向池里
    private static AffixCategory readCategory(JsonObject json) {
        String raw = GsonHelper.getAsString(json, "category", AffixCategory.GENERIC.id());

        AffixCategory category = AffixCategory.find(raw);
        if (category == null) {
            throw new IllegalArgumentException(
                    "Unknown category '" + raw + "', expected weapon, tool, armor or generic");
        }
        return category;
    }

    private static Attribute readAttribute(JsonObject json) {
        String raw = GsonHelper.getAsString(json, "attribute");

        ResourceLocation key = ResourceLocation.tryParse(raw);
        if (key == null) {
            throw new IllegalArgumentException("'" + raw + "' is not a valid attribute id");
        }

        return BuiltInRegistries.ATTRIBUTE.getOptional(key).orElseThrow(
                () -> new IllegalArgumentException("Unknown attribute '" + key
                        + "', the mod providing it may be missing on this side"));
    }

    private static AttributeModifier.Operation readOperation(JsonObject json) {
        String raw = GsonHelper.getAsString(json, "operation", "addition");
        try {
            return AttributeModifier.Operation.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown operation '" + raw + "', expected addition, multiply_base or multiply_total");
        }
    }

    // 品质倍率覆盖表
    private static Map<Quality, Double> readQualityMultipliers(JsonObject json) {
        JsonObject section = GsonHelper.getAsJsonObject(json, "quality_multiplier", null);
        if (section == null) {
            return Map.of();
        }

        Map<Quality, Double> multipliers = new EnumMap<>(Quality.class);
        for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
            Quality quality = Quality.find(entry.getKey());
            if (quality == null) {
                throw new IllegalArgumentException("Unknown quality '" + entry.getKey() + "' in quality_multiplier");
            }

            double multiplier = GsonHelper.convertToDouble(entry.getValue(), entry.getKey());
            if (!Double.isFinite(multiplier)) {
                throw new IllegalArgumentException("Quality multiplier for '" + entry.getKey() + "' must be finite");
            }
            multipliers.put(quality, multiplier);
        }
        return multipliers;
    }
}
