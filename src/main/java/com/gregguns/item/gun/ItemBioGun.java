package com.gregguns.item.gun;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.ChargeRelease;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.projectile.BiogunProjectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Bio Gun item: an {@link ItemGun} whose released charge launches a glob of the charge level (1 to 3, the rounds
 * spent). The left button's plain shot is the level-1 glob the launcher builds. Server side for the shot.
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Bio Gun): the projectile is spawned with level = rounds used.
public class ItemBioGun extends ItemGun {

    /**
     * @param definition the Bio Gun's definition
     */
    public ItemBioGun(GunDefinition definition) {
        super(definition);
    }

    @Override
    public void onChargedShot(EntityPlayerMP player, ChargeRelease release, ShotOffset[] offsets) {
        World world = player.worldObj;
        for (ShotOffset offset : offsets) {
            BiogunProjectile glob = new BiogunProjectile(world);
            glob.fire(player, stats(), offset, 1.0f);
            glob.setLevel(release.units());
            world.spawnEntityInWorld(glob);
        }
    }

    /** Adds the "blobs disabled" line while {@code explosions.blockDamage} is off, so the missing blob is explained. */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        String key = GunTooltipFormat.blobGateKey(ExplosionsConfig.blockDamage);
        if (key != null) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(key));
        }
    }
}
