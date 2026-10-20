package com.gregguns.machine.repair;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.MultiAmpEnergy;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofFrame;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.logic.RepairSpec;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.machine.InteriorLamp;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
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
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;

/**
 * The Armory Gantry: a long row of portal frames on rails that repairs many GregGuns armor pieces at once. A booth
 * (the controller, the buses, power and maintenance), two to eight repair bays and a closed tail are laid end to end;
 * players lengthen the Gantry by adding bays. Damaged items and the repair parts go in on an Input Bus; repaired items
 * come out on an Output Bus with damage 0 and every other tag unchanged.
 *
 * <p>
 * There is no recipe map: each cycle {@link RepairPlanner} reads the items and their repair lists, repairs up to one
 * item per bay (fewer when the hatches cannot supply the power), and runs for {@link RepairMath#durationTicks} of the
 * most damaged item. The tier gate is the lowest Energy Hatch tier. The shape lives in {@link GantryShape}. Construct
 * only inside GT's MTE window ({@link RepairMachines#register()}), after {@link GregGunsCasings#create}. Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Armory Gantry, GGS-08-69 to GGS-08-73),
// docs/design/0011-machine-policy.md
// Source (the pattern: a non-recipe multiblock whose checkProcessing reads item data, outputs through mOutputItems):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L224
// Source (the pattern: one piece per repeated segment, bays checked in a loop, hologram sized by a channel):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L95-L226
// Deviation: the number of items per cycle is the bay count capped by the power the hatches supply (half the recipe
// voltage per item); the sheet says "up to n" without a power cap, and GT shuts a machine down that over-draws.
// Deviation: no NEI recipe tab and no NEI code (GGS-08-31); the controller tooltip carries the rules.
// Deviation: the Repair Cradle's active texture and the hoist/crane animation are not built; the controller has the
// static and active front overlays only.
public final class MTEArmoryGantry extends MTEExtendedPowerMultiBlockBase<MTEArmoryGantry>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String PIECE_HEAD = "head";
    private static final String PIECE_BAY = "bay";
    private static final String PIECE_TAIL = "tail";

    private static final String OVERLAY_DIR = "machines/armory_gantry/";

    // Metas of gt.blockcasings2 (GregTechAPI.sBlockCasings2).
    // Source (casing metas):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/casing/Casings.java#L80-L113
    private static final int GEARBOX_STEEL_META = 3;
    private static final int MOTOR_META = 11;
    private static final int PIPE_STEEL_META = 13;

    private int bays;
    private int glassTier = -1;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id           the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name         GT's internal name
     * @param nameRegional the English display name GT registers for the block
     */
    public MTEArmoryGantry(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
        Overlays.load();
    }

    private MTEArmoryGantry(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEArmoryGantry(mName);
    }

    /** Lazily built, because the casing block must exist before the elements are resolved. */
    private static final class Structure {

        static final IStructureDefinition<MTEArmoryGantry> DEFINITION = build();

        private Structure() {}

        private static IStructureElement<MTEArmoryGantry> casing(int meta) {
            return lazy(() -> ofBlock(GregGunsCasings.INSTANCE, meta));
        }

        /** A fixed GT machine casing from {@code gt.blockcasings2}; read lazily because GT sets the block late. */
        private static IStructureElement<MTEArmoryGantry> gtCasing(int meta) {
            return lazy(() -> ofBlock(GregTechAPI.sBlockCasings2, meta));
        }

        private static IStructureDefinition<MTEArmoryGantry> build() {
            int casingIndex = GregGunsCasings.textureIndex(GregGunsCasings.GANTRY_CASING);
            return StructureDefinition.<MTEArmoryGantry>builder()
                .addShape(PIECE_HEAD, GantryShape.HEAD)
                .addShape(PIECE_BAY, GantryShape.BAY)
                .addShape(PIECE_TAIL, GantryShape.TAIL)
                // Gantry casing: head and tail walls and the bay floors; the hatches go here.
                .addElement(
                    'R',
                    buildHatchAdder(MTEArmoryGantry.class)
                        .atLeast(InputBus, OutputBus, Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(onElementPass(t -> {}, casing(GregGunsCasings.GANTRY_CASING))))
                .addElement('B', casing(GregGunsCasings.GANTRY_BEAM))
                .addElement('H', casing(GregGunsCasings.GANTRY_HOIST))
                .addElement('T', casing(GregGunsCasings.REPAIR_CRADLE))
                .addElement('L', ofFrame(Materials.Titanium))
                // Any glass tier, mixed is fine: the setter records the tier, but the getter always answers "not set",
                // so GT's tiered element never compares one glass block with another (a getter that returned the
                // stored tier would force a single tier). The Retort does the same.
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L891-L925
                .addElement('G', chainAllGlasses(-1, (t, tier) -> t.glassTier = tier, t -> -1))
                // Interior set dressing, required fixed GT blocks that are not casings (they never trade off against
                // hatches): the floor rails and hoist hook (steel frame boxes), the hoist drive and motors, and the
                // hydraulic pipe runs under the beam.
                .addElement('F', ofFrame(Materials.Steel))
                .addElement('X', gtCasing(GEARBOX_STEEL_META))
                .addElement('N', gtCasing(MOTOR_META))
                .addElement('P', gtCasing(PIPE_STEEL_META))
                .addElement(InteriorLamp.SYMBOL, InteriorLamp.element())
                .build();
        }
    }

    @Override
    public IStructureDefinition<MTEArmoryGantry> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseMetaTileEntity, ItemStack stack, List<StructureError> errors) {
        bays = 0;
        glassTier = -1;
        int col = GantryShape.CONTROLLER_COLUMN;
        int row = GantryShape.CONTROLLER_ROW;
        if (!checkPiece(PIECE_HEAD, col, row, 0, errors)) {
            return;
        }
        // Bays: take them while they match; a failed probe's errors are discarded and the tail check reports a real
        // mismatch (the same loop as the Ammunition Works).
        while (bays < GantryShape.MAX_BAYS) {
            if (!checkPiece(PIECE_BAY, col, row, -GantryShape.bayDepth(bays), new ArrayList<>())) {
                break;
            }
            bays++;
        }
        if (!checkPiece(PIECE_TAIL, col, row, -GantryShape.tailDepth(bays), errors)) {
            return;
        }
        if (bays < GantryShape.MIN_BAYS) {
            errors.add(StructureErrors.of("gregguns.machine.armory_gantry.error.too_few_bays"));
            return;
        }
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        if (!mExoticEnergyHatches.isEmpty() && !mEnergyHatches.isEmpty()) {
            errors.add(StructureErrorRegistry.ONE_ENERGY_HATCH_ON_MULTI_OR_LASER);
        }
    }

    /** @return the number of repair bays found by the last structure check, 0 when unformed */
    public int repairBays() {
        return mMachine ? bays : 0;
    }

    // --- the repair cycle -------------------------------------------------------------------------------------------

    /**
     * @return the tier of the weakest Energy Hatch, which is the highest item tier the Gantry repairs (GGS-08-72);
     *         null when there is no hatch
     */
    public @Nullable Tier repairTier() {
        int lowest = Integer.MAX_VALUE;
        for (MTEHatch hatch : validMTEList(mEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        for (MTEHatch hatch : validMTEList(mExoticEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        if (lowest == Integer.MAX_VALUE) {
            return null;
        }
        Tier found = Tier.STEAM;
        for (Tier t : Tier.values()) {
            if (t.voltageIndex() <= lowest) {
                found = t;
            }
        }
        return found;
    }

    /** The slots of every Input Bus, one after another. */
    private RepairMaterials.Slots busSlots() {
        List<MTEHatchInputBus> buses = new ArrayList<>();
        for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
            buses.add(bus);
        }
        return new RepairMaterials.Slots() {

            private int[] offsets;

            private int[] offsets() {
                if (offsets == null) {
                    offsets = new int[buses.size() + 1];
                    for (int i = 0; i < buses.size(); i++) {
                        offsets[i + 1] = offsets[i] + buses.get(i)
                            .getSizeInventory();
                    }
                }
                return offsets;
            }

            @Override
            public int size() {
                return offsets()[buses.size()];
            }

            private int busOf(int index) {
                int[] o = offsets();
                int b = 0;
                while (index >= o[b + 1]) {
                    b++;
                }
                return b;
            }

            @Override
            public ItemStack get(int index) {
                int b = busOf(index);
                return buses.get(b)
                    .getStackInSlot(index - offsets()[b]);
            }

            @Override
            public void set(int index, ItemStack stack) {
                int b = busOf(index);
                buses.get(b)
                    .setInventorySlotContents(index - offsets()[b], stack);
            }
        };
    }

    @Override
    public @Nonnull CheckRecipeResult checkProcessing() {
        Tier tier = repairTier();
        if (tier == null || bays < GantryShape.MIN_BAYS) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        RepairMaterials.Slots slots = busSlots();
        List<Integer> where = new ArrayList<>();
        List<RepairPlanner.Candidate> candidates = new ArrayList<>();
        List<RepairSpec> specs = new ArrayList<>();
        Tier tooHigh = null;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack s = slots.get(i);
            RepairSpec spec = RepairItems.specOf(s);
            if (spec == null || s.getItemDamage() <= 0) {
                continue;
            }
            if (!RepairMath.tierAllows(tier, RepairItems.tierOf(s))) {
                // Skipped and reported (GGS-08-72): the controller names the tier it would need.
                tooHigh = RepairItems.tierOf(s);
                continue;
            }
            where.add(i);
            candidates.add(RepairItems.candidate(s));
            specs.add(spec);
        }
        if (candidates.isEmpty()) {
            return tooHigh != null ? CheckRecipeResultRegistry.insufficientMachineTier(tooHigh.voltageIndex())
                : CheckRecipeResultRegistry.NO_RECIPE;
        }
        long perItem = RepairMath.euPerItemPerTick(tier);
        int powerCap = (int) Math.max(1L, Math.min(GantryShape.MAX_BAYS, getMaxInputPower() / perItem));
        RepairMaterials.Pool pool = RepairMaterials.count(slots, specs);
        RepairPlanner.Plan plan = RepairPlanner.plan(candidates, pool.counts(), tier, Math.min(bays, powerCap));
        if (plan.repaired() == 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        // Take the parts first, then lift each repaired item out of its bus slot into the output.
        List<ItemStack> repaired = new ArrayList<>();
        RepairMaterials.take(slots, pool, plan.consumed());
        for (int c = 0; c < candidates.size(); c++) {
            if (plan.statuses()
                .get(c) == RepairPlanner.Status.REPAIRED) {
                int slot = where.get(c);
                ItemStack done = slots.get(slot);
                slots.set(slot, null);
                ItemStack out = done.copy();
                out.setItemDamage(0);
                repaired.add(out);
            }
        }
        mOutputItems = repaired.toArray(new ItemStack[0]);
        lEUt = -(perItem * plan.repaired());
        mMaxProgresstime = RepairMath.durationTicks(plan.maxFraction());
        mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        mEfficiencyIncrease = 10000;
        updateSlots();
        GregGuns.LOG.debug("Armory Gantry repairing {} item(s) for {} ticks", repaired.size(), mMaxProgresstime);
        return CheckRecipeResultRegistry.SUCCESSFUL;
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
            .addInfo(lang("info.repair"))
            .addInfo(lang("info.tier"))
            .addInfo(lang("info.bays"))
            .addInfo(lang("info.nbt"))
            .beginVariableStructureBlock(
                GantryShape.WIDTH,
                GantryShape.WIDTH,
                GantryShape.HEIGHT,
                GantryShape.HEIGHT,
                GantryShape.totalLength(GantryShape.MIN_BAYS),
                GantryShape.totalLength(GantryShape.MAX_BAYS),
                false)
            .addController(lang("structure.controller"))
            .addCasingInfoRange(StatCollector.translateToLocal("gregguns.casings.3.name"), 0, 999, false)
            .addInputBus(lang("structure.casing"), 1)
            .addOutputBus(lang("structure.casing"), 1)
            .addEnergyHatch(lang("structure.casing"), 1)
            .addMaintenanceHatch(lang("structure.casing"), 1)
            .addCasing("10", lang("structure.glass"), false)
            .addCasing("2 + 5 per bay", lang("structure.rails"), false)
            .addCasing("1 per bay", lang("structure.gearbox"), false)
            .addCasing("2 per bay", lang("structure.motors"), false)
            .addCasing("2 + 2 per bay", lang("structure.pipes"), false)
            .addCasing("1 + 1 per bay", lang("structure.lamp"), false)
            .addAir(lang("structure.air"))
            .addStructureInfo(lang("structure.footer"))
            .addSubChannel(GTStructureChannels.STRUCTURE_LENGTH)
            .addSubChannel(GTStructureChannels.BOROGLASS)
            .toolTipFinisher(com.gregguns.core.text.AddedBy.NAME);
        return tt;
    }

    private static String lang(String key) {
        return StatCollector.translateToLocal("gregguns.machine.armory_gantry." + key);
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

    private static int baysFor(ItemStack trigger) {
        if (GTStructureChannels.STRUCTURE_LENGTH.hasValue(trigger)) {
            return GTStructureChannels.STRUCTURE_LENGTH
                .getValueClamped(trigger, GantryShape.MIN_BAYS, GantryShape.MAX_BAYS);
        }
        return GantryShape.clampBays(trigger.stackSize);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        int col = GantryShape.CONTROLLER_COLUMN;
        int row = GantryShape.CONTROLLER_ROW;
        int count = baysFor(trigger);
        buildPiece(PIECE_HEAD, trigger, hintsOnly, col, row, 0);
        for (int i = 0; i < count; i++) {
            buildPiece(PIECE_BAY, trigger, hintsOnly, col, row, -GantryShape.bayDepth(i));
        }
        buildPiece(PIECE_TAIL, trigger, hintsOnly, col, row, -GantryShape.tailDepth(count));
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        int col = GantryShape.CONTROLLER_COLUMN;
        int row = GantryShape.CONTROLLER_ROW;
        int count = baysFor(trigger);
        int built = survivalBuildPiece(PIECE_HEAD, trigger, col, row, 0, elementBudget, env, false, true);
        if (built >= 0) {
            return built;
        }
        for (int i = 0; i < count; i++) {
            built = survivalBuildPiece(
                PIECE_BAY,
                trigger,
                col,
                row,
                -GantryShape.bayDepth(i),
                elementBudget,
                env,
                false,
                true);
            if (built >= 0) {
                return built;
            }
        }
        return survivalBuildPiece(
            PIECE_TAIL,
            trigger,
            col,
            row,
            -GantryShape.tailDepth(count),
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        // The Gantry lies flat: no pointing up or down, no roll, no upside-down.
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
