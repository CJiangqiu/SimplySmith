# SimplySmith

SimplySmith adds gear quality, random affixes, enhancement, and quality breakthroughs.

## How it works

Damageable equipment receives a quality and a set of affixes when it first enters a player's inventory. Affixes only take effect while the item is held or equipped.

Initial quality is based on the item's vanilla rarity. Enchanted loot keeps vanilla's rarity increase. The three highest qualities are reached through breakthroughs.

| Quality | Default affixes | Value multiplier |
|---|---:|---:|
| Common | 1–3 | ×1 |
| Uncommon | 2–4 | ×1.5 |
| Rare | 3–5 | ×2 |
| Epic | 4–6 | ×3 |
| Legendary | 5–7 | ×5 |
| Mythic | 6–8 | ×7 |
| Ultimate | 7–9 | ×10 |

### Attribute affixes

Attribute-affix values are calculated as `base value × quality multiplier × (1 + enhancement level)`. Each base value is also the increase from one enhancement level before the quality multiplier is applied.

| Affix | Effect |
|---|---|
| Vitality | +2 Max Health |
| Steadfast | +0.05 Knockback Resistance |
| Sturdy | +2 Armor |
| Tenacity | +2 Armor Toughness |
| Nimble | +10% Movement Speed |
| Strength | +1 Attack Damage |
| Frenzy | +0.1 Attack Speed |
| Impact | +0.1 Attack Knockback |
| Fortune | +1 Luck |

### Functional affixes

| Affix | Base value | Effect |
|---|---:|---|
| Lifesteal | 10% | Restore this percentage of actual damage dealt. Healing beyond full health becomes absorption. |
| Bloodrage | +2 | Above 1 health, spend 1 health on a melee attack to add this damage. |
| Experience Mending | 2 | For each held or equipped damaged item, consume 1 experience point per tick to restore this much durability. |
| Eternal | — | Durability is always kept full. |
| Break Army | +100% | Increase vanilla critical-hit damage by this percentage. |
| Immortal | -1 s | On death, remain at 1 health. Cooldown is 60 seconds minus this value, with a minimum of 1 second. Uses Minecraft's normal item cooldown. |
| Dodge | 10% | Values from both hands and all worn armor add together; incoming damage is cancelled on success, capped at 99%. |

Two related items are also added. Both can be found in the Ingredients creative tab:

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
| `quality_multiplier` | No | Per-quality multiplier override. Only the tiers you list are overridden; the rest fall back to the config file |

```json
{
  "attribute": "minecraft:attack_damage",
  "operation": "addition",
  "base_value": 1.5,
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

可损坏的装备第一次进入玩家背包时，会获得品质和一组随机词条。词条只在物品被手持或穿戴时生效。

初始品质取决于物品的原版稀有度，带附魔的战利品会保留原版的稀有度提升。最高的三档品质需要通过突破获得。

| 品质 | 默认词条数 | 数值倍率 |
|---|---:|---:|
| 普通 | 1–3 | ×1 |
| 不凡 | 2–4 | ×1.5 |
| 稀有 | 3–5 | ×2 |
| 史诗 | 4–6 | ×3 |
| 传奇 | 5–7 | ×5 |
| 神话 | 6–8 | ×7 |
| 究极 | 7–9 | ×10 |

### 属性词条

属性词条的实际数值为 `基础值 × 品质倍率 × (1 + 强化等级)`。基础值就是每提升一级强化后的增量，两者都会受到品质倍率影响。

| 词条 | 效果 |
|---|---|
| 强壮 | 最大生命值 +2 |
| 磐石 | 击退抗性 +0.05 |
| 坚固 | 护甲 +2 |
| 坚韧 | 护甲韧性 +2 |
| 轻盈 | 移动速度 +10% |
| 力量 | 攻击力 +1 |
| 狂暴 | 攻击速度 +0.1 |
| 冲击 | 攻击击退 +0.1 |
| 幸运 | 幸运 +1 |

### 功能词条

| 词条 | 基础值 | 效果 |
|---|---:|---|
| 吸血 | 10% | 攻击造成实际伤害时，恢复该比例生命；治疗溢出转化为伤害吸收。 |
| 血怒 | +2 | 生命高于 1 点时，近战攻击消耗 1 点生命并额外造成该伤害。 |
| 经验修补 | 2 | 每件被手持或穿戴、且耐久未满的装备，每 tick 消耗 1 点经验并恢复该耐久。 |
| 永恒 | — | 耐久始终保持满值。 |
| 破军 | +100% | 原版暴击伤害额外提升该百分比。 |
| 不朽 | -1 秒 | 死亡时保留 1 点生命；冷却为 60 秒减去该数值，最低 1 秒，使用原版同类物品共享冷却。 |
| 闪避 | 10% | 主手、副手与全部穿戴装备的数值直接相加；成功时取消本次伤害，最高 99%。 |

新增了两种相关物品，均可在创造模式的原材料栏中找到：

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
| `quality_multiplier` | 否 | 品质倍率覆盖，只有列出的档位会被覆盖，其余档位回落到配置文件 |

```json
{
  "attribute": "minecraft:attack_damage",
  "operation": "addition",
  "base_value": 1.5,
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
