package com.gregguns.gun.logic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What makes a gun a <em>tool gun</em> (the Chainsaw, the Power Hammers): the left button is vanilla melee and mining
 * instead of the trigger, and the fuel or air in the gun pays for it. Pure data and arithmetic with no Minecraft
 * types, both sides.
 *
 * <p>
 * "Fuel" means rounds loaded. A gun with no round loaded still swings and mines, but as a bare hand: melee damage
 * {@link #emptyMeleeDamage()}, dig speed 1 and no tool classes. Each hit, each mined block and each shot spends from
 * the same rounds; costs are in thousandths of a round so a cheaper action can be carried as a remainder (see
 * {@link ToolDrain}).
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 9; docs/gregguns-spec/02-guns/tool-guns.md
// Deviation: there is no internal "default" tool class (GUN-28 listed it in the tooltip) and the harvest check is made
// by Forge per block metadata, not for metadata 0 only (GUN-27).
public final class ToolStats {

    /** The swing sound is sent at most once per this many ticks (Techguns: 5). */
    public static final int DEFAULT_SWING_SOUND_GAP_TICKS = 5;

    private final Map<String, Integer> harvestLevels;
    private final double digSpeed;
    private final double meleeDamage;
    private final double emptyMeleeDamage;
    private final double meleePenetration;
    private final int hitCostMilli;
    private final int blockCostMilli;
    private final int swingSoundGapTicks;

    private ToolStats(Builder b) {
        this.harvestLevels = Collections.unmodifiableMap(new LinkedHashMap<>(b.harvestLevels));
        this.digSpeed = b.digSpeed;
        this.meleeDamage = b.meleeDamage;
        this.emptyMeleeDamage = b.emptyMeleeDamage;
        this.meleePenetration = b.meleePenetration;
        this.hitCostMilli = b.hitCostMilli;
        this.blockCostMilli = b.blockCostMilli;
        this.swingSoundGapTicks = b.swingSoundGapTicks;
        require(digSpeed >= 1.0, "digSpeed must be at least 1");
        require(meleeDamage >= 0 && emptyMeleeDamage >= 0, "melee damage");
        require(meleePenetration >= 0 && meleePenetration <= 1, "meleePenetration");
        require(hitCostMilli >= 0 && blockCostMilli >= 0, "costs");
        require(swingSoundGapTicks >= 1, "swingSoundGapTicks");
    }

    private static void require(boolean ok, String what) {
        if (!ok) {
            throw new IllegalArgumentException("Invalid ToolStats: " + what);
        }
    }

    /** @return a builder with 2 damage melee when empty, one round per hit and per block, and a 5 tick swing gap */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the tool classes ("axe", "pickaxe") the loaded gun mines as; never contains {@code "default"} */
    public Set<String> toolClasses() {
        return harvestLevels.keySet();
    }

    /**
     * @param toolClass a Forge tool class
     * @return the harvest level the gun has for it, or -1 when it is not one of its classes
     */
    public int harvestLevel(String toolClass) {
        Integer level = harvestLevels.get(toolClass);
        return level == null ? -1 : level;
    }

    /**
     * The dig speed against a block.
     *
     * @param hasFuel   whether any round is loaded
     * @param blockTool the block's harvest tool class, or null for a block with none (dirt, leaves)
     * @return {@code digSpeed} when loaded and the block's tool is one of the gun's classes or none, else 1
     */
    public double digSpeed(boolean hasFuel, String blockTool) {
        if (!hasFuel || harvestLevels.isEmpty()) {
            return 1.0;
        }
        return blockTool == null || harvestLevels.containsKey(blockTool) ? digSpeed : 1.0;
    }

    /** @return the dig speed of a loaded gun on its own tool classes */
    public double digSpeed() {
        return digSpeed;
    }

    /**
     * @param hasFuel whether any round is loaded
     * @return the melee damage (the attack attribute's modifier): full when loaded, {@link #emptyMeleeDamage()} else
     */
    public double meleeDamage(boolean hasFuel) {
        return hasFuel ? meleeDamage : emptyMeleeDamage;
    }

    /** @return the melee damage of a loaded gun */
    public double meleeDamage() {
        return meleeDamage;
    }

    /** @return the melee damage with no round loaded */
    public double emptyMeleeDamage() {
        return emptyMeleeDamage;
    }

    /** @return the armor penetration of a loaded melee hit, 0 to 1 */
    public double meleePenetration() {
        return meleePenetration;
    }

    /** @return thousandths of a round spent by one melee hit */
    public int hitCostMilli() {
        return hitCostMilli;
    }

    /** @return thousandths of a round spent by one mined block (or one block broken by the gun's own projectile) */
    public int blockCostMilli() {
        return blockCostMilli;
    }

    /** @return ticks between two swing sounds */
    public int swingSoundGapTicks() {
        return swingSoundGapTicks;
    }

    /** Fluent builder for {@link ToolStats}. */
    public static final class Builder {

        private final Map<String, Integer> harvestLevels = new LinkedHashMap<>();
        private double digSpeed = 1.0;
        private double meleeDamage = 1.0;
        private double emptyMeleeDamage = 2.0;
        private double meleePenetration;
        private int hitCostMilli = ToolDrain.MILLI;
        private int blockCostMilli = ToolDrain.MILLI;
        private int swingSoundGapTicks = DEFAULT_SWING_SOUND_GAP_TICKS;

        private Builder() {}

        /**
         * Adds a tool class the gun mines as.
         *
         * @param toolClass a Forge tool class such as {@code "axe"}
         * @param level     its harvest level, 0 (wood) and up
         * @return this
         */
        public Builder toolClass(String toolClass, int level) {
            if (toolClass == null || toolClass.isEmpty() || level < 0) {
                throw new IllegalArgumentException("Invalid tool class: " + toolClass + " " + level);
            }
            harvestLevels.put(toolClass, level);
            return this;
        }

        /**
         * @param speed dig speed on the gun's classes while loaded, at least 1
         * @return this
         */
        public Builder digSpeed(double speed) {
            this.digSpeed = speed;
            return this;
        }

        /**
         * @param loaded      melee damage while a round is loaded
         * @param empty       melee damage with the tank empty
         * @param penetration armor penetration of a loaded hit, 0 to 1
         * @return this
         */
        public Builder melee(double loaded, double empty, double penetration) {
            this.meleeDamage = loaded;
            this.emptyMeleeDamage = empty;
            this.meleePenetration = penetration;
            return this;
        }

        /**
         * @param hitMilli   thousandths of a round per melee hit
         * @param blockMilli thousandths of a round per block mined or broken
         * @return this
         */
        public Builder costs(int hitMilli, int blockMilli) {
            this.hitCostMilli = hitMilli;
            this.blockCostMilli = blockMilli;
            return this;
        }

        /**
         * @param ticks ticks between swing sounds
         * @return this
         */
        public Builder swingSoundGap(int ticks) {
            this.swingSoundGapTicks = ticks;
            return this;
        }

        /** @return the stats; throws IllegalArgumentException on inconsistent values */
        public ToolStats build() {
            return new ToolStats(this);
        }
    }
}
