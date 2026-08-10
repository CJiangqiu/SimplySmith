package net.simplysmith.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.simplysmith.SimplySmith;
import net.simplysmith.platform.ItemRegistrar;

import java.util.function.Supplier;

/*
Fabric 侧注册：直接写入原版注册表，注册即刻完成
*/
public final class FabricItemRegistrar implements ItemRegistrar {

    @Override
    public Supplier<Item> register(String id, Supplier<Item> factory) {
        Item item = Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(SimplySmith.MOD_ID, id), factory.get());
        return () -> item;
    }
}
