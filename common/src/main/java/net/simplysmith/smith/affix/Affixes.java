package net.simplysmith.smith.affix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.simplysmith.SimplySmith;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
词条注册表
统一池：所有词条共用一张表，不按槽位划分适用范围。
因此会出现一些好玩的情况例如「靴子带攻击力」「剑带护甲」这类组合，这是有意为之的随机性，不是 bug。（别一直给我提相关的issues了QWQ）

内置词条在类加载时静态注册，外部词条由 AffixDataLoader 在每次数据包重载后整批替换。
*/
public final class Affixes {

    private static final Map<ResourceLocation, Affix> BUILT_IN = new LinkedHashMap<>();

    /*
    数据包词条与合并后的池子

    重载在服务端线程写、Tooltip 与属性查询在别的线程读，所以整表替换而不是原地改，
    并用 volatile 保证读到的一定是完整的一版。
    */
    private static volatile Map<ResourceLocation, Affix> dataDriven = Map.of();
    private static volatile List<Affix> pool = List.of();

    /*
    以上三个字段必须声明在词条常量之前

    静态字段按书写顺序初始化，而每注册一条内置词条都会顺手重建池子，
    要是把它们挪到常量后面，第一次注册就会读到 null，类加载直接失败。
    */

    // 强壮：最大生命值提升
    public static final Affix VITALITY = register("vitality", Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 2.0D);

    // 磐石：击退抗性提升
    public static final Affix STEADFAST = register("steadfast", Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION, 0.05D);

    // 坚固：护甲提升
    public static final Affix STURDY = register("sturdy", Attributes.ARMOR, AttributeModifier.Operation.ADDITION, 2.0D);

    // 坚韧：护甲韧性提升
    public static final Affix TENACITY = register("tenacity", Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADDITION, 2.0D);

    //轻盈：速度提升
    public static final Affix NIMBLE = register("nimble", Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE, 0.10D);

    // 力量：攻击力提升
    public static final Affix STRENGTH = register("strength", Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 1.0D);

    // 狂暴：攻击速度提升
    public static final Affix FRENZY = register("frenzy", Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADDITION, 0.1D);

    // 冲击：击退强度提升
    public static final Affix IMPACT = register("impact", Attributes.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADDITION, 0.1D);

    // 幸运：幸运提升
    public static final Affix FORTUNE = register("fortune", Attributes.LUCK, AttributeModifier.Operation.ADDITION, 1.0D);

    // 吸血：按实际造成的伤害恢复生命，满血时转为伤害吸收
    public static final Affix LIFESTEAL = registerEffect("lifesteal", Affix.Kind.LIFESTEAL, 0.10D);

    // 血怒：生命高于 1 点时消耗 1 点生命，增加本次近战伤害
    public static final Affix BLOODRAGE = registerEffect("bloodrage", Affix.Kind.BLOODRAGE, 2.0D);

    // 经验修补：消耗经验点修复装备耐久
    public static final Affix EXPERIENCE_MENDING = registerEffect("experience_mending", Affix.Kind.EXPERIENCE_MENDING, 2.0D);

    // 永恒：耐久始终保持满值
    public static final Affix ETERNAL = registerEffect("eternal", Affix.Kind.ETERNAL, 0.0D);

    // 破军：提升原版暴击后的伤害
    public static final Affix BREAK_ARMY = registerEffect("break_army", Affix.Kind.BREAK_ARMY, 1.0D);

    // 不朽：死亡时保留 1 点生命，基础数值为冷却减免秒数
    public static final Affix IMMORTAL = registerEffect("immortal", Affix.Kind.IMMORTAL, 1.0D);

    // 闪避：手持与穿戴装备的概率直接叠加
    public static final Affix DODGE = registerEffect("dodge", Affix.Kind.DODGE, 0.10D);

    private Affixes() {
    }

    private static Affix register(String path, Attribute attribute,
                                  AttributeModifier.Operation operation, double defaultBaseValue) {
        return put(new Affix(path, attribute, operation, defaultBaseValue));
    }

    private static Affix registerEffect(String path, Affix.Kind kind, double defaultBaseValue) {
        return put(new Affix(path, kind, defaultBaseValue));
    }

    private static Affix put(Affix affix) {
        BUILT_IN.put(affix.id(), affix);
        rebuildPool();
        return affix;
    }

    /*
    整批替换数据包词条

    服务端由 AffixDataLoader 在每次重载后调用，客户端由 AffixSyncPacket 收包后调用。
    与内置词条同 id 的定义已在加载阶段被拒绝，这里拿到的一定是不冲突的一批。
    */
    public static void replaceDataDriven(Map<ResourceLocation, Affix> loaded) {
        dataDriven = Map.copyOf(loaded);
        rebuildPool();
    }

    /*
    合并顺序必须确定

    内置词条按声明序在前，保证配置文件的书写顺序不会因为改了别的东西就抖动；
    数据包词条按 id 排序在后——它们来自 map，不排序的话遍历顺序随 JVM 而变，
    会让配置文件每次启动都重排，抽词条的随机结果也无法复现。
    */
    private static void rebuildPool() {
        List<Affix> merged = new ArrayList<>(BUILT_IN.values());

        List<Affix> external = new ArrayList<>(dataDriven.values());
        external.sort(Comparator.comparing(affix -> affix.id().toString()));
        merged.addAll(external);

        pool = List.copyOf(merged);
    }

    // 顺序稳定，决定配置文件中的书写顺序
    public static List<Affix> all() {
        return pool;
    }

    // 池中可用词条总数，抽取数量超过它时才会触发兜底重复
    public static int size() {
        return pool.size();
    }

    public static boolean isBuiltIn(ResourceLocation id) {
        return BUILT_IN.containsKey(id);
    }

    // 未知 id 返回 null，例如读到了旧版本、已删除的词条，或数据包已被移除
    public static Affix byId(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Affix affix = BUILT_IN.get(id);
        return affix != null ? affix : dataDriven.get(id);
    }

    public static Affix byId(String raw) {
        return byId(parseId(raw));
    }

    /*
    解析 NBT 里的词条 id，非法返回 null

    旧存档存的是不带命名空间的裸 id，必须显式补上自家命名空间——
    直接交给 ResourceLocation 会被补成 minecraft:，那样老物品的词条会一次性全部失配。
    */
    public static ResourceLocation parseId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (raw.indexOf(':') < 0) {
            return ResourceLocation.tryParse(SimplySmith.MOD_ID + ":" + raw);
        }
        return ResourceLocation.tryParse(raw);
    }

    // 触发类加载，确保静态注册在使用前完成
    public static void bootstrap() {
    }
}
