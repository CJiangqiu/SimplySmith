package net.simplysmith.forge;

import net.minecraft.world.item.Item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.simplysmith.SimplySmith;
import net.simplysmith.platform.ItemRegistrar;

import java.util.function.Supplier;

/*
Forge 侧注册：原版注册表在加载后是冻结的，必须经 DeferredRegister 在注册事件里提交
所以取值必须推迟到注册完成之后，RegistryObject 正是这个语义
*/
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
