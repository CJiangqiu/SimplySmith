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

// 可选的 JEI 插件
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

    // 能被强化与突破的装备
    private static List<Item> collectGear() {
        List<Item> result = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (SmithData.canStamp(new ItemStack(item))) {
                result.add(item);
            }
        }
        return result;
    }

    // 强化：一条配方铺开全部装备
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

    // 突破：每一档可突破的品质各一条配方
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

    // 配方里展示用的样品
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
