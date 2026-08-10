package net.simplysmith.smith.affix;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.simplysmith.smith.quality.Quality;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/*
词条数值的显示文本

数值口径刻意与原版属性行保持一致，让词条和装备自带的属性摆在一起看时不会错位：
乘算按 ×100 显示成百分比，击退抗性按 ×10 显示，其余按原值显示。

击退抗性那一档是原版单独做的放大，不是笔误——下界合金胸甲的 0.1 在原版
属性行上写作「+1 击退抗性」。这里若按 0.05 原样显示，玩家会误判磐石比实际弱十倍。
*/
public final class AffixText {

    /*
    与原版属性行同款格式：两位小数，且强制用 ROOT 的符号集。
    不跟随系统语言，否则部分地区会把小数点显示成逗号。
    */
    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private AffixText() {
    }

    /*
    该词条在指定品质与强化等级下的实际数值

    只给数字本身，乘算补个百分号，不带正号——它是要嵌进「增加 %s 点攻击伤害」
    这类句子里的，方向由句子表达，再加个加号反而累赘。负值仍会带负号。
    */
    public static Component value(Affix affix, Quality quality, int level) {
        double shown = affix.valueFor(quality, level);

        if (affix.kind() == Affix.Kind.ETERNAL) {
            return Component.empty();
        }

        // Tooltip 展示实际生效值，而不是会在结算时被规则截断的理论值。
        if (affix.kind() == Affix.Kind.DODGE) {
            shown = Math.min(0.99D, shown);
        } else if (affix.kind() == Affix.Kind.IMMORTAL) {
            shown = Math.max(1.0D, 60.0D - Math.floor(shown));
        }

        boolean percent = switch (affix.kind()) {
            case LIFESTEAL, BREAK_ARMY, DODGE -> true;
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
}
