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

/*
物品上的品质与词条数据，存在 NBT 的 simplysmith 子标签里

子标签存在即视为已盖章，不会被重复处理。
只挂在可损坏物品上，这类物品堆叠上限恒为 1，不会因为 NBT 差异把原本能堆叠的物品拆开。
*/
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

    /*
    能否盖章：可损坏物品即视为装备

    isDamageableItem 覆盖工具、武器、盔甲、盾牌等，其他 Mod 的装备同样满足，
    不需要维护白名单。无耐久物品与标记了 Unbreakable 的物品会被自然排除。
    */
    public static boolean canStamp(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && !isStamped(stack);
    }

    /*
    词条是否在该槽位生效

    只在物品的自然槽位生效，与原版对物品自身属性的处理保持一致：
    剑与工具只在主手（SwordItem 是 slot == MAINHAND），盔甲只在各自部位
    （ArmorItem 是 slot == type.getSlot()），盾牌归副手，鞘翅归胸部。

    不这么隔离的话，原版 Tooltip 会因为每个槽位都查到词条而把六个槽位各打印一段，
    同时玩家还能把盔甲塞进副手再吃一份词条。
    */
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

    /*
    读取词条列表

    读到已被删除或改名的词条 id 时直接跳过，避免旧存档在词条表变动后炸掉；
    数据包被卸载时同理，物品只是掉词条，不会出问题。

    写出的是带命名空间的完整 id，但读入兼容不带命名空间的写法：
    旧存档存的就是裸 id，指令和战利品表里手写裸 id 也应当能用。
    */
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
