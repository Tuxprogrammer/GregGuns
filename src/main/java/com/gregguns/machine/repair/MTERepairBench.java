package com.gregguns.machine.repair;

import java.util.Collections;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.logic.RepairSpec;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

/**
 * The Repair Bench: a powered GT single block that restores the GregGuns armor a player wears, or the item they hold,
 * from parts stocked in its nine slots. One class serves LV to IV; the tier is a constructor argument. A bench repairs
 * items of its own tier and below.
 *
 * <p>
 * Using it: stock the slots with the parts the item's repair list asks for (see {@link RepairMaterials}), open the
 * GUI and press the button for the worn piece or the held item. A repair is all or nothing and charges the job's whole
 * energy from the bench's buffer ({@link RepairMath#benchEu}); with too little energy nothing happens and the player is
 * told. Insertion is allowed from every side, extraction never (hoppers and pipes can feed it, only the GUI empties
 * it).
 *
 * <p>
 * Construct only inside GT's MTE window ({@link RepairMachines#register()}). Both sides; the GUI is built on both and
 * its actions run on the server.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md, docs/gregguns-spec/08-machines-production/
// machine-roster.md (Repair Bench, GGS-08-50), docs/techguns-spec/09-machines-utility/repair-bench.md
// Deviation: the Techguns bench is unpowered and repairs only worn armor. GregGuns' draws EU (the roster's proposal,
// closing the open question in repair-bench.md): the job charges 2 s + 8 s x damage fraction at half the recipe
// voltage up front, from a buffer sized for two worst-case jobs. The held item is also a target, for guns.
public final class MTERepairBench extends MTETieredMachineBlock {

    /** Material slots, one row. */
    public static final int SLOTS = 9;
    /** Button targets 0 to 3 are the worn pieces (helmet first); this one is the held item. */
    public static final int TARGET_HELD = 4;
    /** Largest squared distance between the player and the bench for a repair (GGS-09-23: 8 blocks). */
    private static final double REACH_SQ = 64.0;
    /** Ticks the front stays "active" after a repair. */
    private static final int ACTIVE_TICKS = 30;

    private static final String OVERLAY_DIR = "machines/repair_bench/";

    private int activeTicks;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name            GT's internal name, lower case with dots
     * @param nameRegional    the English display name GT registers for the block
     * @param tier            GT voltage index (1 is LV)
     * @param descriptionKeys lang keys of the tooltip lines, translated when the tooltip is shown
     */
    public MTERepairBench(int id, String name, String nameRegional, int tier, String[] descriptionKeys) {
        super(id, name, nameRegional, tier, SLOTS, descriptionKeys);
        Overlays.load();
    }

    private MTERepairBench(String name, int tier, String[] descriptionKeys) {
        super(name, tier, SLOTS, descriptionKeys, null);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTERepairBench(mName, mTier, mDescriptionArray);
    }

    /** @return this bench's tier as a GregGuns tier */
    public Tier benchTier() {
        for (Tier t : Tier.values()) {
            if (t.voltageIndex() == mTier) {
                return t;
            }
        }
        throw new IllegalStateException("no GregGuns tier for GT tier " + mTier);
    }

    @Override
    public String[] getDescription() {
        String[] lines = new String[mDescriptionArray.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StatCollector.translateToLocal(mDescriptionArray[i]);
        }
        return com.gregguns.core.text.AddedBy.append(lines);
    }

    // --- energy -------------------------------------------------------------------------------------------------

    @Override
    public boolean isEnetInput() {
        return true;
    }

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        return side != getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public long maxEUInput() {
        return GTValues.V[mTier];
    }

    @Override
    public long maxAmperesIn() {
        return 1;
    }

    @Override
    public long maxEUStore() {
        // Two worst-case jobs, never less than the usual 64 ticks of input, so one repair always fits.
        return Math.max(GTValues.V[mTier] * 64L, 2 * RepairMath.benchEu(benchTier(), 1.0));
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return facing != ForgeDirection.UP && facing != ForgeDirection.DOWN && facing != ForgeDirection.UNKNOWN;
    }

    // --- inventory ----------------------------------------------------------------------------------------------

    @Override
    public boolean allowPutStack(IGregTechTileEntity baseMetaTileEntity, int index, ForgeDirection side,
        ItemStack stack) {
        return index >= 0 && index < SLOTS;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity baseMetaTileEntity, int index, ForgeDirection side,
        ItemStack stack) {
        return false;
    }

    // --- repair ---------------------------------------------------------------------------------------------------

    /** Slots view over this bench's inventory. */
    private RepairMaterials.Slots slots() {
        return new RepairMaterials.Slots() {

            @Override
            public int size() {
                return SLOTS;
            }

            @Override
            public ItemStack get(int index) {
                return mInventory[index];
            }

            @Override
            public void set(int index, ItemStack stack) {
                getBaseMetaTileEntity().setInventorySlotContents(index, stack);
            }
        };
    }

    /** What a repair press did, for the tests and the chat message. */
    public enum Outcome {

        /** The item is repaired. */
        REPAIRED("gregguns.repair.ok"),
        /** Too far away, or the bench is not usable. */
        OUT_OF_REACH(null),
        /** Nothing in that slot. */
        NO_ITEM("gregguns.repair.no_item"),
        /** The item has no repair list. */
        CANNOT_REPAIR("gregguns.repair.cannot"),
        /** The item has no damage. */
        UNDAMAGED("gregguns.repair.undamaged"),
        /** The item's tier is above the bench's. */
        TIER_TOO_LOW("gregguns.repair.tier"),
        /** The slots lack a part. */
        MISSING_MATERIALS("gregguns.repair.materials"),
        /** The buffer holds less than the job costs. */
        NO_POWER("gregguns.repair.power");

        private final String langKey;

        Outcome(String langKey) {
            this.langKey = langKey;
        }

        /** @return the lang key of the chat message, or null for none */
        public String langKey() {
            return langKey;
        }
    }

    /**
     * Repairs one item for a player. Runs on the server: checks the reach (GGS-09-23), plans the job with
     * {@link RepairPlanner}, charges the energy, takes the parts and sets the damage to 0 (GGS-09-22). Every other tag
     * on the stack is left as it was.
     *
     * @param player the player who pressed the button
     * @param target 0 to 3 for the helmet to the boots, {@link #TARGET_HELD} for the held item
     * @return what happened; the player also gets a chat message for every outcome but {@code OUT_OF_REACH}
     */
    public Outcome repair(EntityPlayer player, int target) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isClientSide()
            || target < 0
            || target > TARGET_HELD
            || player.worldObj != base.getWorld()
            || player.getDistanceSq(base.getXCoord() + 0.5, base.getYCoord() + 0.5, base.getZCoord() + 0.5) > REACH_SQ
            || !base.isUseableByPlayer(player)) {
            return Outcome.OUT_OF_REACH;
        }
        Outcome outcome = doRepair(base, player, target);
        if (outcome.langKey() != null) {
            player.addChatMessage(new ChatComponentTranslation(outcome.langKey()));
        }
        return outcome;
    }

    private Outcome doRepair(IGregTechTileEntity base, EntityPlayer player, int target) {
        // Worn armor is indexed boots first in the vanilla array; the button order is helmet first.
        ItemStack stack = target == TARGET_HELD ? player.getHeldItem() : player.inventory.armorInventory[3 - target];
        if (stack == null) {
            return Outcome.NO_ITEM;
        }
        RepairSpec spec = RepairItems.specOf(stack);
        if (spec == null) {
            return Outcome.CANNOT_REPAIR;
        }
        RepairPlanner.Candidate candidate = RepairItems.candidate(stack);
        RepairMaterials.Pool pool = RepairMaterials.count(slots(), Collections.singletonList(spec));
        RepairPlanner.Plan plan = RepairPlanner
            .plan(Collections.singletonList(candidate), pool.counts(), benchTier(), 1);
        switch (plan.statuses()
            .get(0)) {
            case UNDAMAGED:
                return Outcome.UNDAMAGED;
            case NOT_REPAIRABLE:
                return Outcome.CANNOT_REPAIR;
            case TIER_TOO_LOW:
                return Outcome.TIER_TOO_LOW;
            case MISSING_MATERIALS:
            case NO_CAPACITY:
                return Outcome.MISSING_MATERIALS;
            default:
                break;
        }
        long eu = RepairMath.benchEu(benchTier(), candidate.fraction());
        if (base.getStoredEU() < eu) {
            return Outcome.NO_POWER;
        }
        base.decreaseStoredEnergyUnits(eu, true);
        RepairMaterials.take(slots(), pool, plan.consumed());
        stack.setItemDamage(0);
        base.setActive(true);
        activeTicks = ACTIVE_TICKS;
        return Outcome.REPAIRED;
    }

    @Override
    public void onPostTick(IGregTechTileEntity baseMetaTileEntity, long tick) {
        super.onPostTick(baseMetaTileEntity, tick);
        if (baseMetaTileEntity.isServerSide() && activeTicks > 0 && --activeTicks == 0) {
            baseMetaTileEntity.setActive(false);
        }
    }

    // --- GUI ------------------------------------------------------------------------------------------------------

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new RepairBenchGui(this).build(guiData, syncManager, uiSettings);
    }

    // --- textures -------------------------------------------------------------------------------------------------

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        // getTexture builds its layers per call from the overlay holder; there is no per-tier set to prepare.
        return null;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][colorIndex + 1];
        IIconContainer overlay;
        if (side == facing) {
            overlay = active ? Overlays.FRONT_ACTIVE : Overlays.FRONT;
        } else if (side == ForgeDirection.UP) {
            overlay = active ? Overlays.TOP_ACTIVE : Overlays.TOP;
        } else if (side == ForgeDirection.DOWN) {
            overlay = Overlays.BOTTOM;
        } else {
            overlay = active ? Overlays.SIDE_ACTIVE : Overlays.SIDE;
        }
        return new ITexture[] { casing, TextureFactory.of(overlay) };
    }

    /** The overlay icons, requested when the first bench is constructed (before GT stitches textures). */
    private static final class Overlays {

        static final IIconContainer FRONT = icon("OVERLAY_FRONT");
        static final IIconContainer FRONT_ACTIVE = icon("OVERLAY_FRONT_ACTIVE");
        static final IIconContainer SIDE = icon("OVERLAY_SIDE");
        static final IIconContainer SIDE_ACTIVE = icon("OVERLAY_SIDE_ACTIVE");
        static final IIconContainer TOP = icon("OVERLAY_TOP");
        static final IIconContainer TOP_ACTIVE = icon("OVERLAY_TOP_ACTIVE");
        static final IIconContainer BOTTOM = icon("OVERLAY_BOTTOM");

        private Overlays() {}

        static void load() {
            // Loading the class runs the initializers above.
        }

        private static IIconContainer icon(String file) {
            return Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        // The inventory is saved by GT; the bench keeps no other state (the active countdown is cosmetic).
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        // Nothing beyond the inventory.
    }
}
