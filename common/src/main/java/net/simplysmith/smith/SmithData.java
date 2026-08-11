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
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
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

/*
物品上的品质与词条数据，存在 NBT 的 simplysmith 子标签里

子标签存在即视为已盖章，不会被重复处理。
只挂在堆叠上限为 1 的物品上，不会因为 NBT 差异把原本能堆叠的物品拆开，判据见 isGear。
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

    public static boolean canStamp(ItemStack stack) {
        return !stack.isEmpty() && !isStamped(stack) && isGear(stack);
    }

    /*
    是否算作装备

    不能只看耐久：有些 Mod 的工具与武器是无法破坏的，耐久上限为 0 或者干脆重写了
    掉耐久的方法，只认耐久会把它们整批漏掉。所以下面几条判据取并集，命中任意一条即可。

    标签与基类两条都留着是有原因的：标签能收下不继承原版基类、但正确打了标签的物品；
    基类能收下没打标签、却确实是那个东西的物品。两边互相补漏，不需要维护白名单，
    也不需要针对具体 Mod 写适配。
    */
    public static boolean isGear(ItemStack stack) {
        /*
        前置：只处理不可堆叠的物品

        盖章要写 NBT，而 NBT 不同的物品无法互相合并。对可堆叠物品盖章会把原本能叠起来的
        物品拆成一格一个。真正的装备堆叠上限本来就是 1，这条不会误伤，
        但能挡住雕刻南瓜、生物头颅这类能戴、却堆叠 64 的物品。
        */
        if (stack.getMaxStackSize() != 1) {
            return false;
        }

        // 有耐久的一律算，这条覆盖面最广
        if (stack.getMaxDamage() > 0) {
            return true;
        }

        // 能穿戴的：盔甲、鞘翅归各自部位，盾牌归副手，都不是主手
        if (LivingEntity.getEquipmentSlotForItem(stack) != EquipmentSlot.MAINHAND) {
            return true;
        }

        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof DiggerItem
                || item instanceof ProjectileWeaponItem) {
            return true;
        }

        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.TOOLS)
                || stack.is(ItemTags.TRIMMABLE_ARMOR)) {
            return true;
        }

        // 兜底：主手能打出伤害的就算武器，收下不继承原版基类也没打标签的那些
        return attackDamage(stack) > 0.0D;
    }

    /*
    物品自身的主手攻击力

    取的是物品的固有修饰符而不是 ItemStack 上的——后者正是我方注入词条属性的出口，
    用它会让一件抽到「力量」的物品凭空满足武器判据。
    */
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
