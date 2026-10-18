package com.gregguns.item.grenade;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tiered;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.projectile.GrenadeMath;
import com.gregguns.projectile.GrenadeProjectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base of the hand grenades: hold the use button to wind up, release to throw. The longer the hold the faster and
 * flatter the throw (full at 20 ticks, {@link GrenadeMath#charge}). A subclass says what it throws ({@link #launch}),
 * its sounds, and its tooltip numbers.
 *
 * <p>
 * Deviation: the throw consumes one grenade from the stack being held, not the first matching stack in the inventory
 * (GUN-30), and a short wind-up cannot divide gravity by zero (GUN-22, see {@link GrenadeMath#gravity}). The use
 * duration is the 72000 ticks the vanilla bow uses (GUN-23: the 144000 Techguns stored was ignored).
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (grenade sheets); docs/design/0001-tier-map.md
public abstract class ItemThrownGrenade extends Item implements Tiered {

    /** Stack size of every hand grenade. */
    public static final int STACK_SIZE = 16;
    /** Use duration, ticks. */
    public static final int USE_DURATION = 72000;

    /** Sets the stack size and no repair. */
    protected ItemThrownGrenade() {
        setMaxStackSize(STACK_SIZE);
        setNoRepair();
    }

    /**
     * Builds, aims and spawns the thrown entity. Server side.
     *
     * @param world  the world
     * @param player the thrower
     * @param charge the throw charge, 0 to 1
     * @return the entity that was spawned
     */
    protected abstract GrenadeProjectile launch(World world, EntityPlayer player, double charge);

    /** @return the damage shown in the tooltip, half hearts; 0 hides the damage lines */
    protected abstract float tooltipDamage();

    /**
     * Plays the throw sound. The default is the vanilla bow release the Stielgranate uses.
     *
     * @param world  the world
     * @param player the thrower
     * @param charge the throw charge, 0 to 1
     */
    protected void playThrowSound(World world, EntityPlayer player, double charge) {
        // Source: Techguns plays vanilla random.bow at this pitch (sheet section 8).
        world.playSoundAtEntity(
            player,
            "random.bow",
            1.0F,
            1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + 0.5F * (float) charge);
    }

    /**
     * Plays the wind-up sound on right-click; the default is none.
     *
     * @param world  the world
     * @param player the thrower
     */
    protected void playPinSound(World world, EntityPlayer player) {}

    /**
     * Plays a GregGuns sound event at the thrower, to everyone in range. Server only (a client world is ignored).
     *
     * @param world  the world
     * @param player the thrower
     * @param event  an unprefixed GregGuns sound event
     */
    protected static void playEvent(World world, EntityPlayer player, String event) {
        GunSoundPlayer
            .play(world, player.posX, player.posY + player.getEyeHeight(), player.posZ, event, 1.0F, 1.0F, null);
    }

    /**
     * Adds the shift-tooltip lines after the damage type.
     *
     * @param tooltip the lines
     */
    @SideOnly(Side.CLIENT)
    protected abstract void addDetails(List<String> tooltip);

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.bow;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        playPinSound(world, player);
        return stack;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        int held = getMaxItemUseDuration(stack) - timeLeft;
        double charge = GrenadeMath.charge(held);
        player.swingItem();
        if (world.isRemote) {
            return;
        }
        launch(world, player, charge);
        playThrowSound(world, player, charge);
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
            if (stack.stackSize <= 0) {
                player.destroyCurrentEquippedItem();
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        float damage = tooltipDamage();
        if (damage > 0.0F) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.gun.tooltip.damage",
                    String.format(Locale.ROOT, "%.1f", damage)));
        }
        if (!GuiScreen.isShiftKeyDown()) {
            tooltip.add(StatCollector.translateToLocal("gregguns.gun.tooltip.shift"));
            return;
        }
        if (damage > 0.0F) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.gun.tooltip.damage_type",
                    StatCollector.translateToLocal(DamageType.EXPLOSION.nameLangKey())));
        }
        addDetails(tooltip);
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gregguns.grenade.tooltip.hold"));
    }
}
