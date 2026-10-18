package com.gregguns.item.grenade;

import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.projectile.GrenadeMath;
import com.gregguns.projectile.GrenadeProjectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Stielgranate (stick grenade). The thrown {@link GrenadeProjectile} bounces three times and explodes after 200
 * ticks, on touching an entity, or on a fourth hard hit. Throw rules and deviations: {@link ItemThrownGrenade}.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Stielgranate); docs/design/0001-tier-map.md
public final class ItemStielgranate extends ItemThrownGrenade {

    /** Damage at the centre of the blast, half hearts. */
    public static final float DAMAGE = 20.0F;
    /** Blast radius, blocks. */
    public static final float RADIUS = 5.0F;

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    protected GrenadeProjectile launch(World world, EntityPlayer player, double charge) {
        GrenadeProjectile grenade = new GrenadeProjectile(world);
        grenade.throwFrom(player, charge, DAMAGE, RADIUS, GrenadeMath.STIELGRANATE_BOUNCES, "stielgranate");
        world.spawnEntityInWorld(grenade);
        return grenade;
    }

    @Override
    protected float tooltipDamage() {
        return DAMAGE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addDetails(List<String> tooltip) {
        addBlastDetails(tooltip, RADIUS, GrenadeMath.STIELGRANATE_BOUNCES, GrenadeMath.FUSE_TICKS);
    }

    /**
     * The radius, bounces and fuse lines every exploding grenade shows.
     *
     * @param tooltip the lines
     * @param radius  blast radius, blocks
     * @param bounces bounces
     * @param fuse    fuse, ticks
     */
    @SideOnly(Side.CLIENT)
    static void addBlastDetails(List<String> tooltip, float radius, int bounces, int fuse) {
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.radius",
                String.format(Locale.ROOT, "%.1f", radius)));
        tooltip.add(StatCollector.translateToLocalFormatted("gregguns.grenade.tooltip.bounces", bounces));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.fuse",
                String.format(Locale.ROOT, "%.0f", fuse / 20.0)));
    }
}
