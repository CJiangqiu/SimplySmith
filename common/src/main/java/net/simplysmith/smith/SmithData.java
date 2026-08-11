package net.simplysmith.smith;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;

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

    // 标签、基类、正攻击力或耐久命中任意一项即可初始化
    public static boolean isGear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.is(ItemTags.TOOLS) || stack.is(ItemTags.TRIMMABLE_ARMOR)) {
            return true;
        }

        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof DiggerItem || stack.getItem() instanceof ProjectileWeaponItem) {
            return true;
        }

        return attackDamage(stack) > 0.0D || stack.isDamageableItem();
    }

    private static double attackDamage(ItemStack stack) {
        double total = 0.0D;
        for (AttributeModifier modifier : stack.getItem()
                .getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += modifier.getAmount();
            }
        }
        return total;
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

        /*
        被配置关掉的词条一并跳过

        这是读取物品词条的唯一出口，属性注入、效果结算与 Tooltip 都经过它，
        所以在这里拦一次就等于「关了就彻底不生效、也不显示」。
        NBT 本身不动，重新开启后原来的词条照旧回来。
        */
        List<Affix> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Affix affix = Affixes.byId(list.getString(i));
            if (affix != null && affix.isEnabled()) {
                result.add(affix);
            }
        }
        return result;
    }
}
