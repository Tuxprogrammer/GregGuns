package com.gregguns.icbm.silo;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.MultiAmpEnergy;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
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
import com.gregguns.icbm.missile.BlockMissileBody;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileStacks;
import com.gregguns.icbm.missile.MissileStatus;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.missile.PadMissiles;
import com.gregguns.icbm.missile.TileMissileBody;
import com.gregguns.icbm.warhead.BlockWarhead;
import com.gregguns.icbm.warhead.TileWarhead;
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
    implements ISurvivalConstructable, ICasingTextureProvider, com.gregguns.icbm.component.LaunchView {

    private static final String PIECE = "silo";
    private static final String OVERLAY_DIR = "machines/thirdparty/ntm/missile_silo/";
    private static final String K_STAGE = "sequenceStage";
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
    private final SiloAim aim = new SiloAim();
    private int launches;
    private int unformedTicks;

    /** Set when the machine stopped itself (power loss, a broken structure, repairs); cleared once it restarts. */
    private boolean resumeWhenFormed;
    private static final String K_RESUME = "resumeWhenFormed";
    private EntitySiloDoors doorEntity;
    private SiloDoors.State pushedState;
    private long pushedStart;

    private int casingCount;
    /** The silo's own assembly in the shaft (D286); the parts themselves stand in the shaft as blocks. */
    private SiloSequence.Stage stage = SiloSequence.Stage.NONE;
    /** What the last look into the shaft saw; a change asks GT for a check. Not saved. */
    private SiloSequence.Shaft lastShaft;
    /** Ticks between looks into the shaft while the silo is formed. */
    private static final int SHAFT_WATCH_TICKS = 20;
    /**
     * Parts an earlier build took out of the bus into a single mating cycle and holds in the controller: 0 for none,
     * else the body's tier number. Read from old saves only; the next check stands them in the shaft.
     */
    private int pendingTier;
    private int pendingGuidance;
    private String pendingExplosive = WarheadData.DEFAULT_EXPLOSIVE;
    private int pendingFill = 1;
    private int pendingCasing = 1;
    /** Read from old saves with {@link #pendingTier}: that cycle had finished. */
    private boolean pendingReady;
    /** The last recipe check refused because the shaft's chunks were not loaded; retry when they are. */
    private boolean waitingForChunks;
    /** Set while a step is refused for want of EU; the silo then looks for power every {@link #POWER_POLL_TICKS}. */
    private boolean waitingForPower;
    /**
     * How often GT asked this silo to look for work, and how often it stopped for want of power (for the scenarios).
     */
    private int checks;
    private int powerStops;
    /** The lowest EU the silo wanted when it last waited for power, for the poll. */
    private long powerWanted;
    /** Ticks between looks at the hatches while the silo waits for power. */
    private static final int POWER_POLL_TICKS = 10;

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
                // The walls (layers 5 to 11): GT's Light or Dark Concrete, any finish, mixed (D116). Any hatch may
                // replace
                // one (D117). The hatch's casing texture stays the Gantry Casing's: GT's concrete has no casing index.
                .addElement(
                    'C',
                    buildHatchAdder(MTEMissileSilo.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(onElementPass(MTEMissileSilo::onCasingAdded, SiloConcrete.element())))
                // The floor's outer ring: the same concrete, where hatches may also go.
                .addElement(
                    'F',
                    buildHatchAdder(MTEMissileSilo.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(onElementPass(MTEMissileSilo::onCasingAdded, SiloConcrete.element())))
                // The three rings above the floor are clear glass so the missile shows (owner, 2026-10-26; D115):
                // Tinkers' Clear Glass or EnderIO's Quite Clear Glass, mixed is fine, and a hatch may replace a pane.
                // The glass is cosmetic and never gates anything.
                .addElement(
                    'G',
                    buildHatchAdder(MTEMissileSilo.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(2)
                        .buildAndChain(SiloGlass.element()))
                // TODO(integration): the Launch Control agent adds SiloLaunchControl.ELEMENT to this atLeast list
                // too; its canonical cell is the front aisle, shape row 8, WINDOW_COLUMN_LOW (the viewer's right, one
                // above the controller), which the generated and the dev layouts leave free.
                // The window edges of the controller's face (the columns either side of the controller): glass, the
                // wall's concrete or a hatch, so hatches can stand in the outer columns with the glass in the middle.
                .addElement(
                    'W',
                    buildHatchAdder(MTEMissileSilo.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(2)
                        .buildAndChain(ofChain(SiloGlass.element(), SiloConcrete.element())))
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

    /** @return the silo's own assembly stage (for the scenarios) */
    SiloSequence.Stage stage() {
        return stage;
    }

    /** @return the tier number of parts an old save held in the controller and not yet stood in the shaft, or 0 */
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

    /** How often an unformed silo asks GT to check its structure again, ticks. */
    private static final int STRUCTURE_RECHECK_TICKS = 40;

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        World w = base.getWorld();
        if (!mMachine) {
            // GT never hears of a block changing inside the shaft floor, and Galacticraft turns nine pad blocks into
            // its formed pad (a Full block and dummy blocks) without a neighbor update the controller sees, so an
            // unformed silo rechecks itself on a slow timer: the next tick runs GT's own structure check.
            // Source: MTEMultiBlockBase#onPostTick runs checkStructure when --mUpdate reaches 0:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L619-L631
            if (tick % STRUCTURE_RECHECK_TICKS == 0 && mUpdate <= 0) {
                mUpdate = 1;
            }
            // A freshly loaded silo is unformed until its first structure check; only a lasting gap counts (D90).
            if (++unformedTicks >= UNFORMED_GRACE_TICKS) {
                comeApart(w);
            }
            return;
        }
        unformedTicks = 0;
        resumeIfFormed(base);
        if (waitingForPower && tick % POWER_POLL_TICKS == 0) {
            retryWhenPowered();
        }
        if (tick % DOOR_SCAN_TICKS == 0 || doors.state() == SiloDoors.State.OPENING
            || doors.state() == SiloDoors.State.CLOSING) {
            updateDoors(w);
        }
        if (tick % 20 == 0) {
            ensureDoorEntity(w);
            retryWhenChunksLoaded();
        }
        if (tick % SHAFT_WATCH_TICKS == 0) {
            watchShaft(w);
        }
    }

    /**
     * GT turns a multiblock off for good when it stops itself, and shows "Shut down due to incomplete structure" until
     * someone finds the soft mallet. A silo that is missing a part (every generated one) forms the moment the part is
     * added, so it switches itself back on then, as the radar does (D195, D216). A player's own stop (soft mallet or
     * a cover) is never undone.
     */
    @Override
    public void stopMachine(@Nonnull gregtech.api.util.shutdown.ShutDownReason reason) {
        super.stopMachine(reason);
        if (reason == gregtech.api.util.shutdown.ShutDownReasonRegistry.POWER_LOSS) {
            powerStops++;
        }
        if (reason == gregtech.api.util.shutdown.ShutDownReasonRegistry.POWER_LOSS
            || reason == gregtech.api.util.shutdown.ShutDownReasonRegistry.STRUCTURE_INCOMPLETE
            || reason == gregtech.api.util.shutdown.ShutDownReasonRegistry.NO_REPAIR) {
            resumeWhenFormed = true;
        }
    }

    /** Switches a formed, repaired silo that stopped itself back on, which also clears the shutdown message. */
    private void resumeIfFormed(IGregTechTileEntity base) {
        if (!resumeWhenFormed || getRepairStatus() <= 0) {
            return;
        }
        resumeWhenFormed = false;
        if (!base.isAllowedToWork()) {
            base.enableWorking();
            scheduleRecipeCheckImmediate();
        }
    }

    /** @return whether the silo is waiting to switch itself back on (for the scenarios) */
    boolean resumeScheduled() {
        return resumeWhenFormed;
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
                    launches++;
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

    // --- OpenComputers view (gg_silo) ------------------------------------------------------------------------------

    private boolean serverLive() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && !base.isClientSide() && ModuleState.isActive(ModuleId.ICBM);
    }

    @Override
    public com.gregguns.icbm.control.LaunchTarget launchTarget() {
        return aim.target();
    }

    @Override
    public void aimAt(int x, int z) {
        if (serverLive()) {
            aim.set(com.gregguns.icbm.control.LaunchTarget.of(x, z));
            getBaseMetaTileEntity().markDirty();
        }
    }

    /** A silo has no data stick slot; a computer sets its target with {@code setTarget}. */
    @Override
    public com.gregguns.icbm.control.TargetStick.Outcome loadTargetFromStick() {
        return com.gregguns.icbm.control.TargetStick.Outcome.NO_STICK;
    }

    @Override
    public MissileStatus missileStatus() {
        return serverLive() && mMachine ? site().status() : MissileStatus.NONE;
    }

    @Override
    public String fuelFluid() {
        MissileStatus ms = missileStatus();
        return ms.present() && ms.tier()
            .fuels().length > 0 ? ms.tier()
                .fuels()[0] : "";
    }

    @Override
    public com.gregguns.icbm.control.ControlStatus launchStatus() {
        return serverLive() ? aim.status(mMachine, site()) : com.gregguns.icbm.control.ControlStatus.NO_PAD;
    }

    @Override
    public com.gregguns.icbm.control.ControlStatus launchNow() {
        return serverLive() ? aim.fire(mMachine, site()) : com.gregguns.icbm.control.ControlStatus.NO_PAD;
    }

    @Override
    public int launches() {
        return launches;
    }

    @Override
    public java.util.Map<String, Object> launchInfo() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("formed", mMachine);
        m.put(
            "doors",
            doors.state()
                .name()
                .toLowerCase(java.util.Locale.ROOT));
        return m;
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

    /** The shaft is not loaded: refuse now, and ask GT for a new check once it is (GT's checks are event-driven). */
    private CheckRecipeResult waitForChunks() {
        waitingForChunks = true;
        return failure("gregguns_silo_chunks");
    }

    private void retryWhenChunksLoaded() {
        if (waitingForChunks && shaftLoaded()) {
            waitingForChunks = false;
            scheduleRecipeCheck(gregtech.common.tileentities.machines.RecipeCheckReason.IMMEDIATE);
        }
    }

    /** @return the EU in all the Energy Hatches, exotic ones included */
    private long storedEu() {
        long sum = 0;
        for (MTEHatch hatch : validMTEList(mEnergyHatches)) {
            sum += hatch.getBaseMetaTileEntity()
                .getStoredEU();
        }
        for (MTEHatch hatch : validMTEList(mExoticEnergyHatches)) {
            sum += hatch.getBaseMetaTileEntity()
                .getStoredEU();
        }
        return sum;
    }

    /**
     * Checked before a step takes any item or fluid: GT power-fails a step it cannot pay on its first tick, which
     * would lose the parts (mating) or the fuel (pump) and, with the silo switching itself back on, loop every tick.
     *
     * @return the "no power" refusal, or null when the hatches hold enough for the first ticks of the tier's draw
     */
    private CheckRecipeResult noPower(MissileTier tier) {
        long draw = GTValues.V[Math.max(SiloRules.requiredVoltageIndex(tier), 0)];
        if (SiloRules.canStart(storedEu(), draw)) {
            waitingForPower = false;
            return null;
        }
        waitingForPower = true;
        powerWanted = draw * SiloRules.MIN_BUFFER_TICKS;
        return failure("gregguns_silo_no_power");
    }

    /** While the silo waits for EU, asks GT for a new check once the hatches hold enough (GT's checks are pushed). */
    private void retryWhenPowered() {
        if (waitingForPower && storedEu() >= powerWanted) {
            waitingForPower = false;
            scheduleRecipeCheckImmediate();
        }
    }

    int checks() {
        return checks;
    }

    int powerStops() {
        return powerStops;
    }

    /** @return whether the silo is refusing steps for want of power (for the scenarios) */
    boolean waitingForPower() {
        return waitingForPower;
    }

    private static CheckRecipeResult failure(String key) {
        return SimpleCheckRecipeResult.ofFailure(key);
    }

    @Override
    public @Nonnull CheckRecipeResult checkProcessing() {
        checks++;
        if (!ModuleState.isActive(ModuleId.ICBM)) {
            return CheckRecipeResultRegistry.NO_RECIPE; // the module is off: registered but inert
        }
        int volts = hatchVoltage();
        if (volts < 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        if (!shaftLoaded()) {
            return waitForChunks();
        }
        if (pendingTier != 0) {
            standLegacyParts();
        }
        SiloSequence.Shaft shaft = seeShaft();
        lastShaft = shaft;
        stage = SiloSequence.reconcile(stage, shaft);
        MissileTier standing = shaftBodyTier();
        BusScan bus = scanBus(standing);
        boolean warhead = stage == SiloSequence.Stage.NONE ? bus.anyWarhead : bus.warhead != null;
        switch (SiloSequence.next(stage, shaft, bus.anyBody, warhead)) {
            case PUMP:
                EntityMissile m = missile();
                return m == null ? failure("gregguns_silo_no_body") : pump(m, volts);
            case PLACE_BODY:
                return placeBody(bus, volts);
            case RUN_BODY:
                return runCycle(standing, volts, SiloRules.BODY_TICKS, "gregguns_silo_body_cycle");
            case PLACE_WARHEAD:
                return placeWarhead(standing, bus.warhead, volts);
            case RUN_WARHEAD:
                stage = SiloSequence.Stage.WARHEAD;
                return runCycle(standing, volts, SiloRules.WARHEAD_TICKS, "gregguns_silo_warhead_cycle");
            case WAIT_BODY:
                return failure("gregguns_silo_waiting_body");
            case WAIT_WARHEAD:
                return failure(
                    bus.anyWarhead ? "gregguns_silo_wrong_warhead"
                        : bus.anyInert ? "gregguns_silo_inert_warhead" : "gregguns_silo_no_warhead");
            case BAD_WARHEAD:
                return failure("gregguns_silo_bad_nose");
            case BLOCKED:
                return failure("gregguns_silo_blocked");
            case NO_BODY:
            default:
                return failure("gregguns_silo_no_body");
        }
    }

    // --- the shaft --------------------------------------------------------------------------------------------------

    /** @return X of the pad's center block, where the silo stands a body */
    private int baseX() {
        return (int) Math.floor(shaftX());
    }

    /** @return Z of the pad's center block */
    private int baseZ() {
        return (int) Math.floor(shaftZ());
    }

    /** @return what stands in the shaft; the caller checks that the shaft is loaded */
    private SiloSequence.Shaft seeShaft() {
        World w = getBaseMetaTileEntity().getWorld();
        if (missile() != null) {
            return SiloSequence.Shaft.MISSILE;
        }
        Block b = w.getBlock(baseX(), floorY(), baseZ());
        if (b instanceof BlockMissileBody) {
            MissileTier tier = ((BlockMissileBody) b).missileTier();
            int noseY = floorY() + tier.heightBlocks();
            Block nose = w.getBlock(baseX(), noseY, baseZ());
            if (!(nose instanceof BlockWarhead)) {
                return SiloSequence.Shaft.BODY;
            }
            net.minecraft.tileentity.TileEntity t = w.getTileEntity(baseX(), noseY, baseZ());
            boolean fits = t instanceof TileWarhead && !((TileWarhead) t).armed()
                && tier.carries(
                    ((BlockWarhead) nose).casing()
                        .tier());
            return fits ? SiloSequence.Shaft.STACK : SiloSequence.Shaft.BAD_STACK;
        }
        // A body anywhere else in the shaft is not on the pad's center: the silo leaves it to the player.
        return stackedBody() != null ? SiloSequence.Shaft.OTHER : SiloSequence.Shaft.EMPTY;
    }

    /** @return the tier of the body block on the pad's center, or null */
    private MissileTier shaftBodyTier() {
        Block b = getBaseMetaTileEntity().getWorld()
            .getBlock(baseX(), floorY(), baseZ());
        return b instanceof BlockMissileBody ? ((BlockMissileBody) b).missileTier() : null;
    }

    /**
     * Looks into the shaft once a second. The world wins (D286): a body or warhead taken away ends or steps back the
     * sequence, a running cycle whose part is gone stops at once, and a warhead the silo seated on a body that is gone
     * comes down as its item, once, through its own block. Any change asks GT for a check, since GT's checks are pushed
     * by hatch events only and a part placed or taken by hand is none of those.
     */
    private void watchShaft(World w) {
        if (w == null || !shaftLoaded()) {
            return;
        }
        SiloSequence.Shaft shaft = seeShaft();
        SiloSequence.Stage before = stage;
        SiloSequence.Stage after = SiloSequence.reconcile(before, shaft);
        boolean running = mMaxProgresstime > 0;
        boolean partGone = after == SiloSequence.Stage.NONE && before != SiloSequence.Stage.NONE
            || before == SiloSequence.Stage.WARHEAD && after == SiloSequence.Stage.BODY_DONE;
        if (before == SiloSequence.Stage.WARHEAD && after == SiloSequence.Stage.NONE
            && shaft != SiloSequence.Shaft.MISSILE) {
            dropLooseWarheads(w);
            shaft = seeShaft();
        }
        if (running && partGone) {
            cancelCycle();
        }
        if (!running || partGone) {
            stage = after;
        }
        if (shaft != lastShaft || stage != before) {
            lastShaft = shaft;
            if (!running || partGone) {
                scheduleRecipeCheckImmediate();
            }
        }
    }

    /**
     * Breaks every warhead left standing in the shaft's center column without a body under it; each block drops its own
     * item ({@link BlockWarhead#breakBlock}), so it comes back exactly once.
     */
    private void dropLooseWarheads(World w) {
        for (int y = floorY(); y < rimTop(); y++) {
            if (w.getBlock(baseX(), y, baseZ()) instanceof BlockWarhead) {
                w.setBlockToAir(baseX(), y, baseZ());
            }
        }
    }

    /** Stops a cycle whose part was taken away: no progress, no draw, and no end-of-cycle step. */
    private void cancelCycle() {
        mMaxProgresstime = 0;
        mProgresstime = 0;
        lEUt = 0;
        mEfficiencyIncrease = 0;
        getBaseMetaTileEntity().setActive(false);
    }

    // --- the steps -------------------------------------------------------------------------------------------------

    /** What the Input Buses hold, for one look. */
    private static final class BusScan {

        /** The first body the Energy Hatch can handle, or null. */
        ItemStack body;
        MissileTier bodyTier;
        /** Any body at all, also one too heavy for the hatch. */
        boolean anyBody;
        /** The heaviest voltage index a body in the bus would need, for the "insufficient tier" message. */
        int lowVoltageIndex;
        /** The first warhead the standing body carries, or null. */
        ItemStack warhead;
        boolean anyWarhead;
        boolean anyInert;
    }

    /**
     * @param standing the tier of the body in the shaft, or null; picks the fitting warhead
     */
    private BusScan scanBus(MissileTier standing) {
        BusScan scan = new BusScan();
        int volts = hatchVoltage();
        for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                ItemStack s = bus.getStackInSlot(i);
                if (s == null || s.stackSize <= 0) {
                    continue;
                }
                MissileTier t = MissileBodies.tierOf(s);
                if (t != null) {
                    scan.anyBody = true;
                    if (scan.body == null && volts >= SiloRules.requiredVoltageIndex(t)) {
                        scan.body = s;
                        scan.bodyTier = t;
                    } else if (scan.body == null) {
                        scan.lowVoltageIndex = Math.max(scan.lowVoltageIndex, SiloRules.requiredVoltageIndex(t));
                    }
                } else if (isWarhead(s)) {
                    scan.anyWarhead = true;
                    if (scan.warhead == null && standing != null && standing.carries(casingOf(s).tier())) {
                        scan.warhead = s;
                    }
                } else if (com.gregguns.icbm.warhead.InertWarheads.is(s)) {
                    // D282: an inert warhead has no charges; the silo never takes it, but says why.
                    scan.anyInert = true;
                }
            }
        }
        return scan;
    }

    /** Takes a body out of the bus, stands it on the pad's center and starts its cycle (D286). */
    private CheckRecipeResult placeBody(BusScan bus, int volts) {
        if (bus.body == null) {
            return CheckRecipeResultRegistry.insufficientMachineTier(bus.lowVoltageIndex);
        }
        MissileTier tier = bus.bodyTier;
        CheckRecipeResult dark = noPower(tier);
        if (dark != null) {
            return dark;
        }
        World w = getBaseMetaTileEntity().getWorld();
        if (!MissileStacks.canPlaceBody(w, baseX(), floorY(), baseZ(), tier)) {
            return failure("gregguns_silo_blocked");
        }
        TileMissileBody tile = MissileStacks
            .placeBody(w, baseX(), floorY(), baseZ(), tier, MissileBodies.guidanceOf(bus.body), true);
        if (tile == null) {
            return failure("gregguns_silo_blocked");
        }
        tile.setOwner(getBaseMetaTileEntity().getOwnerUuid(), getBaseMetaTileEntity().getOwnerName());
        takeOne(bus.body);
        stage = SiloSequence.Stage.BODY;
        lastShaft = SiloSequence.Shaft.BODY;
        begin(SiloRules.BODY_TICKS, volts, tier);
        return SimpleCheckRecipeResult.ofSuccess("gregguns_silo_body_cycle");
    }

    /** Takes a fitting warhead out of the bus, seats it on the standing body's nose and starts the mating cycle. */
    private CheckRecipeResult placeWarhead(MissileTier tier, ItemStack warhead, int volts) {
        if (tier == null || warhead == null) {
            return failure("gregguns_silo_no_warhead");
        }
        if (volts < SiloRules.requiredVoltageIndex(tier)) {
            return CheckRecipeResultRegistry.insufficientMachineTier(SiloRules.requiredVoltageIndex(tier));
        }
        CheckRecipeResult dark = noPower(tier);
        if (dark != null) {
            return dark;
        }
        World w = getBaseMetaTileEntity().getWorld();
        if (!MissileStacks.placeWarhead(
            w,
            baseX(),
            floorY() + tier.heightBlocks(),
            baseZ(),
            warhead,
            getBaseMetaTileEntity().getOwnerUuid(),
            getBaseMetaTileEntity().getOwnerName())) {
            return failure("gregguns_silo_blocked");
        }
        takeOne(warhead);
        stage = SiloSequence.Stage.WARHEAD;
        lastShaft = SiloSequence.Shaft.STACK;
        begin(SiloRules.WARHEAD_TICKS, volts, tier);
        return SimpleCheckRecipeResult.ofSuccess("gregguns_silo_warhead_cycle");
    }

    /** Runs a cycle on a part that already stands in the shaft: first run of an adopted part, or after a stop. */
    private CheckRecipeResult runCycle(MissileTier tier, int volts, int ticks, String key) {
        if (tier == null) {
            return failure("gregguns_silo_no_body");
        }
        if (volts < SiloRules.requiredVoltageIndex(tier)) {
            return CheckRecipeResultRegistry.insufficientMachineTier(SiloRules.requiredVoltageIndex(tier));
        }
        CheckRecipeResult dark = noPower(tier);
        if (dark != null) {
            return dark;
        }
        begin(ticks, volts, tier);
        return SimpleCheckRecipeResult.ofSuccess(key);
    }

    private void takeOne(ItemStack stack) {
        stack.stackSize--;
        if (stack.stackSize <= 0) {
            removeFromBuses(stack);
        }
        updateSlots();
    }

    /** Pumps one portion of fuel into the missile in the shaft. */
    private CheckRecipeResult pump(EntityMissile m, int volts) {
        if (m.fuelMb() >= m.capacityMb()) {
            return failure("gregguns_silo_full");
        }
        if (volts < SiloRules.requiredVoltageIndex(m.tier())) {
            return CheckRecipeResultRegistry.insufficientMachineTier(SiloRules.requiredVoltageIndex(m.tier()));
        }
        CheckRecipeResult dark = noPower(m.tier());
        if (dark != null) {
            return dark;
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
        return SimpleCheckRecipeResult.ofSuccess("gregguns_silo_fueling");
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

    /**
     * A cycle ended. A body cycle leaves the body ready for its warhead; a mating cycle arms the stack into the missile
     * entity (no drops: the blocks become the missile), which the fuel cycles then fill. GT asks for the next check
     * right after this, so the sequence runs on by itself.
     */
    @Override
    protected void outputAfterRecipe() {
        World w = getBaseMetaTileEntity().getWorld();
        if (w == null || w.isRemote || stage == SiloSequence.Stage.NONE) {
            return;
        }
        if (!shaftLoaded()) {
            // The shaft went away mid-cycle: keep the stage; the cycle runs again once it is back.
            return;
        }
        SiloSequence.Shaft shaft = seeShaft();
        SiloSequence.Stage now = SiloSequence.reconcile(stage, shaft);
        if (now != stage) {
            stage = now; // a part went missing at the last moment; the next check sorts it out
            return;
        }
        if (stage == SiloSequence.Stage.WARHEAD) {
            if (shaft != SiloSequence.Shaft.STACK) {
                return; // a lit or wrong warhead: the next check says so
            }
            MissileStacks.Armed armed = MissileStacks.armIntoEntity(w, baseX(), floorY(), baseZ(), null);
            if (armed.result != MissileStacks.ArmResult.OK) {
                return; // an incomplete body mends itself on its next update; the cycle runs again
            }
            lastShaft = SiloSequence.Shaft.MISSILE;
        }
        stage = SiloSequence.afterCycle(stage);
    }

    /**
     * An earlier build held the body and warhead of a mating cycle inside the controller. Stand them in the shaft as
     * this sequence would have, body and warhead, so the mating cycle runs again on them; if the shaft is taken, they
     * drop as items. Nothing is lost either way.
     */
    private void standLegacyParts() {
        MissileTier tier = MissileTier.byNumber(pendingTier);
        World w = getBaseMetaTileEntity().getWorld();
        if (tier == null || w == null) {
            pendingTier = 0;
            return;
        }
        if (seeShaft() != SiloSequence.Shaft.EMPTY
            || !MissileStacks.canPlaceBody(w, baseX(), floorY(), baseZ(), tier)) {
            dropParts(tier, w);
            return;
        }
        TileMissileBody body = MissileStacks.placeBody(w, baseX(), floorY(), baseZ(), tier, pendingGuidance, true);
        if (body == null) {
            dropParts(tier, w);
            return;
        }
        body.setOwner(getBaseMetaTileEntity().getOwnerUuid(), getBaseMetaTileEntity().getOwnerName());
        WarheadCasing c = com.gregguns.icbm.missile.MissileCasings.byNumber(pendingCasing);
        ItemStack warhead = WarheadStacks.make(
            net.minecraft.item.Item.getItemFromBlock(com.gregguns.icbm.warhead.WarheadBlocks.of(c)),
            c,
            pendingExplosive,
            pendingFill);
        boolean seated = MissileStacks.placeWarhead(
            w,
            baseX(),
            floorY() + tier.heightBlocks(),
            baseZ(),
            warhead,
            getBaseMetaTileEntity().getOwnerUuid(),
            getBaseMetaTileEntity().getOwnerName());
        if (!seated) {
            w.spawnEntityInWorld(new EntityItem(w, shaftX(), floorY() + 1.0, shaftZ(), warhead));
        }
        stage = seated ? SiloSequence.Stage.WARHEAD : SiloSequence.Stage.BODY_DONE;
        pendingTier = 0;
        pendingReady = false;
    }

    /** Puts the old save's held body and warhead into the world; keeps them held when the shaft is not loaded. */
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
        tag.setString(K_STAGE, stage.name());
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
        aim.save(tag);
        tag.setBoolean(K_RESUME, resumeWhenFormed);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        stage = SiloSequence.parse(tag.getString(K_STAGE));
        pendingTier = tag.getInteger(K_PENDING);
        pendingGuidance = tag.getInteger(K_P_GUIDANCE);
        pendingExplosive = tag.hasKey(K_P_EXPLOSIVE) ? tag.getString(K_P_EXPLOSIVE) : WarheadData.DEFAULT_EXPLOSIVE;
        pendingFill = Math.max(1, tag.getInteger(K_P_FILL));
        pendingCasing = Math.max(1, tag.getInteger(K_P_CASING));
        pendingReady = tag.getBoolean(K_P_READY);
        resumeWhenFormed = tag.getBoolean(K_RESUME);
        SiloDoors.State st;
        try {
            st = SiloDoors.State.valueOf(tag.getString(K_DOOR_STATE));
        } catch (IllegalArgumentException e) {
            st = SiloDoors.State.CLOSED;
        }
        doors.restore(st, tag.getLong(K_DOOR_START), tag.hasKey(K_DOOR_DUR) ? tag.getInteger(K_DOOR_DUR) : 40);
        aim.load(tag);
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

    /** A generated silo becomes the first real player's who uses it (D250); then GT's own handling. */
    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        com.gregguns.icbm.complex.gen.GtMachineHook.handOver(base, player);
        return super.onRightclick(base, player, side, x, y, z);
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
                lang("structure.concrete"),
                SiloShape.CASING_MINIMUM,
                SiloShape.casingPositions(),
                false)
            .addInputBus(lang("structure.hatch"), 1)
            .addInputHatch(lang("structure.hatch"), 1)
            .addEnergyHatch(lang("structure.hatch"), 1)
            .addMaintenanceHatch(lang("structure.hatch"), 1)
            .addCasing(Integer.toString(SiloShape.count("G") + SiloShape.count("W")), lang("structure.glass"), false)
            .addCasing(Integer.toString(SiloShape.count("B")), lang("structure.rim"), false)
            .addAir(lang("structure.shaft"))
            .addStructureInfo(lang("structure.footer"))
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
        static final IIconContainer FRONT_ACTIVE = icon("OVERLAY_FRONT_ACTIVE");

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
