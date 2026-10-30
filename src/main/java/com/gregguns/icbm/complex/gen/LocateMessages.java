package com.gregguns.icbm.complex.gen;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

/** The chat lines of {@code /gregguns locate}, all lang keys under {@code gregguns.locate}. */
final class LocateMessages {

    private LocateMessages() {}

    /** The command a click on a built complex's line runs. */
    static String tpCommand(StructureLocator.Candidate c) {
        return "/gregguns locate tp " + c.plan.id()
            + " "
            + c.plan.sizeClass()
                .name()
                .toLowerCase(java.util.Locale.ROOT)
            + " "
            + c.cellX()
            + " "
            + c.cellZ();
    }

    /** @return the structure's display name as a lang component */
    static IChatComponent kind(StructureLocator.Candidate c) {
        return new ChatComponentTranslation("gregguns.locate.kind." + c.plan.id());
    }

    /** @return {@code comp}, clickable to teleport to the complex when {@code c} is built */
    static IChatComponent clickable(IChatComponent comp, StructureLocator.Candidate c) {
        comp.setChatStyle(
            new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand(c)))
                .setChatHoverEvent(
                    new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentTranslation("gregguns.locate.hover"))));
        return comp;
    }

    /** The status of one candidate, as the tail of a list row or a progress line. */
    static IChatComponent status(StructureLocator.Candidate c, int[] arrival) {
        switch (c.status) {
            case GENERATED:
                return new ChatComponentTranslation(
                    "gregguns.locate.status.generated",
                    arrival[0],
                    c.groundY + 1,
                    arrival[1]);
            case SKIPPED:
                return new ChatComponentTranslation(
                    "gregguns.locate.status.skipped",
                    new ChatComponentTranslation("gregguns.locate.reason." + c.reason));
            default:
                return new ChatComponentTranslation("gregguns.locate.status.unknown");
        }
    }

    /** One numbered row: cell, block position, distance, status; clickable when built. */
    static IChatComponent row(int n, StructureLocator.Candidate c) {
        int[] arrival = c.status == StructureLocator.Status.GENERATED ? StructureLocator.arrival(c.plan)
            : new int[] { c.centerX(), c.centerZ() };
        IChatComponent row = new ChatComponentTranslation(
            "gregguns.locate.row",
            n,
            kind(c),
            c.cellX(),
            c.cellZ(),
            c.centerX(),
            c.centerZ(),
            (int) Math.round(c.distance),
            status(c, arrival));
        return c.status == StructureLocator.Status.GENERATED ? clickable(row, c) : row;
    }
}
