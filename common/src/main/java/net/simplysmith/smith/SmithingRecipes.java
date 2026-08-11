package net.simplysmith.smith;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.config.SimplySmithConfig;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.AffixCategory;
import net.simplysmith.smith.affix.AffixRoller;
import net.simplysmith.smith.quality.Quality;

import java.util.ArrayList;
import java.util.List;

// 铁砧中的强化与突破
public final class SmithingRecipes {

    private SmithingRecipes() {
    }

    public enum Kind {
        ENHANCE,
        BREAKTHROUGH
    }

    // 识别这是哪种操作，不匹配返回 null
    public static Kind identify(ItemStack gear, ItemStack material) {
        if (gear.isEmpty() || material.isEmpty() || !SmithData.isStamped(gear)) {
            return null;
        }
        if (material.is(SmithItems.enhancementStone())) {
            return Kind.ENHANCE;
        }
        if (material.is(SmithItems.breakthroughStone())) {
            return Kind.BREAKTHROUGH;
        }
        return null;
    }

    // 产出结果物品，无法执行时返回空
    public static ItemStack assemble(ItemStack gear, Kind kind, RandomSource random) {
        ItemStack result = gear.copy();

        if (kind == Kind.ENHANCE) {
            SmithData.setLevel(result, SmithData.level(result) + 1);
            return result;
        }

        Quality quality = SmithData.quality(gear);
        if (quality.isMax()) {
            return ItemStack.EMPTY;
        }

        Quality upgraded = quality.next();
        SmithData.setQuality(result, upgraded);
        SmithData.setAffixes(result, expandAffixes(gear, SmithData.affixes(gear), upgraded, random));
        return result;
    }

    // 突破后补充词条：补到新品质的词条数量上限
    private static List<Affix> expandAffixes(ItemStack gear, List<Affix> current,
                                             Quality upgraded, RandomSource random) {
        int target = SimplySmithConfig.get().affixMax(upgraded);
        List<Affix> result = new ArrayList<>(current);
        if (result.size() >= target) {
            return result;
        }

        result.addAll(AffixRoller.draw(AffixCategory.of(gear), target - result.size(), current, random));
        return result;
    }
}
