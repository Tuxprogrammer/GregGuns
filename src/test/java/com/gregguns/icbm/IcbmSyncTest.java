package com.gregguns.icbm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards the client-to-server rule: ModularUI2 refuses a client write ("Sync handler is unable to send packets to
 * server") unless the sync value allows it, so a number field that forgot it silently never saves. Every editable
 * ICBM value (the sync names starting {@code field_}) must be built with {@code IcbmSync.editableText}, which allows
 * client writes. ModularUI is not on the unit-test
 * classpath, so this reads the sources instead of building the values; the client recheck proves the packets.
 */
class IcbmSyncTest {

    private static final Path ICBM = Paths.get("src/main/java/com/gregguns/icbm");
    private static final Pattern FIELD = Pattern.compile("\"(field_[a-z_]+)\",\\s*0,\\s*([A-Za-z.]+)\\(?");

    @Test
    void everyEditableFieldOfEveryIcbmScreenGoesThroughTheHelper() throws IOException {
        List<Path> guis;
        try (Stream<Path> files = Files.walk(ICBM)) {
            guis = files.filter(
                p -> p.getFileName()
                    .toString()
                    .endsWith("Gui.java"))
                .collect(Collectors.toList());
        }
        int fields = 0;
        for (Path gui : guis) {
            Matcher m = FIELD.matcher(new String(Files.readAllBytes(gui), StandardCharsets.UTF_8));
            while (m.find()) {
                fields++;
                assertEquals("IcbmSync.editableText", m.group(2), gui.getFileName() + " field " + m.group(1));
            }
        }
        // Launch Control X and Z, radar and tower radius, EMP radius: a refactor that loses them must not pass.
        assertTrue(fields >= 5, "found the editable fields (" + fields + ")");
    }
}
