package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of the inert warhead's name and tooltip (D41, D282): the payload, the tier, the empty charge count and
 * the inert line, then the next step behind Shift. It takes already localized templates, so it has no Minecraft types
 * and is unit tested.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The inert warhead)
public final class InertWarheadTooltipFormat {

    private InertWarheadTooltipFormat() {}

    /** The localized templates; {@code %s} slots are filled by this class. */
    public static final class Labels {

        /** Payload line, one slot: the payload name or {@link #payloadNone}. */
        public String payload;
        /** The payload line's value for a bare T1 casing (charges alone make condensed). */
        public String payloadNoneT1;
        /** The payload line's value for a bare T2 to T5 casing. */
        public String payloadNone;
        /** Tier line, one slot: the colored tier text. */
        public String tier;
        /** Charges line, two slots: the charges (always 0) and the cap. */
        public String charges;
        /** The inert line, no slots. */
        public String inert;
        /** The hint shown without Shift. */
        public String shiftHint;
        /** Detail for a bare T1 casing, no slots. */
        public String nextBareT1;
        /** Detail for a bare T2 to T5 casing, no slots. */
        public String nextBare;
        /** Detail for a casing with a payload, no slots. */
        public String nextFitted;
        /** Detail: how charges are loaded, one slot: the most per crafting-grid load. */
        public String howToCharge;
    }

    /** The values of one stack. */
    public static final class Facts {

        /** The localized payload name, or null for a bare casing. */
        public String payloadName;
        /** True for the T1 casing. */
        public boolean t1;
        /** The tier text, already colored. */
        public String tierText;
        /** The casing's charge cap for this payload. */
        public int cap;
    }

    /**
     * The display name: {@code Inert T1 Warhead}, or {@code Inert Shrapnel T1 Warhead} with a payload.
     *
     * @param bareTemplate   one slot: the casing number
     * @param fittedTemplate two slots: the payload name, then the casing number
     * @param payloadName    the localized payload name, or null when bare
     * @param casingNumber   1 to 5
     * @return the name
     */
    public static String displayName(String bareTemplate, String fittedTemplate, String payloadName, int casingNumber) {
        String n = Integer.toString(casingNumber);
        return payloadName == null ? String.format(bareTemplate, n) : String.format(fittedTemplate, payloadName, n);
    }

    /**
     * @param l     the templates
     * @param f     the stack's values
     * @param shift whether Shift is held
     * @return the tooltip lines
     */
    public static List<String> lines(Labels l, Facts f, boolean shift) {
        List<String> out = new ArrayList<>();
        String payload = f.payloadName != null ? f.payloadName : f.t1 ? l.payloadNoneT1 : l.payloadNone;
        out.add(String.format(l.payload, payload));
        out.add(String.format(l.tier, f.tierText));
        out.add(String.format(l.charges, 0, f.cap));
        out.add(l.inert);
        if (!shift) {
            out.add(l.shiftHint);
            return out;
        }
        out.add(f.payloadName != null ? l.nextFitted : f.t1 ? l.nextBareT1 : l.nextBare);
        out.add(String.format(l.howToCharge, WarheadCharging.MAX_PER_CRAFT));
        return out;
    }
}
