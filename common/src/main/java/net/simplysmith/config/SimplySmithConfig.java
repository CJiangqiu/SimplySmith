package net.simplysmith.config;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.Affixes;
import net.simplysmith.platform.PlatformBridge;
import net.simplysmith.smith.quality.Quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

// 配置读写，对应 .minecraft/config/simplysmith.toml
public final class SimplySmithConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplySmith.MOD_ID);

    // 词条数量的合法配置区间
    public static final int MIN_AFFIX_COUNT = 0;
    public static final int MAX_AFFIX_COUNT = 64;

    private static final String FILE_NAME = SimplySmith.MOD_ID + ".toml";

    // 抽词条时偏向该装备所属分类的概率
    public static final double DEFAULT_CATEGORY_BIAS = 0.5D;

    private static final String SECTION_AFFIX_COUNT = "affix_count";
    private static final String SECTION_QUALITY_MULTIPLIER = "quality_multiplier";
    private static final String SECTION_AFFIX_BASE = "affix_base";
    private static final String KEY_CATEGORY_BIAS = "category_bias";

    private static SimplySmithConfig instance = new SimplySmithConfig();

    private final Map<Quality, Integer> affixMin = new EnumMap<>(Quality.class);
    private final Map<Quality, Integer> affixMax = new EnumMap<>(Quality.class);
    private final Map<Quality, Double> qualityMultiplier = new EnumMap<>(Quality.class);

    // 各词条在普通品质下的基础值
    private final Map<String, Double> affixBase = new LinkedHashMap<>();

    /*
    开关式词条的启用状态

    永恒与变色龙的效果与数值无关，配置项是一个布尔而不是数值，
    与基础值同处 [affix_base] 段——玩家找某条词条的设置时只需看一个地方。
    */
    private final Map<String, Boolean> affixEnabled = new LinkedHashMap<>();

    private double categoryBias = DEFAULT_CATEGORY_BIAS;

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
        // 概率，超出 0~1 会被截断
        categoryBias = Math.max(0.0D, Math.min(1.0D, toml.getDouble(KEY_CATEGORY_BIAS, categoryBias)));

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

            double multiplier = toml.getDouble(SECTION_QUALITY_MULTIPLIER + "." + quality.id(),
                    qualityMultiplier.get(quality));
            qualityMultiplier.put(quality, multiplier);
        }

        // 已知词条缺项或值写坏时回落各自的内置默认值
        for (Affix affix : Affixes.all()) {
            String key = affix.configKey();
            if (affix.hasValue()) {
                affixBase.put(key, toml.getDouble(SECTION_AFFIX_BASE + "." + key, affix.defaultBaseValue()));
            } else {
                affixEnabled.put(key, toml.getBoolean(SECTION_AFFIX_BASE + "." + key, true));
            }
        }

        // 再原样收下文件里我方还不认识的键
        for (String key : toml.keysIn(SECTION_AFFIX_BASE)) {
            if (affixBase.containsKey(key) || affixEnabled.containsKey(key)) {
                continue;
            }
            double value = toml.getDouble(SECTION_AFFIX_BASE + "." + key, Double.NaN);
            if (Double.isFinite(value)) {
                affixBase.put(key, value);
            }
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

        // 无段落键写在首个 section 之前
        sb.append("#Chance that each affix roll is drawn from the item's own category pool\n");
        sb.append("#instead of the whole pool. Generic affixes count toward every category.\n");
        sb.append("#Valid range 0.0 ~ 1.0. Set it to 0 to disable category bias entirely.\n");
        sb.append("#每次抽取词条时，从该装备所属分类的池子中抽取的概率，未命中则从全池抽取。\n");
        sb.append("#普通分类的词条计入所有装备的分类池。\n");
        sb.append("#合法范围 0.0 ~ 1.0，填 0 即完全关闭分类偏向。\n");
        sb.append(KEY_CATEGORY_BIAS).append(" = ").append(categoryBias).append("\n\n");

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
        sb.append("#Note: nimble, lifesteal, break_army and dodge are percentages: 0.1 means 10%.\n");
        sb.append("#各词条在「普通」品质下的基础数值。\n");
        sb.append("#注意：nimble（轻盈）、lifesteal（吸血）、break_army（破军）和 dodge（闪避）是百分比，0.1 表示 10%。\n");
        sb.append("#Switch-style affixes take true/false instead of a number: false disables them entirely.\n");
        sb.append("#开关式词条填 true/false 而不是数值，填 false 即完全停用该词条。\n");
        sb.append('[').append(SECTION_AFFIX_BASE).append("]\n");

        // 先按池子的顺序写已知词条，再把剩下的键补在后面
        Map<String, Double> remaining = new LinkedHashMap<>(affixBase);
        for (Affix affix : Affixes.all()) {
            String key = affix.configKey();
            sb.append(Toml.quoteIfNeeded(key)).append(" = ");

            // 开关式词条写布尔，其余写数值
            if (!affix.hasValue()) {
                sb.append(affixEnabled.getOrDefault(key, true)).append('\n');
                continue;
            }

            Double value = remaining.remove(key);
            sb.append(value != null ? value : affix.defaultBaseValue()).append('\n');
        }
        remaining.forEach((key, value) ->
                sb.append(Toml.quoteIfNeeded(key)).append(" = ").append(value).append('\n'));

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

    // 抽取数量超出池子容量时提醒一句
    public void warnIfPoolTooSmall(int poolSize) {
        for (Quality quality : Quality.values()) {
            int max = affixMax(quality);
            if (max > poolSize) {
                LOGGER.warn(SimplySmith.LOG_PREFIX
                                + "Quality '{}' allows up to {} affixes but the pool only holds {}, duplicates will appear",
                        quality.id(), max, poolSize);
            }
        }
    }

    public double qualityMultiplier(Quality quality) {
        return qualityMultiplier.getOrDefault(quality, 1.0D);
    }

    public double categoryBias() {
        return categoryBias;
    }

    public void setCategoryBias(double bias) {
        if (Double.isFinite(bias)) {
            categoryBias = Math.max(0.0D, Math.min(1.0D, bias));
        }
    }

    // 配置中缺失时回落到传入的默认值
    public double affixBaseValue(String affixId, double fallback) {
        return affixBase.getOrDefault(affixId, fallback);
    }

    // 只有开关式词条会有条目，其余词条恒为启用
    public boolean affixEnabled(String affixId) {
        return affixEnabled.getOrDefault(affixId, true);
    }

    public void setAffixEnabled(String affixId, boolean enabled) {
        affixEnabled.put(affixId, enabled);
    }
}
