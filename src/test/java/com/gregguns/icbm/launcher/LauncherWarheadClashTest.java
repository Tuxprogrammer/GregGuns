package com.gregguns.icbm.launcher;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;
import com.gregguns.icbm.warhead.WarheadRecipeSpec;

/**
 * Guards the launcher warhead Assembler recipes against clashing with any other ICBM Assembler recipe (I5 review item
 * 14). GT crafts the first recipe whose inputs are all present under the same programmed circuit, so a recipe whose
 * inputs are contained in another's (with the same circuit, at any voltage: a higher machine runs the lower recipes
 * too) can be crafted from the other's ingredients. The check is in both directions, over the launcher warheads, the
 * warhead recipes and the parts.
 */
class LauncherWarheadClashTest {

    /** One recipe's identity for the check. */
    private static final class R {

        final String name;
        final int circuit;
        final List<Ing> inputs;

        R(String name, int circuit, List<Ing> inputs) {
            this.name = name;
            this.circuit = circuit;
            this.inputs = inputs;
        }
    }

    private static List<R> others() {
        List<R> all = new ArrayList<>();
        for (WarheadRecipeSpec s : WarheadRecipeSpec.values()) {
            all.add(new R("warhead " + s, s.circuit(), s.inputs()));
        }
        for (IcbmPart p : IcbmPart.values()) {
            all.add(new R("part " + p, p.circuit(), p.inputs()));
        }
        return all;
    }

    /** @return true when every input of a is present in b with at least a's count (so b's ingredients craft a) */
    static boolean contained(List<Ing> a, List<Ing> b) {
        for (Ing x : a) {
            boolean found = false;
            for (Ing y : b) {
                if (x.src() == y.src() && x.id()
                    .equals(y.id()) && y.count() >= x.count()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    static boolean clash(R a, R b) {
        return a.circuit == b.circuit && (contained(a.inputs, b.inputs) || contained(b.inputs, a.inputs));
    }

    private static List<String> clashesOf(LauncherWarhead w, int circuit) {
        R me = new R("launcher warhead " + w, circuit, w.inputs());
        List<String> out = new ArrayList<>();
        for (R o : others()) {
            if (clash(me, o)) {
                out.add(o.name);
            }
        }
        for (LauncherWarhead v : LauncherWarhead.values()) {
            if (v != w) {
                R other = new R("launcher warhead " + v, v.circuit(), v.inputs());
                if (clash(me, other)) {
                    out.add(other.name);
                }
            }
        }
        return out;
    }

    @Test
    void noLauncherWarheadCanBeCraftedFromAnotherRecipesInputsOrTheOtherWayAround() {
        List<String> bad = new ArrayList<>();
        for (LauncherWarhead w : LauncherWarhead.values()) {
            List<String> c = clashesOf(w, w.circuit());
            if (!c.isEmpty()) {
                bad.add(w + " (circuit " + w.circuit() + ") clashes with " + c);
            }
        }
        assertTrue(bad.isEmpty(), String.join("; ", bad));
    }

    @Test
    void theCheckSeesTheOldHeClash() {
        // HE was circuit 1 with {2 plates, 4 TNT, 2 PE, fuze}: contained in the T1 condensed warhead's inputs.
        R condensed = null;
        for (R o : others()) {
            if (o.name.equals("warhead CONDENSED")) {
                condensed = o;
            }
        }
        assertTrue(
            clash(new R("he", condensed.circuit, LauncherWarhead.HE.inputs()), condensed),
            "the check must flag HE against the condensed warhead");
    }
}
