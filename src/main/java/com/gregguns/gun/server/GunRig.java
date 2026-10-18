package com.gregguns.gun.server;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.PlayerAmmoStore;
import com.gregguns.projectile.GenericProjectile;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The test rig behind the {@code gun.*} scenarios, for any gun: a {@link FakePlayer} holding a real gun stack in hotbar
 * slot 0, facing east, with a stone-floored corridor in front of it. It drives the gun through the same
 * {@link ServerGunHandler} entry points the network packets reach, and counts the shots and projectiles it sees. A new
 * gun's scenarios build one with its {@link ItemGun} and write only the expectations. Dev environment only, server
 * side, main thread.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     GunRig rig = new GunRig(ctx, GunItems.revolver(), GunCatalog.REVOLVER.magazineSize());
 *     EntityZombie target = GunRig.zombie(ctx, 6);
 *     rig.press();
 *     rig.release();
 *     rig.tick(); // the shot
 * }
 * </pre>
 */
public final class GunRig {

    private static final float EPS = 0.05F;
    private static final float EAST_YAW = -90.0F;
    private static final String SHOOTER_NAME = "[GregGunsGunTest]";

    /** The scenario context the rig was built in. */
    public final ScenarioContext ctx;
    /** The shooter, holding {@link #gun} in hotbar slot 0. */
    public final FakePlayer shooter;
    /** The gun stack. */
    public final ItemStack gun;
    private final GunStats stats;
    private final Map<GenericProjectile, Boolean> seen = new IdentityHashMap<>();
    /** Projectiles already in the world when the rig was built (earlier scenarios'): never counted. */
    private final Map<GenericProjectile, Boolean> before = new IdentityHashMap<>();
    private int shots;
    private int lastLoaded;

    /**
     * Clears a corridor 12 blocks long and builds the shooter.
     *
     * @param ctx    the scenario context
     * @param item   the gun to hold
     * @param loaded rounds loaded at the start
     */
    public GunRig(ScenarioContext ctx, ItemGun item, int loaded) {
        this(ctx, item, loaded, 12);
    }

