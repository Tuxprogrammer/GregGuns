package com.gregguns.recipe;

/**
 * The programmed circuits of the GregGuns casing recipes made from six Stainless Steel plates and a Stainless Steel
 * frame. GT's own Stainless Steel Frame Casing ({@code blockcasings4:1}) is that exact mix with circuit 1, and the
 * Assembler keys a recipe on its inputs and circuit, so a GregGuns casing on circuit 1 would clash with it (and with
 * the other GregGuns casing). A full recipe dump of GTNH 2.9.0-RC-2 shows no recipe on circuits 6 and 7 with these
 * inputs.
 */
public final class CasingCircuits {

    /** The circuit of GT's Stainless Steel Frame Casing, the mix these recipes must not match. */
    public static final int GT_STAINLESS_CASING = 1;
    /** The Works Casing recipe's circuit. */
    public static final int WORKS_CASING = 6;
    /** The Gantry Casing recipe's circuit. */
    public static final int GANTRY_CASING = 7;

    private CasingCircuits() {}
}
