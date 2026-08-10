package net.simplysmith.config;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.Affix;
import net.simplysmith.smith.Affixes;
import net.simplysmith.platform.PlatformBridge;
import net.simplysmith.smith.Quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
配置读写，对应 .minecraft/config/simplysmith.toml

两端共用同一份实现，所以生成的配置文件在 Fabric 与 Forge 上完全一致。
不用 Forge 的配置框架是因为那套 API 在 Fabric 端不存在。

加载策略：读到什么用什么，缺失项回落到内置默认值，加载后整份重写——
新版本新增的配置项会自动补进用户已有的文件，同时保留他们改过的值。
*/
public final class SimplySmithConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplySmith.MOD_ID);

    // 词条数量的合法配置区间
    public static final int MIN_AFFIX_COUNT = 0;
    public static final int MAX_AFFIX_COUNT = 64;

    private static final String FILE_NAME = SimplySmith.MOD_ID + ".toml";

    private static final String SECTION_AFFIX_COUNT = "affix_count";
    private static final String SECTION_QUALITY_MULTIPLIER = "quality_multiplier";
    private static final String SECTION_AFFIX_BASE = "affix_base";

    private static SimplySmithConfig instance = new SimplySmithConfig();

    private final Map<Quality, Integer> affixMin = new EnumMap<>(Quality.class);
    private final Map<Quality, Integer> affixMax = new EnumMap<>(Quality.class);
    private final Map<Quality, Double> qualityMultiplier = new EnumMap<>(Quality.class);

    // 各词条在普通品质下的基础值
    private final Map<String, Double> affixBase = new LinkedHashMap<>();

    private SimplySmithConfig() {
        applyDefaults();
    }

    public static SimplySmithConfig get() {
        return instance;
    }

    public static int defaultAffixMin(Quality quality) {
        return switch (quality) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            case LEGENDARY -> 5;
            case MYTHIC -> 6;
            case ULTIMATE -> 7;
        };
    }

    public static int defaultAffixMax(Quality quality) {
        return switch (quality) {
            case COMMON -> 3;
            case UNCOMMON -> 4;
            case RARE -> 5;
            case EPIC -> 6;
            case LEGENDARY -> 7;
            case MYTHIC -> 8;
            case ULTIMATE -> 9;
        };
    }

    public static double defaultQualityMultiplier(Quality quality) {
        return switch (quality) {
            case COMMON -> 1.0D;
            case UNCOMMON -> 1.5D;
            case RARE -> 2.0D;
            case EPIC -> 3.0D;
            case LEGENDARY -> 5.0D;
            case MYTHIC -> 7.0D;
            case ULTIMATE -> 10.0D;
        };
    }

    // 内置默认值，配置文件缺失对应项时回落到这里
    private void applyDefaults() {
        for (Quality quality : Quality.values()) {
            affixMin.put(quality, defaultAffixMin(quality));
            affixMax.put(quality, defaultAffixMax(quality));
            qualityMultiplier.put(quality, defaultQualityMultiplier(quality));
        }
    }

    // 文件不存在则以默认值生成一份
    public static void load() {
        Affixes.bootstrap();

        SimplySmithConfig config = new SimplySmithConfig();
        Path file = PlatformBridge.configDir().resolve(FILE_NAME);

        try {
            Toml toml = Toml.parse(file);
            config.readFrom(toml);
        } catch (IOException e) {
            LOGGER.error(SimplySmith.LOG_PREFIX + "Failed to read {}, falling back to default values", file, e);
        }

        instance = config;

        try {
            config.writeTo(file);
        } catch (IOException e) {
            LOGGER.error(SimplySmith.LOG_PREFIX + "Failed to write {}", file, e);
        }
    }

    private void readFrom(Toml toml) {
        for (Quality quality : Quality.values()) {
            int min = toml.getInt(SECTION_AFFIX_COUNT + "." + quality.id() + "_min", affixMin.get(quality));
            int max = toml.getInt(SECTION_AFFIX_COUNT + "." + quality.id() + "_max", affixMax.get(quality));

            min = clampCount(min);
            max = clampCount(max);
            if (min > max) {
                LOGGER.warn(SimplySmith.LOG_PREFIX + "Quality '{}' has affix min {} above max {}, min aligned to max",
                        quality.id(), min, max);
                min = max;
            }
            affixMin.put(quality, min);
            affixMax.put(quality, max);

            // 不阻断启动：抽取时先尝试不放回，池子抽干后才兜底重复
            if (max > Affixes.size()) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                                + "Quality '{}' allows up to {} affixes but the pool only holds {}, duplicates will appear",
                        quality.id(), max, Affixes.size());
            }

            double multiplier = toml.getDouble(SECTION_QUALITY_MULTIPLIER + "." + quality.id(),
                    qualityMultiplier.get(quality));
            qualityMultiplier.put(quality, multiplier);
        }

        for (Affix affix : Affixes.all()) {
            affixBase.put(affix.id(),
                    toml.getDouble(SECTION_AFFIX_BASE + "." + affix.id(), affix.defaultBaseValue()));
        }
    }

    private static int clampCount(int value) {
        return Math.max(MIN_AFFIX_COUNT, Math.min(MAX_AFFIX_COUNT, value));
    }

    private void writeTo(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append("#SimplySmith configuration\n");
        sb.append("#This file is rewritten on every startup: new entries are added automatically,\n");
        sb.append("#while values you edited are preserved. Delete an entry to restore its default.\n");
        sb.append("#SimplySmith 配置文件\n");
        sb.append("#本文件在每次启动时重写：新增的配置项会自动补全，你改过的值会保留。\n");
        sb.append("#删除任意一项即可恢复该项的默认值。\n\n");

        sb.append("#Number of affixes rolled for each quality, picked at random within min~max.\n");
        sb.append("#Valid range ").append(MIN_AFFIX_COUNT).append(" ~ ").append(MAX_AFFIX_COUNT)
                .append(", values outside are clamped.\n");
        sb.append("#The affix pool currently holds ").append(Affixes.size())
                .append(" entries; a max above that will produce duplicate affixes.\n");
        sb.append("#各品质随机抽取的词条数量区间（在 min~max 之间随机取值）。\n");
        sb.append("#合法范围 ").append(MIN_AFFIX_COUNT).append(" ~ ").append(MAX_AFFIX_COUNT)
                .append("，超出会被自动截断。\n");
        sb.append("#当前词条池共 ").append(Affixes.size()).append(" 条；上限超过该数时，超出部分会出现重复词条。\n");
        sb.append('[').append(SECTION_AFFIX_COUNT).append("]\n");
        for (Quality quality : Quality.values()) {
            sb.append(quality.id()).append("_min = ").append(affixMin.get(quality)).append('\n');
            sb.append(quality.id()).append("_max = ").append(affixMax.get(quality)).append('\n');
        }
        sb.append('\n');

        sb.append("#Value multiplier per quality: actual value = base value in [")
                .append(SECTION_AFFIX_BASE).append("] x this multiplier.\n");
        sb.append("#各品质的词条数值倍率：实际数值 = 下方 ").append(SECTION_AFFIX_BASE).append(" 的基础值 × 此倍率。\n");
        sb.append('[').append(SECTION_QUALITY_MULTIPLIER).append("]\n");
        for (Quality quality : Quality.values()) {
            sb.append(quality.id()).append(" = ").append(qualityMultiplier.get(quality)).append('\n');
        }
        sb.append('\n');

        sb.append("#Base value of each affix at Common quality.\n");
        sb.append("#Note: nimble is a percentage bonus, 0.1 means +10%; all others are flat bonuses.\n");
        sb.append("#各词条在「普通」品质下的基础数值。\n");
        sb.append("#注意：nimble（轻盈）是百分比加成，0.1 表示 +10%；其余均为固定数值加成。\n");
        sb.append('[').append(SECTION_AFFIX_BASE).append("]\n");
        for (Affix affix : Affixes.all()) {
            sb.append(affix.id()).append(" = ")
                    .append(affixBase.getOrDefault(affix.id(), affix.defaultBaseValue())).append('\n');
        }

        Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    // 保存当前运行时配置；游戏内配置页与启动加载共用同一种文件格式
    public void save() {
        Path file = PlatformBridge.configDir().resolve(FILE_NAME);
        try {
            writeTo(file);
        } catch (IOException e) {
            LOGGER.error(SimplySmith.LOG_PREFIX + "Failed to write {}", file, e);
        }
    }

    public void setAffixRange(Quality quality, int min, int max) {
        int safeMin = clampCount(min);
        int safeMax = clampCount(max);
        if (safeMin > safeMax) {
            safeMin = safeMax;
        }
        affixMin.put(quality, safeMin);
        affixMax.put(quality, safeMax);
    }

    public void setQualityMultiplier(Quality quality, double multiplier) {
        if (Double.isFinite(multiplier)) {
            qualityMultiplier.put(quality, multiplier);
        }
    }

    public void setAffixBaseValue(String affixId, double value) {
        if (Affixes.byId(affixId) != null && Double.isFinite(value)) {
            affixBase.put(affixId, value);
        }
    }

    public int affixMin(Quality quality) {
        return affixMin.getOrDefault(quality, 0);
    }

    public int affixMax(Quality quality) {
        return affixMax.getOrDefault(quality, 0);
    }

    public double qualityMultiplier(Quality quality) {
        return qualityMultiplier.getOrDefault(quality, 1.0D);
    }

    // 配置中缺失时回落到传入的默认值
    public double affixBaseValue(String affixId, double fallback) {
        return affixBase.getOrDefault(affixId, fallback);
    }
}
