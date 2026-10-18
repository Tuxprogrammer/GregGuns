package com.gregguns.armor.oxygen.gc;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.config.EquipmentConfig;

import micdoodle8.mods.galacticraft.core.items.ItemOxygenTank;

/**
 * The Oxygen Tanks when Galacticraft is loaded: a subclass of Galacticraft's {@code ItemOxygenTank}, so it fits a tank
 * slot of Galacticraft's own inventory and GC drains it one point per 9 ticks outside a breathable block. A tank must
 * be a real GC tank because GC computes "air empty" from its own tank slots only. The capacity is the config option
 * {@code equipment.oxygenTankCapacity} (default GC's tier 2, 2000). Loaded only when GalacticraftCore is present
 * (GGS-05-23). Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/oxygen-gear.md (GGS-05-22, GGS-05-23, GGS-05-24, GGS-05-28)
// Source: GC tank slots accept ItemOxygenTank by class; IBreathableArmor cannot replace a tank:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/entities/player/GCPlayerHandler.java#L721-L726
// Source: ItemOxygenTank(int tier, String) sets stack size 1, setMaxDamage from the tier and no-repair; setMaxDamage
// is called again here:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/items/ItemOxygenTank.java#L19-L27
public final class OxygenTanksGC extends ItemOxygenTank implements Tiered {

    private CreativeTabs tab;

    /** Creates the tanks. */
    public OxygenTanksGC() {
        super(2, "oxygen_tanks");
        setMaxDamage(EquipmentConfig.oxygenTankCapacity);
    }

    @Override
    public Item setCreativeTab(CreativeTabs tab) {
        this.tab = tab;
        return super.setCreativeTab(tab);
    }

    @Override
    public CreativeTabs getCreativeTab() {
        // Galacticraft's override returns its own tab; GregGuns items belong in the GregGuns tab.
        return tab;
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal("gregguns.equipment.tooltip.space_suit_note"));
    }
}
