package com.gregguns.armor.flight;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;

import com.gregguns.api.CamoChangeable;
import com.gregguns.api.Tier;
import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightMode;
import com.gregguns.armor.flight.logic.FlightPhysics;
import com.gregguns.armor.flight.logic.FlightPolicy;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.armor.slots.logic.PowerUse;
import com.gregguns.core.config.FlightConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Anti Gravity Device: an IV electric back device. The back-device key switches it on and off. While switched on,
 * charged and airborne it hovers the wearer (jump climbs, sneak descends, the move keys fly at 125% of creative speed)
 * and draws {@code flight.antiGravityDriveEuPerTick} EU per tick, and fall damage is cancelled for the ticks it paid
 * for. It runs dry, or comes off, and the wearer simply falls: it never writes the vanilla flight ability, so it can
 * neither grant nor revoke flight another mod gave (GGS-05-17). Creative players fly for free.
 *
 * <p>
 * Server owns the EU and the switch; the client applies the motion
 * ({@code com.gregguns.client.flight.FlightClient}). Five camos, cycled by the Camo Bench; the camo shows on the worn
 * model
 * ({@code com.gregguns.client.accessory}), in the name and in the tooltip.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md 4.5; docs/gregguns-spec/05-armor/powered-flight.md
// (GGS-05-17, GGS-05-18, GGS-05-19); docs/techguns-spec/05-armor/textures.md 10 (five AntiGravPack colors)
// Deviation: motion-based hover instead of Techguns' allowFlying creative flight (FLT-DEV-01 in implementation-notes).
public final class ItemAntiGravityDevice extends ItemElectricBackDevice implements CamoChangeable {

    /** Registry name. */
    public static final String NAME = "anti_gravity_device";
    /** Number of camos: default, bronze, black, dark green, silver. */
    public static final int CAMOS = 5;

    private static final String ENGAGED_TAG = "engaged";
    private static final String CAMO_TAG = "camo";

    @Override
    public Tier getTier() {
        return Tier.IV;
    }

    @Override
    protected double capacity() {
        return FlightConfig.antiGravityCapacityEu;
    }

    /**
     * @param stack an Anti Gravity Device
     * @return whether the switch is on
     */
    public static boolean engaged(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(ENGAGED_TAG);
    }

    private static void setEngaged(ItemStack stack, boolean on) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean(ENGAGED_TAG, on);
    }

    /**
     * @param stack an Anti Gravity Device
     * @return whether it holds the EU for one tick of drive
     */
    public static boolean powered(ItemStack stack) {
        return PowerUse.works(true, ElectricGear.charge(stack), FlightConfig.antiGravityDriveEuPerTick);
    }

    @Override
    public void onBackTick(EntityPlayer player, ItemStack stack) {
        FlightServer server = Flight.server();
        boolean engaged = engaged(stack);
        boolean powered = powered(stack);
        FlightMode mode = FlightPolicy.mode(
            BackKind.ANTI_GRAVITY,
            engaged,
            powered,
            Flight.airborne(player),
            player.isSneaking(),
            player.capabilities.isFlying);
        if (mode == FlightMode.FLY) {
            if (!player.capabilities.isCreativeMode) {
                double cost = PowerUse.drain(true, ElectricGear.charge(stack), FlightConfig.antiGravityDriveEuPerTick);
                ElectricGear.use(stack, cost, player);
            }
            // Same vertical formula the client predicts with (up is a client key, so the server hovers or sinks).
            player.motionY = FlightPhysics.verticalFlight(player.motionY, false, player.isSneaking());
            player.fallDistance = 0.0F;
            server.markAssisted(player);
            powered = powered(stack);
        }
        if (engaged && !powered) {
            // Empty: switch off so a later charge does not lift the player without a key press.
            setEngaged(stack, false);
            engaged = false;
            player.addChatMessage(new ChatComponentTranslation("gregguns.flight.anti_gravity.empty"));
        }
        server.publish(player, new FlightStatus(BackKind.ANTI_GRAVITY, engaged, powered));
    }

    @Override
    public void onBackKey(EntityPlayer player, ItemStack stack, boolean down) {
        if (!down) {
            return;
        }
        boolean on = !engaged(stack);
        if (on && !powered(stack)) {
            player.addChatMessage(new ChatComponentTranslation("gregguns.flight.anti_gravity.empty"));
            return;
        }
        setEngaged(stack, on);
        player.addChatMessage(
            new ChatComponentTranslation(on ? "gregguns.flight.anti_gravity.on" : "gregguns.flight.anti_gravity.off"));
    }

    @Override
    public void onBackRemoved(EntityPlayer player, Item item) {
        Flight.server()
            .forget(player);
    }

    // --- CamoChangeable ----------------------------------------------------------------------------------------------

    @Override
    public int camoCount(ItemStack stack) {
        return CAMOS;
    }

    @Override
    public int getCamo(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        int camo = tag == null ? 0 : tag.getInteger(CAMO_TAG);
        return camo < 0 || camo >= CAMOS ? 0 : camo;
    }

    @Override
    public void setCamo(ItemStack stack, int camo) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(CAMO_TAG, camo < 0 || camo >= CAMOS ? 0 : camo);
    }

    @Override
    public String camoNameKey(ItemStack stack) {
        return "item.gregguns." + NAME + ".camoname." + getCamo(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.flight.tooltip.anti_gravity"));
        tooltip.add(
            StatCollector.translateToLocal(
                engaged(stack) ? "gregguns.flight.tooltip.engaged_on" : "gregguns.flight.tooltip.engaged_off"));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.armor.tooltip.camo",
                StatCollector.translateToLocal(camoNameKey(stack))));
        super.addInformation(stack, player, tooltip, advanced);
    }
}
