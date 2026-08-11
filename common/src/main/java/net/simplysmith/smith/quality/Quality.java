package net.simplysmith.smith.quality;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

// 物品品质
public enum Quality {

    COMMON("common", Rarity.COMMON, ChatFormatting.WHITE),
    UNCOMMON("uncommon", Rarity.UNCOMMON, ChatFormatting.YELLOW),
    RARE("rare", Rarity.RARE, ChatFormatting.AQUA),
    EPIC("epic", Rarity.EPIC, ChatFormatting.LIGHT_PURPLE),
    LEGENDARY("legendary", Rarity.EPIC, ChatFormatting.GOLD),
    MYTHIC("mythic", Rarity.EPIC, ChatFormatting.RED),
    ULTIMATE("ultimate", Rarity.EPIC, ChatFormatting.WHITE);

    private final String id;
    private final Rarity vanillaRarity;
    private final ChatFormatting color;

    Quality(String id, Rarity vanillaRarity, ChatFormatting color) {
        this.id = id;
        this.vanillaRarity = vanillaRarity;
        this.color = color;
    }

    // NBT、配置键、语言文件键共用的稳定标识
    public String id() {
        return id;
    }

    public Rarity vanillaRarity() {
        return vanillaRarity;
    }

    // 单色品质的配色；究极由 QualityText 生成彩虹渐变，此处的白色只作兜底
    public ChatFormatting color() {
        return color;
    }

    // 其他 Mod 的装备只要用了原版四档稀有度就会在这里被自动识别。
    public static Quality fromRarity(Rarity rarity) {
        if (rarity == Rarity.UNCOMMON) {
            return UNCOMMON;
        }
        if (rarity == Rarity.RARE) {
            return RARE;
        }
        if (rarity == Rarity.EPIC) {
            return EPIC;
        }
        // 无法识别的扩展稀有度与原版 COMMON 都按普通处理
        return COMMON;
    }

    // 原版 Rarity 无法表达的品质需要由我方客户端 Mixin 接管显示样式
    public boolean hasCustomVisual() {
        return ordinal() > EPIC.ordinal();
    }

    // 是否已到最高档
    public boolean isMax() {
        return ordinal() == values().length - 1;
    }

    // 下一档品质；已到顶则返回自身
    public Quality next() {
        return isMax() ? this : values()[ordinal() + 1];
    }

    // 读 NBT 用：认不出来的一律按普通处理，不让坏数据阻断流程
    public static Quality byId(String id) {
        Quality quality = find(id);
        return quality != null ? quality : COMMON;
    }

    // 严格查找，找不到返回 null
    public static Quality find(String id) {
        for (Quality quality : values()) {
            if (quality.id.equals(id)) {
                return quality;
            }
        }
        return null;
    }
}
