package net.simplysmith.client.config;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.simplysmith.SimplySmith;
import net.simplysmith.config.SimplySmithConfig;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.AffixText;
import net.simplysmith.smith.affix.Affixes;
import net.simplysmith.smith.quality.Quality;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

// 可选的 Cloth Config 配置页面
public final class SimplySmithConfigScreen {

    private SimplySmithConfigScreen() {
    }

    public static Screen create(Screen parent) {
        SimplySmithConfig config = SimplySmithConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.simplysmith.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        Map<Quality, Integer> affixMin = new EnumMap<>(Quality.class);
        Map<Quality, Integer> affixMax = new EnumMap<>(Quality.class);
        Map<Quality, Double> multipliers = new EnumMap<>(Quality.class);
        Map<String, Double> affixBaseValues = new LinkedHashMap<>();
        Map<String, Boolean> affixToggles = new LinkedHashMap<>();
        // 单值项，用一格数组在 lambda 之间传递
        double[] categoryBias = { config.categoryBias() };

        addAffixCountEntries(builder, entries, config, affixMin, affixMax);
        addQualityMultiplierEntries(builder, entries, config, multipliers);
        addAffixBaseEntries(builder, entries, config, affixBaseValues, affixToggles);
        addCategoryBiasEntry(builder, entries, config, categoryBias);

        builder.setSavingRunnable(() -> {
            for (Quality quality : Quality.values()) {
                config.setAffixRange(quality, affixMin.get(quality), affixMax.get(quality));
                config.setQualityMultiplier(quality, multipliers.get(quality));
            }
            affixBaseValues.forEach(config::setAffixBaseValue);
            affixToggles.forEach(config::setAffixEnabled);
            config.setCategoryBias(categoryBias[0]);
            config.save();
        });
        return builder.build();
    }

    private static void addCategoryBiasEntry(ConfigBuilder builder, ConfigEntryBuilder entries,
                                             SimplySmithConfig config, double[] categoryBias) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.simplysmith.category.affix_category"));

        category.addEntry(entries.startDoubleField(
                        Component.translatable("config.simplysmith.affix_category.bias"), categoryBias[0])
                .setDefaultValue(SimplySmithConfig.DEFAULT_CATEGORY_BIAS)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Component.translatable("config.simplysmith.affix_category.bias.tooltip"))
                .setSaveConsumer(value -> categoryBias[0] = value)
                .build());
    }

    private static void addAffixCountEntries(ConfigBuilder builder, ConfigEntryBuilder entries,
                                              SimplySmithConfig config, Map<Quality, Integer> affixMin,
                                              Map<Quality, Integer> affixMax) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.simplysmith.category.affix_count"));

        for (Quality quality : Quality.values()) {
            Component qualityName = qualityName(quality);
            affixMin.put(quality, config.affixMin(quality));
            affixMax.put(quality, config.affixMax(quality));

            category.addEntry(entries.startIntField(
                            Component.translatable("config.simplysmith.affix_count.min", qualityName),
                            config.affixMin(quality))
                    .setDefaultValue(SimplySmithConfig.defaultAffixMin(quality))
                    .setMin(SimplySmithConfig.MIN_AFFIX_COUNT)
                    .setMax(SimplySmithConfig.MAX_AFFIX_COUNT)
                    .setSaveConsumer(value -> affixMin.put(quality, value))
                    .build());
            category.addEntry(entries.startIntField(
                            Component.translatable("config.simplysmith.affix_count.max", qualityName),
                            config.affixMax(quality))
                    .setDefaultValue(SimplySmithConfig.defaultAffixMax(quality))
                    .setMin(SimplySmithConfig.MIN_AFFIX_COUNT)
                    .setMax(SimplySmithConfig.MAX_AFFIX_COUNT)
                    .setSaveConsumer(value -> affixMax.put(quality, value))
                    .build());
        }
    }

    private static void addQualityMultiplierEntries(ConfigBuilder builder, ConfigEntryBuilder entries,
                                                     SimplySmithConfig config,
                                                     Map<Quality, Double> multipliers) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.simplysmith.category.quality_multiplier"));

        for (Quality quality : Quality.values()) {
            multipliers.put(quality, config.qualityMultiplier(quality));
            category.addEntry(entries.startDoubleField(qualityName(quality), config.qualityMultiplier(quality))
                    .setDefaultValue(SimplySmithConfig.defaultQualityMultiplier(quality))
                    .setSaveConsumer(value -> multipliers.put(quality, value))
                    .build());
        }
    }

    private static void addAffixBaseEntries(ConfigBuilder builder, ConfigEntryBuilder entries,
                                            SimplySmithConfig config, Map<String, Double> affixBaseValues,
                                            Map<String, Boolean> affixToggles) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.simplysmith.category.affix_base"));

        // 没进世界时数据包词条尚未加载，这里只列得出内置的那些。
        for (Affix affix : Affixes.all()) {
            String key = affix.configKey();

            // 开关式词条给一个勾选框，它们的效果与数值无关
            if (!affix.hasValue()) {
                affixToggles.put(key, config.affixEnabled(key));
                category.addEntry(entries.startBooleanToggle(
                                Component.translatable(affix.translationKey()), config.affixEnabled(key))
                        .setDefaultValue(true)
                        .setTooltip(affixDescription(affix))
                        .setSaveConsumer(value -> affixToggles.put(key, value))
                        .build());
                continue;
            }

            double currentValue = config.affixBaseValue(key, affix.defaultBaseValue());
            affixBaseValues.put(key, currentValue);
            category.addEntry(entries.startDoubleField(Component.translatable(affix.translationKey()), currentValue)
                    .setDefaultValue(affix.defaultBaseValue())
                    .setTooltip(affixDescription(affix))
                    .setSaveConsumer(value -> affixBaseValues.put(key, value))
                    .build());
        }
    }

    /*
    悬浮提示直接复用词条在 Tooltip 上的那句描述，不另写一份文案

    描述里的 %s 用普通品质、+0 级的数值填——这一栏编辑的正是这个基础值，
    所以填进去的数字与输入框里的含义一致。没有数值可讲的词条（永恒、变色龙）
    描述本身就不带 %s，多传的参数会被忽略。
    */
    private static Component affixDescription(Affix affix) {
        return Component.translatable(affix.descriptionKey(), AffixText.value(affix, Quality.COMMON, 0));
    }

    private static Component qualityName(Quality quality) {
        return Component.translatable("quality." + SimplySmith.MOD_ID + "." + quality.id());
    }
}
