# SimplySmith

SimplySmith adds gear quality, random affixes, enhancement, and quality breakthroughs.

## How it works

Equipment receives a quality and a set of affixes when it first enters a player's inventory. Affixes only take effect while the item is held or equipped.
An item counts as equipment if it does not stack and matches any of: it has durability, it is worn in an armor or off-hand slot, it is a sword, tool, bow or crossbow, it carries a vanilla equipment tag, or it deals attack damage.
Initial quality is based on the item's vanilla rarity, enchanted loot keeps vanilla's rarity increase, and the three highest qualities are reached through breakthroughs.

| Quality | Default affixes | Value multiplier |
|---|---:|---:|
| Common | 1–3 | ×1 |
| Uncommon | 2–4 | ×1.5 |
| Rare | 3–5 | ×2 |
| Epic | 4–6 | ×3 |
| Legendary | 5–7 | ×5 |
| Mythic | 6–8 | ×7 |
| Ultimate | 7–9 | ×10 |

### Affix categories

Affixes are split into four category pools: weapon, tool, armor and generic. Each roll has a 50% chance by default to be drawn from the item's own category pool instead of the whole pool, so a sword sees weapon affixes more often, and generic affixes count toward every item's category pool. Categories only shift the odds and never restrict an item to a single pool; the bias is configurable.

### Attribute affixes

Attribute-affix values are calculated as `base value × quality multiplier × (1 + enhancement level)`. Each base value is also the increase from one enhancement level before the quality multiplier is applied.

| Affix | Category | Effect |
|---|---|---|
| Strength | Weapon | +1 Attack Damage |
| Frenzy | Weapon | +0.1 Attack Speed |
| Impact | Weapon | +0.1 Attack Knockback |
| Vitality | Armor | +2 Max Health |
| Steadfast | Armor | +0.05 Knockback Resistance |
| Sturdy | Armor | +2 Armor |
| Tenacity | Armor | +2 Armor Toughness |
| Nimble | Generic | +10% Movement Speed |
| Fortune | Generic | +1 Luck |

### Functional affixes

| Affix | Category | Base value | Effect |
|---|---|---:|---|
| Lifesteal | Weapon | 10% | Restore this percentage of actual damage dealt. Healing beyond full health becomes absorption. |
| Bloodrage | Weapon | +2 | Above 1 health, spend 1 health on a melee attack to add this damage. |
| Break Army | Weapon | +100% | Increase vanilla critical-hit damage by this percentage. |
| Sharpshooter | Weapon | +10% | All damage you deal grows by this much per block of distance, counting from 2 blocks out. No distance cap. |
| Backstab | Weapon | +200% | Melee attacks landed within a 90° arc behind the target's body deal this much extra damage. |
| Nightingale | Weapon | +300% | Turns truly invisible while sneaking and immediately breaks combat. Attacking adds this much damage, reveals you, and puts the weapon on a fixed 6 second cooldown. |
| Eternal | Tool | — | Durability is always kept full. |
| Refined Drill | Tool | +1 | Raises the tool's mining level by this many tiers. Only applies to blocks that use vanilla tier requirements. |
| Excavator | Tool | +50% | Increases mining speed by this percentage. |
| Midas Touch | Tool | 10% | Chance to drop an extra gold nugget when mining stone blocks. Capped at 100%. |
| Immortal | Armor | -1 s | On death, remain at 1 health. Cooldown is 60 seconds minus this value, with a minimum of 1 second. Uses Minecraft's normal item cooldown. |
| Chameleon | Armor | — | Turns fully invisible while standing still, including armor and held items, and reappears the moment you move. |
| Experience Mending | Generic | 2 | For each held or equipped damaged item, consume 1 experience point per tick to restore this much durability. |
| Dodge | Generic | 10% | Values from both hands and all worn armor add together; incoming damage is cancelled on success, capped at 99%. |
| Flame Infusion | Generic | 3 s | Damaging an enemy sets it on fire for this long. |
| Frost Infusion | Generic | 3 s | Damaging an enemy freezes it solid for this long. |
| Lightning Infusion | Generic | 2 | Damaging an enemy calls down a purely visual bolt and deals this much lightning damage. |
| Poison Infusion | Generic | 3 s | Damaging an enemy applies Poison III for this long. |
| Wither Infusion | Generic | 3 s | Damaging an enemy applies Wither III for this long. |
| True Damage Infusion | Generic | 2 | Damaging an enemy deals this much extra true damage. |

### Added items

SimplySmith adds two upgrade items. Both can be found in the Ingredients creative tab:

- **Enhancement Stone**: combine it with stamped equipment in an anvil to increase every affix on the item. Enhancement has no level cap and costs no experience.
- **Breakthrough Stone**: combine it with stamped equipment in an anvil to raise its quality by one tier and fill the new affix slots. Ultimate equipment cannot be raised further.

