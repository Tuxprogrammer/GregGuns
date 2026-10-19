package com.gregguns.mob.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * One node of a mob model's part tree: a pivot, a rest rotation and any number of boxes. A child's pivot is relative
 * to its parent's pivot, and the child follows the parent's rotation (the {@code ModelRenderer} child rule). Pure
 * data.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md
public final class MobPartDef {

    private final String name;
    private final @Nullable String parent;
    private final float[] pivot;
    private final float[] rotateDegrees;
    private final boolean fullbright;
    private final List<MobBoxDef> boxes;

    /**
     * @param name          unique part name
     * @param parent        the parent part's name, or null for a root
     * @param pivot         x, y, z of the rotation point, relative to the parent's pivot (a root: to the model origin)
     * @param rotateDegrees rest rotation about x, y, z in degrees
     * @param fullbright    whether the part and its descendants draw at full light
     * @param boxes         the part's boxes (may be empty for a pure pivot)
     */
    public MobPartDef(String name, @Nullable String parent, float[] pivot, float[] rotateDegrees, boolean fullbright,
        List<MobBoxDef> boxes) {
        this.name = name;
        this.parent = parent;
        this.pivot = pivot.clone();
        this.rotateDegrees = rotateDegrees.clone();
        this.fullbright = fullbright;
        this.boxes = Collections.unmodifiableList(new ArrayList<>(boxes));
    }

    /** @return the part name */
    public String name() {
        return name;
    }

    /** @return the parent's name, or null for a root */
    public @Nullable String parent() {
        return parent;
    }

    /** @return a copy of the pivot */
    public float[] pivot() {
        return pivot.clone();
    }

    /** @return a copy of the rest rotation, degrees about x, y, z */
    public float[] rotateDegrees() {
        return rotateDegrees.clone();
    }

    /** @return whether the part draws at full light */
    public boolean fullbright() {
        return fullbright;
    }

    /** @return the boxes, in file order */
    public List<MobBoxDef> boxes() {
        return boxes;
    }
}
