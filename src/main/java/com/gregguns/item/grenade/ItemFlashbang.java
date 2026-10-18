package com.gregguns.item.grenade;

import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.explosion.FlashMath;
import com.gregguns.projectile.FlashbangProjectile;
import com.gregguns.projectile.GrenadeMath;
import com.gregguns.projectile.GrenadeProjectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Flashbang (MV): a thrown grenade that blinds and disorients everything with a line of sight within 8 blocks and
 * hurts nothing and no block. Two bounces, a 40 tick fuse. Throw rules and deviations: {@link ItemThrownGrenade}.
 * Techguns never registered its flashbang (GUN-24); this is a finished design, see
 * {@code docs/gregguns-spec/04-projectiles-fx/grenades.md}.
 */
// Spec: docs/design/0001-tier-map.md (Flashbang, MV, build as a finished design);
// docs/gregguns-spec/04-projectiles-fx/grenades.md
public final class ItemFlashbang extends ItemThrownGrenade {

    @Override
    public Tier getTier() {
        return Tier.MV;
    }

    @Override
    protected GrenadeProjectile launch(World world, EntityPlayer player, double charge) {
        FlashbangProjectile grenade = new FlashbangProjectile(world);
        // Damage and radius are unused by a flash; the entity carries 0 so nothing could hurt by mistake.
        grenade.throwFrom(player, charge, 0.0F, 0.0F, GrenadeMath.FLASHBANG_BOUNCES, "flashbang");
        world.spawnEntityInWorld(grenade);
        return grenade;
    }

    @Override
    protected void playPinSound(World world, EntityPlayer player) {
        playEvent(world, player, ItemFragGrenade.PIN);
    }

    @Override
    protected void playThrowSound(World world, EntityPlayer player, double charge) {
        playEvent(world, player, ItemFragGrenade.THROW);
    }

    @Override
    protected float tooltipDamage() {
        return 0.0F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addDetails(List<String> tooltip) {
        tooltip.add(StatCollector.translateToLocal("gregguns.grenade.tooltip.flash"));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.flash_range",
                String.format(Locale.ROOT, "%.0f", FlashMath.RADIUS),
                String.format(Locale.ROOT, "%.0f", FlashMath.MAX_TICKS / 20.0)));
        tooltip.add(
            StatCollector.translateToLocalFormatted("gregguns.grenade.tooltip.bounces", GrenadeMath.FLASHBANG_BOUNCES));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.fuse",
                String.format(Locale.ROOT, "%.0f", GrenadeMath.FLASHBANG_FUSE_TICKS / 20.0)));
    }
}
