package com.gregguns;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** The MIT notice must travel in every copy of the jar, so the shipped file is the repository's file. */
class ThirdPartyNoticesTest {

    @Test
    void theJarCopyIsIdenticalToTheRepositoryFile() throws IOException {
        byte[] repo = Files.readAllBytes(Paths.get("THIRD-PARTY-NOTICES.md"));
        try (InputStream in = ThirdPartyNoticesTest.class.getResourceAsStream("/THIRD-PARTY-NOTICES.md")) {
            assertNotNull(in, "src/main/resources/THIRD-PARTY-NOTICES.md must exist");
            assertArrayEquals(repo, readAll(in), "update src/main/resources/THIRD-PARTY-NOTICES.md to match");
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0;) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
