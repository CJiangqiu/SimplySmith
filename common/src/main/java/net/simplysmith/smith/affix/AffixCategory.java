package net.simplysmith.smith.affix;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

/*
词条分类

分类只影响抽取概率，不影响可能性——统一池的设定没变，剑照样抽得到盔甲类词条，
只是变少了。别把它当成「限定某类装备才能出某类词条」。
*/
public enum AffixCategory {

    WEAPON("weapon"),
    TOOL("tool"),
    ARMOR("armor"),

    /*
    普通是中立分类，不对应任何物品类型

    它会并进每一件装备的偏向池：剑的偏向池是「武器 + 普通」，镐是「工具 + 普通」。
    反过来，判定不出武器/工具/盔甲的物品（盾、鞘翅、钓竿）偏向池就只有普通。
    */
    GENERIC("generic");

    private final String id;

    AffixCategory(String id) {
        this.id = id;
    }

    // 数据包字段、配置键与语言文件键共用的稳定标识
    public String id() {
        return id;
    }

    // 严格查找，找不到返回 null；解析数据包时用，拼错要报错而不是静默落进普通
    public static AffixCategory find(String id) {
        for (AffixCategory category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return null;
    }

    /*
    判定一件装备属于哪一类

    自上而下先中者胜：盔甲位或副手位 → 能挖方块的是工具 → 弓弩是武器 →
    主手带攻击力的是武器 → 其余普通。斧子在工具那条就被拦下，算工具。

    副手位归盔甲是为了收下盾牌：原版实现 Equipable 的只有盔甲、鞘翅、盾牌三类，
    其中只有盾牌返回副手，所以这个判据不会误伤别的东西；其他 Mod 的盾牌只要声明了
    自己戴在副手，同样会被收进来，不需要按类名适配。

    取的是物品自身的固有修饰符，不是 ItemStack 上的——我方的词条属性正是通过
    ItemStack#getAttributeModifiers 注入的，用那个会让一面挂了「力量」的盾牌
    被判成武器，突破补词条时分类还会跟着变。
    */
    public static AffixCategory of(ItemStack stack) {
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        if (slot.isArmor() || slot == EquipmentSlot.OFFHAND) {
            return ARMOR;
        }

        if (stack.getItem() instanceof DiggerItem) {
            return TOOL;
        }

        /*
        弓弩单列一条，不能只靠下面的攻击力判据

        它们的伤害由弹射物结算，物品本身一个攻击力修饰符都没有，
        光看属性会被判成普通。
        */
        if (stack.getItem() instanceof ProjectileWeaponItem) {
            return WEAPON;
        }

        if (stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND)
                .containsKey(Attributes.ATTACK_DAMAGE)) {
            return WEAPON;
        }

        return GENERIC;
    }
}
