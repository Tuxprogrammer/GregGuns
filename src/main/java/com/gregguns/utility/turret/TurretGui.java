package com.gregguns.utility.turret;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gregguns.api.Tier;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.utility.ItemTurretArmor;

import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The Turret panel, top to bottom: the title with a one-word state, a dark status screen (EU, target, rounds loaded,
 * armor plate, owner), the slot groups (nine ammo slots, the gun and armor plate slots, nine empty-magazine slots), the
 * redstone button, and the security and Target Animals buttons. Each group and button carries its name and a tooltip.
 * The security button is the owner's: locked, with the reason in its tooltip, for anyone else. Every button is an
 * {@link InteractionSyncHandler} whose action runs on the server and re-checks the pressing player
 * ({@link MTETurret#cycleSecurity} and friends), so a modified client cannot change a setting it may not. The settings
 * and status reach the client through read-only sync values. Built on both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (Container, GUI), docs/gregguns-spec/08-machines-production/
// modularui2-guis.md, docs/gregguns-spec/09-machines-utility/turret.md (GUI)
// Deviation: GT's frame replaces turretBase_GUI.png; the Techguns power bar, repair bar and animal icons become status
// lines and labeled buttons.
final class TurretGui extends MTETieredMachineBlockBaseGui<MTETurret> {

    private static final String SECURITY = "security";
    private static final String REDSTONE = "redstone";
    private static final String ANIMALS = "animals";

    private static final int WIDTH = 192;
    private static final int HEIGHT = 242;
    /** Content width: the panel minus the frame (8) and the content padding (6). */
    private static final int CONTENT = WIDTH - 14;
    private static final int BTN_H = 14;
    /** The Security button is the wider of the pair: "Security: Friends" must not touch its edges. */
    private static final int SECURITY_W = 98;

    private static final int TEXT_DARK = 0xFF404040;
    private static final int TEXT_LOCKED = 0xFF4A4A4A;
    private static final int TEXT_LIVE = 0xFF202020;
    private static final int SCREEN_BG = 0xFF1A1A1A;
    private static final int SCREEN_EDGE = 0xFF000000;
    private static final int AMBER = 0xFFFFB000;
    private static final int ALARM = 0xFFFF6060;
    private static final int GOOD = 0xFF55FF55;

    private final TurretGuiModel.State state = new TurretGuiModel.State();
    /** The player this screen is built for; sync getters read it because the sync manager has no player yet. */
    private EntityPlayer viewer;

    TurretGui(MTETurret machine) {
        super(machine);
    }

    @Override
    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        viewer = guiData.getPlayer();
        return super.build(guiData, syncManager, uiSettings);
    }

    @Override
    protected int getBasePanelWidth() {
        return WIDTH;
    }

    @Override
    protected int getBasePanelHeight() {
        return HEIGHT;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        // Read-only on the server's side of the channel: applyClientSync ignores a server-side call.
        syncManager.syncValue("settings_security", 0, new IntSyncValue(machine::securityId, machine::syncSecurity));
        syncManager.syncValue("settings_redstone", 0, new IntSyncValue(machine::redstoneId, machine::syncRedstone));
        syncManager.syncValue("settings_animals", 0, new IntSyncValue(machine::animalsFlag, machine::syncAnimals));
        syncManager.syncValue("settings_repair", 0, new IntSyncValue(machine::repairTicksForSync, machine::syncRepair));
        // Status lines: the getters run on the server, the setters fill the state the screen reads on the client.
        syncManager.syncValue("gui_flags", 0, new IntSyncValue(machine::guiFlags, v -> state.flags = v));
        syncManager.syncValue("gui_eu", 0, new LongSyncValue(machine::guiStoredEu, v -> state.eu = v));
        syncManager.syncValue("gui_target", 0, new StringSyncValue(machine::guiTarget, v -> state.target = v));
        syncManager.syncValue("gui_owner", 0, new StringSyncValue(machine::guiOwnerName, v -> state.owner = v));
        syncManager.syncValue(
            "gui_can_use",
            0,
            new IntSyncValue(() -> machine.mayUse(viewer) ? 1 : 0, v -> state.canUse = v != 0));
        syncManager.syncValue(
            "gui_can_security",
            0,
            new IntSyncValue(() -> machine.mayChangeSecurity(viewer) ? 1 : 0, v -> state.canSecurity = v != 0));
        syncManager.syncValue(
            SECURITY,
            0,
            new InteractionSyncHandler().setOnMousePressed(mouse -> machine.cycleSecurity(syncManager.getPlayer())));
        syncManager.syncValue(
            REDSTONE,
            0,
            new InteractionSyncHandler().setOnMousePressed(mouse -> machine.cycleRedstone(syncManager.getPlayer())));
        syncManager.syncValue(
            ANIMALS,
            0,
            new InteractionSyncHandler().setOnMousePressed(mouse -> machine.toggleAnimals(syncManager.getPlayer())));
    }

    @Override
    protected boolean supportsPowerSwitch() {
        return false;
    }

    @Override
    protected boolean supportsMuffler() {
        return false;
    }

    @Override
    protected boolean doesAddGregTechLogo() {
        return false;
    }

    // --- layout -----------------------------------------------------------------------------------------------

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();

        // Status screen. The frame's tab already names the machine, so the screen opens with the state word.
        ParentWidget<?> screen = new ParentWidget<>().pos(0, 0)
            .size(CONTENT, 49)
            .background(screenDrawable());
        screen
            .child(text(this::stateText, 4, 3, CONTENT - 8, 9, Alignment.CenterLeft, () -> toneColor(status().tone())));
        screen.child(
            text(
                () -> trf(
                    "gregguns.turret.gui.power",
                    TurretGuiModel.eu(state.eu),
                    TurretGuiModel.eu(machine.maxEUStore())),
                4,
                12,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> state.eu <= 0 ? ALARM : AMBER));
        screen.child(
            text(
                this::targetText,
                4,
                21,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> TurretGuiModel.targetDisplay(state.target, StatCollector::translateToLocal) == null ? AMBER
                    : ALARM));
        screen.child(text(this::roundsText, 4, 30, 84, 9, Alignment.CenterLeft, () -> AMBER));
        screen.child(text(this::plateText, 90, 30, CONTENT - 94, 9, Alignment.CenterLeft, () -> AMBER));
        screen.child(
            text(
                () -> state.owner.isEmpty() ? tr("gregguns.turret.gui.owner_none")
                    : trf("gregguns.turret.gui.owner", state.owner),
                4,
                39,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> AMBER));
        content.child(screen);

        // Slot groups: ammo left, gun and plate in the middle, empties right.
        int labelY = 52;
        int slotY = 62;
        int rightX = CONTENT - 54;
        int midX = 54;
        int midW = CONTENT - 108;
        content.child(
            label(
                "gregguns.turret.gui.slots.ammo",
                "gregguns.turret.gui.slots.ammo.tip",
                0,
                labelY,
                54,
                Alignment.CenterLeft));
        content.child(
            label(
                "gregguns.turret.gui.slots.empties",
                "gregguns.turret.gui.slots.empties.tip",
                rightX,
                labelY,
                54,
                Alignment.CenterRight));
        content.child(
            label(
                "gregguns.turret.gui.slots.gun",
                "gregguns.turret.gui.slots.gun.tip",
                midX,
                labelY,
                midW,
                Alignment.Center));
        int slotX = midX + (midW - 18) / 2;
        content.child(grid(MTETurret.AMMO_FIRST, 0, slotY, true));
        content.child(grid(MTETurret.OUT_FIRST, rightX, slotY, false));
        content.child(
            new ItemSlot().slot(single(MTETurret.GUN_SLOT, MTETurret::isGun))
                .pos(slotX, slotY));
        content.child(
            label(
                "gregguns.turret.gui.slots.plate",
                "gregguns.turret.gui.slots.plate.tip",
                midX,
                slotY + 20,
                midW,
                Alignment.Center));
        content.child(
            new ItemSlot().slot(single(MTETurret.ARMOR_SLOT, MTETurret::isPlate))
                .pos(slotX, slotY + 30));

        // Controls.
        int y = 119;
        content.child(
            control(
                REDSTONE,
                0,
                y,
                CONTENT,
                () -> trf("gregguns.turret.gui.redstone", tr(TurretGuiModel.redstoneKey(machine.redstoneId()))),
                () -> state.canUse,
                false,
                () -> TurretGuiModel.redstoneKey(machine.redstoneId()) + ".tip"));
        y += BTN_H + 2;
        content.child(
            control(
                SECURITY,
                0,
                y,
                SECURITY_W,
                () -> trf("gregguns.turret.gui.security", tr(TurretGuiModel.securityKey(machine.securityId()))),
                () -> state.canSecurity,
                true,
                () -> TurretGuiModel.securityKey(machine.securityId()) + ".tip"));
        content.child(
            control(
                ANIMALS,
                SECURITY_W + 4,
                y,
                CONTENT - SECURITY_W - 4,
                () -> trf(
                    "gregguns.turret.gui.animals",
                    tr(machine.animalsFlag() == 1 ? "gregguns.turret.gui.yes" : "gregguns.turret.gui.no")),
                () -> state.canUse,
                false,
                () -> machine.animalsFlag() == 1 ? "gregguns.turret.gui.animals.on.tip"
                    : "gregguns.turret.gui.animals.off.tip"));
        return content;
    }

    // --- status text ------------------------------------------------------------------------------------------

    private TurretGuiModel.Status status() {
        return TurretGuiModel.status(state.flags, machine.mountedGun() != null);
    }

    private String stateText() {
        TurretGuiModel.Status status = status();
        if (status == TurretGuiModel.Status.REPAIRING) {
            return trf("gregguns.turret.gui.repair", machine.repairSecondsLeft());
        }
        return tr(status.langKey());
    }

    private String targetText() {
        String target = TurretGuiModel.targetDisplay(state.target, StatCollector::translateToLocal);
        return trf("gregguns.turret.gui.target", target == null ? tr("gregguns.turret.gui.target_idle") : target);
    }

    private String roundsText() {
        ItemStack gun = machine.mountedGun();
        if (gun == null) {
            return trf("gregguns.turret.gui.rounds_none");
        }
        GunStats stats = ((ItemGun) gun.getItem()).stats();
        int loaded = GunNbt.read(gun.getTagCompound(), stats)
            .loaded();
        return trf("gregguns.turret.gui.rounds", loaded, stats.magazineSize());
    }

    private String plateText() {
        ItemStack plate = machine.getStackInSlot(MTETurret.ARMOR_SLOT);
        if (!MTETurret.isPlate(plate)) {
            return tr("gregguns.turret.gui.plate_none");
        }
        ItemTurretArmor item = (ItemTurretArmor) plate.getItem();
        String tier = item.getTier() == Tier.STEAM ? tr("gregguns.turret.gui.tier_steam")
            : item.getTier()
                .gtName();
        return trf("gregguns.turret.gui.plate", tier, item.armorValue());
    }

    private static int toneColor(TurretGuiModel.Tone tone) {
        switch (tone) {
            case GOOD:
                return GOOD;
            case WARN:
                return AMBER;
            default:
                return ALARM;
        }
    }

    // --- widgets ----------------------------------------------------------------------------------------------

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private static String trf(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private static TextWidget<?> text(Supplier<String> value, int x, int y, int w, int h, Alignment align,
        IntSupplier color) {
        return new TextWidget<>(IKey.dynamic(value)).pos(x, y)
            .size(w, h)
            .alignment(align)
            .color(color)
            .shadow(false);
    }

    /** A slot-group name with a tooltip that says what goes in. */
    private static IWidget label(String key, String tipKey, int x, int y, int w, Alignment align) {
        TextWidget<?> label = text(() -> tr(key), x, y, w, 10, align, () -> TEXT_DARK);
        label.tooltipDynamic(tip -> tip.addLine(IKey.lang(tipKey)));
        return label;
    }

    private static IDrawable screenDrawable() {
        return (ctx, x, y, w, h, theme) -> {
            GuiDraw.drawRect(x, y, w, h, SCREEN_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, w - 2, h - 2, SCREEN_BG);
        };
    }

    /**
     * A labeled setting button. Live buttons read dark; locked ones read gray and their tooltip names the reason
     * first. The tooltip then describes the current value.
     */
    private IWidget control(String name, int x, int y, int w, Supplier<String> text, BooleanSupplier live,
        boolean ownerOnly, Supplier<String> valueTipKey) {
        return new ButtonWidget<>().syncHandler(name, 0)
            .size(w, BTN_H)
            .pos(x, y)
            .overlay(
                IKey.dynamic(text)
                    .color(() -> live.getAsBoolean() ? TEXT_LIVE : TEXT_LOCKED))
            .tooltipDynamic(tip -> {
                String locked = TurretGuiModel.lockedKey(live.getAsBoolean(), ownerOnly);
                if (locked != null) {
                    tip.addLine(IKey.lang(locked));
                }
                tip.addLine(IKey.lang(valueTipKey.get()));
            });
    }

    private ModularSlot single(int index, Predicate<ItemStack> filter) {
        return new ModularSlot(machine.inventoryHandler, index) {

            @Override
            public int getItemStackLimit(ItemStack stack) {
                return 1;
            }
        }.filter(stack -> filter.test(stack) && (index != MTETurret.GUN_SLOT || machine.mayMount(stack)));
    }

    private IWidget grid(int first, int x, int y, boolean input) {
        ParentWidget<?> group = new ParentWidget<>().pos(x, y)
            .size(54, 54);
        for (int i = 0; i < 9; i++) {
            ModularSlot slot = new ModularSlot(machine.inventoryHandler, first + i);
            if (!input) {
                slot.filter(stack -> false);
            }
            group.child(
                new ItemSlot().slot(slot)
                    .pos((i % 3) * 18, (i / 3) * 18));
        }
        return group;
    }
}
