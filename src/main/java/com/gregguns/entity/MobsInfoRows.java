package com.gregguns.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.item.gun.ItemGun;
import com.kuba6000.mobsinfo.api.DummyWorld;
import com.kuba6000.mobsinfo.api.MobDrop;
import com.kuba6000.mobsinfo.api.MobRecipe;

/**
 * Reads one mob's rows off Mobs Info's NEI page as plain strings, for {@code npc.mobsinfo_drops}. The only class of
 * the scenarios that names Mobs Info types: it loads only when that scenario runs, which it does only with Mobs Info
 * loaded. Dev environment only, server side.
 */
// Source:
// https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/api/MobRecipe.java#L298
final class MobsInfoRows {

    private MobsInfoRows() {}

    /**
     * @param entityName the mob's {@code EntityList} name
     * @return {type, "gun" or "item", item name, chance, "info" or ""} per row, or null when the mob has no page
     */
    static List<String[]> of(String entityName) {
        MobRecipe recipe = MobRecipe.MobNameToRecipeMap.get(entityName);
        if (recipe == null) {
            return null;
        }
        List<String[]> out = new ArrayList<>();
        for (MobDrop d : recipe.mOutputs) {
            boolean gun = d.stack != null && d.stack.getItem() instanceof ItemGun;
            out.add(
                new String[] { String.valueOf(d.type), gun ? "gun" : "item",
                    d.stack == null ? "?" : d.stack.getUnlocalizedName() + "x" + d.stack.stackSize,
                    String.valueOf(d.chance), d.additionalInfo == null || d.additionalInfo.isEmpty() ? "" : "info" });
        }
        return out;
    }

    /**
     * Rolls an NPC's equipment in Mobs Info's own dummy world, as its equipment simulation does (it swallows any
     * exception there, which leaves the page without the equipment rows).
     *
     * @param entityName the mob's {@code EntityList} name
     * @return null when the roll ran, else what went wrong
     */
    static String rollInDummyWorld(String entityName) {
        try {
            Entity e = EntityList.createEntityByName(entityName, new DummyWorld());
            if (!(e instanceof NpcEntity)) {
                return entityName + " is not an NPC in the dummy world: " + e;
            }
            ((NpcEntity) e).rollEquipment();
            int n = 0;
            for (ItemStack s : ((NpcEntity) e).getLastActiveItems()) {
                n += s == null ? 0 : 1;
            }
            return n > 0 ? null : entityName + " rolled no equipment in the dummy world";
        } catch (RuntimeException | LinkageError ex) {
            StringBuilder b = new StringBuilder(entityName + " threw " + ex);
            for (StackTraceElement t : ex.getStackTrace()) {
                if (b.length() > 1200) {
                    break;
                }
                b.append(" | ")
                    .append(t);
            }
            return b.toString();
        }
    }
}
