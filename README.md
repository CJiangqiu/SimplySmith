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

Affix values are calculated as `base value × quality multiplier × (1 + enhancement level)`. Each base value is also the increase from one enhancement level before the quality multiplier is applied.

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

Two related items are also added. Both can be found in the Ingredients creative tab:

- **Enhancement Stone**: combine it with stamped equipment in an anvil to increase every affix on the item. Enhancement has no level cap and costs no experience.
- **Breakthrough Stone**: combine it with stamped equipment in an anvil to raise its quality by one tier and fill the new affix slots. Ultimate equipment cannot be raised further.

[Cloth Config API](https://modrinth.com/mod/cloth-config) and [Mod Menu](https://modrinth.com/mod/modmenu) are optional. SimplySmith works without them; on Fabric, installing both adds an in-game configuration screen. On Forge, Cloth Config API alone adds the same screen.

---

# SimplySmith

SimplySmith 添加装备品质、随机词条、强化和品质突破。

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

词条的实际数值为 `基础值 × 品质倍率 × (1 + 强化等级)`。基础值就是每提升一级强化后的增量，两者都会受到品质倍率影响。

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

新增了两种相关物品，均可在创造模式的原材料栏中找到：

- **强化石**：在铁砧中与已生成品质的装备合成，提升装备上的全部词条。强化等级没有上限，也不消耗经验。
- **突破石**：在铁砧中与已生成品质的装备合成，将品质提升一档，并补齐新品质增加的词条槽位。品质到顶后无法继续突破。

[Cloth Config API](https://modrinth.com/mod/cloth-config) 和 [Mod Menu](https://modrinth.com/mod/modmenu) 都是可选项，不安装也不会影响 SimplySmith 正常运行。Fabric 安装这两个 Mod 后可在游戏内打开配置页面；Forge 只需安装 Cloth Config API 即可。
