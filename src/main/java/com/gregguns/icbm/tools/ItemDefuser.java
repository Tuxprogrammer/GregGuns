package com.gregguns.icbm.tools;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.warhead.TileWarhead;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The defuser: right-click a lit warhead block to put its fuse out, left-click an unlaunched missile standing on a pad
 * or in a silo to take it apart (the body and warhead drop as items, the fuel is lost, as with the sneak-click
 * disarm). Once the countdown runs or the engines have lit, it refuses (D120). It takes durability, not energy: 32
 * successful uses, then it breaks (D121). Anyone may use it: defusing only makes a device safer (D120). Both sides; the
 * work is server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md (Defuser)
public final class ItemDefuser extends Item implements Tiered {

    /** The registry name. */
    public static final String NAME = "defuser";

    /** Creates the defuser. */
    public ItemDefuser() {
        setMaxStackSize(1);
        setMaxDamage(HandheldRules.DEFUSER_USES - 1);
        setNoRepair();
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    /**
     * Puts out the fuse of the warhead at a position, from any of its cells. No cost; the caller charges the tool.
     *
     * @return {@link DefuseResult#FUSE_OUT}, {@link DefuseResult#NOT_LIT} or {@link DefuseResult#NOT_A_TARGET}
     */
    public static DefuseResult defuseBlock(World world, int x, int y, int z) {
        TileWarhead warhead = Warheads.main(world, x, y, z);
        if (warhead == null) {
            return DefuseResult.NOT_A_TARGET;
        }
        return warhead.defuse() ? DefuseResult.FUSE_OUT : DefuseResult.NOT_LIT;
    }

    /**
     * Takes an unlaunched missile apart. No cost; the caller charges the tool.
     *
     * @return {@link DefuseResult#MISSILE_DISARMED}, {@link DefuseResult#COUNTING_DOWN} or
     *         {@link DefuseResult#IGNITED}
     */
    public static DefuseResult defuseMissile(EntityMissile missile) {
        if (missile.phase() == FlightPhase.PAD) {
            missile.disarm();
            return missile.isDead ? DefuseResult.MISSILE_DISARMED : DefuseResult.IGNITED;
        }
        return missile.phase() == FlightPhase.WARMUP && missile.abortable() ? DefuseResult.COUNTING_DOWN
            : DefuseResult.IGNITED;
    }

    private static void finish(EntityPlayer player, ItemStack stack, DefuseResult r) {
        player.addChatMessage(new ChatComponentTranslation(r.langKey()));
        if (r.success() && !player.capabilities.isCreativeMode) {
            stack.damageItem(1, player);
            if (stack.stackSize <= 0) {
                // damageItem empties the stack but leaves it in the hand on the left-click path.
                player.destroyCurrentEquippedItem();
            }
        }
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            // Never consume the click here: PlayerControllerMP skips the block-placement packet when this returns
            // true, and the server would never defuse (I4 review item 1).
            return HandheldRules.clientConsumesUse();
        }
        DefuseResult r = defuseBlock(world, x, y, z);
        if (r == DefuseResult.NOT_A_TARGET) {
            return false;
        }
        finish(player, stack, r);
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!(entity instanceof EntityMissile)) {
            return false;
        }
        if (!player.worldObj.isRemote) {
            finish(player, stack, defuseMissile((EntityMissile) entity));
        }
        // Whatever the answer, the click was meant for the defuser: do not also hit the missile.
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(getTier()));
        List<String> details = new ArrayList<>();
        details.add(StatCollector.translateToLocal("gregguns.icbm.defuser.tooltip.block"));
        details.add(StatCollector.translateToLocal("gregguns.icbm.defuser.tooltip.missile"));
        details.add(StatCollector.translateToLocal("gregguns.icbm.defuser.tooltip.refused"));
        details.add(
            String.format(
                StatCollector.translateToLocal("gregguns.icbm.defuser.tooltip.uses"),
                stack.getMaxDamage() + 1 - stack.getItemDamage()));
        tooltip.addAll(
            HandheldTooltipFormat.lines(
                tierLine,
                StatCollector.translateToLocal("gregguns.icbm.defuser.summary"),
                details,
                StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift"),
                shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
