package net.simplysmith.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.smith.affix.AffixRoller;
import net.simplysmith.smith.affix.FunctionalAffixEffects;
import net.simplysmith.smith.SmithData;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// 物品首次进入背包时初始化
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Unique
    private static final double SIMPLYSMITH$MOVEMENT_EPSILON = 1.0E-6D;

    @Shadow
    @Final
    public Player player;

    @Shadow
    @Final
    private List<NonNullList<ItemStack>> compartments;

    @Unique
    private boolean simplysmith$hasLastPosition;

    @Unique
    private double simplysmith$lastX;

    @Unique
    private double simplysmith$lastY;

    @Unique
    private double simplysmith$lastZ;

    @Unique
    private boolean simplysmith$affixSetInvisible;

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplysmith$stampNewItems(CallbackInfo ci) {
        // 只在服务端盖章，结果随容器同步下发给客户端
        if (player.level().isClientSide) {
            return;
        }

        for (NonNullList<ItemStack> compartment : compartments) {
            for (int i = 0; i < compartment.size(); i++) {
                ItemStack stack = compartment.get(i);
                if (SmithData.canStamp(stack)) {
                    AffixRoller.stamp(stack, player.getRandom());
                }
                FunctionalAffixEffects.keepEternal(stack);
            }
        }

        boolean moved = simplysmith$hasLastPosition
                && player.distanceToSqr(simplysmith$lastX, simplysmith$lastY, simplysmith$lastZ)
                > SIMPLYSMITH$MOVEMENT_EPSILON;
        simplysmith$lastX = player.getX();
        simplysmith$lastY = player.getY();
        simplysmith$lastZ = player.getZ();
        simplysmith$hasLastPosition = true;

        simplysmith$affixSetInvisible = FunctionalAffixEffects.tickInvisibility(
                player, moved, simplysmith$affixSetInvisible);
        FunctionalAffixEffects.tickEquipment(player);
    }
}
