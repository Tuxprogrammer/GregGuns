package com.gregguns.icbm.missile;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.detect.ContactMath;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadData;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gregguns.utility.security.OwnerProfiles;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A missile from the moment it is armed on the pad to its impact: one entity for the whole life (D25), not a rocket.
 * It does not extend Galacticraft's {@code EntityAutoRocket}, so its launch never removes the pad (D32); it only
 * implements the docking and fuel interfaces, behind {@code @Optional}, so the pad's fuel loader can fill it and
 * {@code RocketFuels} can gate the fuel. The position is a function of the flight tick ({@link ArcPath}), so a reload
 * resumes exactly. Impact is the only fuse (D26). Both sides; the flight itself runs on the server.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-entity.md, flight.md, chunk-loading.md, fuel.md
@Optional.InterfaceList({
    @Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.entity.IDockable", modid = "GalacticraftCore") })
public abstract class EntityMissile extends Entity implements micdoodle8.mods.galacticraft.api.entity.IDockable {

    /** Checks (10 ticks apart) without a formed silo before a silo-only body stands down: 300 ticks, past a reload. */
    private static final int STRAY_CHECKS = 30;
    /** Blocks the missile climbs straight up before it turns onto the arc. */
    public static final double CLIMB_HEIGHT = 12.0;
    /** Climb acceleration, blocks per tick per tick. */
    private static final double CLIMB_ACCEL = 0.08;
    /** Height of the pad surface above the pad block's Y. */
    public static final double PAD_SURFACE = 0.2;

    /** Blocks per tick squared a fried missile gains in fall speed. */
    private static final double FRIED_GRAVITY = 0.04;
    /** Fastest a fried missile falls, blocks per tick. */
    private static final double FRIED_TERMINAL = 3.0;

    /** Ticks between repeats of the state packet, for clients that start tracking late. */
    private static final int RESEND_TICKS = 20;

    private final MissileTickets tickets = new MissileTickets();
    private ArcPath path;
    private Object landingPad;

    private FlightPhase phase = FlightPhase.PAD;
    private int phaseTick;
    /** Ticks of the warm-up spent waiting for the silo doors (0 on a pad); set at launch (D88). */
    private int warmupLead;
    /** Lead plus countdown, ticks; set at launch (D88). */
    private int warmupTotal;
    /** Consecutive checks a silo-only body found no formed silo under it; not saved. */
    private int strayChecks;
    private int fuelMb;
    private int guidance;
    private float health;
    private String explosive = WarheadData.DEFAULT_EXPLOSIVE;
    private int fill = 1;
    private int casing = 1;
    /** The launch site's accuracy factor (1 on a pad, the silo's config value in a silo); set at launch (D70). */
    private double siteScatter = 1.0;
    /** The launch site's range factor (1 on a pad, the silo's config value in a silo); set at launch (D70). */
    private double siteRange = 1.0;
    private UUID ownerId;
    private String ownerName = "";
    private double launchX;
    private double launchY;
    private double launchZ;
    private double arcX;
    private double arcY;
    private double arcZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double plannedDistance;
    private boolean detonated;
    private boolean stateDirty = true;
    /** True once an EMP fried the guidance (D16): the missile falls where it is and detonates on landing. */
    private boolean fried;
    private double fallSpeed;
    private double driftX;
    private double driftZ;

    /**
     * @param world the world
     */
    protected EntityMissile(World world) {
        super(world);
        this.noClip = true;
        this.ignoreFrustumCheck = true;
        this.preventEntitySpawning = false;
        this.health = tier().health();
        setSize(0.8F, tier().heightBlocks() + 1.0F);
    }

    /** @return this missile's tier */
    public abstract MissileTier tier();

    @Override
    protected void entityInit() {
        // No DataWatcher slot (GGS-01-11): phase, casing and fuel reach clients through PacketMissileState.
    }

    /**
     * Client side: takes the state the server sent.
     *
     * @param newPhase  the flight phase
     * @param newCasing the warhead casing
     * @param newFuel   mB in the tank
     * @param isNuclear true when the warhead is nuclear (picks the nose model; the client knows no other explosive)
     */
    void applyState(FlightPhase newPhase, int newCasing, int newFuel, boolean isNuclear) {
        if (isNuclear) {
            this.explosive = "nuclear";
        } else if ("nuclear".equals(this.explosive)) {
            this.explosive = WarheadData.DEFAULT_EXPLOSIVE;
        }
        this.phase = newPhase;
        this.casing = newCasing;
        this.fuelMb = newFuel;
    }

    /**
     * Client side: takes the warm-up clock the server sent.
     *
     * @param tick  ticks into the warm-up
     * @param lead  ticks waiting for the doors
     * @param total lead plus countdown
     */
    void applyCountdown(int tick, int lead, int total) {
        this.phaseTick = tick;
        this.warmupLead = lead;
        this.warmupTotal = total;
    }

    /** @return ticks into the warm-up (client: counted locally between packets) */
    public int warmupTick() {
        return phaseTick;
    }

    /** @return ticks of the warm-up spent waiting for the doors */
    public int warmupLead() {
        return warmupLead;
    }

    /** @return lead plus countdown, ticks */
    public int warmupTotal() {
        return warmupTotal;
    }

    /** @return ticks left on the visible countdown, or -1 when the missile is not in its warm-up */
    public int countdownLeft() {
        return phase == FlightPhase.WARMUP ? LaunchTimer.countdownLeft(phaseTick, warmupLead, warmupTotal) : -1;
    }

    /** @return true while the missile is in its warm-up and the launch can still be aborted (D88) */
    public boolean abortable() {
        return phase == FlightPhase.WARMUP && LaunchTimer.abortable(phaseTick, warmupTotal);
    }

    /**
     * Stops the countdown and stands the missile down on its pad, armed and fueled as before (D88). Refused once the
     * engines have ignited. Server side.
     *
     * @return true when the launch was aborted
     */
    public boolean abort() {
        if (worldObj.isRemote || isDead || !abortable()) {
            return false;
        }
        tickets.release();
        strayChecks = 0;
        phaseTick = 0;
        warmupLead = 0;
        warmupTotal = 0;
        setPhase(FlightPhase.PAD);
        worldObj.playSoundEffect(posX, posY, posZ, "random.click", 1.0F, 0.6F);
        return true;
    }

    // ------------------------------------------------------------------ state and API

    /**
     * Fills in what the arming step carried over. Server side, before the entity is spawned.
     *
     * @param guidanceTiers guidance tiers above the body's own
     * @param explosiveId   the warhead's explosive
     * @param warheadFill   the warhead's fill
     * @param casingNumber  the warhead casing (1 to 5)
     * @param owner         who armed it, may be null
     */
    public void configure(int guidanceTiers, String explosiveId, int warheadFill, int casingNumber,
        EntityPlayer owner) {
        this.guidance = Math.max(0, Math.min(MissileBodies.MAX_GUIDANCE, guidanceTiers));
        this.explosive = explosiveId;
        this.fill = warheadFill;
        this.casing = casingNumber;
        stateDirty = true;
        if (owner != null) {
            this.ownerId = owner.getUniqueID();
            this.ownerName = owner.getCommandSenderName();
        }
    }

    /**
     * @param id   the owner's UUID
     * @param name the owner's name
     */
    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name == null ? "" : name;
    }

    /** @return the flight phase */
    public FlightPhase phase() {
        return phase;
    }

    /** @return true while it stands on the pad (or in its silo) and can be fueled and launched */
    public boolean onPad() {
        return phase() == FlightPhase.PAD;
    }

    /**
     * @param tier  a body tier
     * @param world the world
     * @return a new, unconfigured missile entity of that tier
     */
    public static EntityMissile create(MissileTier tier, World world) {
        switch (tier) {
            case T1:
                return new EntityMissileT1(world);
            case T2:
                return new EntityMissileT2(world);
            default:
                return new EntityMissileT3(world);
        }
    }

    /** @return the warhead's explosive id (on a client only "nuclear" is known, for the nose model) */
    public String explosiveId() {
        return explosive;
    }

    /** @return the warhead casing number, 1 to 5; drawn on the nose */
    public int casingNumber() {
        return casing;
    }

    /** @return mB in the tank */
    public int fuelMb() {
        return fuelMb;
    }

    /** @return the tank size, mB, with the config factor (D20) */
    public int capacityMb() {
        return FuelMath.tank(tier().baseFuelMb(), IcbmConfig.missileFuelFactor);
    }

    /** @return the range in blocks with the config factor */
    public int maxRange() {
        return maxRange(siteRange);
    }

    /**
     * @param siteRangeFactor the launch site's range factor (1 for a pad)
     * @return the range in blocks with the config factor and the site's factor; the launch-control GUI shows this for
     *         the site it is linked to (D70)
     */
    public int maxRange(double siteRangeFactor) {
        return (int) Math.max(
            tier().minRange() + 1L,
            Math.round(tier().baseMaxRange() * IcbmConfig.missileRangeFactor * Math.max(0.0, siteRangeFactor)));
    }

    /**
     * Pours fuel into the tank directly (the scenarios, and the Galacticraft path below).
     *
     * @param mb     the amount offered
     * @param doFill false to only report
     * @return mB that fit; 0 unless the missile is on the pad
     */
    public int fill(int mb, boolean doFill) {
        if (worldObj.isRemote || phase != FlightPhase.PAD || mb <= 0) {
            return 0;
        }
        int fit = Math.min(mb, capacityMb() - fuelMb);
        if (fit > 0 && doFill) {
            fuelMb += fit;
            stateDirty = true;
        }
        return Math.max(0, fit);
    }

    /** @return a snapshot for the launch-control GUI */
    public MissileStatus status() {
        return new MissileStatus(true, tier(), fuelMb(), capacityMb(), guidance, explosive, fill);
    }

    /**
     * @param tx target X
     * @param tz target Z
     * @return the blast radius at that target with the fuel bonus, blocks (the GUI shows it, D20)
     */
    public double expectedRadius(double tx, double tz) {
        return expectedRadius(tx, tz, siteRange);
    }

    /**
     * @param tx              target X
     * @param tz              target Z
     * @param siteRangeFactor the launch site's range factor
     * @return the blast radius at that target with the fuel bonus for that site's range
     */
    public double expectedRadius(double tx, double tz, double siteRangeFactor) {
        ExplosiveDef def = Explosives.get(explosive);
        if (def == null) {
            return 0.0;
        }
        double radius = def.radius(fill);
        double dist = Math.sqrt((tx - posX) * (tx - posX) + (tz - posZ) * (tz - posZ));
        return radius + FuelMath.bonusRadius(radius, dist, maxRange(siteRangeFactor), IcbmConfig.fuelBonusMax);
    }

    /** @return how many missiles are in the air in every loaded world; the D11 cap counts these */
    public static int flying() {
        int n = 0;
        for (WorldServer w : net.minecraft.server.MinecraftServer.getServer().worldServers) {
            if (w == null) {
                continue;
            }
            for (Object o : w.loadedEntityList) {
                if (o instanceof EntityMissile && !((EntityMissile) o).isDead
                    && ((EntityMissile) o).phase != FlightPhase.PAD) {
                    n++;
                }
            }
        }
        return n;
    }

    /** @return the chunk tickets this missile holds now; the exit gate says never more than two chunks */
    public MissileTickets tickets() {
        return tickets;
    }

    /** @return the hit points left; a shot-down missile detonates where it is when this reaches 0 (ICBM-03-03) */
    public float health() {
        return health;
    }

    /** @return the planned flat distance of the launch, blocks; 0 before launch */
    public double plannedDistance() {
        return plannedDistance;
    }

    /** @return the aimed (scattered) target, x y z; meaningful after launch */
    public double[] aimPoint() {
        return new double[] { targetX, targetY, targetZ };
    }

    /**
     * @return where the missile will come down, x z: the aimed target in flight, and for a missile with fried guidance
     *         the drift line carried to the ground (D14)
     */
    public double[] predictedImpact() {
        if (!fried) {
            return new double[] { targetX, targetZ };
        }
        double ground = 0.0;
        if (worldObj.getChunkProvider()
            .chunkExists(((int) Math.floor(posX)) >> 4, ((int) Math.floor(posZ)) >> 4)) {
            ground = worldObj.getTopSolidOrLiquidBlock((int) Math.floor(posX), (int) Math.floor(posZ));
        }
        return ContactMath
            .friedImpact(posX, posY, posZ, driftX, driftZ, fallSpeed, FRIED_GRAVITY, FRIED_TERMINAL, ground);
    }

    /** @return the launch site's scatter factor this missile carries (1 for a pad, saved with the missile) */
    public double siteScatter() {
        return siteScatter;
    }

    /** @return the launch site's range factor this missile carries (1 for a pad, saved with the missile) */
    public double siteRange() {
        return siteRange;
    }

    /**
     * Launches. Server side; every rule is checked here.
     *
     * @param tx     the target X
     * @param tz     the target Z
     * @param source who fired it, may be null
     * @return the outcome
     */
    public LaunchResult launch(double tx, double tz, EntityPlayer source) {
        if (tier().siloOnly()) {
            return LaunchResult.SILO_ONLY; // the pad path: a silo-only body leaves only through a formed silo (D8, D70)
        }
        return launch(tx, tz, source, 1.0, 1.0);
    }

    /**
     * Launches from a site that changes the numbers (D70): the silo tightens the aim and stretches the range, a pad
     * passes 1 and 1. The factors stay with the missile for the whole flight. Server side.
     *
     * @param tx           the target X
     * @param tz           the target Z
     * @param source       who fired it, may be null
     * @param scatterScale the site's scatter factor
     * @param rangeScale   the site's range factor
     * @return the outcome
     */
    public LaunchResult launch(double tx, double tz, EntityPlayer source, double scatterScale, double rangeScale) {
        return launch(tx, tz, source, scatterScale, rangeScale, 0);
    }

    /**
     * Launches with a lead: the warm-up first waits {@code leadTicks} for the silo doors to open, then runs the
     * configured countdown (D88). The lead is part of the launch, so a missile never climbs into closed doors. Server
     * side.
     *
     * @param tx           the target X
     * @param tz           the target Z
     * @param source       who fired it, may be null
     * @param scatterScale the site's scatter factor
     * @param rangeScale   the site's range factor
     * @param leadTicks    ticks until the doors are open, 0 on a pad
     * @return the outcome
     */
    public LaunchResult launch(double tx, double tz, EntityPlayer source, double scatterScale, double rangeScale,
        int leadTicks) {
        double oldScatter = siteScatter;
        double oldRange = siteRange;
        siteScatter = scatterScale;
        siteRange = rangeScale;
        LaunchResult r = launchChecked(tx, tz, source);
        if (r == LaunchResult.OK) {
            warmupLead = Math.max(0, leadTicks);
            warmupTotal = warmupLead + Math.max(1, IcbmConfig.countdownTicks);
            phaseTick = 0;
        } else {
            // A refused launch leaves the missile as it was.
            siteScatter = oldScatter;
            siteRange = oldRange;
        }
        return r;
    }

    private LaunchResult launchChecked(double tx, double tz, EntityPlayer source) {
        if (worldObj.isRemote || isDead) {
            return LaunchResult.BAD_TARGET;
        }
        if (phase != FlightPhase.PAD) {
            return LaunchResult.BUSY;
        }
        if (fuelMb < capacityMb()) {
            return LaunchResult.NOT_FUELED;
        }
        double dist = Math.sqrt((tx - posX) * (tx - posX) + (tz - posZ) * (tz - posZ));
        LaunchResult range = MissileLimits.range(dist, tier().minRange(), maxRange());
        if (range != LaunchResult.OK) {
            return range;
        }
        if (!MissileLimits.underCap(flying(), IcbmConfig.maxMissilesInFlight)) {
            return LaunchResult.OVER_CAP;
        }
        if (!climbColumnClear()) {
            return LaunchResult.BLOCKED;
        }
        double half = Scatter.halfWidth(tier().baseScatter(), guidance, dist, maxRange(), siteScatter);
        double aimX = tx + Scatter.offset(rand, half);
        double aimZ = tz + Scatter.offset(rand, half);
        int bx = net.minecraft.util.MathHelper.floor_double(aimX);
        int bz = net.minecraft.util.MathHelper.floor_double(aimZ);
        // The flight needs a ticket from the first tick (warmup included); Forge may be out of them.
        if (!tickets.update(this, ChunkWindow.wanted(posX, posZ, 0.0, 0.0))) {
            return LaunchResult.NO_TICKET;
        }
        // The target height: load the target chunk once, with no ticket, to read the surface there. An unloaded
        // chunk would aim the arc at the pad's height and miss by about half a block per block of height difference.
        if (worldObj instanceof WorldServer) {
            ((WorldServer) worldObj).theChunkProviderServer.loadChunk(bx >> 4, bz >> 4);
        }
        this.targetX = aimX;
        this.targetZ = aimZ;
        this.plannedDistance = dist;
        this.targetY = worldObj.getHeightValue(bx, bz);
        this.launchX = posX;
        this.launchY = posY;
        this.launchZ = posZ;
        if (source != null) {
            this.ownerId = source.getUniqueID();
            this.ownerName = source.getCommandSenderName();
        }
        setPhase(FlightPhase.WARMUP);
        worldObj.playSoundEffect(posX, posY, posZ, "random.click", 1.0F, 1.0F);
        return LaunchResult.OK;
    }

    /**
     * @return true when nothing solid stands in the column the missile climbs through: from the pad to the top of the
     *         body at the end of the climb (ICBM-04-26)
     */
    private boolean climbColumnClear() {
        int x0 = net.minecraft.util.MathHelper.floor_double(posX - width / 2.0);
        int x1 = net.minecraft.util.MathHelper.floor_double(posX + width / 2.0);
        int z0 = net.minecraft.util.MathHelper.floor_double(posZ - width / 2.0);
        int z1 = net.minecraft.util.MathHelper.floor_double(posZ + width / 2.0);
        int y0 = net.minecraft.util.MathHelper.floor_double(posY) + 1;
        int y1 = net.minecraft.util.MathHelper.floor_double(posY + CLIMB_HEIGHT + tier().heightBlocks());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (worldObj.getBlock(x, y, z)
                        .getCollisionBoundingBoxFromPool(worldObj, x, y, z) != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            clientTick();
            return;
        }
        if (stateDirty || ticksExisted % RESEND_TICKS == 0) {
            stateDirty = false;
            com.gregguns.network.GregGunsNetwork.channel()
                .sendToAllTracking(new PacketMissileState(this), this);
        }
        motionX = 0;
        motionY = 0;
        motionZ = 0;
        if (fried && inFlight()) {
            friedTick();
            return;
        }
        switch (phase) {
            case PAD:
                padTick();
                break;
            case WARMUP:
                warmupStep();
                break;
            case CLIMB:
                climbTick();
                break;
            default:
                arcTick();
                break;
        }
    }

    /** One warm-up tick: count, and ignite when the countdown ends; from then on there is no abort (D88). */
    private void warmupStep() {
        // A silo-only body whose silo is gone does not go on alone (D90). A reload leaves the silo unformed for a
        // while, so only a lasting gap counts. The pad is a slab PAD_SURFACE high, so the body's feet are the pad
        // block's own cell; one half a block down (as padTick looks for support) is the cell below the floor, where
        // there is never a silo to find.
        if (tier().siloOnly() && ticksExisted % 10 == 0) {
            boolean floor = com.gregguns.icbm.silo.MTEMissileSilo.isFloorOf(
                worldObj,
                net.minecraft.util.MathHelper.floor_double(posX),
                net.minecraft.util.MathHelper.floor_double(posY - PAD_SURFACE / 2.0),
                net.minecraft.util.MathHelper.floor_double(posZ));
            strayChecks = floor ? 0 : strayChecks + 1;
            if (strayChecks >= STRAY_CHECKS) {
                strayChecks = 0;
                abort();
                return;
            }
        }
        if (++phaseTick >= warmupTotal) {
            phaseTick = 0;
            setPhase(FlightPhase.CLIMB);
            worldObj.playSoundEffect(posX, posY, posZ, "fireworks.launch", 4.0F, 0.6F);
        }
    }

    private void padTick() {
        // A missile whose support was removed from under it does not hang in the air: it comes apart into items.
        if (ticksExisted % 20 == 0 && worldObj
            .getBlock(
                net.minecraft.util.MathHelper.floor_double(posX),
                net.minecraft.util.MathHelper.floor_double(posY - 0.5),
                net.minecraft.util.MathHelper.floor_double(posZ))
            .isAir(
                worldObj,
                net.minecraft.util.MathHelper.floor_double(posX),
                net.minecraft.util.MathHelper.floor_double(posY - 0.5),
                net.minecraft.util.MathHelper.floor_double(posZ))) {
            disarm();
        }
    }

    private void climbTick() {
        phaseTick++;
        double climbed = 0.5 * CLIMB_ACCEL * phaseTick * phaseTick;
        double y = launchY + climbed;
        if (climbed >= CLIMB_HEIGHT) {
            arcX = launchX;
            arcY = launchY + CLIMB_HEIGHT;
            arcZ = launchZ;
            path = new ArcPath(arcX, arcY, arcZ, targetX, targetY, targetZ, tier().speed());
            phaseTick = 0;
            setPhase(FlightPhase.ARC);
            y = arcY;
        }
        moveTo(launchX, y, launchZ);
    }

    private void arcTick() {
        if (path == null) {
            // A reload in the arc: rebuild it from the saved start; the tick count picks the point.
            path = new ArcPath(arcX, arcY, arcZ, targetX, targetY, targetZ, tier().speed());
        }
        phaseTick++;
        double[] p = path.position(phaseTick);
        Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
        Vec3 to = Vec3.createVectorHelper(p[0], p[1], p[2]);
        MovingObjectPosition hit = worldObj.func_147447_a(from, to, false, true, false);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            detonateAt(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
            return;
        }
        moveTo(p[0], p[1], p[2]);
        if (phaseTick > path.ticks() + IcbmConfig.missileTimeoutTicks || posY < 0.0) {
            detonateAt(posX, Math.max(posY, 1.0), posZ);
        }
    }

    /** @return true while the missile is climbing or in its arc, the phases an EMP can reach */
    public boolean inFlight() {
        return phase == FlightPhase.CLIMB || phase == FlightPhase.ARC;
    }

    /** @return true when an EMP has fried the guidance and the missile is falling */
    public boolean guidanceFried() {
        return fried;
    }

    /**
     * An EMP fries the guidance (D16): no more thrust, so the missile keeps its sideways drift, falls under gravity
     * and detonates where it hits. Idempotent, server side, in flight only.
     *
     * @return true when this call changed the missile
     */
    public boolean fryGuidance() {
        if (worldObj.isRemote || fried || isDead || !inFlight()) {
            return false;
        }
        fried = true;
        fallSpeed = 0.0;
        // The last tick's step is the missile's velocity (moveTo stored it in the motion fields).
        driftX = motionX;
        driftZ = motionZ;
        return true;
    }

    /**
     * An EMP destroys the missile outright (D16): it is removed and its fuel goes off as a flash and a bang that break
     * nothing; the warhead does not detonate. Server side.
     */
    public void destroyByEmp() {
        if (worldObj.isRemote || isDead) {
            return;
        }
        detonated = true;
        ((WorldServer) worldObj).func_147487_a("hugeexplosion", posX, posY, posZ, 1, 0.0, 0.0, 0.0, 0.0);
        worldObj.playSoundEffect(posX, posY, posZ, "random.explode", 4.0F, 0.9F);
        setDead();
    }

    /** One tick of a missile whose guidance an EMP fried: gravity, the old drift, and a landing detonation. */
    private void friedTick() {
        fallSpeed = Math.min(FRIED_TERMINAL, fallSpeed + FRIED_GRAVITY);
        Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
        Vec3 to = Vec3.createVectorHelper(posX + driftX, posY - fallSpeed, posZ + driftZ);
        MovingObjectPosition hit = worldObj.func_147447_a(from, to, false, true, false);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            detonateAt(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
            return;
        }
        moveTo(to.xCoord, to.yCoord, to.zCoord);
        if (posY < 0.0) {
            detonateAt(posX, 1.0, posZ);
        }
    }

    private void moveTo(double x, double y, double z) {
        double ox = posX;
        double oy = posY;
        double oz = posZ;
        setPosition(x, y, z);
        motionX = x - ox;
        motionY = y - oy;
        motionZ = z - oz;
        velocityChanged = true;
        // Hold two chunks: the one it is in and the next on its heading (D25).
        tickets.update(this, ChunkWindow.wanted(x, z, motionX, motionZ));
    }

    @SideOnly(Side.CLIENT)
    private void clientTickParticles() {
        FlightPhase ph = phase();
        if (ph == FlightPhase.PAD) {
            return;
        }
        if (ph == FlightPhase.WARMUP) {
            com.gregguns.client.icbm.LaunchSmokeFX.emit(
                worldObj,
                posX,
                posY,
                posZ,
                LaunchTimer.smokeIntensity(phaseTick, warmupLead, warmupTotal),
                LaunchTimer.untilIgnition(phaseTick, warmupTotal) <= LaunchTimer.FLAME_TICKS);
            return;
        }
        double len = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        double ux = len > 1e-6 ? motionX / len : 0;
        double uy = len > 1e-6 ? motionY / len : 1;
        double uz = len > 1e-6 ? motionZ / len : 0;
        // The tail is one body-height back along the heading.
        double bx = posX - ux * 0.5;
        double by = posY - uy * 0.5;
        double bz = posZ - uz * 0.5;
        worldObj.spawnParticle("flame", bx, by, bz, -ux * 0.1, -uy * 0.1, -uz * 0.1);
        worldObj.spawnParticle("largesmoke", bx, by, bz, -ux * 0.05, -uy * 0.05, -uz * 0.05);
    }

    private void clientTick() {
        if (phase() == FlightPhase.WARMUP) {
            phaseTick++;
            clientTickParticles();
        } else if (phase() != FlightPhase.PAD) {
            // Between server updates the client carries the last velocity forward.
            setPosition(posX + motionX, posY + motionY, posZ + motionZ);
            clientTickParticles();
        }
        double h = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double len = Math.sqrt(h * h + motionY * motionY);
        if (len > 1e-6) {
            prevRotationYaw = rotationYaw;
            prevRotationPitch = rotationPitch;
            rotationYaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
            // Degrees from straight up: 0 is vertical, 90 level, above 90 nose down.
            rotationPitch = (float) Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, motionY / len))));
        } else {
            rotationPitch = 0.0F;
        }
    }

    private void setPhase(FlightPhase next) {
        this.phase = next;
        stateDirty = true;
    }

    // ------------------------------------------------------------------ impact, damage, disarm

    /**
     * Detonates the warhead at a point (server side) and removes the missile and its chunk tickets. The fuel left
     * adds to the radius (D20). Idempotent.
     *
     * @param x blast X
     * @param y blast Y
     * @param z blast Z
     */
    public void detonateAt(double x, double y, double z) {
        if (worldObj.isRemote || detonated) {
            return;
        }
        detonated = true;
        WorldServer world = (WorldServer) worldObj;
        ExplosiveDef def = Explosives.get(explosive);
        tickets.release();
        setDead();
        if (def == null) {
            return;
        }
        EntityPlayer actor = OwnerProfiles.playerFor(
            world,
            ownerId,
            ownerId == null ? null : new GameProfile(ownerId, ownerName.isEmpty() ? "[GregGuns]" : ownerName));
        double radius = def.radius(fill);
        double bonus = FuelMath.bonusRadius(radius, plannedDistance, maxRange(), IcbmConfig.fuelBonusMax);
        IcbmBlasts.detonate(world, x, y, z, def, fill, bonus, actor);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (worldObj.isRemote || isDead || amount <= 0.0F) {
            return false;
        }
        if (phase == FlightPhase.PAD) {
            // On the pad only a player's hit takes it apart (a creative punch or a sneaking hit); nothing else harms
            // it.
            if (source.getEntity() instanceof EntityPlayer && ((EntityPlayer) source.getEntity()).isSneaking()) {
                disarm();
                return true;
            }
            return false;
        }
        health -= amount;
        if (health <= 0.0F) {
            // Shot down: the warhead goes off where it is (ICBM-03-03: lethal damage always ends the missile).
            detonateAt(posX, posY, posZ);
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return phase() == FlightPhase.PAD;
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (worldObj.isRemote || phase != FlightPhase.PAD) {
            return true;
        }
        if (player.isSneaking() && player.getHeldItem() == null) {
            disarm();
        } else {
            player.addChatMessage(
                new ChatComponentTranslation(
                    "gregguns.icbm.missile.status",
                    tier().number(),
                    fuelMb,
                    capacityMb(),
                    guidance));
        }
        return true;
    }

    /**
     * Takes an unlaunched missile apart: the body and the warhead drop as items, the fuel is lost. Server side.
     */
    public void disarm() {
        if (worldObj.isRemote || isDead || phase != FlightPhase.PAD) {
            return;
        }
        ItemStack body = MissileBodies.body(tier(), guidance);
        WarheadCasing c = MissileCasings.byNumber(casing);
        ItemStack warhead = WarheadStacks
            .make(net.minecraft.item.Item.getItemFromBlock(WarheadBlocks.of(c)), c, explosive, fill);
        for (ItemStack s : new ItemStack[] { body, warhead }) {
            EntityItem item = new EntityItem(worldObj, posX, posY + 0.5, posZ, s);
            item.delayBeforeCanPickup = 10;
            worldObj.spawnEntityInWorld(item);
        }
        tickets.release();
        setDead();
    }

    @Override
    public void setDead() {
        tickets.release();
        super.setDead();
    }

    // ------------------------------------------------------------------ Galacticraft docking and fuel

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public int addFuel(FluidStack fluid, boolean doDrain) {
        // The loader has already checked its tank against RocketFuels for this class (D19); it offers generic fuel.
        return fluid == null ? 0 : fill(fluid.amount, doDrain);
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public FluidStack removeFuel(int amount) {
        // A missile's tank cannot be drained: the fuel is part of the weapon.
        return null;
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public void setPad(micdoodle8.mods.galacticraft.api.tile.IFuelDock pad) {
        this.landingPad = pad;
        if (pad != null) {
            pad.dockEntity(this);
        }
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public micdoodle8.mods.galacticraft.api.tile.IFuelDock getLandingPad() {
        return (micdoodle8.mods.galacticraft.api.tile.IFuelDock) landingPad;
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public void onPadDestroyed() {
        // The pad was broken under an unlaunched missile: it comes apart into items instead of hanging in the air.
        landingPad = null;
        if (phase == FlightPhase.PAD) {
            disarm();
        }
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public boolean isDockValid(micdoodle8.mods.galacticraft.api.tile.IFuelDock dock) {
        return phase == FlightPhase.PAD;
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public micdoodle8.mods.galacticraft.api.entity.ICargoEntity.EnumCargoLoadingState addCargo(ItemStack stack,
        boolean doAdd) {
        return micdoodle8.mods.galacticraft.api.entity.ICargoEntity.EnumCargoLoadingState.NOINVENTORY;
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public micdoodle8.mods.galacticraft.api.entity.ICargoEntity.RemovalResult removeCargo(boolean doRemove) {
        return new micdoodle8.mods.galacticraft.api.entity.ICargoEntity.RemovalResult(
            micdoodle8.mods.galacticraft.api.entity.ICargoEntity.EnumCargoLoadingState.NOINVENTORY,
            null);
    }

    // ------------------------------------------------------------------ save and load

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setString("phase", phase.name());
        tag.setInteger("phaseTick", phaseTick);
        tag.setInteger("warmupLead", warmupLead);
        tag.setInteger("warmupTotal", warmupTotal);
        tag.setInteger("fuel", fuelMb);
        tag.setInteger("guidance", guidance);
        tag.setFloat("health", health);
        tag.setString("explosive", explosive);
        tag.setInteger("fill", fill);
        tag.setInteger("casing", casing);
        tag.setDouble("siteScatter", siteScatter);
        tag.setDouble("siteRange", siteRange);
        if (ownerId != null) {
            tag.setString("ownerId", ownerId.toString());
            tag.setString("ownerName", ownerName);
        }
        tag.setDouble("launchX", launchX);
        tag.setDouble("launchY", launchY);
        tag.setDouble("launchZ", launchZ);
        tag.setDouble("arcX", arcX);
        tag.setDouble("arcY", arcY);
        tag.setDouble("arcZ", arcZ);
        tag.setDouble("targetX", targetX);
        tag.setDouble("targetY", targetY);
        tag.setDouble("targetZ", targetZ);
        tag.setDouble("distance", plannedDistance);
        tag.setBoolean("fried", fried);
        tag.setDouble("fallSpeed", fallSpeed);
        tag.setDouble("driftX", driftX);
        tag.setDouble("driftZ", driftZ);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        try {
            phase = FlightPhase.valueOf(tag.getString("phase"));
        } catch (IllegalArgumentException e) {
            phase = FlightPhase.PAD;
        }
        phaseTick = Math.max(0, tag.getInteger("phaseTick"));
        warmupLead = Math.max(0, tag.getInteger("warmupLead"));
        // A save from before the countdown (D88) has no total: finish its warm-up quickly rather than hang.
        warmupTotal = tag.hasKey("warmupTotal") ? Math.max(0, tag.getInteger("warmupTotal")) : 40;
        fuelMb = Math.max(0, Math.min(capacityMb(), tag.getInteger("fuel")));
        guidance = Math.max(0, Math.min(MissileBodies.MAX_GUIDANCE, tag.getInteger("guidance")));
        // The tier is fixed by the class, so the clamp is right whatever order the keys load in (ICBM-03-04).
        health = tag.hasKey("health") ? Math.min(tag.getFloat("health"), tier().health()) : tier().health();
        explosive = tag.getString("explosive");
        if (explosive.isEmpty()) {
            explosive = WarheadData.DEFAULT_EXPLOSIVE;
        }
        fill = Math.max(1, tag.getInteger("fill"));
        casing = MissileCasings.byNumber(tag.getInteger("casing"))
            .number();
        siteScatter = tag.hasKey("siteScatter") ? tag.getDouble("siteScatter") : 1.0;
        siteRange = tag.hasKey("siteRange") ? tag.getDouble("siteRange") : 1.0;
        ownerId = null;
        ownerName = tag.getString("ownerName");
        if (tag.hasKey("ownerId")) {
            try {
                ownerId = UUID.fromString(tag.getString("ownerId"));
            } catch (IllegalArgumentException e) {
                ownerId = null;
            }
        }
        launchX = tag.getDouble("launchX");
        launchY = tag.getDouble("launchY");
        launchZ = tag.getDouble("launchZ");
        arcX = tag.getDouble("arcX");
        arcY = tag.getDouble("arcY");
        arcZ = tag.getDouble("arcZ");
        targetX = tag.getDouble("targetX");
        targetY = tag.getDouble("targetY");
        targetZ = tag.getDouble("targetZ");
        plannedDistance = tag.getDouble("distance");
        fried = tag.getBoolean("fried");
        fallSpeed = tag.getDouble("fallSpeed");
        driftX = tag.getDouble("driftX");
        driftZ = tag.getDouble("driftZ");
        stateDirty = true;
    }

    /** @return the owner's name for display; empty if none */
    public String ownerName() {
        return ownerName;
    }
}
