package com.gregguns.item.gun;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.ChargeRelease;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.projectile.BiogunProjectile;

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
}
