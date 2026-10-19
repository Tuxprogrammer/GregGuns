package com.gregguns.armor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.armor.flight.FlightItems;
import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.EquipmentItems;
import com.gregguns.armor.slots.ItemBauble;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * {@code armor.showcase.accessories}: puts each worn accessory (masks, goggles, back devices, every camo) on a named
 * player and signals the client to take third-person screenshots, from the front and from behind. It needs one
 * connected client running with the dev autotest hooks, so it is not part of {@code test all}; the evidence is the
 * screenshots {@code <player>-acc-<item>-front.png} and {@code ...-back.png} in {@code run/client/screenshots/}.
 *
 * <p>
 * The player is {@code Alice} when online, otherwise the first connected player. The glider is shown stowed on the
 * ground and with its wings out while the player falls. Items worn in a Galacticraft slot instead of a Baubles slot
 * (the Oxygen Mask and Tanks when Galacticraft is loaded) cannot be equipped here and are skipped with a note.
 * Dev environment only, server side.
 */
// Spec: docs/testing/README.md (visual autotest); chunk H of the model review
final class AccessoryShowcaseScenario implements Scenario {

    static final String NAME = "armor.showcase.accessories";
    private static final String PLAYER = "Alice";
    private static final int STEP_TICKS = 26;
    private static final int FALL_HEIGHT = 40;

    /** One thing to put on the player. */
    private interface Look {

        String label();

        /** Puts it on; returns a note when it could not be. */
        String equip(EntityPlayerMP player);
    }

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new AccessoryShowcaseScenario());
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Puts every accessory (and camo) on a player and signals front and back third-person screenshots. "
            + "Needs 1 client with the autotest hooks.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP pick() {
        List<EntityPlayerMP> all = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        all.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        for (EntityPlayerMP p : all) {
            if (p.getCommandSenderName()
                .equalsIgnoreCase(PLAYER)) {
                return p;
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    private static void clearWorn(EntityPlayerMP player) {
        player.inventory.armorInventory[3] = null;
        for (BaubleSlot s : BaubleSlot.values()) {
            BaublesCompat.clear(player, s);
        }
    }

    private static Look helmet(String label, Item item, int camo) {
        return new Look() {

            @Override
            public String label() {
                return label;
            }

            @Override
            public String equip(EntityPlayerMP player) {
                if (item == null) {
                    return label + ": item is not registered";
                }
                ItemStack stack = new ItemStack(item);
                if (camo >= 0) {
                    EquipmentItems.tacticalMask()
                        .setCamo(stack, camo);
                }
                player.inventory.armorInventory[3] = stack;
                return null;
            }
        };
    }

    private static Look bauble(String label, Item item, BaubleSlot slot, int camo) {
        return new Look() {

            @Override
            public String label() {
                return label;
            }

            @Override
            public String equip(EntityPlayerMP player) {
                if (item == null) {
                    return label + ": item is not registered";
                }
                if (!(item instanceof ItemBauble)) {
                    return label + ": a Galacticraft slot item, not worn through Baubles; skipped";
                }
                ItemStack stack = new ItemStack(item);
                if (camo >= 0) {
                    FlightItems.antiGravityDevice()
                        .setCamo(stack, camo);
                }
                return BaublesCompat.equip(player, slot, stack) ? null
                    : label + ": no free " + slot.typeName() + " slot";
            }
        };
    }

    private static List<Look> looks() {
        List<Look> list = new ArrayList<>();
        list.add(helmet("gas-mask", EquipmentItems.gasMask(), -1));
        for (int i = 0; i < 4; i++) {
            list.add(helmet("tactical-mask-camo" + i, EquipmentItems.tacticalMask(), i));
        }
        list.add(bauble("goggles", EquipmentItems.nightVisionGoggles(), BaubleSlot.HEAD, -1));
        list.add(bauble("oxygen-mask", EquipmentItems.oxygenMask(), BaubleSlot.HEAD, -1));
        list.add(bauble("oxygen-tanks", EquipmentItems.oxygenTanks(), BaubleSlot.WINGS, -1));
        list.add(bauble("jump-pack", FlightItems.jumpPack(), BaubleSlot.WINGS, -1));
        for (int i = 0; i < 5; i++) {
            list.add(bauble("anti-gravity-camo" + i, FlightItems.antiGravityDevice(), BaubleSlot.WINGS, i));
        }
        list.add(bauble("glider-stowed", FlightItems.glider(), BaubleSlot.WINGS, -1));
        return list;
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        EntityPlayerMP a = pick();
        if (a == null) {
            return ScenarioResult.fail(NAME, "needs a connected player (" + PLAYER + " by preference)");
        }
        final double ox = ctx.originX() + 0.5;
        final double oy = ctx.originY();
        final double oz = ctx.originZ() + 0.5;
        ctx.clearArea(6, 3);
        a.inventory.clearInventory(null, -1);
        a.setGameType(WorldSettings.GameType.CREATIVE);
        a.setHealth(a.getMaxHealth());
        a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, 0.0f, 0.0f);
        List<Look> looks = looks();
        List<String> notes = new ArrayList<>();
        final int first = 10;
        final int gliderAt = first + looks.size() * STEP_TICKS;
        final int end = gliderAt + 50;
        return ScenarioResult.pending(NAME, end + 200, t -> {
            if (t >= first && t < gliderAt && (t - first) % STEP_TICKS == 0) {
                Look look = looks.get((t - first) / STEP_TICKS);
                clearWorn(a);
                String note = look.equip(a);
                if (note != null) {
                    notes.add(note);
                }
            }
            int step = t < first ? -1 : (t - first) % STEP_TICKS;
            if (t >= first && t < gliderAt) {
                Look look = looks.get((t - first) / STEP_TICKS);
                String p = "acc-" + look.label();
                switch (step) {
                    case 2 -> signal(a, "view", "2");
                    case 10 -> signal(a, "shot", p + "-front");
                    case 13 -> signal(a, "view", "1");
                    case 21 -> signal(a, "shot", p + "-back");
                    default -> {}
                }
            }
            // The glider with its wings out: drop the player from a height and photograph the fall from behind.
            if (t == gliderAt) {
                a.playerNetServerHandler.setPlayerLocation(ox, oy + FALL_HEIGHT, oz, 0.0f, 0.0f);
                signal(a, "view", "1");
            } else if (t == gliderAt + 12) {
                signal(a, "shot", "acc-glider-deployed-back");
            } else if (t == gliderAt + 14) {
                signal(a, "view", "2");
            } else if (t == gliderAt + 20) {
                signal(a, "shot", "acc-glider-deployed-front");
            } else if (t == gliderAt + 26) {
                a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, 0.0f, 0.0f);
            }
            if (t < end) {
                return null;
            }
            clearWorn(a);
            signal(a, "view", "0");
            ScenarioResult.Builder b = ScenarioResult.builder(NAME)
                .note("signals sent to " + a.getCommandSenderName() + "; read the acc-* screenshots");
            for (String n : notes) {
                b.note(n);
            }
            return b.build();
        });
    }
}
