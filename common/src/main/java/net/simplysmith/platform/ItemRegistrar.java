package net.simplysmith.platform;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/*
物品注册的平台抽象

两端的注册方式不通用：Forge 的原版注册表在加载后是冻结的，必须走 DeferredRegister
在注册事件里提交；Fabric 则直接调 Registry.register。所以 common 层只声明要注册什么，
真正的提交动作由各平台入口实现。
*/
public interface ItemRegistrar {

    /*
    登记一个待注册物品

    返回的 Supplier 在注册完成前不保证可用，取值一律推迟到实际需要时——
    Forge 的 DeferredRegister 就是这个语义。
    */
    Supplier<Item> register(String id, Supplier<Item> factory);
}
