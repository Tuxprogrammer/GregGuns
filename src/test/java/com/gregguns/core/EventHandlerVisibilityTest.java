package com.gregguns.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Every {@code @SubscribeEvent} handler must be reachable from FML's generated {@code ASMEventHandler}, which lives in
 * another package: the method must be public, and its declaring class must be public in its own class-file header.
 * Java 8 checks only that header, not the enclosing classes, so a public static nested class inside a package-private
 * class is fine (and a private or package-private nested class is not).
 *
 * <p>
 * A package-private handler class compiles and passes every headless run, then crashes the client on its first tick
 * with an {@code IllegalAccessError} (M3's {@code StepHeightClient}, M1's renderer). This test reads the compiled class
 * files with ASM, so it never loads or initializes them (client classes included).
 */
// Source: FML 1.7.10 ASMEventHandler generates a class in its own package that calls the handler directly
// https://github.com/MinecraftForge/FML/blob/1.7.10/src/main/java/cpw/mods/fml/common/eventhandler/ASMEventHandler.java
class EventHandlerVisibilityTest {

    private static final String SUBSCRIBE = "Lcpw/mods/fml/common/eventhandler/SubscribeEvent;";

    /** Internal class name to the access flags in its own class-file header, which is what the JVM checks. */
    private final Map<String, Integer> access = new HashMap<>();
    /** "class#method" for every method annotated @SubscribeEvent, with the method's access flags. */
    private final Map<String, Integer> handlers = new HashMap<>();

    @Test
    void everySubscribeEventHandlerIsReachableFromFml() throws IOException, URISyntaxException {
        Path root = classesRoot();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.filter(
                f -> f.toString()
                    .endsWith(".class"))
                .collect(Collectors.toList())) {
                try (InputStream in = Files.newInputStream(p)) {
                    new ClassReader(in).accept(new Scanner(), ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                }
            }
        }
        assertTrue(!handlers.isEmpty(), "found no @SubscribeEvent handlers under " + root);
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Integer> h : handlers.entrySet()) {
            String owner = h.getKey()
                .substring(
                    0,
                    h.getKey()
                        .indexOf('#'));
            if ((h.getValue() & Opcodes.ACC_PUBLIC) == 0) {
                problems.add(h.getKey() + ": the method is not public");
            }
            Integer flags = access.get(owner);
            if (flags == null || (flags & Opcodes.ACC_PUBLIC) == 0) {
                problems.add(h.getKey() + ": class " + owner.replace('/', '.') + " is not public");
            }
        }
        problems.sort(String::compareTo);
        assertTrue(
            problems.isEmpty(),
            "FML cannot call these event handlers (IllegalAccessError on first fire):\n" + String.join("\n", problems));
    }

    private static Path classesRoot() throws URISyntaxException {
        // getSystemResource does not load the class, so no static initializer runs.
        URL url = ClassLoader.getSystemResource("com/gregguns/GregGuns.class");
        if (url == null) {
            url = EventHandlerVisibilityTest.class.getClassLoader()
                .getResource("com/gregguns/GregGuns.class");
        }
        assertTrue(url != null && "file".equals(url.getProtocol()), "main classes are not a directory: " + url);
        return Paths.get(url.toURI())
            .getParent()
            .getParent()
            .getParent();
    }

    private final class Scanner extends ClassVisitor {

        private String name;

        Scanner() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int acc, String className, String signature, String superName,
            String[] interfaces) {
            this.name = className;
            access.put(className, acc);
        }

        @Override
        public MethodVisitor visitMethod(int acc, String method, String desc, String signature, String[] exceptions) {
            final String key = name + "#" + method;
            return new MethodVisitor(Opcodes.ASM5) {

                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    if (SUBSCRIBE.equals(annotation)) {
                        handlers.put(key, acc);
                    }
                    return null;
                }
            };
        }
    }
}
