package net.simplysmith.forge;

import net.minecraft.world.item.Item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.simplysmith.SimplySmith;
import net.simplysmith.platform.ItemRegistrar;

import java.util.function.Supplier;

// Forge 侧物品注册
public final class ForgeItemRegistrar implements ItemRegistrar {

    private final DeferredRegister<Item> items =
            DeferredRegister.create(ForgeRegistries.ITEMS, SimplySmith.MOD_ID);

    public void attach(IEventBus modEventBus) {
        items.register(modEventBus);
    }

    @Override
    public Supplier<Item> register(String id, Supplier<Item> factory) {
        RegistryObject<Item> holder = items.register(id, factory);
        return holder;
    }
}
