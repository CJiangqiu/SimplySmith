package net.simplysmith.smith.affix;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.config.SimplySmithConfig;
import net.simplysmith.smith.quality.Quality;
import net.simplysmith.smith.SmithData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// 品质判定与词条抽取
public final class AffixRoller {

    private AffixRoller() {
    }

    // 判定品质
    public static Quality rollQuality(ItemStack stack) {
        return Quality.fromRarity(stack.getRarity());
    }

    // 抽取词条
    public static List<Affix> rollAffixes(Quality quality, AffixCategory category, RandomSource random) {
        SimplySmithConfig config = SimplySmithConfig.get();
        int min = config.affixMin(quality);
        int max = config.affixMax(quality);

        int count = min >= max ? min : min + random.nextInt(max - min + 1);
        return draw(category, count, List.of(), random);
    }

    // 按分类偏向抽取指定数量的词条
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

    // 单次抽取
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
