package com.gregguns.icbm.launcher;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.icbm.warhead.WarheadBlasts;

/**
 * The seam between the GregGuns launchers and the ICBM blasts (D12, D150): the stack NBT of a loaded warhead, spending
 * a charge at fire time and starting the blast. The projectiles call {@link #consume} when they are fired and
 * {@link #detonate} where they would explode. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public final class LauncherWarheads {

    /** The compound on the gun stack. */
    public static final String TAG = "gg_launcher_warhead";
    private static final String KEY = "key";
    private static final String CHARGES = "charges";

    private LauncherWarheads() {}

    /**
     * @param gun a launcher stack
     * @return the warhead loaded on it, or null for none or an unknown key
     */
    public static LauncherWarhead loaded(ItemStack gun) {
        if (gun == null || !gun.hasTagCompound()
            || !gun.getTagCompound()
                .hasKey(TAG, 10)) {
            return null;
        }
        NBTTagCompound t = gun.getTagCompound()
            .getCompoundTag(TAG);
        return t.getInteger(CHARGES) > 0 ? LauncherWarhead.byKey(t.getString(KEY)) : null;
    }

    /**
     * @param gun a launcher stack
     * @return whether any warhead is loaded on it
     */
    public static boolean hasWarhead(ItemStack gun) {
        return loaded(gun) != null;
    }

    /**
     * @param gun a launcher stack
     * @return the charges loaded, 0 for none
     */
    public static int charges(ItemStack gun) {
        return loaded(gun) == null ? 0
            : gun.getTagCompound()
                .getCompoundTag(TAG)
                .getInteger(CHARGES);
    }

    /**
     * Writes a loaded warhead; zero charges removes it.
     *
     * @param gun     a launcher stack
     * @param warhead the warhead
     * @param charges the charges, 0 to clear
     */
    public static void set(ItemStack gun, LauncherWarhead warhead, int charges) {
        if (charges <= 0) {
            if (gun.hasTagCompound()) {
                gun.getTagCompound()
                    .removeTag(TAG);
            }
            return;
        }
        if (!gun.hasTagCompound()) {
            gun.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound t = new NBTTagCompound();
        t.setString(KEY, warhead.key());
        t.setInteger(CHARGES, charges);
        gun.getTagCompound()
            .setTag(TAG, t);
    }

    /**
     * Spends one charge of the warhead on the shooter's held launcher.
     *
     * @param shooter the shooter
     * @param gunName the registry name of the gun firing
     * @return the warhead key to carry on the projectile, or null when none is loaded, the gun does not fit it or the
     *         ICBM module is off
     */
    public static String consume(EntityLivingBase shooter, String gunName) {
        if (shooter == null || shooter.worldObj.isRemote || !ModuleState.isActive(ModuleId.ICBM)) {
            return null;
        }
        ItemStack held = shooter.getHeldItem();
        LauncherWarhead w = loaded(held);
        if (w == null || !w.kind()
            .fits(gunName)) {
            return null;
        }
        set(held, w, LauncherRules.afterShot(charges(held)));
        return w.key();
    }

    /**
     * Starts the ICBM blast of a warhead key.
     *
     * @param key     a key from {@link #consume}, or null
     * @param shooter the shooter, or null
     * @return true when a blast started; false means the projectile should explode as it normally does
     */
    public static boolean detonate(World world, double x, double y, double z, String key, EntityLivingBase shooter) {
        LauncherWarhead w = key == null ? null : LauncherWarhead.byKey(key);
        if (w == null || world.isRemote) {
            return false;
        }
        WarheadBlasts.registerAll();
        ExplosiveDef def = Explosives.get(w.blastId());
        return def != null && IcbmBlasts.detonate(world, x, y, z, def, LauncherRules.FILL, shooter);
    }
}
