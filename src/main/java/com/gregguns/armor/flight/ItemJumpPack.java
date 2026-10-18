package com.gregguns.armor.flight;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.core.config.FlightConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Jump Pack: an HV electric back device. Each jump while it holds {@code flight.jumpPackJumpCostEu} adds a boost
 * (+0.8 motion), and falls longer than 3 blocks are absorbed for {@code flight.jumpPackFallCostEu}, up to 8 blocks
 * free. The server spends the EU in {@link FlightEvents}; this item only reports whether the next jump is covered.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md 4.4; docs/gregguns-spec/05-armor/powered-flight.md
// Deviation: Techguns costs 1 durability per jump or fall; GregGuns spends EU (0003). The air-tank reading in
// fluids.md is not used: the pack is electric (recipes.md open question, decided in implementation-notes FLT-DEV-03).
public final class ItemJumpPack extends ItemElectricBackDevice {

    /** Registry name. */
    public static final String NAME = "jump_pack";

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    protected double capacity() {
        return FlightConfig.jumpPackCapacityEu;
    }

    /**
     * @param stack a Jump Pack
     * @return whether it holds the EU for one boost jump
     */
    public static boolean canJump(ItemStack stack) {
        double charge = ElectricGear.charge(stack);
        return charge > 0.0 && charge >= FlightConfig.jumpPackJumpCostEu;
    }

    @Override
    public void onBackTick(EntityPlayer player, ItemStack stack) {
        Flight.server()
            .publish(player, new FlightStatus(BackKind.JUMP_PACK, false, canJump(stack)));
    }

    @Override
    public void onBackRemoved(EntityPlayer player, Item item) {
        Flight.server()
            .forget(player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.flight.tooltip.jump_pack"));
        super.addInformation(stack, player, tooltip, advanced);
    }
}
