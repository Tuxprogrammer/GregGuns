package com.gregguns.api;

/**
 * Marker for GregGuns items and blocks that are deliberately absent from the creative tab, such as technical blocks,
 * placeholder items and anything the NEI-visibility sheet hides.
 *
 * <p>
 * The dev self-check requires every GregGuns item either to have a creative tab or to implement this interface, so a
 * forgotten {@code setCreativeTab} call fails the dev server instead of shipping an unobtainable item. The creative tab
 * code should treat an implementor as "never listed". For a block, implement it on the {@code Block}.
 *
 * <p>
 * Spec: docs/gregguns-spec/06-items/nei-visibility.md. Both sides.
 */
public interface HiddenFromCreativeTab {
}