## Adding custom affixes

Any mod or datapack can add attribute affixes to the shared pool. No dependency on SimplySmith is needed.

Put custom affix JSON files at:

```
data/<namespace>/simplysmith/affixes/<affix_name>.json
```

The affix id comes from the file path, so `data/mymod/simplysmith/affixes/deadly.json` registers `mymod:deadly`.

| Field | Required | Description |
|---|---|---|
| `attribute` | Yes | Registry id of the attribute to modify |
| `operation` | No | `addition`, `multiply_base` or `multiply_total`. Defaults to `addition` |
| `base_value` | Yes | Value at Common quality |
| `category` | No | `weapon`, `tool`, `armor` or `generic`. Defaults to `generic` |
| `quality_multiplier` | No | Per-quality multiplier override. Only the tiers you list are overridden; the rest fall back to the config file |

```json
{
  "attribute": "minecraft:attack_damage",
  "operation": "addition",
  "base_value": 1.5,
  "category": "weapon",
  "quality_multiplier": {
    "epic": 4.0,
    "ultimate": 20.0
  }
}
```

### Affix names and descriptions

Affix names and descriptions are read from language files rather than the affix JSON. The key format is:

```
affix.<namespace>.<affix_name>         name
affix.<namespace>.<affix_name>.desc    description, shown while holding Shift
```

So for `mymod:deadly`, add to `assets/mymod/lang/en_us.json`:

```json
{
  "affix.mymod.deadly": "Deadly",
  "affix.mymod.deadly.desc": "Increases attack damage by %s"
}
```

`%s` receives the current value. Leave it out if the affix has no number worth showing.

A plain datapack has no `assets/` folder. If you ship a datapack rather than a mod, add a matching resource pack, otherwise the tooltip shows the raw translation key.

### Notes

- Only attribute affixes can be added this way.
- `base_value` can be overridden per affix under `[affix_base]` in `config/simplysmith.toml`, and the config wins where an entry exists. `quality_multiplier` in the JSON always wins for the tiers it lists.

Compatible Mods:

