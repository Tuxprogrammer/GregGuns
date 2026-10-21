package com.gregguns.compat.questbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads the questbook files by path relative to the questbook root. A jar cannot list a directory, so the installer
 * asks only for the paths {@code index.json} names.
 */
public interface QuestbookSource {

    /** Where our jar keeps the questbook (design record 0015, the questbook contract). */
    String JAR_ROOT = "assets/gregguns/questbook/";

    /**
     * @param relativePath a path such as {@code Quests/GregGuns-x/Name-y.json}, relative to the root
     * @return the file as UTF-8 text, or {@code null} when it does not exist
     */
    String read(String relativePath) throws IOException;

    /** A source over a class loader's resources under {@code root}. */
    final class Classpath implements QuestbookSource {

        private final ClassLoader loader;
        private final String root;

        public Classpath(ClassLoader loader, String root) {
            this.loader = loader;
            this.root = root.endsWith("/") ? root : root + "/";
        }

        /** Resolves a relative path under the root, refusing anything that would climb out of it. */
        static String resolve(String root, String relativePath) {
            if (relativePath == null || relativePath.isEmpty()
                || relativePath.startsWith("/")
                || relativePath.contains("\\")
                || relativePath.contains("..")) {
                throw new IllegalArgumentException("Bad questbook path: " + relativePath);
            }
            return root + relativePath;
        }

        @Override
        public String read(String relativePath) throws IOException {
            try (InputStream in = loader.getResourceAsStream(resolve(root, relativePath))) {
                if (in == null) {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int n; (n = in.read(buffer)) > 0;) {
                    out.write(buffer, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }
}
