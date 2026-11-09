package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.ObtainTier;
import com.kuba6000.mobsinfo.api.IMobExtraInfoProvider;
import com.kuba6000.mobsinfo.api.MobDrop;
import com.kuba6000.mobsinfo.api.MobRecipe;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Adds the drops of a GregGuns NPC's carried gun (D312), and describes them to Mobs Info's NEI mob page.
 * <ul>
 * <li>On a player kill ({@code recentlyHit}) of a profile with the rare drop, a fresh copy of the gun at
 * {@link NpcDrops#rareGunChance}.</li>
 * <li>On every death that drops loot, whatever killed the NPC: the gun's ammo, a full tank, or for an energy gun a few
 * parts of its recipe ({@link NpcDrops#gunDrops}).</li>
 * </ul>
 * Forge posts {@link LivingDropsEvent} even when the {@code doMobLoot} rule is off or the mob is a child, so the
 * handler checks vanilla's loot gate itself. Every draw uses the NPC's own random source. Registered by the NPC module
 * at normal priority, always.
 *
 * <p>
 * Mobs Info finds an {@link IMobExtraInfoProvider} only on a registered drops or death event listener, so this class
 * is that provider too ({@code @Optional}: Mobs Info is not needed). Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-38 to GGS-10-40, Mobs Info)
// Source:
// https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/extras/ExtraLoader.java#L44-L69
@Optional.Interface(iface = "com.kuba6000.mobsinfo.api.IMobExtraInfoProvider", modid = NpcGunDropsHandler.MOBS_INFO)
public final class NpcGunDropsHandler implements IMobExtraInfoProvider {

    /** Mobs Info's mod id. */
    public static final String MOBS_INFO = "mobsinfo";
    /** Ticks before a dropped item can be picked up, as vanilla's own drops. */
    private static final int PICKUP_DELAY = 10;

    /**
     * @param event the drops event
     */
    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.entity.worldObj.isRemote || !(event.entityLiving instanceof NpcEntity)) {
            return;
        }
        NpcEntity npc = (NpcEntity) event.entityLiving;
        NpcProfile profile = npc.npcProfile();
        ItemStack held = npc.getHeldItem();
        if (profile == null || held == null || !(held.getItem() instanceof ItemGun)) {
            return;
        }
        // Vanilla's own loot gate (EntityLivingBase.onDeath): no loot from children or with doMobLoot off.
        if (npc.isChild() || !npc.worldObj.getGameRules()
            .getGameRuleBooleanValue("doMobLoot")) {
            return;
        }
        ItemGun gun = (ItemGun) held.getItem();
        Random rand = npc.getRNG();
        if (event.recentlyHit && profile.rareDrop() && rand.nextFloat() < NpcDrops.rareGunChance(event.lootingLevel)) {
            add(event, npc, new ItemStack(gun, 1));
        }
        for (GunSalvage.Pick pick : NpcDrops.gunDrops(
            gun.definition()
                .name(),
            gun.stats(),
            rand)) {
            add(event, npc, LootCategories.stack(pick.ref(), pick.count()));
        }
    }

    private static void add(LivingDropsEvent event, NpcEntity npc, ItemStack stack) {
        // A gun or ammo above loot.maxTier by its whole crafting chain stays with the body (D364).
        if (stack == null || stack.stackSize <= 0 || !ObtainTier.allowed(stack)) {
            return;
        }
        EntityItem item = new EntityItem(npc.worldObj, npc.posX, npc.posY, npc.posZ, stack);
        item.delayBeforeCanPickup = PICKUP_DELAY;
        event.drops.add(item);
    }

    /**
     * Removes any "Additional" gun row (vanilla's 8.5% equipment drop, which our NPCs never make) and adds the rare gun
     * drop and each gun's ammo or salvage, at the carry chances of the NPC's own equipment profile
     * ({@link NpcDropInfo}). Armor rows stay.
     */
    @Override
    @Optional.Method(modid = MOBS_INFO)
    public void provideExtraDropsInformation(@Nonnull String entityString, @Nonnull ArrayList<MobDrop> drops,
        @Nonnull MobRecipe recipe) {
        if (!(recipe.entity instanceof NpcEntity)) {
            return;
        }
        NpcProfile profile = ((NpcEntity) recipe.entity).npcProfile();
        if (profile == null || profile.equipment() == null) {
            return;
        }
        for (Iterator<MobDrop> it = drops.iterator(); it.hasNext();) {
            MobDrop drop = it.next();
            if (drop.type == MobDrop.DropType.Additional && drop.stack != null
                && drop.stack.getItem() instanceof ItemGun) {
                it.remove();
            }
        }
        Map<String, Double> guns = NpcDropInfo.carryChances(
            profile.equipment(),
            NpcEntity.EGG_DANGER_RANGE,
            ref -> NpcItems.item(ref) instanceof ItemGun);
        Map<String, String> names = new LinkedHashMap<>();
        for (String gun : guns.keySet()) {
            names.put(gun, new ItemStack(NpcItems.item(gun)).getDisplayName());
        }
        for (NpcDropInfo.Row row : NpcDropInfo.transform(
            guns,
            profile.rareDrop(),
            gun -> GunDefinitions.byName(gun) == null ? null
                : NpcDrops.gunDrop(
                    gun,
                    GunDefinitions.byName(gun)
                        .stats()))) {
            ItemStack stack = LootCategories.stack(row.ref(), 1);
            if (stack == null) {
                continue;
            }
            String gunName = names.get(row.gun());
            MobDrop drop = MobDrop.create(stack)
                .withChance(row.chance());
            if (row.type() == NpcDropInfo.Type.RARE) {
                drop.withType(MobDrop.DropType.Rare)
                    .withLooting()
                    .withHardPlayerRestriction()
                    .withAdditionalInfo(StatCollector.translateToLocalFormatted("gregguns.npc.drops.rare_looting"));
            } else if (row.min() != row.max()) {
                drop.withAdditionalInfo(
                    StatCollector.translateToLocalFormatted("gregguns.npc.drops.range", row.min(), row.max(), gunName));
            } else {
                drop.withAdditionalInfo(StatCollector.translateToLocalFormatted("gregguns.npc.drops.from", gunName));
            }
            drop.clampChance();
            drops.add(drop);
        }
    }
}
