package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SoundChecksTest {

    private static List<String> run(ContentChecksTest.FakeResources resources) {
        return SoundChecks.check(resources);
    }

    private static ContentChecksTest.FakeResources withJson(String json) {
        return new ContentChecksTest.FakeResources().with(SoundChecks.SOUNDS_JSON_PATH, json);
    }

    @Test
    void absentFileMeansNoEvents() {
        assertTrue(run(new ContentChecksTest.FakeResources()).isEmpty());
    }

    @Test
    void emptyObjectMeansNoEvents() {
        assertTrue(run(withJson("{}")).isEmpty());
    }

    @Test
    void existingFilesPass() {
        ContentChecksTest.FakeResources r = withJson(
            "{\"gun.shot\":{\"category\":\"player\",\"sounds\":[\"guns/shot\",{\"name\":\"gregguns:guns/shot2\"}]}}")
                .with("assets/gregguns/sounds/guns/shot.ogg", "x")
                .with("assets/gregguns/sounds/guns/shot2.ogg", "x");
        assertTrue(run(r).isEmpty());
    }

    @Test
    void missingOggIsReportedWithItsPath() {
        List<String> violations = run(withJson("{\"gun.shot\":{\"sounds\":[\"guns/gone\"]}}"));
        assertEquals(1, violations.size());
        assertTrue(
            violations.get(0)
                .contains("assets/gregguns/sounds/guns/gone.ogg"));
        assertTrue(
            violations.get(0)
                .contains("gun.shot"));
    }

    @Test
    void everyMissingFileIsReported() {
        assertEquals(2, run(withJson("{\"a\":{\"sounds\":[\"x\",\"y\"]}}")).size());
    }

    @Test
    void otherDomainsAreResolvedInTheirOwnFolder() {
        ContentChecksTest.FakeResources r = withJson("{\"a\":{\"sounds\":[\"minecraft:random/click\"]}}");
        assertTrue(
            run(r).get(0)
                .contains("assets/minecraft/sounds/random/click.ogg"));
    }

    @Test
    void eventReferencesAreNotFiles() {
        assertTrue(run(withJson("{\"a\":{\"sounds\":[{\"name\":\"gregguns:b\",\"type\":\"event\"}]}}")).isEmpty());
    }

    @Test
    void malformedJsonAndBadShapesAreReported() {
        assertTrue(
            run(withJson("{not json")).get(0)
                .contains("cannot parse"));
        assertTrue(
            run(withJson("[]")).get(0)
                .contains("JSON object"));
        assertTrue(
            run(withJson("{\"a\":{}}")).get(0)
                .contains("sounds"));
        assertTrue(
            run(withJson("{\"a\":{\"sounds\":[5]}}")).get(0)
                .contains("neither"));
    }
}
