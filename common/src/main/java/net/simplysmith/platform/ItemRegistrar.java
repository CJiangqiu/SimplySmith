package net.simplysmith.platform;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

// 物品注册的平台抽象
public interface ItemRegistrar {

    // 登记一个待注册物品
    Supplier<Item> register(String id, Supplier<Item> factory);
}