    /**
     * As {@link #GunRig(ScenarioContext, ItemGun, int)} with a corridor of a chosen length.
     *
     * @param length blocks east of the shooter to clear and floor
     */
    public GunRig(ScenarioContext ctx, ItemGun item, int loaded, int length) {
        this.ctx = ctx;
        this.stats = item.stats();
        for (int dx = -1; dx <= length; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                for (int dy = 0; dy <= 3; dy++) {
                    ctx.setBlock(dx, dy, dz, Blocks.air, 0);
                }
            }
        }
        shooter = fakePlayer(ctx, SHOOTER_NAME, 0);
        gun = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort(GunNbt.KEY_AMMO, (short) loaded);
        tag.setByte(GunNbt.KEY_CAMO, (byte) 0);
        gun.setTagCompound(tag);
        shooter.inventory.currentItem = 0;
        shooter.inventory.setInventorySlotContents(0, gun);
        lastLoaded = loaded;
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof GenericProjectile) {
                before.put((GenericProjectile) o, Boolean.TRUE);
            }
        }
    }

    /** Puts items into the shooter's inventory after the gun. */
    public void give(String registryName, int count) {
        Item item = GameRegistry.findItem("gregguns", registryName);
        shooter.inventory.addItemStackToInventory(new ItemStack(item, count));
    }

    /** @return how many items of this registry name the shooter carries */
    public int count(String registryName) {
        return new PlayerAmmoStore(shooter).count(registryName);
    }

    /** @return rounds loaded in the gun stack now */
    public int loaded() {
        return GunNbt.read(gun.getTagCompound(), stats)
            .loaded();
    }

    /** @return true while the shooter's reload is running */
    public boolean reloading() {
        return GunPlayerData.get(shooter)
            .controller()
            .reload()
            .isReloading();
    }

    /** @return rounds spent since the rig was built, from the NBT going down */
    public int shots() {
        return shots;
    }

    /** Presses the fire button. */
    public void press() {
        ServerGunHandler.onTrigger(shooter, true);
    }

    /** Releases the fire button. */
    public void release() {
        ServerGunHandler.onTrigger(shooter, false);
    }

    /** One server tick of the gun logic (what the player tick event does), then a note of new bullets. */
    public void tick() {
        ServerGunHandler.onPlayerTick(shooter);
        int now = loaded();
        if (now < lastLoaded) {
            shots += lastLoaded - now;
        }
        lastLoaded = now;
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof GenericProjectile && !before.containsKey(o)) {
                seen.putIfAbsent((GenericProjectile) o, Boolean.TRUE);
            }
        }
    }

    /** @return how many distinct projectiles have been seen in the world */
    public int bullets() {
        return seen.size();
    }

    /** @return true when every projectile seen has ended */
    public boolean bulletsGone() {
        for (GenericProjectile p : seen.keySet()) {
            if (!p.isDead) {
                return false;
            }
        }
        return true;
    }

    // ----- shared helpers ----------------------------------------------------------------------------------------

    /** A fake player that can be hurt: a fresh server player is invulnerable for its first ticks. */
    private static final class Mortal extends FakePlayer {

        Mortal(WorldServer world, GameProfile profile) {
            super(world, profile);
            // The spawn-protection counter (ticks of immunity after joining); a fake player is never ticked down.
            clearSpawnProtection();
        }

        @Override
        public boolean isEntityInvulnerable() {
            return false;
        }

        @Override
        public boolean canAttackPlayer(EntityPlayer other) {
            // Forge's FakePlayer refuses all player-versus-player damage; the scenario is exactly that case.
            return true;
        }

        private void clearSpawnProtection() {
            try {
                // field_147101_bU is private in EntityPlayerMP and has no accessor; the dev environment keeps SRG
                // names.
                java.lang.reflect.Field field = EntityPlayerMP.class.getDeclaredField("field_147101_bU");
                field.setAccessible(true);
                field.setInt(this, 0);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot clear the fake player's spawn protection", e);
            }
        }
    }

    /**
     * A fake player facing east at {@code dx} blocks east of the scenario origin, mortal and past its spawn protection.
     */
    public static FakePlayer fakePlayer(ScenarioContext ctx, String name, int dx) {
        FakePlayer fake = new Mortal(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        fake.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, EAST_YAW, 0.0F);
        fake.rotationYawHead = EAST_YAW;
        return fake;
    }

    /** A zombie {@code dx} blocks east of the origin facing the shooter, wearing a pumpkin so it never burns. */
    public static EntityZombie zombie(ScenarioContext ctx, int dx) {
        EntityZombie z = new EntityZombie(ctx.world());
        z.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
        // A pumpkin is not armor, but it stops the zombie burning in daylight.
        z.setCurrentItemOrArmor(4, new ItemStack(Blocks.pumpkin));
        return ctx.spawn(z);
    }

    /** The health a target should lose from {@code rawDamage} of projectile damage, per the damage model. */
    public static float expectedLoss(EntityLivingBase target, double rawDamage) {
        return expectedLoss(target, rawDamage, 0.0);
    }

    /**
     * As {@link #expectedLoss(EntityLivingBase, double)} for a gun with armor penetration.
     *
     * @param penetration the gun's armor penetration, 0 to 1
     */
    public static float expectedLoss(EntityLivingBase target, double rawDamage, double penetration) {
        double armor = target.getTotalArmorValue();
        return (float) DamageModel.resolve(
            DamageType.PROJECTILE,
            rawDamage,
            penetration,
            share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    /** @return true if two health values agree to within a twentieth of a half heart */
    public static boolean near(float expected, float actual) {
        return Math.abs(expected - actual) < EPS;
    }
}
