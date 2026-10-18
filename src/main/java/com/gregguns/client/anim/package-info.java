/**
 * Pure animation maths for gun rendering: curves, timing windows, per-shooter state, zoom easing and sprite-sheet
 * frames. No Minecraft types appear in any signature, so everything here is unit-tested. Time is passed in as ticks
 * plus the partial tick (one double, {@code tick + partial}).
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/rendering-framework.md sections 3, 5, 7 and 9.
 */
package com.gregguns.client.anim;
