package net.simplysmith.smith;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.config.SimplySmithConfig;

import java.util.ArrayList;
import java.util.List;

/*
铁砧中的强化与突破

判定与产出都在这里，铁砧 mixin 只负责把两个输入槽递进来、把结果填回去。
*/
public final class SmithingRecipes {

    private SmithingRecipes() {
    }

    public enum Kind {
        ENHANCE,
        BREAKTHROUGH
    }

    /*
    识别这是哪种操作，不匹配返回 null

    要求左槽是已盖章的装备，右槽是对应的石头。
    */
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

    /*
    产出结果物品，无法执行时返回空

    突破石对已到最高品质的装备无效，此时返回空让铁砧不出结果。
    */
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
        SmithData.setAffixes(result, expandAffixes(SmithData.affixes(gear), upgraded, random));
        return result;
    }

    /*
    突破后补充词条：补到新品质的词条数量上限

    差额 = 新品质上限 - 当前词条数。补充时沿用「优先不放回」的规则，
    已有的词条不会被重复抽到；池子不够时才允许重复。
    */
    private static List<Affix> expandAffixes(List<Affix> current, Quality upgraded, RandomSource random) {
        int target = SimplySmithConfig.get().affixMax(upgraded);
        List<Affix> result = new ArrayList<>(current);
        if (result.size() >= target) {
            return result;
        }

        List<Affix> pool = new ArrayList<>(Affixes.all());
        pool.removeAll(result);

        while (result.size() < target) {
            if (pool.isEmpty()) {
                // 池子抽干，重新装填以兜底重复
                pool.addAll(Affixes.all());
                if (pool.isEmpty()) {
                    break;
                }
            }
            result.add(pool.remove(random.nextInt(pool.size())));
        }
        return result;
    }
}
