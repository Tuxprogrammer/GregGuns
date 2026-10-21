package com.gregguns.client.render.profile;

import java.util.Arrays;
import java.util.List;

/** Test helpers: the placement a gun's profile should have, solved from {@link InHandTargets}. */
final class InHandTestSupport {

    private InHandTestSupport() {}

    /** The boxes the fit is taken on: the rifle alone for the Infiltrator (its beam planes are part 1). */
    static BoxSpec[] fitBoxes(ClientGunDefinition d) {
        if (d.gunName()
            .equals("infiltrator")) {
            return Arrays.stream(d.boxes())
                .filter(b -> b.part() == 0)
                .toArray(BoxSpec[]::new);
        }
        return d.boxes();
    }

    /** @return the frame family: true for the Z family (pistol and Mac-10), whose rows are rotY 225 and 135 */
    static boolean zFamily(ClientGunDefinition d) {
        return d.gunName()
            .equals("pistol_9mm")
            // The Golden Gun wears the pistol's model and transforms.
            || d.gunName()
                .equals("golden_gun")
            || d.gunName()
                .equals("mac10");
    }

    static double fpRotY(ClientGunDefinition d) {
        return zFamily(d) ? 225 : -45;
    }

    static double tpRotY(ClientGunDefinition d) {
        return zFamily(d) ? 135 : 225;
    }

    static InHandFit.Placement placement(ClientGunDefinition d) {
        return InHandFit.fit(
            d.gunName(),
            fitBoxes(d),
            d.modelScale(),
            !zFamily(d),
            fpRotY(d),
            tpRotY(d),
            d.transforms()
                .tpAim());
    }

    /** @return the rows a third-person base program should replay for the gun */
    static List<String> tpBaseRows(String gunName) {
        ClientGunDefinition d = ClientGunDefinitions.byName(gunName);
        InHandFit.Placement p = placement(d);
        double s = p.tpScale;
        return Arrays.asList(
            "R(180.0000;1,0,0)",
            String.format("R(%.4f;0,1,0)", tpRotY(d)),
            String.format("S(%.4f,%.4f,%.4f)", s, s, s),
            String.format("T(%.4f,%.4f,%.4f)", p.tpTranslate[0], p.tpTranslate[1], p.tpTranslate[2]));
    }
}
