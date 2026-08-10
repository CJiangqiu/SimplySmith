package net.simplysmith.client.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.quality.Quality;
import net.simplysmith.smith.SmithData;
import net.simplysmith.smith.SmithItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.List;

/*
可选的 JEI 插件

作用是让「没看过 mod 介绍、只是在游戏里捡到石头」的玩家也能查到用法。
两条路同时铺：铁砧配方负责被查到，物品信息页负责讲清槽位图表达不了的规则。

配方挂在原版铁砧分类下，左槽铺开所有可强化的装备。这样玩家对着自己手里的
任意一件装备按「用途」也能查到，不必先注意到石头的存在。JEI 的铁砧分类会自己
新建一个 AnvilMenu 模拟一遍来算经验消耗，我方的铁砧 mixin 在客户端同样生效，
于是那里读到的消耗就是 0，不需要额外告诉 JEI 这件事。

两端的加载方式不同：Forge 扫 @JeiPlugin 注解，Fabric 只认 fabric.mod.json 里的
jei_mod_plugin 入口点，两边都配上即可，Fabric 版不扫注解所以不会重复注册。
*/
@JeiPlugin
public final class SimplySmithJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(SimplySmith.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<Item> gear = collectGear();
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();

        List<IJeiAnvilRecipe> recipes = new ArrayList<>();
        recipes.add(enhancementRecipe(factory, gear));
        addBreakthroughRecipes(factory, gear, recipes);
        registration.addRecipes(RecipeTypes.ANVIL, recipes);

        registration.addItemStackInfo(new ItemStack(SmithItems.enhancementStone()),
                info(SmithItems.ENHANCEMENT_STONE_ID));
        registration.addItemStackInfo(new ItemStack(SmithItems.breakthroughStone()),
                info(SmithItems.BREAKTHROUGH_STONE_ID));
    }

    /*
    能被强化与突破的装备

    判定条件与背包盖章完全一致，走同一个方法，避免两处各写一套而漂移。
    其他 Mod 的装备只要满足条件就会一并出现在配方里。
    */
    private static List<Item> collectGear() {
        List<Item> result = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (SmithData.canStamp(new ItemStack(item))) {
                result.add(item);
            }
        }
        return result;
    }

    /*
    强化：一条配方铺开全部装备

    强化与品质无关，所以左槽取该装备自然会得到的品质，右槽固定一个强化石。
    左右两侧数量相等且右槽只有一项时，JEI 会把左槽与产出槽联动，
    玩家聚焦某件装备就只显示那一件的强化结果。
    */
    private static IJeiAnvilRecipe enhancementRecipe(IVanillaRecipeFactory factory, List<Item> gear) {
        List<ItemStack> inputs = new ArrayList<>(gear.size());
        List<ItemStack> outputs = new ArrayList<>(gear.size());

        for (Item item : gear) {
            Quality quality = naturalQuality(item);
            inputs.add(sample(item, quality, 0));
            outputs.add(sample(item, quality, 1));
        }
        return factory.createAnvilRecipe(inputs, List.of(new ItemStack(SmithItems.enhancementStone())), outputs,
                new ResourceLocation(SimplySmith.MOD_ID, SmithItems.ENHANCEMENT_STONE_ID));
    }

    /*
    突破：每一档可突破的品质各一条配方

    档数不写死，按品质的声明顺序推导，最高档没有下一档所以跳过。
    以后新增品质时这里自动跟着变。
    */
    private static void addBreakthroughRecipes(IVanillaRecipeFactory factory, List<Item> gear,
                                               List<IJeiAnvilRecipe> recipes) {
        List<ItemStack> stone = List.of(new ItemStack(SmithItems.breakthroughStone()));

        for (Quality quality : Quality.values()) {
            if (quality.isMax()) {
                continue;
            }

            List<ItemStack> inputs = new ArrayList<>(gear.size());
            List<ItemStack> outputs = new ArrayList<>(gear.size());
            for (Item item : gear) {
                inputs.add(sample(item, quality, 0));
                outputs.add(sample(item, quality.next(), 0));
            }
            // 每条配方要一个唯一 id，用起始品质的稳定标识拼出来
            ResourceLocation uid = new ResourceLocation(SimplySmith.MOD_ID,
                    SmithItems.BREAKTHROUGH_STONE_ID + "/" + quality.id());
            recipes.add(factory.createAnvilRecipe(inputs, stone, outputs, uid));
        }
    }

    // 未盖章时读到的是物品固有稀有度，即该装备进背包时会判定到的品质
    private static Quality naturalQuality(Item item) {
        return Quality.fromRarity(new ItemStack(item).getRarity());
    }

    /*
    配方里展示用的样品

    刻意不带词条：词条是进背包时随机抽的，摆一组固定词条在配方页上会让人
    以为强化产出是定死的。词条怎么来的由物品信息页讲。
    */
    private static ItemStack sample(Item item, Quality quality, int level) {
        ItemStack stack = new ItemStack(item);
        SmithData.write(stack, quality, List.of());
        SmithData.setLevel(stack, level);
        return stack;
    }

    private static Component info(String itemId) {
        return Component.translatable("jei." + SimplySmith.MOD_ID + "." + itemId + ".info");
    }
}
