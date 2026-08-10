package net.simplysmith.smith;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.config.SimplySmithConfig;

import java.util.ArrayList;
import java.util.List;

/*
品质判定与词条抽取
*/
public final class AffixRoller {

    private AffixRoller() {
    }

    /*
    判定品质

    直接用 stack.getRarity()，即保留原版「附魔提升稀有度」的行为：
    附魔过的物品会被抬升 1~2 档。这是有意保留的，不要改成读物品固有稀有度。

    由于盖章发生在物品首次进入背包时，玩家自己给已有物品附魔并不会重新抽取，
    所以该加成实际只作用于到手时就已带附魔的物品，例如战利品箱、村民交易、怪物掉落。
    */
    public static Quality rollQuality(ItemStack stack) {
        return Quality.fromRarity(stack.getRarity());
    }

    /*
    抽取词条

    数量在该品质配置的区间内随机。抽取优先不放回，同一条词条不会重复出现；
    仅当需求数量超过词条池容量时，才允许重复补足。
    */
    public static List<Affix> rollAffixes(Quality quality, RandomSource random) {
        SimplySmithConfig config = SimplySmithConfig.get();
        int min = config.affixMin(quality);
        int max = config.affixMax(quality);

        int count = min >= max ? min : min + random.nextInt(max - min + 1);
        if (count <= 0) {
            return List.of();
        }

        List<Affix> pool = new ArrayList<>(Affixes.all());
        List<Affix> result = new ArrayList<>(count);

        while (result.size() < count) {
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

    // 判定品质并抽取词条，结果写入物品 NBT
    public static void stamp(ItemStack stack, RandomSource random) {
        Quality quality = rollQuality(stack);
        SmithData.write(stack, quality, rollAffixes(quality, random));
    }
}
