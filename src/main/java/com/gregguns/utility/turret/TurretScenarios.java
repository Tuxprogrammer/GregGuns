package com.gregguns.utility.turret;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.entity.TurretEntity;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * In-game scenarios for the Turret ({@code utility.turret_*}): registration facts, a turret that kills a zombie,
 * owner and friends being spared, and a base whose link, entity and block are abused without a single exception. The
 * Turret is a real GT block ticking in the world. Dev environment only, registered from {@code TurretRegistration}.
 * Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-01, GGS-09-02, GGS-09-03, GGS-09-04, GGS-09-07),
// docs/techguns-spec/BUGS.md (NPC-12, MUTL-25)
public final class TurretScenarios {

    private static final int AREA_RADIUS = 16;
    private static final String OWNER_NAME = "gregguns-turret-owner";

    private TurretScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new RegisteredScenario());
        registry.register(new KillsZombieScenario());
        registry.register(new SparesOwnerScenario());
        registry.register(new NoNpeScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------

    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private static FakePlayer player(WorldServer world, String name) {
        return new FakePlayer(world, new GameProfile(uuid(name), name));
    }

    private static IGregTechTileEntity place(ScenarioContext ctx, int id, UUID owner) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        ctx.setBlock(0, 0, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException("no GT tile entity at the origin");
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setOwnerName(OWNER_NAME);
        base.setOwnerUuid(owner);
        return base;
    }

    /** Ticks to let GT finish initializing a freshly placed tile entity before it is used. */
    private static final int WARMUP_TICKS = 45;

    /**
     * The turret base the world holds at the origin right now, or null. GT may replace a tile entity during its first
     * ticks, so scenarios fetch it again after the warm-up instead of keeping the placement-tick handle.
     */
    private static IGregTechTileEntity live(ScenarioContext ctx) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
        return tile instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) tile).getMetaTileEntity() instanceof MTETurret ? (IGregTechTileEntity) tile
                : null;
    }

    private static void arm(IGregTechTileEntity base, String gun, String ammo, int ammoCount) {
        base.setInventorySlotContents(MTETurret.GUN_SLOT, new ItemStack(GunItems.get(gun)));
        Item ammoItem = GameRegistry.findItem(GregGuns.MODID, ammo);
        base.setInventorySlotContents(0, new ItemStack(ammoItem, ammoCount));
    }

    private static void power(IGregTechTileEntity base) {
        base.increaseStoredEnergyUnits(base.getEUCapacity() - base.getStoredEU(), true);
    }

    private static void empty(IGregTechTileEntity base) {
        for (int i = 0; i < base.getSizeInventory(); i++) {
            base.setInventorySlotContents(i, null);
        }
    }

    private static MTETurret mte(IGregTechTileEntity base) {
        return (MTETurret) base.getMetaTileEntity();
    }

    // --- scenarios --------------------------------------------------------------------------------------------

    /** Registration facts. */
    private static final class RegisteredScenario implements Scenario {

        @Override
        public String name() {
            return "utility.turret_registered";
        }

        @Override
        public String description() {
            return "The four Turrets are MTETurret at the right ids and tiers; the turret entity is registered and is neither a mob nor an animal.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (TurretRecipeSpec.Row row : TurretRecipeSpec.Row.values()) {
                Object mte = GregTechAPI.METATILEENTITIES[row.mteId()];
                out.check(
                    mte instanceof MTETurret,
                    row.tier() + " Turret (" + row.mteId() + ") should be an MTETurret");
                if (mte instanceof MTETurret) {
                    out.checkEquals(row.tier() + " Turret tier", row.tier(), ((MTETurret) mte).turretTier());
                }
            }
            out.checkEquals("MV id", GregGunsMTEIDs.TURRET_MV, TurretRecipeSpec.Row.MV.mteId());
            out.check(
                net.minecraft.entity.EntityList.stringToClassMapping.get("gregguns.turret") == TurretEntity.class,
                "the turret entity should be registered as gregguns.turret");
            TurretEntity entity = new TurretEntity(ctx.world());
            out.check(
                !(entity instanceof net.minecraft.entity.monster.IMob),
                "the turret must not be a hostile mob (Infernal Mobs)");
            out.check(entity instanceof net.minecraft.entity.IEntityOwnable, "the turret must be IEntityOwnable");
            out.check(Math.abs(entity.getMaxHealth() - 30.0F) < 1.0e-6, "max health should be 30");
            return out.build();
        }
    }

    /** A gun, ammo and power: a zombie within range dies. */
    private static final class KillsZombieScenario implements Scenario {

        @Override
        public String name() {
            return "utility.turret_kills_zombie";
        }

        @Override
        public String description() {
            return "A powered MV Turret with a 9mm Pistol and a magazine shoots a zombie dead; the empty magazine is handed back.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ctx.clearArea(AREA_RADIUS, 6);
            place(ctx, GregGunsMTEIDs.TURRET_MV, uuid(OWNER_NAME));
            EntityZombie zombie = new EntityZombie(ctx.world());
            zombie.setLocationAndAngles(ctx.originX() + 0.5 + 9, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            zombie.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 4000));
            IGregTechTileEntity[] held = { null };
            return ScenarioResult.pending(name(), 700 + WARMUP_TICKS, ticks -> {
                if (held[0] == null) {
                    // Arm the turret and spawn the target only after the warm-up, on the world's current tile.
                    if (ticks < WARMUP_TICKS) {
                        return null;
                    }
                    IGregTechTileEntity fresh = live(ctx);
                    if (fresh == null) {
                        return ScenarioResult.fail(name(), "the turret base is gone after the warm-up");
                    }
                    arm(fresh, "pistol_9mm", "mag_9mm_pistol", 4);
                    // One round loaded, so the second shot reloads from the slots and hands back an empty magazine.
                    ItemStack mounted = fresh.getStackInSlot(MTETurret.GUN_SLOT);
                    mounted.setTagCompound(new NBTTagCompound());
                    GunNbt.write(mounted.getTagCompound(), new GunState(1));
                    ctx.spawn(zombie);
                    held[0] = fresh;
                }
                IGregTechTileEntity base = live(ctx);
                if (base == null) {
                    return ScenarioResult.fail(name(), "the turret base vanished while the scenario ran");
                }
                power(base);
                if (!zombie.isDead && zombie.getHealth() > 0) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    mte(base).linkedTurret() != null || mte(base).logic()
                        .isDead(),
                    "the base should have a turret");
                int empties = 0;
                for (int i = MTETurret.OUT_FIRST; i < MTETurret.OUT_END; i++) {
                    ItemStack s = base.getStackInSlot(i);
                    if (s != null) {
                        empties += s.stackSize;
                    }
                }
                out.checkEquals("empty magazines returned", 1, empties);
                ItemStack gun = base.getStackInSlot(MTETurret.GUN_SLOT);
                if (gun == null) {
                    out.check(false, "the gun slot is unexpectedly empty");
                } else {
                    GunState state = GunNbt.read(gun.getTagCompound(), ((ItemGun) gun.getItem()).stats());
                    out.check(state.loaded() < 18, "the gun should have fired rounds");
                }
                ItemStack ammoLeft = base.getStackInSlot(0);
                out.check(ammoLeft != null, "the ammo slot is unexpectedly empty");
                out.checkEquals("magazines left in the ammo slot", 3, ammoLeft == null ? -1 : ammoLeft.stackSize);
                empty(base);
                return out.build();
            });
        }
    }

    /** Owner and friends are spared at level 2, a stranger is not, and mobs always are. */
    private static final class SparesOwnerScenario implements Scenario {

        @Override
        public String name() {
            return "utility.turret_spares_owner";
        }

        @Override
        public String description() {
            return "At security 2 the turret does not target its owner or a stub friend, targets a second player and a zombie; at 0 it targets no player; animals follow the toggle.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ctx.clearArea(AREA_RADIUS, 6);
            UUID owner = uuid(OWNER_NAME);
            UUID friendId = uuid("gregguns-turret-friend");
            place(ctx, GregGunsMTEIDs.TURRET_HV, owner);
            WorldServer world = (WorldServer) ctx.world();
            FakePlayer ownerPlayer = player(world, OWNER_NAME);
            FakePlayer friend = player(world, "gregguns-turret-friend");
            FakePlayer stranger = player(world, "gregguns-turret-stranger");
            EntityZombie zombie = new EntityZombie(world);
            EntityPig pig = new EntityPig(world);
            boolean[] armed = { false };
            return ScenarioResult.pending(name(), 120 + WARMUP_TICKS, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx);
                if (base == null) {
                    return ScenarioResult.fail(name(), "the turret base is gone after the warm-up");
                }
                if (!armed[0]) {
                    arm(base, "pistol_9mm", "mag_9mm_pistol", 1);
                    power(base);
                    mte(base).setFriendProvider((o, other) -> o.equals(owner) && other.equals(friendId));
                    armed[0] = true;
                }
                TurretEntity turret = mte(base).linkedTurret();
                if (turret == null) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                setSecurity(base, SecurityLevel.PUBLIC);
                check(out, "level 0 spares a stranger", false, turret, stranger);
                check(out, "level 0 shoots a zombie", true, turret, zombie);
                setSecurity(base, SecurityLevel.OWNER);
                check(out, "level 2 spares the owner", false, turret, ownerPlayer);
                check(out, "level 2 spares a friend", false, turret, friend);
                check(out, "level 2 shoots a stranger", true, turret, stranger);
                check(out, "level 2 shoots a zombie", true, turret, zombie);
                setSecurity(base, SecurityLevel.FRIENDS);
                check(out, "level 1 spares a friend", false, turret, friend);
                check(out, "level 1 shoots a stranger", true, turret, stranger);
                mte(base).setFriendProvider(null);
                check(out, "level 1 without friends spares the owner", false, turret, ownerPlayer);
                check(out, "level 1 without friends shoots the old friend (GGS-09-04)", true, turret, friend);
                check(out, "animals are spared by default", false, turret, pig);
                mte(base).toggleAnimals(ownerPlayer);
                check(out, "animals are shot with Target Animals on", true, turret, pig);
                out.check(!mte(base).cycleSecurity(stranger), "a stranger must not change the security level");
                out.check(mte(base).cycleSecurity(ownerPlayer), "the owner may change the security level");
                empty(base);
                return out.build();
            });
        }

        private static void setSecurity(IGregTechTileEntity base, SecurityLevel level) {
            FakePlayer owner = player((WorldServer) base.getWorld(), OWNER_NAME);
            while (mte(base).security() != level) {
                mte(base).cycleSecurity(owner);
            }
        }

        private static void check(ScenarioResult.Builder out, String what, boolean expected, TurretEntity turret,
            EntityLivingBase target) {
            out.checkEquals(what, expected, turret.wouldTarget(target));
        }
    }

    /** Killing the entity, planting a stale link, an orphan turret and breaking the block throw nothing. */
    private static final class NoNpeScenario implements Scenario {

        @Override
        public String name() {
            return "utility.turret_no_npe";
        }

        @Override
        public String description() {
            return "The turret dies and respawns after the repair, a link to a zombie respawns it, a turret with no base dies quietly, and breaking the base kills the turret: no exception.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ctx.clearArea(AREA_RADIUS, 6);
            place(ctx, GregGunsMTEIDs.TURRET_MV, uuid(OWNER_NAME));
            EntityZombie zombie = new EntityZombie(ctx.world());
            zombie.setLocationAndAngles(ctx.originX() + 0.5 + 40, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            zombie.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 4000));
            ctx.spawn(zombie);
            int[] stage = { 0 };
            TurretEntity[] first = { null };
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), 900 + WARMUP_TICKS, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx);
                if (base == null) {
                    return ScenarioResult.fail(name(), "the turret base is gone after the warm-up");
                }
                power(base);
                try {
                    MTETurret turret = mte(base);
                    switch (stage[0]) {
                        case 0:
                            if (turret.linkedTurret() == null) {
                                return null;
                            }
                            first[0] = turret.linkedTurret();
                            first[0].attackEntityFrom(DamageSource.outOfWorld, 1000.0F);
                            stage[0] = 1;
                            return null;
                        case 1:
                            if (!turret.logic()
                                .isDead()) {
                                return null;
                            }
                            out.check(turret.linkedTurret() == null, "a dead turret must be unlinked");
                            TurretEntity orphan = new TurretEntity(ctx.world());
                            orphan.setLocationAndAngles(ctx.originX() + 5.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
                            ctx.spawn(orphan);
                            orphan.attackEntityFrom(DamageSource.outOfWorld, 1000.0F);
                            out.check(
                                turret.logic()
                                    .isDead(),
                                "an orphan's death must not change the base (NPC-12)");
                            stage[0] = 2;
                            return null;
                        case 2:
                            if (turret.linkedTurret() == null) {
                                return null;
                            }
                            first[0] = turret.linkedTurret();
                            turret.setLinkedId(zombie.getUniqueID());
                            first[0].setDead();
                            stage[0] = 3;
                            return null;
                        case 3:
                            if (turret.linkedTurret() == null) {
                                return null;
                            }
                            out.check(
                                turret.linkedTurret() != first[0] && !turret.linkedTurret().isDead,
                                "a link to a zombie must respawn a new turret (MUTL-25)");
                            TurretEntity live = turret.linkedTurret();
                            NBTTagCompound tag = new NBTTagCompound();
                            turret.saveNBTData(tag);
                            tag.setString("turretUuid", "not-a-uuid");
                            turret.loadNBTData(tag);
                            out.check(turret.linkedId() == null, "a malformed saved link must read as unlinked");
                            turret.onBlockDestroyed();
                            out.check(live.isDead, "breaking the base must kill the mounted turret");
                            out.check(turret.linkedTurret() == null, "the base must drop the link");
                            empty(base);
                            return out.build();
                        default:
                            return null;
                    }
                } catch (RuntimeException e) {
                    GregGuns.LOG.error("utility.turret_no_npe threw", e);
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }
    }
}
