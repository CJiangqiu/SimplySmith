package net.simplysmith.smith.affix;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.config.SimplySmithConfig;
import net.simplysmith.smith.quality.Quality;
import net.simplysmith.smith.SmithData;

import java.util.ArrayList;
import java.util.Collection;
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
    public static List<Affix> rollAffixes(Quality quality, AffixCategory category, RandomSource random) {
        SimplySmithConfig config = SimplySmithConfig.get();
        int min = config.affixMin(quality);
        int max = config.affixMax(quality);

        int count = min >= max ? min : min + random.nextInt(max - min + 1);
        return draw(category, count, List.of(), random);
    }

    /*
    按分类偏向抽取指定数量的词条

    每抽一条先掷一次偏向判定：命中就从该装备的偏向池抽，未命中就从全池抽。
    全池本身也包含偏向池，所以实际落在偏向池上的概率高于配置的偏向值——
    这是刻意的，分类只提高概率，不限定可能性。

    exclude 是已经在物品上的词条，用于突破补词条时避开重复。
    */
    public static List<Affix> draw(AffixCategory category, int count,
                                   Collection<Affix> exclude, RandomSource random) {
        if (count <= 0) {
            return List.of();
        }

        double bias = SimplySmithConfig.get().categoryBias();
        List<Affix> pool = new ArrayList<>(Affixes.all());
        pool.removeAll(exclude);

        List<Affix> result = new ArrayList<>(count);
        while (result.size() < count) {
            if (pool.isEmpty()) {
                // 池子抽干，重新装填以兜底重复
                pool.addAll(Affixes.all());
                if (pool.isEmpty()) {
                    break;
                }
            }

            Affix picked = pick(pool, category, bias, random);
            // 不放回是全局的：从偏向池选中的那条，也要从全池里移除
            pool.remove(picked);
            result.add(picked);
        }
        return result;
    }

    /*
    单次抽取

    偏向池为空时直接退回全池，否则偏向分支会抽不出东西——
    池子里一条该分类的词条都没有是完全合法的配置。
    */
    private static Affix pick(List<Affix> pool, AffixCategory category, double bias, RandomSource random) {
        if (bias > 0.0D && random.nextDouble() < bias) {
            List<Affix> favored = new ArrayList<>();
            for (Affix affix : pool) {
                if (affix.isFavoredBy(category)) {
                    favored.add(affix);
                }
            }
            if (!favored.isEmpty()) {
                return favored.get(random.nextInt(favored.size()));
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }

    // 判定品质并抽取词条，结果写入物品 NBT
    public static void stamp(ItemStack stack, RandomSource random) {
        Quality quality = rollQuality(stack);
        SmithData.write(stack, quality, rollAffixes(quality, AffixCategory.of(stack), random));
    }
}
