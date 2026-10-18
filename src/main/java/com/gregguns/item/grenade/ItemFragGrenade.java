package com.gregguns.item.grenade;

import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.explosion.FragMath;
import com.gregguns.projectile.FragGrenadeProjectile;
import com.gregguns.projectile.GrenadeMath;
import com.gregguns.projectile.GrenadeProjectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Frag Grenade (MV): a 25 damage, radius 6 blast with 64 steel fragments, two bounces and a 120 tick fuse. Throw
 * rules and deviations: {@link ItemThrownGrenade}. The wind-up plays {@link #PIN} and the throw {@link #THROW}.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Frag Grenade); docs/design/0001-tier-map.md
public final class ItemFragGrenade extends ItemThrownGrenade {

    /** Damage at the centre of the blast, half hearts. */
    public static final float DAMAGE = 25.0F;
    /** Blast radius, blocks. */
    public static final float RADIUS = 6.0F;
    /** Event: the pin pulled on wind-up. */
    public static final String PIN = "guns.grenade.pin";
    /** Event: the throw. */
    public static final String THROW = "guns.grenade.throw";

    @Override
    public Tier getTier() {
        return Tier.MV;
    }

    @Override
    protected GrenadeProjectile launch(World world, EntityPlayer player, double charge) {
        FragGrenadeProjectile grenade = new FragGrenadeProjectile(world);
        grenade.throwFrom(player, charge, DAMAGE, RADIUS, GrenadeMath.FRAG_BOUNCES, "frag_grenade");
        world.spawnEntityInWorld(grenade);
        return grenade;
    }

    @Override
    protected void playPinSound(World world, EntityPlayer player) {
        playEvent(world, player, PIN);
    }

    @Override
    protected void playThrowSound(World world, EntityPlayer player, double charge) {
        playEvent(world, player, THROW);
    }

    @Override
    protected float tooltipDamage() {
        return DAMAGE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addDetails(List<String> tooltip) {
        ItemStielgranate.addBlastDetails(tooltip, RADIUS, GrenadeMath.FRAG_BOUNCES, GrenadeMath.FRAG_FUSE_TICKS);
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.fragments",
                FragMath.COUNT,
                String.format(Locale.ROOT, "%.1f", FragMath.DAMAGE_MAX)));
    }
}
