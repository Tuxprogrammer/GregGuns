package com.gregguns.client.render.profile;
import org.junit.jupiter.api.Test;
class TmpAngleTest {
    @Test void print() {
        StringBuilder sb = new StringBuilder();
        for (AdsSpec spec : AdsTable.ALL) {
            ClientGunDefinition def = ClientGunDefinitions.byName(spec.gunName());
            AdsPose p = AdsTable.poseOf(def);
            double[] pt = AdsOffset.sightPoint(def.boxes(), def.modelScale(), spec);
            double[] e = AdsOffset.eyeOf(def.transforms(), AdsOffset.sightPart(def.boxes(), spec), pt, def.xFamily());
            sb.append(String.format("ANG %s angle=%.1f eye=%.2f,%.2f,%.2f shift=%.2f,%.2f,%.2f%n", spec.gunName(), p.angleDegrees(), e[0], e[1], e[2], p.shift()[0], p.shift()[1], p.shift()[2]));
        }
        throw new AssertionError(sb.toString());
    }
}
