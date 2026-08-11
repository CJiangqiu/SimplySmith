package net.simplysmith.smith.affix;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.simplysmith.smith.quality.Quality;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

// 词条数值的显示文本
public final class AffixText {

    // 与原版属性行同款格式：两位小数，且强制用 ROOT 的符号集。
    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private AffixText() {
    }

    // 该词条在指定品质与强化等级下的实际数值
    public static Component value(Affix affix, Quality quality, int level) {
        double shown = affix.valueFor(quality, level);

        // 没有数值可讲的词条不给数字，描述文本里也不写 %s
        if (affix.kind() == Affix.Kind.ETERNAL || affix.kind() == Affix.Kind.CHAMELEON) {
            return Component.empty();
        }

        // Tooltip 展示实际生效值，而不是会在结算时被规则截断的理论值。
        if (affix.kind() == Affix.Kind.DODGE) {
            shown = Math.min(0.99D, shown);
        } else if (affix.kind() == Affix.Kind.IMMORTAL) {
            shown = Math.max(1.0D, 60.0D - Math.floor(shown));
        } else if (affix.kind() == Affix.Kind.MIDAS_TOUCH) {
            shown = Math.min(1.0D, shown);
        } else if (affix.kind() == Affix.Kind.MINING_LEVEL) {
            // 等级是整数，结算时向下取整，显示也得取整，不然会写出「提升 1.5 级」
            shown = Math.floor(shown);
        } else if (isDurationInSeconds(affix.kind())) {
            // 秒数同理，结算时取整，显示也取整
            shown = Math.floor(shown);
        }

        boolean percent = switch (affix.kind()) {
            case LIFESTEAL, BREAK_ARMY, DODGE, MINING_SPEED, MIDAS_TOUCH,
                 NIGHTINGALE, BACKSTAB, SHARPSHOOTER -> true;
            case ATTRIBUTE -> affix.operation() != net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION;
            default -> false;
        };

        if (percent) {
            shown *= 100.0D;
        } else if (affix.isAttribute() && affix.attribute().equals(Attributes.KNOCKBACK_RESISTANCE)) {
            shown *= 10.0D;
        }

        return Component.literal(FORMAT.format(shown) + (percent ? "%" : ""));
    }

    // 这几条的数值是持续秒数，结算时按整秒取，显示不该出现小数
    private static boolean isDurationInSeconds(Affix.Kind kind) {
        return switch (kind) {
            case FLAME, FROST, POISON, WITHER -> true;
            default -> false;
        };
    }
}
