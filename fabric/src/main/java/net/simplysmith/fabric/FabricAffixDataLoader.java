package net.simplysmith.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import net.minecraft.resources.ResourceLocation;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.AffixDataLoader;

/*
Fabric 侧的词条加载器

逻辑全在 common 的 AffixDataLoader 里，这里只补 Fabric 注册重载监听器时要求的标识。
*/
public final class FabricAffixDataLoader extends AffixDataLoader implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(SimplySmith.MOD_ID, "affixes");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
