package com.gregguns.icbm.silo;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.MultiAmpEnergy;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.missile.BlockMissileBody;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileStatus;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.missile.PadMissiles;
import com.gregguns.icbm.warhead.BlockWarhead;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadData;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * The Missile Silo: a tall shaft that holds one armed missile and is the only launcher for T3 (D8 step 5, D70). The
 * body and the warhead arrive on an Input Bus (or are stacked in the shaft by hand, as on a pad) and are mated into an
 * armed missile entity in the shaft; fuel arrives through an Input Hatch and is checked against the same fuel lists
 * as the Galacticraft pad (D22); the Launch Control fires it through {@link LaunchSite}. The player never touches the
 * missile.
 *
 * <p>
 * There is no recipe map. Each cycle {@link #checkProcessing} does the next job: mate a body and warhead from the buses
 * (the missile appears when the cycle ends, {@link #outputAfterRecipe}), or pump one portion of fuel from the hatches
 * into the missile in the shaft. Both jobs draw the body tier's voltage, so the weakest Energy Hatch decides the
 * heaviest body (HV T1, EV T2, LuV T3). A launch does not touch a block of the silo: the missile only climbs out of the
 * open shaft. Construct only inside GT's MTE window ({@code SiloMachines.register}), after
 * {@link GregGunsCasings#create} has run. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md, docs/design/0011-machine-policy.md
// Source (the pattern: a non-recipe multiblock whose checkProcessing reads the buses, with a finish hook):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L877
// Deviation: the controller has the static and active front overlays only; the gantry crane and the door animation
// are not built.
public final class MTEMissileSilo extends MTEExtendedPowerMultiBlockBase<MTEMissileSilo>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String PIECE = "silo";
    private static final String OVERLAY_DIR = "machines/missile_silo/";
    private static final String K_PENDING = "pendingTier";
    private static final String K_P_GUIDANCE = "pendingGuidance";
    private static final String K_P_EXPLOSIVE = "pendingExplosive";
    private static final String K_P_FILL = "pendingFill";
    private static final String K_P_CASING = "pendingCasing";
    private static final String K_P_READY = "pendingReady";

    private static final String K_DOOR_STATE = "doorState";
    private static final String K_DOOR_START = "doorStart";
    private static final String K_DOOR_DUR = "doorDuration";

    /** Ticks between looks for a missile around the shaft while the doors rest. */
    private static final int DOOR_SCAN_TICKS = 5;

    /** Ticks the silo may stay unformed before a launch in progress stands down; a reload is unformed for a while. */
    static final int UNFORMED_GRACE_TICKS = 200;

    private final SiloDoors doors = new SiloDoors();
    private int unformedTicks;
    private EntitySiloDoors doorEntity;
    private SiloDoors.State pushedState;
    private long pushedStart;

    private int casingCount;
    private int glassTier = -1;
    /** The mating in progress, saved so a reload mid-cycle does not eat the items; 0 for none, else the tier number. */
    private int pendingTier;
    private int pendingGuidance;
    private String pendingExplosive = WarheadData.DEFAULT_EXPLOSIVE;
    private int pendingFill = 1;
    private int pendingCasing = 1;
    /** The mating cycle finished but the missile could not be placed yet (the shaft chunks were unloaded). */
    private boolean pendingReady;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id           the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name         GT's internal name
     * @param nameRegional the English display name GT registers for the block
     */
    public MTEMissileSilo(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
        Overlays.load();
    }

    private MTEMissileSilo(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEMissileSilo(mName);
    }

    /** Lazily built, because the casing block must exist before the elements are resolved. */
    private static final class Structure {

        static final IStructureDefinition<MTEMissileSilo> DEFINITION = build();

        private Structure() {}

        private static IStructureElement<MTEMissileSilo> casing(int meta) {
            return lazy(() -> ofBlock(GregGunsCasings.INSTANCE, meta));
        }

        private static IStructureDefinition<MTEMissileSilo> build() {
            int casingIndex = GregGunsCasings.textureIndex(GregGunsCasings.GANTRY_CASING);
            return StructureDefinition.<MTEMissileSilo>builder()
                .addShape(PIECE, SiloShape.piece())
                // Plain silo casing: the walls above the glass, and the floor under the shaft.
                .addElement('C', onElementPass(MTEMissileSilo::onCasingAdded, casing(GregGunsCasings.GANTRY_CASING)))
                // The floor's outer ring: the same casing, where the hatches may go.
                .addElement(
                    'F',
                    buildHatchAdder(MTEMissileSilo.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(MTEMissileSilo::onCasingAdded, casing(GregGunsCasings.GANTRY_CASING))))
                // The three rings above the floor are glass so the missile shows (owner, 2026-10-26): any glass tier,
                // mixed is fine (the getter answers "not set", as the Armory Gantry does), and a hatch may replace
                // a pane.
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L891-L925
                .addElement(
                    'G',
                    buildHatchAdder(MTEMissileSilo.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(2)
                        .buildAndChain(chainAllGlasses(-1, (t, tier) -> t.glassTier = tier, t -> -1)))
                .addElement('P', com.gregguns.icbm.compat.GalacticraftPads.element())
                .addElement('B', casing(GregGunsCasings.GANTRY_BEAM))
                .build();
        }
    }

    private void onCasingAdded() {
        casingCount++;
    }

    @Override
    public IStructureDefinition<MTEMissileSilo> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseMetaTileEntity, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        glassTier = -1;
        if (!checkPiece(PIECE, SiloShape.CONTROLLER_COLUMN, SiloShape.CONTROLLER_ROW, 0, errors)) {
            return;
        }
        checkCasingMin(errors, casingCount, SiloShape.CASING_MINIMUM);
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasInputHatch(errors);
        if (!mExoticEnergyHatches.isEmpty() && !mEnergyHatches.isEmpty()) {
            errors.add(StructureErrorRegistry.ONE_ENERGY_HATCH_ON_MULTI_OR_LASER);
        }
    }

    /**
     * @return the tier number of the body and warhead taken out of the buses and not yet standing in the shaft, or 0
     */
    int pendingTier() {
        return pendingTier;
    }

    /** @return whether the last structure check found a complete silo */
    public boolean isFormed() {
        return mMachine;
    }

    // --- geometry --------------------------------------------------------------------------------------------------

    /** @return the direction from the controller into the silo */
    private ForgeDirection back() {
        return getBaseMetaTileEntity().getBackFacing();
    }

    /** @return X of the shaft's center column */
    private double shaftX() {
        IGregTechTileEntity b = getBaseMetaTileEntity();
        return b.getXCoord() + back().offsetX * SiloShape.SHAFT_CENTER_DEPTH + 0.5;
    }

    /** @return Z of the shaft's center column */
    private double shaftZ() {
        IGregTechTileEntity b = getBaseMetaTileEntity();
        return b.getZCoord() + back().offsetZ * SiloShape.SHAFT_CENTER_DEPTH + 0.5;
    }

    /** @return Y of the first air layer of the shaft, the layer just above the launch pad */
    private int floorY() {
        return getBaseMetaTileEntity().getYCoord() - SiloShape.FLOOR_SURFACE_BELOW_CONTROLLER;
    }

    /** @return Y of the launch pad blocks in the shaft floor */
    private int padY() {
        return floorY() - 1;
    }

    private AxisAlignedBB shaftBox() {
        return AxisAlignedBB
            .getBoundingBox(shaftX() - 1.5, padY(), shaftZ() - 1.5, shaftX() + 1.5, floorY() + 12.0, shaftZ() + 1.5);
    }

    /**
     * @return whether every chunk the shaft stands in is loaded (a missing chunk hides the missile and drops spawns)
     */
    private boolean shaftLoaded() {
        World w = getBaseMetaTileEntity().getWorld();
        if (w == null) {
            return false;
        }
        AxisAlignedBB b = shaftBox();
        return w.checkChunksExist(
            (int) Math.floor(b.minX),
            (int) b.minY,
            (int) Math.floor(b.minZ),
            (int) Math.floor(b.maxX),
            (int) b.maxY,
            (int) Math.floor(b.maxZ));
    }

    /**
     * @param w    a world
     * @param padX X of a Galacticraft pad's centre block
     * @param padY Y of that block
     * @param padZ Z of that block
     * @return whether that pad is the floor of a formed silo's shaft (D87: a silo-only body may be stacked on it)
     */
    public static boolean isFloorOf(World w, int padX, int padY, int padZ) {
        for (ForgeDirection d : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST,
            ForgeDirection.WEST }) {
            int cx = padX - d.offsetX * SiloShape.SHAFT_CENTER_DEPTH;
            int cy = padY + SiloShape.FLOOR_SURFACE_BELOW_CONTROLLER + 1;
            int cz = padZ - d.offsetZ * SiloShape.SHAFT_CENTER_DEPTH;
            if (cy > 255 || !w.blockExists(cx, cy, cz)) {
                continue;
            }
            net.minecraft.tileentity.TileEntity t = w.getTileEntity(cx, cy, cz);
            if (t instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) t).getMetaTileEntity() instanceof MTEMissileSilo) {
                MTEMissileSilo silo = (MTEMissileSilo) ((IGregTechTileEntity) t).getMetaTileEntity();
                if (silo.isFormed() && silo.back() == d) {
                    return true;
                }
            }
        }
        return false;
    }

    /** @return the unlaunched missile standing in the shaft, or null */
    public EntityMissile missile() {
        World w = getBaseMetaTileEntity().getWorld();
        if (w == null) {
            return null;
        }
        for (Object o : w.getEntitiesWithinAABB(EntityMissile.class, shaftBox())) {
            EntityMissile m = (EntityMissile) o;
            if (!m.isDead && m.onPad()) {
                return m;
            }
        }
        return null;
    }

    /** @return Y of the top of the rim layer, where the door panels lie */
    private int rimTop() {
        return padY() + SiloShape.HEIGHT;
    }

    /** @return the column above the shaft in which a missile counts as leaving: the shaft and the climb */
    private AxisAlignedBB launchBox() {
        return AxisAlignedBB.getBoundingBox(
            shaftX() - 2.0,
            padY(),
            shaftZ() - 2.0,
            shaftX() + 2.0,
            padY() + PadMissiles.LAUNCH_COLUMN,
            shaftZ() + 2.0);
    }

    /** @return the missile in this silo that is counting down or climbing out, or null */
    private EntityMissile launching() {
        World w = getBaseMetaTileEntity().getWorld();
        return w == null ? null : PadMissiles.launchingIn(w, launchBox());
    }

    /** @return the door machine, for the scenarios */
    SiloDoors doors() {
        return doors;
    }

    // --- the doors (D89) ---------------------------------------------------------------------------------------------

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        World w = base.getWorld();
        if (!mMachine) {
            // A freshly loaded silo is unformed until its first structure check; only a lasting gap counts (D90).
            if (++unformedTicks >= UNFORMED_GRACE_TICKS) {
                comeApart(w);
            }
            return;
        }
        unformedTicks = 0;
        if (tick % DOOR_SCAN_TICKS == 0 || doors.state() == SiloDoors.State.OPENING
            || doors.state() == SiloDoors.State.CLOSING) {
            updateDoors(w);
        }
        if (tick % 20 == 0) {
            ensureDoorEntity(w);
        }
    }

    /** The silo is not formed: no doors, and a countdown still running stands down (D90). */
    private void comeApart(World w) {
        if (doors.state() != SiloDoors.State.CLOSED) {
            doors.reset();
        }
        if (doorEntity != null) {
            doorEntity.setDead();
            doorEntity = null;
        }
        if (w == null || !shaftLoaded()) {
            return;
        }
        EntityMissile m = launching();
        if (m != null && m.abortable()) {
            m.abort();
        }
    }

    /**
     * Opens the doors while a missile counts down or climbs out below the close height, closes them otherwise, and
     * finishes a swing that has run its time.
     */
    private void updateDoors(World w) {
        if (w == null || !shaftLoaded()) {
            return;
        }
        long now = w.getTotalWorldTime();
        EntityMissile m = PadMissiles.flyingIn(w, launchBox());
        boolean want = m != null && (m.phase() == FlightPhase.WARMUP || m.posY < rimTop() + IcbmConfig.doorCloseHeight);
        if (want) {
            doors.open(now, IcbmConfig.doorOpenTicks);
        } else {
            doors.close(now, IcbmConfig.doorOpenTicks);
        }
        doors.update(now);
        pushDoors();
    }

    private void pushDoors() {
        if (doorEntity != null && !doorEntity.isDead
            && (pushedState != doors.state() || pushedStart != doors.start())) {
            doorEntity.push(doors);
            pushedState = doors.state();
            pushedStart = doors.start();
        }
    }

    /** Makes sure one door entity stands at the rim; it is never saved, so every load makes a new one. */
    private void ensureDoorEntity(World w) {
        if (doorEntity != null && !doorEntity.isDead && doorEntity.worldObj == w) {
            return;
        }
        IGregTechTileEntity b = getBaseMetaTileEntity();
        double x = shaftX();
        double z = shaftZ();
        if (!w.blockExists((int) Math.floor(x), rimTop() - 1, (int) Math.floor(z))) {
            return;
        }
        AxisAlignedBB near = AxisAlignedBB
            .getBoundingBox(x - 1.0, rimTop() - 1.0, z - 1.0, x + 1.0, rimTop() + 1.0, z + 1.0);
        for (Object o : w.getEntitiesWithinAABB(EntitySiloDoors.class, near)) {
            EntitySiloDoors e = (EntitySiloDoors) o;
            if (!e.isDead && e.servesController(b.getXCoord(), b.getYCoord(), b.getZCoord())) {
                doorEntity = e;
                pushedState = null;
                pushDoors();
                return;
            }
        }
        EntitySiloDoors e = new EntitySiloDoors(w);
        e.setPosition(x, rimTop(), z);
        e.bind(b.getXCoord(), b.getYCoord(), b.getZCoord());
        e.push(doors);
        if (w.spawnEntityInWorld(e)) {
            doorEntity = e;
            pushedState = doors.state();
            pushedStart = doors.start();
        }
    }

    /** @return the tier of a body block a player stacked in the shaft and has not armed yet, or null */
    private MissileTier stackedBody() {
        World w = getBaseMetaTileEntity().getWorld();
        int cx = (int) Math.floor(shaftX());
        int cz = (int) Math.floor(shaftZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    Block b = w.getBlock(cx + dx, floorY() + dy, cz + dz);
                    if (b instanceof BlockMissileBody) {
                        return ((BlockMissileBody) b).missileTier();
                    }
                }
            }
        }
        return null;
    }

    /** @return this silo as the thing the Launch Control fires (D70) */
    public LaunchSite site() {
        return new LaunchSite() {

            @Override
            public double centerX() {
                return shaftX();
            }

            @Override
            public double centerZ() {
                return shaftZ();
            }

            @Override
            public MissileStatus status() {
                EntityMissile m = missile();
                if (m != null) {
                    return m.status();
                }
                MissileTier stacked = stackedBody();
                return stacked == null ? MissileStatus.NONE : new MissileStatus(false, stacked, 0, 0, 0, null, 0);
            }

            @Override
            public int maxRange() {
                EntityMissile m = missile();
                return m == null ? 0 : m.maxRange(IcbmConfig.siloRangeFactor);
            }

            @Override
            public double expectedRadius(double targetX, double targetZ) {
                EntityMissile m = missile();
                return m == null ? 0.0 : m.expectedRadius(targetX, targetZ, IcbmConfig.siloRangeFactor);
            }

            @Override
            public LaunchResult hold() {
                EntityMissile m = missile();
                if (m != null && hatchVoltage() < SiloRules.requiredVoltageIndex(m.tier())) {
                    return LaunchResult.SILO_TIER;
                }
                return LaunchResult.OK;
            }

            @Override
            public LaunchResult launch(double targetX, double targetZ,
                net.minecraft.entity.player.EntityPlayer source) {
                if (!mMachine) {
                    return LaunchResult.NO_MISSILE;
                }
                EntityMissile m = missile();
                if (m == null) {
                    return stackedBody() == null ? LaunchResult.NO_MISSILE : LaunchResult.NOT_ARMED;
                }
                LaunchResult held = hold();
                if (held != LaunchResult.OK) {
                    return held;
                }
                // The doors open first; the missile's warm-up waits for them, so it never climbs into closed doors
                // (D89).
                World w = getBaseMetaTileEntity().getWorld();
                long now = w.getTotalWorldTime();
                SiloDoors probe = new SiloDoors();
                probe.restore(doors.state(), doors.start(), doors.duration());
                probe.open(now, IcbmConfig.doorOpenTicks);
                LaunchResult r = m.launch(
                    targetX,
                    targetZ,
                    source,
                    IcbmConfig.siloScatterFactor,
                    IcbmConfig.siloRangeFactor,
                    probe.ticksUntilOpen(now));
                if (r == LaunchResult.OK) {
                    doors.open(now, IcbmConfig.doorOpenTicks);
                    pushDoors();
                }
                return r;
            }

            @Override
            public int countdownTicks() {
                EntityMissile m = launching();
                return m == null ? -1 : m.countdownLeft();
            }

            @Override
            public boolean ignited() {
                EntityMissile m = launching();
                return m != null && m.phase() == FlightPhase.CLIMB;
            }

            @Override
            public boolean abort() {
                EntityMissile m = launching();
                if (m == null || !m.abort()) {
                    return false;
                }
                updateDoors(getBaseMetaTileEntity().getWorld());
                return true;
            }
        };
    }

    // --- the cycle -------------------------------------------------------------------------------------------------

    /** @return the voltage index of the weakest Energy Hatch, or -1 when there is none */
    private int hatchVoltage() {
        int lowest = Integer.MAX_VALUE;
        for (MTEHatch hatch : validMTEList(mEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        for (MTEHatch hatch : validMTEList(mExoticEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        return lowest == Integer.MAX_VALUE ? -1 : lowest;
    }

    private static CheckRecipeResult failure(String key) {
        return SimpleCheckRecipeResult.ofFailure(key);
    }

    @Override
    public @Nonnull CheckRecipeResult checkProcessing() {
        if (!ModuleState.isActive(ModuleId.ICBM)) {
            return CheckRecipeResultRegistry.NO_RECIPE; // the module is off: registered but inert
        }
        int volts = hatchVoltage();
        if (volts < 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        if (!shaftLoaded()) {
            return failure("gregguns_silo_chunks");
        }
        if (pendingTier != 0) {
            CheckRecipeResult resumed = resumePending(volts);
            if (resumed != null) {
                return resumed;
            }
        }
        EntityMissile m = missile();
        if (m != null) {
            return pump(m, volts);
        }
        if (stackedBody() != null) {
            return failure("gregguns_silo_hand_stacked");
        }
        return startMating(volts);
    }

    /**
     * A mating cycle was cut short (power loss, a broken structure, no repair) or its missile could not be placed: the
     * parts are already out of the bus, so finish with them instead of taking new ones.
     *
     * @return the cycle result, or null when the shaft now holds the missile and the normal flow continues
     */
    private CheckRecipeResult resumePending(int volts) {
        MissileTier tier = MissileTier.byNumber(pendingTier);
        World w = getBaseMetaTileEntity().getWorld();
        if (tier == null || w == null) {
            pendingTier = 0;
            return null;
        }
        if (missile() != null || stackedBody() != null) {
            dropParts(tier, w); // the shaft was taken meanwhile; keeps the parts when the drop fails
            return failure("gregguns_silo_hand_stacked");
        }
        if (pendingReady) {
            return spawnPending(tier, w) ? null : failure("gregguns_silo_chunks");
        }
        if (volts < SiloRules.requiredVoltageIndex(tier)) {
            return CheckRecipeResultRegistry.insufficientMachineTier(SiloRules.requiredVoltageIndex(tier));
        }
        begin(SiloRules.MATE_TICKS, volts, tier);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /** Pumps one portion of fuel into the missile in the shaft. */
    private CheckRecipeResult pump(EntityMissile m, int volts) {
        if (m.fuelMb() >= m.capacityMb()) {
            return failure("gregguns_silo_full");
        }
        if (volts < SiloRules.requiredVoltageIndex(m.tier())) {
            return CheckRecipeResultRegistry.insufficientMachineTier(SiloRules.requiredVoltageIndex(m.tier()));
        }
        FluidStack offered = null;
        boolean sawFluid = false;
        for (FluidStack fs : getStoredFluids()) {
            if (fs == null || fs.getFluid() == null || fs.amount <= 0) {
                continue;
            }
            sawFluid = true;
            if (m.tier()
                .acceptsFuel(
                    fs.getFluid()
                        .getName())) {
                offered = fs;
                break;
            }
        }
        if (offered == null) {
            return failure(sawFluid ? "gregguns_silo_wrong_fuel" : "gregguns_silo_no_fuel");
        }
        int mb = SiloRules.pumpAmount(m.capacityMb(), m.fuelMb(), offered.amount);
        if (mb <= 0 || !depleteInput(new FluidStack(offered.getFluid(), mb))) {
            return failure("gregguns_silo_no_fuel");
        }
        m.fill(mb, true);
        begin(SiloRules.PUMP_TICKS, volts, m.tier());
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /** Takes a body and a fitting warhead out of the buses and starts the mating cycle. */
    private CheckRecipeResult startMating(int volts) {
        ItemStack bodyStack = null;
        ItemStack warheadStack = null;
        MissileTier tier = null;
        boolean anyWarhead = false;
        boolean lowVoltageBody = false;
        int lowVoltageIndex = 0;
        for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                ItemStack s = bus.getStackInSlot(i);
                if (s == null || s.stackSize <= 0) {
                    continue;
                }
                MissileTier t = MissileBodies.tierOf(s);
                if (t != null && bodyStack == null && volts >= SiloRules.requiredVoltageIndex(t)) {
                    bodyStack = s;
                    tier = t;
                } else if (t != null && bodyStack == null) {
                    // A body this hatch cannot handle: keep looking, but remember it for the message.
                    lowVoltageBody = true;
                    lowVoltageIndex = Math.max(lowVoltageIndex, SiloRules.requiredVoltageIndex(t));
                } else if (isWarhead(s)) {
                    anyWarhead = true;
                }
            }
        }
        if (bodyStack == null) {
            return lowVoltageBody ? CheckRecipeResultRegistry.insufficientMachineTier(lowVoltageIndex)
                : failure("gregguns_silo_no_body");
        }
        for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                ItemStack s = bus.getStackInSlot(i);
                if (s != null && s.stackSize > 0 && isWarhead(s) && tier.carries(casingOf(s).tier())) {
                    warheadStack = s;
                    break;
                }
            }
            if (warheadStack != null) {
                break;
            }
        }
        ExplosiveTier warheadTier = warheadStack == null ? null : casingOf(warheadStack).tier();
        SiloRules.Mate verdict = SiloRules.mate(tier, warheadTier, volts);
        switch (verdict) {
            case NO_BODY:
                return failure("gregguns_silo_no_body");
            case NO_WARHEAD:
                return failure(anyWarhead ? "gregguns_silo_wrong_warhead" : "gregguns_silo_no_warhead");
            case WRONG_WARHEAD:
                return failure("gregguns_silo_wrong_warhead");
            case LOW_VOLTAGE:
                return CheckRecipeResultRegistry.insufficientMachineTier(SiloRules.requiredVoltageIndex(tier));
            default:
                break;
        }
        WarheadCasing casing = casingOf(warheadStack);
        WarheadData data = WarheadStacks.read(casing, warheadStack);
        pendingTier = tier.number();
        pendingGuidance = MissileBodies.guidanceOf(bodyStack);
        pendingExplosive = data.explosive();
        pendingFill = data.fill();
        pendingCasing = casing.number();
        pendingReady = false;
        bodyStack.stackSize--;
        if (bodyStack.stackSize <= 0) {
            removeFromBuses(bodyStack);
        }
        warheadStack.stackSize--;
        if (warheadStack.stackSize <= 0) {
            removeFromBuses(warheadStack);
        }
        updateSlots();
        begin(SiloRules.MATE_TICKS, volts, tier);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private void removeFromBuses(ItemStack empty) {
        for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                if (bus.getStackInSlot(i) == empty) {
                    bus.setInventorySlotContents(i, null);
                    return;
                }
            }
        }
    }

    private void begin(int ticks, int volts, MissileTier tier) {
        mMaxProgresstime = ticks;
        lEUt = -GTValues.V[Math.max(SiloRules.requiredVoltageIndex(tier), 0)];
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
    }

    private static boolean isWarhead(ItemStack s) {
        return Block.getBlockFromItem(s.getItem()) instanceof BlockWarhead;
    }

    private static WarheadCasing casingOf(ItemStack s) {
        return ((BlockWarhead) Block.getBlockFromItem(s.getItem())).casing();
    }

    /** The mating cycle ended: the armed missile stands in the shaft. */
    @Override
    protected void outputAfterRecipe() {
        if (pendingTier == 0) {
            return;
        }
        MissileTier tier = MissileTier.byNumber(pendingTier);
        World w = getBaseMetaTileEntity().getWorld();
        if (tier == null || w == null) {
            pendingTier = 0;
            return;
        }
        if (w.isRemote) {
            return;
        }
        pendingReady = true;
        if (shaftLoaded() && (missile() != null || stackedBody() != null)) {
            // The shaft was taken while the cycle ran: give the parts back rather than lose them.
            dropParts(tier, w);
            return;
        }
        // On failure (shaft chunks unloaded) pendingTier stays and checkProcessing retries.
        spawnPending(tier, w);
    }

    /** @return whether the missile stands in the shaft now; false keeps the pending parts for a retry */
    private boolean spawnPending(MissileTier tier, World w) {
        if (!shaftLoaded()) {
            return false;
        }
        EntityMissile missile = EntityMissile.create(tier, w);
        // The missile stands on the pad's top face, as on an open pad.
        missile.setLocationAndAngles(shaftX(), padY() + EntityMissile.PAD_SURFACE, shaftZ(), 0.0F, 0.0F);
        missile.configure(pendingGuidance, pendingExplosive, pendingFill, pendingCasing, null);
        UUID owner = getBaseMetaTileEntity().getOwnerUuid();
        if (owner != null) {
            missile.setOwner(owner, getBaseMetaTileEntity().getOwnerName());
        }
        if (!w.spawnEntityInWorld(missile)) {
            return false;
        }
        pendingTier = 0;
        pendingReady = false;
        return true;
    }

    /** Puts the pending body and warhead into the world; keeps them pending when the shaft's chunks are not loaded. */
    private void dropParts(MissileTier tier, World w) {
        if (!shaftLoaded()) {
            return;
        }
        ItemStack body = MissileBodies.body(tier, pendingGuidance);
        WarheadCasing c = com.gregguns.icbm.missile.MissileCasings.byNumber(pendingCasing);
        ItemStack warhead = WarheadStacks.make(
            net.minecraft.item.Item.getItemFromBlock(com.gregguns.icbm.warhead.WarheadBlocks.of(c)),
            c,
            pendingExplosive,
            pendingFill);
        boolean dropped = true;
        for (ItemStack s : new ItemStack[] { body, warhead }) {
            dropped &= w.spawnEntityInWorld(new EntityItem(w, shaftX(), floorY() + 1.0, shaftZ(), s));
        }
        if (dropped) {
            pendingTier = 0;
            pendingReady = false;
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger(K_PENDING, pendingTier);
        tag.setInteger(K_P_GUIDANCE, pendingGuidance);
        tag.setString(K_P_EXPLOSIVE, pendingExplosive);
        tag.setInteger(K_P_FILL, pendingFill);
        tag.setInteger(K_P_CASING, pendingCasing);
        tag.setBoolean(K_P_READY, pendingReady);
        tag.setString(
            K_DOOR_STATE,
            doors.state()
                .name());
        tag.setLong(K_DOOR_START, doors.start());
        tag.setInteger(K_DOOR_DUR, doors.duration());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        pendingTier = tag.getInteger(K_PENDING);
        pendingGuidance = tag.getInteger(K_P_GUIDANCE);
        pendingExplosive = tag.hasKey(K_P_EXPLOSIVE) ? tag.getString(K_P_EXPLOSIVE) : WarheadData.DEFAULT_EXPLOSIVE;
        pendingFill = Math.max(1, tag.getInteger(K_P_FILL));
        pendingCasing = Math.max(1, tag.getInteger(K_P_CASING));
        pendingReady = tag.getBoolean(K_P_READY);
        SiloDoors.State st;
        try {
            st = SiloDoors.State.valueOf(tag.getString(K_DOOR_STATE));
        } catch (IllegalArgumentException e) {
            st = SiloDoors.State.CLOSED;
        }
        doors.restore(st, tag.getLong(K_DOOR_START), tag.hasKey(K_DOOR_DUR) ? tag.getInteger(K_DOOR_DUR) : 40);
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return 0;
    }

    // --- tooltip ----------------------------------------------------------------------------------------------------

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(lang("type"))
            .addInfo(lang("info.mate"))
            .addInfo(lang("info.fuel"))
            .addInfo(lang("info.tier"))
            .addInfo(lang("info.bonus"))
            .addInfo(lang("info.launch"))
            .beginStructureBlock(SiloShape.WIDTH, SiloShape.HEIGHT, SiloShape.DEPTH, false)
            .addController(lang("structure.controller"))
            .addCasingInfoRange(
                StatCollector.translateToLocal("gregguns.casings.3.name"),
                SiloShape.CASING_MINIMUM,
                SiloShape.casingPositions(),
                false)
            .addInputBus(lang("structure.hatch"), 1)
            .addInputHatch(lang("structure.hatch"), 1)
            .addEnergyHatch(lang("structure.hatch"), 1)
            .addMaintenanceHatch(lang("structure.hatch"), 1)
            .addCasing(Integer.toString(SiloShape.count("G")), lang("structure.glass"), false)
            .addCasing(Integer.toString(SiloShape.count("B")), lang("structure.rim"), false)
            .addAir(lang("structure.shaft"))
            .addStructureInfo(lang("structure.footer"))
            .addSubChannel(gregtech.common.misc.GTStructureChannels.BOROGLASS)
            .toolTipFinisher(com.gregguns.core.text.AddedBy.NAME);
        return tt;
    }

    private static String lang(String key) {
        return StatCollector.translateToLocal("gregguns.machine.missile_silo." + key);
    }

    // --- textures -------------------------------------------------------------------------------------------------

    /** The overlay icons, requested when the first controller is constructed (before GT stitches textures). */
    private static final class Overlays {

        static final IIconContainer FRONT = icon("OVERLAY_FRONT");
        static final IIconContainer FRONT_GLOW = icon("OVERLAY_FRONT_GLOW");
        static final IIconContainer FRONT_ACTIVE = icon("OVERLAY_FRONT_ACTIVE");
        static final IIconContainer FRONT_ACTIVE_GLOW = icon("OVERLAY_FRONT_ACTIVE_GLOW");

        private Overlays() {}

        static void load() {
            // Loading the class runs the initializers above.
        }

        private static IIconContainer icon(String file) {
            return Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file);
        }
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture casing = getCasingTexture();
        if (side != facing) {
            return new ITexture[] { casing };
        }
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? Overlays.FRONT_ACTIVE : Overlays.FRONT)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(active ? Overlays.FRONT_ACTIVE_GLOW : Overlays.FRONT_GLOW)
                .extFacing()
                .glow()
                .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(GregGunsCasings.textureIndex(GregGunsCasings.GANTRY_CASING));
    }

    // --- construction -----------------------------------------------------------------------------------------------

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(PIECE, trigger, hintsOnly, SiloShape.CONTROLLER_COLUMN, SiloShape.CONTROLLER_ROW, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        return survivalBuildPiece(
            PIECE,
            trigger,
            SiloShape.CONTROLLER_COLUMN,
            SiloShape.CONTROLLER_ROW,
            0,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        // The silo stands upright: no pointing up or down, no roll, no upside-down.
        return (d, r, f) -> (d.flag & (ForgeDirection.UP.flag | ForgeDirection.DOWN.flag)) == 0 && r.isNotRotated()
            && !f.isVerticallyFliped();
    }

    @Override
    public boolean isRotationChangeAllowed() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_COMPRESSOR;
    }
}
