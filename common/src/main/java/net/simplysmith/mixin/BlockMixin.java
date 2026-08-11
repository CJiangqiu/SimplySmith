package net.simplysmith.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.simplysmith.smith.affix.FunctionalAffixEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 点石成金的结算点
@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "playerDestroy", at = @At("TAIL"))
    private void simplysmith$midasTouch(Level level, Player player, BlockPos pos, BlockState state,
                                        BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        FunctionalAffixEffects.tryMidasTouch(level, pos, state, tool);
    }
}