- [Cloth Config API](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu)
- [JEI](https://modrinth.com/mod/jei)

---

# SimplySmith

SimplySmith 添加了装备品质、随机词条、强化和品质突破。

## 玩法

装备第一次进入玩家背包时，会获得品质和一组随机词条。词条只在物品被手持或穿戴时生效。
判定为装备的条件是不可堆叠，且满足以下任意一条：有耐久、穿戴在盔甲位或副手位、是剑或工具或弓弩、带有原版装备标签、主手有攻击力。
初始品质取决于物品的原版稀有度，带附魔的战利品会保留原版的稀有度提升，最高的三档品质需要通过突破获得。

| 品质 | 默认词条数 | 数值倍率 |
|---|---:|---:|
| 普通 | 1–3 | ×1 |
| 不凡 | 2–4 | ×1.5 |
| 稀有 | 3–5 | ×2 |
| 史诗 | 4–6 | ×3 |
| 传奇 | 5–7 | ×5 |
| 神话 | 6–8 | ×7 |
| 究极 | 7–9 | ×10 |

### 词条分类

词条池分为武器、工具、盔甲、普通四类，每次抽取默认有 50% 的概率从该装备所属分类的池子中抽取，而不是从全池抽取，因此剑更容易抽到武器词条，普通分类的词条会计入所有装备的分类池。分类只改变概率，不做物品类型限定，偏向概率可进行配置。

### 属性词条

属性词条的实际数值为 `基础值 × 品质倍率 × (1 + 强化等级)`。基础值就是每提升一级强化后的增量，两者都会受到品质倍率影响。

| 词条 | 分类 | 效果 |
|---|---|---|
| 力量 | 武器 | 攻击力 +1 |
| 狂暴 | 武器 | 攻击速度 +0.1 |
| 冲击 | 武器 | 攻击击退 +0.1 |
| 强壮 | 盔甲 | 最大生命值 +2 |
| 磐石 | 盔甲 | 击退抗性 +0.05 |
| 坚固 | 盔甲 | 护甲 +2 |
| 坚韧 | 盔甲 | 护甲韧性 +2 |
| 轻盈 | 普通 | 移动速度 +10% |
| 幸运 | 普通 | 幸运 +1 |

### 功能词条

| 词条 | 分类 | 基础值 | 效果 |
|---|---|---:|---|
| 吸血 | 武器 | 10% | 攻击造成实际伤害时，恢复该比例生命；治疗溢出转化为伤害吸收。 |
| 血怒 | 武器 | +2 | 生命高于 1 点时，近战攻击消耗 1 点生命并额外造成该伤害。 |
| 破军 | 武器 | +100% | 原版暴击伤害额外提升该百分比。 |
| 神射手 | 武器 | +10% | 你造成的一切伤害，从 2 格外开始每格提升该比例，不设距离上限。 |
| 背刺 | 武器 | +200% | 近战命中目标身体正后方 90 度扇形内时，额外造成该伤害。 |
| 夜莺 | 武器 | +300% | 潜行时进入真实隐身，立刻脱战；攻击时额外造成该伤害、解除隐身，并使武器进入固定 6 秒冷却。 |
| 永恒 | 工具 | — | 耐久始终保持满值。 |
| 精炼钻头 | 工具 | +1 | 该工具的挖掘等级提升对应级数，仅对使用原版等级要求的方块生效。 |
| 挖掘机 | 工具 | +50% | 挖掘速度提升对应百分比。 |
| 点石成金 | 工具 | 10% | 挖掘石头类方块时，按该概率额外掉落一个金粒，最高 100%。 |
| 不朽 | 盔甲 | -1 秒 | 死亡时保留 1 点生命；冷却为 60 秒减去该数值，最低 1 秒，使用原版同类物品共享冷却。 |
| 变色龙 | 盔甲 | — | 站定不动时完全隐形，连同盔甲与手持物一并消失，一旦移动立即现形。 |
| 经验修补 | 普通 | 2 | 每件被手持或穿戴、且耐久未满的装备，每 tick 消耗 1 点经验并恢复该耐久。 |
| 闪避 | 普通 | 10% | 主手、副手与全部穿戴装备的数值直接相加；成功时取消本次伤害，最高 99%。 |
| 火焰附加 | 普通 | 3 秒 | 造成伤害时使敌人着火该时长。 |
| 冰冻附加 | 普通 | 3 秒 | 造成伤害时使敌人完全冻结该时长。 |
| 雷电附加 | 普通 | 2 | 造成伤害时召唤一道纯视觉的闪电，并造成该点数的雷电伤害。 |
| 毒素附加 | 普通 | 3 秒 | 造成伤害时使敌人中毒 III，持续该时长。 |
| 凋零附加 | 普通 | 3 秒 | 造成伤害时使敌人凋零 III，持续该时长。 |
| 真伤附加 | 普通 | 2 | 造成伤害时额外造成该点数的真实伤害。 |

### 新增物品
本Mod新增了两种强化物品，均可在创造模式的原材料栏中找到：

- **强化石**：在铁砧中与已生成品质的装备合成，提升装备上的全部词条。强化等级没有上限，也不消耗经验。
- **突破石**：在铁砧中与已生成品质的装备合成，将品质提升一档，并补齐新品质增加的词条槽位。品质到顶后无法继续突破。

## 添加自定义词条

任何 Mod 或数据包都可以往共用词条池里添加属性词条，不需要依赖 SimplySmith。

将自定义词条 json 放在：

```
data/<命名空间>/simplysmith/affixes/<词条名>.json
```

词条 id 由文件路径推导，因此 `data/mymod/simplysmith/affixes/deadly.json` 注册的是 `mymod:deadly`。

| 字段 | 必填 | 说明 |
|---|---|---|
| `attribute` | 是 | 要修改的属性的注册 id |
| `operation` | 否 | `addition`、`multiply_base` 或 `multiply_total`，默认 `addition` |
| `base_value` | 是 | 普通品质下的数值 |
| `category` | 否 | `weapon`、`tool`、`armor` 或 `generic`，默认 `generic` |
| `quality_multiplier` | 否 | 品质倍率覆盖，只有列出的档位会被覆盖，其余档位回落到配置文件 |

```json
{
  "attribute": "minecraft:attack_damage",
  "operation": "addition",
  "base_value": 1.5,
  "category": "weapon",
  "quality_multiplier": {
    "epic": 4.0,
    "ultimate": 20.0
  }
}
```

### 词条名和描述

词条的名字和相关描述从语言文件中读取而不是在词条 json 里面，命名规则如下：

```
affix.<命名空间>.<词条名>         名称
affix.<命名空间>.<词条名>.desc    描述，按住 Shift 时显示
```

因此 `mymod:deadly` 需要在 `assets/mymod/lang/zh_cn.json` 中添加：

```json
{
  "affix.mymod.deadly": "致命",
  "affix.mymod.deadly.desc": "攻击力提升 %s 点"
}
```

`%s` 会接收当前数值。没有数值可展示的词条省略它即可。

纯数据包没有 `assets/` 目录。如果你发布的是数据包而不是 Mod，需要另外配一个资源包，否则 Tooltip 会显示原始的翻译键。

### 注意事项

- 只能通过这种方式添加属性词条。
- `base_value` 可以在 `config/simplysmith.toml` 的 `[affix_base]` 中按词条覆盖，存在条目时以配置文件为准。JSON 中的 `quality_multiplier` 对它列出的档位始终优先。

兼容 Mod：

- [Cloth Config API](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu)
- [JEI](https://modrinth.com/mod/jei)
