package com.gregguns.machine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.MultiAmpEnergy;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.chainItemPipeCasings;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.recipe.GregGunsRecipeMaps;
import com.gregguns.recipe.ProductionRecipes;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.IGTHatchAdder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.tooltip.TooltipTier;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;

/**
 * The Ammunition Works: a long, non-cube production hall that runs the {@code gregguns.ammoPress} recipe map with
 * many parallels. A feeder (where the controller, the input hatches, the energy hatch, the maintenance hatch and the
 * muffler sit), two to eight press bays, and a packing station (the output hatches) are laid end to end; players
 * lengthen the hall by adding bays. Parallels are {@code 4 x bays x voltage tier} and the belt (a line of one GT item
 * pipe casing tier down the floor) speeds recipes up.
 *
 * <p>
 * The numbers live in {@link AmmoWorksShape}; this class is the StructureLib glue. Construct only inside GT's MTE
 * window ({@link AmmoPressMachines#register()}), and only after {@link GregGunsCasings#create} has run. Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Ammunition Works, GGS-08-57 to GGS-08-63),
// docs/design/0011-machine-policy.md
// Source (the pattern: one piece per repeated segment, bay loop in checkMachine, hologram sized by a channel):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L440-L505
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L95-L226
// Deviation: no Energy Hatch tier limit (the sheet says LV to IV): parallels are already capped by the power the
// hatches
// supply, and a higher hatch only makes the hall faster, so refusing it would add a rule with no benefit.
// Deviation: the animation (ram pistons, belt rounds, crane) and its screwdriver toggle are not built; the controller
// has the static and active front overlays only. The screwdriver toggles input separation, as on GT's Mixer.
// Deviation: the Ram Housing counts as one of the casing positions; the belt is read as GT's item pipe casing tier
// (1 to 8, as the Mixer numbers it), so the sheet's "belt tier 2" example is the second pipe casing.
public final class MTEAmmoWorks extends MTEExtendedPowerMultiBlockBase<MTEAmmoWorks>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String PIECE_FEEDER = "feeder";
    private static final String PIECE_BAY = "bay";
    private static final String PIECE_STATION = "station";
    private static final String PIECE_BENCH = "bench";

    /** Pollution per second the running hall adds, the sheet's proposal. */
    private static final int POLLUTION_PER_SECOND = 40;

    // Metas of gt.blockcasings2 (GregTechAPI.sBlockCasings2).
    private static final int GEARBOX_STEEL_META = 3;
    private static final int ASSEMBLER_META = 9;
    private static final int MOTOR_META = 11;
    private static final int PIPE_STEEL_META = 13;

    private static final String OVERLAY_DIR = "machines/ammunition_works/";

    // Per-check scratch, reset at the start of every checkMachine.
    private int casingCount;
    private int beltTier = -1;
    private int bays;
    private boolean benchActive;
    private final List<MTEHatchInputBus> benchInputBuses = new ArrayList<>();
    private final List<MTEHatchOutputBus> benchOutputBuses = new ArrayList<>();

    // The filling bench's running cycle (server only, not saved: nothing leaves a bus until a cycle ends).
    private AmmoWorksFill.Plan fillPlan;
    private int fillTicksLeft;
    private int fillIdle;
    private int fillCursor;
    private List<AmmoWorksFill.Recipe> fillRecipes;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id           the MTE id, from {@link GregGunsMTEIDs}
     * @param name         GT's internal name
     * @param nameRegional the English display name GT registers for the block
     */
    public MTEAmmoWorks(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
        // Touch the overlay holder so the icons are requested now, before GT stitches the block atlas.
        Overlays.load();
    }

    private MTEAmmoWorks(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEAmmoWorks(mName);
    }

    /** Lazily built, because the casing block must exist before the elements are resolved. */
    private static final class Structure {

        static final IStructureDefinition<MTEAmmoWorks> DEFINITION = build();

        private Structure() {}

        private static IStructureElement<MTEAmmoWorks> casing(int meta) {
            return onElementPass(MTEAmmoWorks::onCasingAdded, lazy(() -> ofBlock(GregGunsCasings.INSTANCE, meta)));
        }

        /**
         * A fixed GT machine casing from {@code gt.blockcasings2}; the block is read lazily because GT sets it late.
         */
        private static IStructureElement<MTEAmmoWorks> gtCasing(int meta) {
            return lazy(() -> ofBlock(GregTechAPI.sBlockCasings2, meta));
        }

        private static IStructureDefinition<MTEAmmoWorks> build() {
            int casingIndex = GregGunsCasings.textureIndex(GregGunsCasings.WORKS_CASING);
            return StructureDefinition.<MTEAmmoWorks>builder()
                .addShape(PIECE_FEEDER, AmmoWorksShape.FEEDER)
                .addShape(PIECE_BAY, AmmoWorksShape.BAY)
                .addShape(PIECE_STATION, AmmoWorksShape.STATION)
                .addShape(PIECE_BENCH, AmmoWorksShape.BENCH)
                // Feeder casing: where fluid and item inputs, power, maintenance and the muffler go.
                .addElement(
                    'F',
                    buildHatchAdder(MTEAmmoWorks.class)
                        .atLeast(InputBus, InputHatch, Energy, MultiAmpEnergy, Maintenance, Muffler)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(casing(GregGunsCasings.WORKS_CASING)))
                // Packing casing: where the output hatches go, and spare power.
                .addElement(
                    'O',
                    buildHatchAdder(MTEAmmoWorks.class).atLeast(OutputBus, OutputHatch, Energy, MultiAmpEnergy)
                        .casingIndex(casingIndex)
                        .hint(2)
                        .buildAndChain(casing(GregGunsCasings.WORKS_CASING)))
                .addElement('A', casing(GregGunsCasings.WORKS_CASING))
                // Bench casing: the optional filling bench's own buses go here. It is not counted toward the casing
                // minimum, so an absent or half-built bench never changes whether the hall forms.
                .addElement(
                    'K',
                    buildHatchAdder(MTEAmmoWorks.class).atLeast(BenchHatch.BenchInputBus, BenchHatch.BenchOutputBus)
                        .casingIndex(casingIndex)
                        .hint(3)
                        .buildAndChain(lazy(() -> ofBlock(GregGunsCasings.INSTANCE, GregGunsCasings.WORKS_CASING))))
                .addElement(BenchTable.SYMBOL, BenchTable.element())
                .addElement('M', casing(GregGunsCasings.RAM_HOUSING))
                // Windows: any GT glass tier (and the other glasses GT lists). The tier getter always answers "not
                // set", so mixed glass passes and the hall has no glass progression wall.
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L891-L925
                // (vanilla glass is tier 3 in GlassTier, so even it is accepted:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GlassTier.java)
                .addElement('G', chainAllGlasses())
                // Interior set dressing. These are required, fixed GT blocks that are not counted as casings, so
                // they never trade off against hatches: press rods, ceiling pipe runs, drive gearboxes, the feed
                // motors and the packing station's assembler arch.
                // Source (casing metas):
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/casing/Casings.java#L80-L113
                .addElement('R', ofFrame(Materials.Steel))
                .addElement('P', gtCasing(PIPE_STEEL_META))
                .addElement('B', gtCasing(GEARBOX_STEEL_META))
                .addElement('N', gtCasing(MOTOR_META))
                .addElement('Y', gtCasing(ASSEMBLER_META))
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L993-L1011
                // (one tier for every block of the line: a second tier fails the check)
                .addElement(InteriorLamp.SYMBOL, InteriorLamp.element())
                .addElement('I', chainItemPipeCasings(-1, (t, tier) -> t.beltTier = tier, t -> t.beltTier))
                .build();
        }
    }

    /**
     * The filling bench's two hatch kinds. They are separate lists from the hall's own buses, so the normal recipe
     * never reads or fills them.
     */
    // Source (the pattern: a private enum of IHatchElement with its own adder and count):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L522-L540
    private enum BenchHatch implements IHatchElement<MTEAmmoWorks> {

        BenchInputBus(MTEHatchInputBus.class) {

            @Override
            public IGTHatchAdder<MTEAmmoWorks> adder() {
                return MTEAmmoWorks::addBenchInputBus;
            }

            @Override
            public long count(MTEAmmoWorks t) {
                return t.benchInputBuses.size();
            }
        },
        BenchOutputBus(MTEHatchOutputBus.class) {

            @Override
            public IGTHatchAdder<MTEAmmoWorks> adder() {
                return MTEAmmoWorks::addBenchOutputBus;
            }

            @Override
            public long count(MTEAmmoWorks t) {
                return t.benchOutputBuses.size();
            }
        };

        private final Class<? extends IMetaTileEntity> mteClass;

        BenchHatch(Class<? extends IMetaTileEntity> mteClass) {
            this.mteClass = mteClass;
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return Collections.singletonList(mteClass);
        }
    }

    private boolean addBenchInputBus(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof MTEHatchInputBus)
            || tile.getMetaTileEntity() instanceof MTEHatchInputBusME) {
            return false;
        }
        MTEHatchInputBus bus = (MTEHatchInputBus) tile.getMetaTileEntity();
        bus.updateTexture(casingIndex);
        return benchInputBuses.add(bus);
    }

    private boolean addBenchOutputBus(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof MTEHatchOutputBus)) {
            return false;
        }
        MTEHatchOutputBus bus = (MTEHatchOutputBus) tile.getMetaTileEntity();
        bus.updateTexture(casingIndex);
        return benchOutputBuses.add(bus);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        benchInputBuses.clear();
        benchOutputBuses.clear();
    }

    @Override
    public IStructureDefinition<MTEAmmoWorks> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    private void onCasingAdded() {
        casingCount++;
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseMetaTileEntity, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        beltTier = -1;
        bays = 0;
        benchActive = false;
        int col = AmmoWorksShape.CONTROLLER_COLUMN;
        int row = AmmoWorksShape.CONTROLLER_ROW;
        if (!checkPiece(PIECE_FEEDER, col, row, 0, errors)) {
            return;
        }
        // Bays: take them while they match. A probe that fails leaves partial counts behind, so the casing count is
        // rolled back and its errors are discarded; the station check after the loop reports a real mismatch.
        while (bays < AmmoWorksShape.MAX_BAYS) {
            int savedCasings = casingCount;
            if (!checkPiece(PIECE_BAY, col, row, -AmmoWorksShape.bayDepth(bays), new ArrayList<>())) {
                casingCount = savedCasings;
                break;
            }
            bays++;
        }
        if (!checkPiece(PIECE_STATION, col, row, -AmmoWorksShape.stationDepth(bays), errors)) {
            return;
        }
        if (bays < AmmoWorksShape.MIN_BAYS) {
            errors.add(StructureErrors.of("gregguns.machine.ammo_works.error.too_few_bays"));
            return;
        }
        checkBench(col, row);
        checkCasingMin(errors, casingCount, AmmoWorksShape.casingMinimum(bays));
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        checkHasMufflerHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        if (!mExoticEnergyHatches.isEmpty() && !mEnergyHatches.isEmpty()) {
            errors.add(StructureErrorRegistry.ONE_ENERGY_HATCH_ON_MULTI_OR_LASER);
        }
    }

    /**
     * The optional filling bench. It is probed after the station with its errors thrown away, and a probe that fails
     * (not built, half built, or without both bus kinds) leaves no trace: its hatches are dropped again, so the hall
     * stays formed and the bench is just not active.
     */
    private void checkBench(int col, int row) {
        boolean matched = checkPiece(PIECE_BENCH, col, row, -AmmoWorksShape.benchDepth(bays), new ArrayList<>());
        benchActive = matched && !benchInputBuses.isEmpty() && !benchOutputBuses.isEmpty();
        if (!benchActive) {
            benchInputBuses.clear();
            benchOutputBuses.clear();
        }
    }

    /** @return whether the filling bench was complete, with both bus kinds, at the last structure check */
    public boolean isBenchActive() {
        return mMachine && benchActive;
    }

    /** @return the number of press bays found by the last structure check, 0 when unformed */
    public int pressBays() {
        return mMachine ? bays : 0;
    }

    /** @return the belt's item pipe casing tier found by the last check, or -1 */
    public int beltTier() {
        return beltTier;
    }

    @Override
    public int getMaxParallelRecipes() {
        return AmmoWorksShape.parallels(bays, GTUtility.getTier(getMaxInputVoltage()));
    }

    /** @return the duration multiplier the belt gives (below 1 is faster) */
    double getSpeedBonus() {
        return AmmoWorksShape.speedMultiplier(beltTier);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L216-L219
        return new ProcessingLogic().setMaxParallelSupplier(this::getTrueParallel)
            .setSpeedBonusSupplier(this::getSpeedBonus);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GregGunsRecipeMaps.AMMO_PRESS;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return POLLUTION_PER_SECOND;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        // The lang file is read when the tooltip is first built, which is after the game has loaded it.
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(lang("type"))
            .addInfo(lang("info.map"))
            .addInfo(lang("info.parallel"))
            .addInfo(lang("info.bays"))
            .addInfo(lang("info.bench"))
            .addDynamicSpeedBonusInfo((float) AmmoWorksShape.BELT_STEP, TooltipTier.ITEM_PIPE_CASING)
            .addPollutionAmount(POLLUTION_PER_SECOND)
            .beginVariableStructureBlock(
                AmmoWorksShape.WIDTH,
                AmmoWorksShape.WIDTH,
                AmmoWorksShape.HEIGHT,
                AmmoWorksShape.HEIGHT,
                AmmoWorksShape.totalLength(AmmoWorksShape.MIN_BAYS),
                AmmoWorksShape.totalLength(AmmoWorksShape.MAX_BAYS),
                false)
            .addController(lang("structure.controller"))
            .addCasing(
                AmmoWorksShape.casingMinimum(AmmoWorksShape.MIN_BAYS) + "+",
                StatCollector.translateToLocal("gregguns.casings.0.name"),
                false)
            .addCasing("3 per bay", StatCollector.translateToLocal("gregguns.casings.1.name"), false)
            .addCasing("2 + 3 per bay + 3", lang("structure.belt"), true)
            .addCasing("16 + 6 per bay", lang("structure.glass"), true)
            .addCasing("10 + 6 per bay", lang("structure.pipes"), false)
            .addCasing("4 + 2 per bay", lang("structure.gearboxes"), false)
            .addCasing("2", lang("structure.motors"), false)
            .addCasing("3", lang("structure.assembler"), false)
            .addCasing("2 + 2 per bay", lang("structure.frames"), false)
            .addCasing("2 + 1 per bay", lang("structure.lamp"), false)
            .addInputBus("1+", lang("structure.feeder"), 1)
            .addInputHatch("0+", lang("structure.feeder"), 1)
            .addEnergyHatch("1+", lang("structure.feeder_or_packing"), 1, 2)
            .addMaintenanceHatch("1", lang("structure.feeder"), 1)
            .addMufflerHatch("1", lang("structure.feeder"), 1)
            .addOutputBus("1+", lang("structure.packing"), 2)
            .addOutputHatch("0+", lang("structure.packing"), 2)
            .addAir(lang("structure.air"))
            .addStructureInfo(lang("structure.bench"))
            .addStructureInfo(lang("structure.bench_hatches"))
            .addStructureInfo(lang("structure.bench_table"))
            .addStructureInfo(lang("structure.footer"))
            .addSubChannel(GTStructureChannels.STRUCTURE_LENGTH)
            .addSubChannel(GTStructureChannels.ITEM_PIPE_CASING)
            .addSubChannel(GTStructureChannels.BOROGLASS)
            .toolTipFinisher(com.gregguns.core.text.AddedBy.NAME);
        return tt;
    }

    private static String lang(String key) {
        return StatCollector.translateToLocal("gregguns.machine.ammo_works." + key);
    }

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
        return Textures.BlockIcons.getCasingTextureForId(GregGunsCasings.textureIndex(GregGunsCasings.WORKS_CASING));
    }

    /**
     * @param trigger the hologram projector or survival tool stack
     * @return how many bays to build: the length channel if the stack carries one, else its stack size
     */
    private static int baysFor(ItemStack trigger) {
        if (GTStructureChannels.STRUCTURE_LENGTH.hasValue(trigger)) {
            return GTStructureChannels.STRUCTURE_LENGTH
                .getValueClamped(trigger, AmmoWorksShape.MIN_BAYS, AmmoWorksShape.MAX_BAYS);
        }
        return AmmoWorksShape.clampBays(trigger.stackSize);
    }

    /**
     * @param trigger the hologram projector or survival tool stack
     * @return whether to build the optional filling bench: the stack's "height" channel is set to 1 or more
     */
    private static boolean benchFor(ItemStack trigger) {
        return GTStructureChannels.STRUCTURE_HEIGHT.getValue(trigger) >= 1;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        int col = AmmoWorksShape.CONTROLLER_COLUMN;
        int row = AmmoWorksShape.CONTROLLER_ROW;
        int count = baysFor(trigger);
        buildPiece(PIECE_FEEDER, trigger, hintsOnly, col, row, 0);
        for (int i = 0; i < count; i++) {
            buildPiece(PIECE_BAY, trigger, hintsOnly, col, row, -AmmoWorksShape.bayDepth(i));
        }
        buildPiece(PIECE_STATION, trigger, hintsOnly, col, row, -AmmoWorksShape.stationDepth(count));
        if (benchFor(trigger)) {
            buildPiece(PIECE_BENCH, trigger, hintsOnly, col, row, -AmmoWorksShape.benchDepth(count));
        }
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        int col = AmmoWorksShape.CONTROLLER_COLUMN;
        int row = AmmoWorksShape.CONTROLLER_ROW;
        int count = baysFor(trigger);
        int built = survivalBuildPiece(PIECE_FEEDER, trigger, col, row, 0, elementBudget, env, false, true);
        if (built >= 0) {
            return built;
        }
        for (int i = 0; i < count; i++) {
            built = survivalBuildPiece(
                PIECE_BAY,
                trigger,
                col,
                row,
                -AmmoWorksShape.bayDepth(i),
                elementBudget,
                env,
                false,
                true);
            if (built >= 0) {
                return built;
            }
        }
        built = survivalBuildPiece(
            PIECE_STATION,
            trigger,
            col,
            row,
            -AmmoWorksShape.stationDepth(count),
            elementBudget,
            env,
            false,
            true);
        if (built >= 0 || !benchFor(trigger)) {
            return built;
        }
        return survivalBuildPiece(
            PIECE_BENCH,
            trigger,
            col,
            row,
            -AmmoWorksShape.benchDepth(count),
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void onPostTick(IGregTechTileEntity baseMetaTileEntity, long tick) {
        super.onPostTick(baseMetaTileEntity, tick);
        if (baseMetaTileEntity.isServerSide()) {
            fillTick(baseMetaTileEntity);
        }
    }

    /**
     * One tick of the filling bench: plan a cycle, pay for it a tick at a time (the cycle stalls when the hatches are
     * short), and move the items when it ends. Runs beside the normal recipe and never blocks it.
     */
    private void fillTick(IGregTechTileEntity baseMetaTileEntity) {
        if (!mMachine || !benchActive || !baseMetaTileEntity.isAllowedToWork()) {
            fillPlan = null;
            fillIdle = 0;
            return;
        }
        if (fillPlan == null) {
            if (--fillIdle > 0) {
                return;
            }
            fillPlan = choosePlan();
            if (fillPlan == null) {
                fillIdle = AmmoWorksFill.IDLE_RETRY_TICKS;
                return;
            }
            fillTicksLeft = fillPlan.ticks;
        }
        if (!hasStoredEnergy(fillPlan.eutPerTick) || !drainEnergyInput(fillPlan.eutPerTick)) {
            return;
        }
        if (--fillTicksLeft <= 0) {
            AmmoWorksFill.commit(fillPlan, benchInputStore(), stationRoundStore(), benchOutputStore());
            fillPlan = null;
            fillIdle = 0;
        }
    }

    /** The first recipe, from a rotating start so every magazine gets a turn, that can run at least once. */
    private AmmoWorksFill.Plan choosePlan() {
        if (fillRecipes == null) {
            fillRecipes = AmmoWorksFill.recipes(ProductionRecipes::eutFor);
        }
        int n = fillRecipes.size();
        for (int i = 0; i < n; i++) {
            int at = (fillCursor + i) % n;
            AmmoWorksFill.Plan plan = AmmoWorksFill.plan(
                fillRecipes.get(at),
                getMaxParallelRecipes(),
                GTUtility.getTier(getMaxInputVoltage()),
                getSpeedBonus(),
                getMaxInputEu(),
                benchInputStore(),
                stationRoundStore(),
                benchOutputStore());
            if (plan != null) {
                fillCursor = (at + 1) % n;
                return plan;
            }
        }
        return null;
    }

    private boolean hasStoredEnergy(long eu) {
        long stored = 0;
        for (gregtech.api.metatileentity.implementations.MTEHatch hatch : getExoticAndNormalEnergyHatchList()) {
            if (hatch.isValid()) {
                stored += hatch.getBaseMetaTileEntity()
                    .getStoredEU();
            }
        }
        return stored >= eu;
    }

    /** The station's plain Output Buses: only items actually in a bus can be pulled, an ME bus has none to give. */
    private AmmoWorksFill.Store stationRoundStore() {
        List<IInventory> inventories = new ArrayList<>();
        for (MTEHatchOutputBus bus : mOutputBusses) {
            if (bus.isValid() && !(bus instanceof MTEHatchOutputBusME)) {
                inventories.add(bus.getBaseMetaTileEntity());
            }
        }
        return new SlotStore(inventories);
    }

    private AmmoWorksFill.Store benchInputStore() {
        List<IInventory> inventories = new ArrayList<>();
        for (MTEHatchInputBus bus : benchInputBuses) {
            if (bus.isValid()) {
                inventories.add(bus.getBaseMetaTileEntity());
            }
        }
        return new SlotStore(inventories);
    }

    private AmmoWorksFill.Store benchOutputStore() {
        return new BusOutputStore(benchOutputBuses);
    }

    private static Item item(String name) {
        return GameRegistry.findItem(GregGuns.MODID, name);
    }

    /** Items counted and taken slot by slot from plain inventories; it never stores. */
    private static final class SlotStore implements AmmoWorksFill.Store {

        private final List<IInventory> inventories;

        SlotStore(List<IInventory> inventories) {
            this.inventories = inventories;
        }

        @Override
        public long count(String name) {
            Item item = item(name);
            long total = 0;
            if (item != null) {
                for (IInventory inv : inventories) {
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (stack != null && stack.getItem() == item) {
                            total += stack.stackSize;
                        }
                    }
                }
            }
            return total;
        }

        @Override
        public long take(String name, long amount) {
            Item item = item(name);
            long left = amount;
            if (item != null) {
                for (IInventory inv : inventories) {
                    for (int i = 0; i < inv.getSizeInventory() && left > 0; i++) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (stack != null && stack.getItem() == item) {
                            int moved = (int) Math.min(left, stack.stackSize);
                            stack.stackSize -= moved;
                            inv.setInventorySlotContents(i, stack.stackSize > 0 ? stack : null);
                            left -= moved;
                        }
                    }
                }
            }
            return amount - left;
        }

        @Override
        public boolean fits(String name, long amount) {
            throw new UnsupportedOperationException("a source store never receives items");
        }

        @Override
        public void put(String name, long amount) {
            throw new UnsupportedOperationException("a source store never receives items");
        }
    }

    /** The bench Output Buses as the destination: simulated first with GT's own storePartial, then stored. */
    private static final class BusOutputStore implements AmmoWorksFill.Store {

        private final List<MTEHatchOutputBus> buses;

        BusOutputStore(List<MTEHatchOutputBus> buses) {
            this.buses = buses;
        }

        @Override
        public long count(String name) {
            return 0;
        }

        @Override
        public long take(String name, long amount) {
            return 0;
        }

        @Override
        public boolean fits(String name, long amount) {
            return store(name, amount, true);
        }

        @Override
        public void put(String name, long amount) {
            store(name, amount, false);
        }

        // Source (storePartial: simulate flag, returns true when the whole stack went in):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEHatchOutputBus.java#L185-L222
        private boolean store(String name, long amount, boolean simulate) {
            Item item = item(name);
            if (item == null || amount > Integer.MAX_VALUE) {
                return false;
            }
            ItemStack stack = new ItemStack(item, (int) amount);
            for (MTEHatchOutputBus bus : buses) {
                if (bus.isValid() && bus.storePartial(stack, simulate)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String[] getInfoData() {
        String[] base = super.getInfoData();
        String[] out = new String[base.length + 1];
        System.arraycopy(base, 0, out, 0, base.length);
        out[base.length] = lang("info.bench_status." + (isBenchActive() ? "active" : "inactive"));
        return out;
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        // The hall lies flat: no pointing up or down, no roll, no upside-down.
        return (d, r, f) -> (d.flag & (ForgeDirection.UP.flag | ForgeDirection.DOWN.flag)) == 0 && r.isNotRotated()
            && !f.isVerticallyFliped();
    }

    @Override
    public boolean isRotationChangeAllowed() {
        return false;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        inputSeparation = !inputSeparation;
        GTUtility.sendChatTrans(
            player,
            inputSeparation ? "GT5U.machines.separatebus.true" : "GT5U.machines.separatebus.false");
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_COMPRESSOR;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public Set<VoidingMode> getAllowedVoidingModes() {
        return VoidingMode.ITEM_ONLY_MODES;
    }
}
