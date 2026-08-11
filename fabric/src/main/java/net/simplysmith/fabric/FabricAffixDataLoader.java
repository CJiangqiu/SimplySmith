package net.simplysmith.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import net.minecraft.resources.ResourceLocation;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.AffixDataLoader;

// Fabric 侧的词条加载器
public final class FabricAffixDataLoader extends AffixDataLoader implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(SimplySmith.MOD_ID, "affixes");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
