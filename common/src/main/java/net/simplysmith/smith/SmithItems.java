package net.simplysmith.smith;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.simplysmith.platform.ItemRegistrar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/*
本 Mod 注册的物品

注册在平台入口调用 register 时完成，之后才能通过下面的 Supplier 取值。
*/
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

    /*
    本 Mod 注册的全部物品，供各平台塞进创造模式物品栏

    塞进标签页的动作两端不通用（Forge 走 mod 事件总线的事件，Fabric 走 fabric-api），
    所以 common 只负责报出有哪些物品，实际挂载在各自平台入口完成。
    */
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
