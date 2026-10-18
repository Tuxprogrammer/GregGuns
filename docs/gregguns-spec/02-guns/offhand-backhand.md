# Offhand, akimbo and right-click firing with Backhand

- **Techguns baseline:** [gun-lifecycle.md § input](../../techguns-spec/02-guns/gun-lifecycle.md#4-input-client) (left-click fire, right-click zoom), [gun-lifecycle.md § akimbo](../../techguns-spec/02-guns/gun-lifecycle.md#8-akimbo)
- **Design records:** none decide this. The tier table in [0001](../../design/0001-tier-map.md) lists the akimbo guns.
- **Pack mods:**
  - Backhand (`backhand`, `1.8.16`)
  - Battlegear2-for-Backhand (`battlegear2`, `1.6.8-backhand`)
- **Roadmap:** M1 (input and zoom), M2 (akimbo, grenades)

## What changes from Techguns

Techguns has no offhand. Akimbo guns are one item that fires with alternating hands in the model. The pack adds a real offhand slot (Backhand), and Backhand tries a **second item use** with the offhand whenever the main-hand use does nothing. That second use is the part that matters here.

| Techguns behaviour | In the pack | GregGuns behaviour |
|---|---|---|
| Fire is left-click, tracked by GregGuns' own key state, and always the held (main-hand) item | Backhand's offhand attack is off (`OffhandAttack=false`) | Fire stays main-hand only. A gun in the offhand never fires. |
| Right-click on a gun only flips client zoom (or starts a charge). The use "does nothing" to vanilla. | Backhand then tries the **offhand** item with the same click | Zoom and charge items are listed in the Backhand "stop fallback" list, so one click does one thing. |
| Akimbo guns are single items | No change | Akimbo stays one item. It does not use the offhand slot. |

## Integration points

### Backhand: the right-click flow

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/backhand.cfg`, `general` category:
  - `OffhandAttack=false`, `OffhandBreakBlocks=false` and `EmptyOffhand=false`. Left-click never uses the offhand.
  - `S:offhandBlacklist` is shipped **empty**.
  - `S:mainhandUseStopsOffhandFallback` is shipped with the Matter Manipulator, Forestry bags, Draconic magnet, AE2 network tools and the Extra Utilities watering can. These are items that "act on the server but return false on the client", the same shape as a zoom toggle.
- **Source (Backhand `1.8.16`):**
  - Backhand replaces `Minecraft.func_147121_ag` (the right-click handler): [MixinMinecraft.java#L89-L198](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/mixins/early/minecraft/MixinMinecraft.java#L89-L198).
  - The hand order is main then off, unless the offhand item class is registered as a priority item, or the main-hand class is registered as deprioritised: [L98-L101](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/mixins/early/minecraft/MixinMinecraft.java#L98-L101) and [L324-L341](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/mixins/early/minecraft/MixinMinecraft.java#L324-L341). **If the main-hand item is on the stop list, priority is ignored.**
  - A hand "used" the item when `playerController.sendUseItem(...)` returns true **or** the player has an item in use: [L273-L283](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/mixins/early/minecraft/MixinMinecraft.java#L273-L283). If the main hand did not use it, Backhand moves on to the offhand, unless the main-hand item is on the stop list ([L173-L179](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/mixins/early/minecraft/MixinMinecraft.java#L173-L179)).
  - Both lists are matched by registry name, `modid:itemname`, per item (all metadata values of the item match): [Backhand.java#L59-L77](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/Backhand.java#L59-L77).
  - The blacklist is enforced in three places: the offhand slot refuses the item ([BackhandSlot.java#L15-L18](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/api/core/BackhandSlot.java#L15-L18)), the swap key refuses it, and a blacklisted item already in the offhand is moved to the inventory at login ([CommonProxy.java#L27-L60](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/CommonProxy.java#L27-L60)).
  - Public API: `BackhandUtils.addOffhandPriorityItem(Class...)`, `addDeprioritizedMainhandItem(Class...)`, `useOffhandItem(...)`, `isUsingOffhand(...)`: [BackhandUtils.java#L35-L98](https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/api/core/BackhandUtils.java#L35-L98). There is no API to register a blacklist entry; only the config does that.
- **What this means for guns:**
  - A gun in the main hand with toggle zoom changes client zoom and returns the same stack. `sendUseItem` returns false and nothing is "in use". Backhand then uses the offhand item too, so one right-click can zoom **and** eat, place a torch or raise a shield.
  - Charge items (Bio Gun, Power Hammer charge, grenade wind-up) behave the same unless they set an item in use.
  - Fire does not need a fix: GregGuns tracks the fire key itself and reads the held item.
- **GregGuns does:**
  - Firing and reloading read only the main-hand held item. When Backhand temporarily swaps the held slot for an offhand use, a gun does not fire from that call.
  - Where a right-click use can be expressed as "item in use" (a hold-to-charge), the item calls `setItemInUse`, so Backhand sees it as used without any config.
  - For zoom toggles and other right-clicks that cannot, the item is added to the pack's `mainhandUseStopsOffhandFallback` list (pack-side change below).
  - Guns are added to `offhandBlacklist`. A gun in the offhand cannot fire, so the slot only hides a loaded weapon. Grenades are **not** blacklisted: right-click throw works from the offhand.
  - GregGuns does **not** register offhand-priority items. It would be ignored while a stop-listed gun is in hand, and gains nothing otherwise.
- **Pack-side change:** two keys in `config/backhand.cfg`.
  - `general` / `mainhandUseStopsOffhandFallback`: add `gregguns:<registry name>` for each gun item with right-click zoom, charge or reload, and each hold-to-charge tool gun. Grenades are added only if their throw does not set an item in use (confirm in M2).
  - `general` / `offhandBlacklist`: add `gregguns:<registry name>` for each gun item.
  - The registry names are fixed when the items are registered (M1 and M2) and listed in [PACK-CHANGES.md](../PACK-CHANGES.md).

### Battlegear2 (Backhand edition)

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/battlegear2.cfg`. `Disabled Items` and `Disabled Recipies` are both empty. Its seven enchantments use IDs 126 to 132: `BashDamage=128`, `BashPower=127`, `BashWeight=126`, `BowCharge=132`, `BowLoot=131`, `ShieldRecovery=130`, `ShieldUsage=129`.
- **Source (`1.6.8-backhand`):**
  - This build "depends on Backhand and doesn't have any dedicated equipment slots" ([README.md#L14](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/1.6.8-backhand/README.md#L14)). It adds shields, daggers, quivers and similar items.
  - It keeps the old API interfaces `IOffhandWield` and `IOffhandDual`. `BattlegearUtils.isOffHand` asks an item that implements them, otherwise it accepts shields, quivers and weapons that carry an attack modifier: [BattlegearUtils.java#L222-L236](https://github.com/GTNewHorizons/Battlegear2-for-Backhand/blob/1.6.8-backhand/src/main/java/mods/battlegear2/api/core/BattlegearUtils.java#L222-L236).
  - Backhand's source has no reference to Battlegear2 (the dependency runs the other way). So Backhand's blacklist, not Battlegear2's interfaces, decides what may sit in the offhand slot.
- **GregGuns does:** nothing. GregGuns does not implement `IOffhandWield` or `IOffhandDual`.
- **Enchantments:** GregGuns registers none, and guns have enchantability 0 in Techguns ([gun-lifecycle.md § item basics](../../techguns-spec/02-guns/gun-lifecycle.md#1-item-basics)), so Battlegear2's IDs 126 to 132 cannot collide with anything from this area. Infernal Mobs only enchants a drop when the item's enchantability allows it, so a gun dropped by an infernal mob is plain: [InfernalMobsCore.java#L662-L714](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L662-L714) passes `item.getItemEnchantability()` to vanilla's enchantment builder, which returns no enchantments for 0. (The vanilla rule is from general 1.7.10 knowledge; no pinned source.) GT's own enchantment IDs come from its generated config and are not in the pack's shipped files.
- **Pack-side change:** none.

## Requirements

- **GGS-02-24.** A gun fires, reloads and zooms only when it is the player's main-hand held item, in the normal state and in the state where Backhand has swapped the held slot.
- **GGS-02-25.** Every gun item with right-click zoom, charge or reload is listed in the pack's `mainhandUseStopsOffhandFallback`, or its right-click sets an item in use.
- **GGS-02-26.** Every gun item is listed in the pack's `offhandBlacklist`. Grenade items are not.
- **GGS-02-27.** Akimbo guns are single items and never need the offhand slot.
- **GGS-02-28.** GregGuns registers no enchantment and no Backhand offhand-priority item. Guns report enchantability 0.

## Tests

- GGS-02-24: `/gregguns test` scenario that puts a gun in the offhand and clicks both buttons (in a dev environment with Backhand loaded). Manual check on a real client.
- GGS-02-25, 26: manual check in `runClient` with the pack-side config applied: one right-click with a zoom gun in the main hand and food in the offhand; trying to drag a gun into the offhand slot.
- GGS-02-27, 28: unit test on the item classes (`getItemEnchantability()` is 0).

## Open questions

- **Which items get stop-list entries** depends on final registry names and on whether charge items can use `setItemInUse`. Revisit when M1 and M2 register their items.
- **Grenade in the offhand while a gun is in hand.** A zoom gun on the stop list stops Backhand trying the offhand grenade. Giving the grenade priority does not help (the stop list wins). If players want it, zoom has to move to a key. That is an input decision for [01-core/keybinds.md](../01-core/keybinds.md).
