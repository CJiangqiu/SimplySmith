package net.simplysmith.smith;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.Util;

/*
品质文本样式
前六档使用单色；究极品质按时间连续改变色相，并让相邻字符错开色相形成缓慢流动的彩虹。
*/
public final class QualityText {

    // 完整流动一轮所需时间；数值越大变化越慢
    private static final double CYCLE_MILLIS = 10_000.0D;

    // 相邻字符之间的色相距离，避免整行文字同时变成同一种颜色
    private static final double CHARACTER_HUE_STEP = 0.10D;

    private QualityText() {
    }

    public static MutableComponent apply(Quality quality, Component source) {
        if (quality != Quality.ULTIMATE) {
            return source.copy().withStyle(quality.color());
        }

        String text = source.getString();
        int[] codePoints = text.codePoints().toArray();
        MutableComponent result = Component.empty();
        Style baseStyle = source.getStyle();
        double phase = Util.getMillis() % CYCLE_MILLIS / CYCLE_MILLIS;

        for (int i = 0; i < codePoints.length; i++) {
            int color = flowingColor(phase, i);
            String character = new String(Character.toChars(codePoints[i]));
            result.append(Component.literal(character)
                    .setStyle(baseStyle.withColor(TextColor.fromRgb(color))));
        }
        return result;
    }

    private static int flowingColor(double phase, int characterIndex) {
        double hue = phase - characterIndex * CHARACTER_HUE_STEP;
        hue -= Math.floor(hue);
        return hsvToRgb(hue, 0.67D, 1.0D);
    }

    private static int hsvToRgb(double hue, double saturation, double value) {
        double scaledHue = hue * 6.0D;
        int sector = (int) Math.floor(scaledHue);
        double fraction = scaledHue - sector;
        double low = value * (1.0D - saturation);
        double falling = value * (1.0D - saturation * fraction);
        double rising = value * (1.0D - saturation * (1.0D - fraction));

        double red;
        double green;
        double blue;
        switch (sector % 6) {
            case 0 -> {
                red = value;
                green = rising;
                blue = low;
            }
            case 1 -> {
                red = falling;
                green = value;
                blue = low;
            }
            case 2 -> {
                red = low;
                green = value;
                blue = rising;
            }
            case 3 -> {
                red = low;
                green = falling;
                blue = value;
            }
            case 4 -> {
                red = rising;
                green = low;
                blue = value;
            }
            default -> {
                red = value;
                green = low;
                blue = falling;
            }
        }

        return toChannel(red) << 16 | toChannel(green) << 8 | toChannel(blue);
    }

    private static int toChannel(double value) {
        return (int) Math.round(value * 255.0D);
    }
}
