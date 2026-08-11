package net.simplysmith.smith;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.simplysmith.platform.ItemRegistrar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

// 本 Mod 注册的物品
public final class SmithItems {

    public static final String ENHANCEMENT_STONE_ID = "enhancement_stone";
    public static final String BREAKTHROUGH_STONE_ID = "breakthrough_stone";

    private static final List<Supplier<Item>> ALL = new ArrayList<>();

    private static Supplier<Item> enhancementStone;
    private static Supplier<Item> breakthroughStone;

    private SmithItems() {
    }

    public static void register(ItemRegistrar registrar) {
        ALL.clear();
        enhancementStone = add(registrar.register(ENHANCEMENT_STONE_ID,
                () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))));
        // 突破石不会被火与岩浆烧毁
        breakthroughStone = add(registrar.register(BREAKTHROUGH_STONE_ID,
                () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant())));
    }

    private static Supplier<Item> add(Supplier<Item> item) {
        ALL.add(item);
        return item;
    }

    // 本 Mod 注册的全部物品，供各平台塞进创造模式物品栏
    public static List<Supplier<Item>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Item enhancementStone() {
        return enhancementStone.get();
    }

    public static Item breakthroughStone() {
        return breakthroughStone.get();
    }
}
