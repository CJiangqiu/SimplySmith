package net.simplysmith.smith;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.simplysmith.SimplySmith;
import net.simplysmith.smith.affix.Affix;
import net.simplysmith.smith.affix.Affixes;
import net.simplysmith.smith.quality.Quality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 物品上的品质与词条数据，存在 NBT 的 simplysmith 子标签里
public final class SmithData {

    private static final String TAG_ROOT = SimplySmith.MOD_ID;
    private static final String TAG_QUALITY = "Quality";
    private static final String TAG_AFFIXES = "Affixes";
    private static final String TAG_LEVEL = "Level";

    private SmithData() {
    }

    // 是否已盖章
    public static boolean isStamped(ItemStack stack) {
        return stack.getTagElement(TAG_ROOT) != null;
    }

    public static boolean canStamp(ItemStack stack) {
        return !stack.isEmpty() && !isStamped(stack) && isGear(stack);
    }

    // 不可堆叠物品都可初始化
    public static boolean isGear(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1;
    }

    // 词条是否在该槽位生效
    public static boolean appliesTo(ItemStack stack, EquipmentSlot slot) {
        return LivingEntity.getEquipmentSlotForItem(stack) == slot;
    }

    public static void write(ItemStack stack, Quality quality, List<Affix> affixes) {
        CompoundTag root = stack.getOrCreateTagElement(TAG_ROOT);
        root.putString(TAG_QUALITY, quality.id());

        ListTag list = new ListTag();
        for (Affix affix : affixes) {
            list.add(StringTag.valueOf(affix.id().toString()));
        }
        root.put(TAG_AFFIXES, list);
    }

    // 强化等级，未强化为 0
    public static int level(ItemStack stack) {
        CompoundTag root = stack.getTagElement(TAG_ROOT);
        return root == null ? 0 : root.getInt(TAG_LEVEL);
    }

    public static void setLevel(ItemStack stack, int level) {
        stack.getOrCreateTagElement(TAG_ROOT).putInt(TAG_LEVEL, level);
    }

    public static void setQuality(ItemStack stack, Quality quality) {
        stack.getOrCreateTagElement(TAG_ROOT).putString(TAG_QUALITY, quality.id());
    }

    public static void setAffixes(ItemStack stack, List<Affix> affixes) {
        ListTag list = new ListTag();
        for (Affix affix : affixes) {
            list.add(StringTag.valueOf(affix.id().toString()));
        }
        stack.getOrCreateTagElement(TAG_ROOT).put(TAG_AFFIXES, list);
    }

    // 未盖章的物品返回普通
    public static Quality quality(ItemStack stack) {
        CompoundTag root = stack.getTagElement(TAG_ROOT);
        if (root == null) {
            return Quality.COMMON;
        }
        return Quality.byId(root.getString(TAG_QUALITY));
    }

    // 读取词条列表
    public static List<Affix> affixes(ItemStack stack) {
        CompoundTag root = stack.getTagElement(TAG_ROOT);
        if (root == null) {
            return Collections.emptyList();
        }

        ListTag list = root.getList(TAG_AFFIXES, Tag.TAG_STRING);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<Affix> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Affix affix = Affixes.byId(list.getString(i));
            if (affix != null) {
                result.add(affix);
            }
        }
        return result;
    }
}
