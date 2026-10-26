package com.gregguns.api;

/**
 * Marks an item that is meant to be better than the thing it goes into, so the dev self-check does not flag it as an
 * input of a higher tier than the recipe's output (D1). Today only the missile guidance computers: a Mk II computer in
 * a T1 body gives it a step of guidance (D24). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Guidance)
public interface TierUpgrade {
}
