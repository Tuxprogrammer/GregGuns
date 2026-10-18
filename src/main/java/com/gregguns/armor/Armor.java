package com.gregguns.armor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.BonusMap;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.logic.SetEvaluator;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.combat.TypedArmor;

/**
 * The read API for worn GregGuns armor. Everything reads the four vanilla armor slots of any living entity (a player or
 * an NPC), so it works the same for both. Nothing here changes state.
 *
 * <pre>
 * 
 * double armor = Armor.armorValue(player, DamageType.ENERGY); // summed armor points, before the 24-point clamp
 * double resist = Armor.penetrationResistance(player, DamageType.PROJECTILE);
 * double acc = Armor.bonus(player, ArmorBonus.GUN_ACCURACY); // any bonus, pieces plus full sets
 * boolean full = Armor.isWearingFullSet(player, ArmorCatalog.T1_SCOUT);
 * </pre>
 *
 * Both sides.
 */
// Spec: docs/techguns-spec/05-armor/set-bonuses.md section 2
public final class Armor {

    private Armor() {}

    /**
     * @param entity a living entity
     * @return the worn GregGuns pieces, helmet first
     */
    public static List<SetEvaluator.Worn> worn(EntityLivingBase entity) {
        List<SetEvaluator.Worn> out = new ArrayList<>(4);
        for (int slot = 4; slot >= 1; slot--) {
            ItemStack stack = entity.getEquipmentInSlot(slot);
            ItemGregGunsArmor item = stack == null ? null : ArmorItems.of(stack.getItem());
            if (item != null) {
                boolean powered = item.set()
                    .power()
                    .powered() && ArmorPower.setPowered(entity, item.set());
                out.add(new SetEvaluator.Worn(item.set(), item.piece(), item.isBroken(stack), powered));
            }
        }
        return out;
    }

    /**
     * @param entity a living entity
     * @return the evaluated bonuses and complete sets
     */
    public static SetEvaluator.Result evaluate(EntityLivingBase entity) {
        return SetEvaluator.evaluate(worn(entity));
    }

    /**
     * @param entity a living entity
     * @return all bonuses currently given by its worn GregGuns armor
     */
    public static BonusMap bonuses(EntityLivingBase entity) {
        return evaluate(entity).bonuses();
    }

    /**
     * @param entity a living entity
     * @param bonus  the bonus
     * @return its summed amount, 0 when none
     */
    public static double bonus(EntityLivingBase entity, ArmorBonus bonus) {
        return bonuses(entity).get(bonus);
    }

    /**
     * @param entity a living entity
     * @param set    a set
     * @return true when every piece of the set is worn and intact
     */
    public static boolean isWearingFullSet(EntityLivingBase entity, SetDef set) {
        return evaluate(entity).completeSets()
            .contains(set.id());
    }

    /**
     * The summed armor points of the worn GregGuns pieces against a type. Vanilla and other mods' armor is not included
     * (it takes part in the hit through Forge, see the damage handler).
     *
     * @param entity a living entity
     * @param type   a damage type
     * @return armor points, 0 for an armor-bypassing type
     */
    public static double armorValue(EntityLivingBase entity, DamageType type) {
        double sum = 0.0;
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = entity.getEquipmentInSlot(slot);
            if (stack != null && stack.getItem() instanceof TypedArmor) {
                sum += ((TypedArmor) stack.getItem()).armorValue(stack, type);
            }
        }
        return type.bypassesArmor() ? 0.0 : sum;
    }

    /**
     * @param entity a living entity
     * @param type   a damage type
     * @return the summed penetration resistance of the worn GregGuns pieces, which the damage handler subtracts once
     *         per hit
     */
    public static double penetrationResistance(EntityLivingBase entity, DamageType type) {
        double sum = 0.0;
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = entity.getEquipmentInSlot(slot);
            if (stack != null && stack.getItem() instanceof TypedArmor) {
                sum += ((TypedArmor) stack.getItem()).penetrationResistance(stack, type);
            }
        }
        return sum;
    }

    /**
     * @param armorPoints summed armor points
     * @return the fraction of the armored damage that is blocked, 0..0.96
     */
    public static double blockedFraction(double armorPoints) {
        return 1.0 - DamageModel.vanillaArmorFactor(armorPoints);
    }
}
