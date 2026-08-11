package net.simplysmith.smith.affix;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

// 词条分类
public enum AffixCategory {

    WEAPON("weapon"),
    TOOL("tool"),
    ARMOR("armor"),

    // 普通是中立分类，不对应任何物品类型
    GENERIC("generic");

    private final String id;

    AffixCategory(String id) {
        this.id = id;
    }

    // 数据包字段、配置键与语言文件键共用的稳定标识
    public String id() {
        return id;
    }

    // 严格查找分类
    public static AffixCategory find(String id) {
        for (AffixCategory category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return null;
    }

    // 判定一件装备属于哪一类
    public static AffixCategory of(ItemStack stack) {
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        if (slot.isArmor() || slot == EquipmentSlot.OFFHAND) {
            return ARMOR;
        }

        if (stack.getItem() instanceof DiggerItem) {
            return TOOL;
        }

        // 弓弩单列一条，不能只靠下面的攻击力判据
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
