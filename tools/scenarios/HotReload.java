import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Hot-reload client for dev-server.sh. Run as a source file, so nothing is compiled ahead of time and the build gets
 * no new dependency:
 *
 * <pre>
 *   java tools/scenarios/HotReload.java snapshot &lt;manifest&gt; &lt;classesDir&gt;...
 *   java tools/scenarios/HotReload.java redefine &lt;port&gt; &lt;manifest&gt; &lt;classesDir&gt;...
 * </pre>
 *
 * The manifest lists the SHA-1 of every class file the live server was started with (or last reloaded with). {@code
 * redefine} compares the class directories with it, attaches to the server over JDWP (localhost only), and redefines
 * every changed class that the server has loaded, in one atomic call. A changed class the server has not loaded yet
 * needs nothing: it is read from the directory when first used. On success the manifest is updated.
 *
 * <p>Exit codes: 0 reloaded (or nothing to do), 3 the JVM refused the redefinition (a schema change on a plain JVM;
 * the server must restart), 4 could not attach.
 */
public final class HotReload {
  private HotReload() {}

  public static void main(String[] args) throws Exception {
    if (args.length >= 3 && args[0].equals("snapshot")) {
      write(Paths.get(args[1]), scan(dirs(args, 2)));
    } else if (args.length >= 4 && args[0].equals("redefine")) {
      System.exit(redefine(Integer.parseInt(args[1]), Paths.get(args[2]), dirs(args, 3)));
    } else {
      System.err.println("usage: snapshot <manifest> <dir>... | redefine <port> <manifest> <dir>...");
      System.exit(2);
    }
  }

  private static List<Path> dirs(String[] args, int from) {
    List<Path> out = new ArrayList<>();
    for (int i = from; i < args.length; i++) out.add(Paths.get(args[i]));
    return out;
  }

  /** Maps each class name ("a.b.C$D") to its file and hash. Later directories do not override earlier ones. */
  private static Map<String, String[]> scan(List<Path> dirs) throws Exception {
    Map<String, String[]> out = new TreeMap<>();
    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) continue;
      List<Path> files;
      try (Stream<Path> s = Files.walk(dir)) {
        files = s.filter(p -> p.toString().endsWith(".class")).toList();
      }
      for (Path p : files) {
        String rel = dir.relativize(p).toString();
        String name = rel.substring(0, rel.length() - ".class".length()).replace('/', '.');
        out.putIfAbsent(name, new String[] {p.toString(), sha1(Files.readAllBytes(p))});
      }
    }
    return out;
  }

  private static String sha1(byte[] b) throws Exception {
    StringBuilder sb = new StringBuilder();
    for (byte x : MessageDigest.getInstance("SHA-1").digest(b)) sb.append(String.format("%02x", x));
    return sb.toString();
  }

  private static void write(Path manifest, Map<String, String[]> now) throws IOException {
    List<String> lines = new ArrayList<>();
    now.forEach((k, v) -> lines.add(k + " " + v[1]));
    Files.write(manifest, lines);
  }

  private static Map<String, String> read(Path manifest) throws IOException {
    Map<String, String> out = new HashMap<>();
    if (!Files.exists(manifest)) return out;
    for (String l : Files.readAllLines(manifest)) {
      int i = l.indexOf(' ');
      if (i > 0) out.put(l.substring(0, i), l.substring(i + 1));
    }
    return out;
  }

  private static int redefine(int port, Path manifest, List<Path> dirs) throws Exception {
    Map<String, String[]> now = scan(dirs);
    Map<String, String> old = read(manifest);
    List<String> changed = new ArrayList<>();
    List<String> added = new ArrayList<>();
    for (Map.Entry<String, String[]> e : now.entrySet()) {
      String before = old.get(e.getKey());
      if (before == null) added.add(e.getKey());
      else if (!before.equals(e.getValue()[1])) changed.add(e.getKey());
    }
    if (changed.isEmpty()) {
      System.out.println("hotreload: no changed classes (" + added.size() + " new, they load on first use)");
      write(manifest, now);
      return 0;
    }
    AttachingConnector conn = null;
    for (AttachingConnector c : Bootstrap.virtualMachineManager().attachingConnectors()) {
      if (c.name().equals("com.sun.jdi.SocketAttach")) conn = c;
    }
    if (conn == null) {
      System.err.println("hotreload: no SocketAttach connector");
      return 4;
    }
    Map<String, Connector.Argument> a = conn.defaultArguments();
    a.get("hostname").setValue("127.0.0.1");
    a.get("port").setValue(Integer.toString(port));
    VirtualMachine vm;
    try {
      vm = conn.attach(a);
    } catch (Exception e) {
      System.err.println("hotreload: cannot attach to 127.0.0.1:" + port + ": " + e);
      return 4;
    }
    try {
      Map<ReferenceType, byte[]> toRedefine = new HashMap<>();
      List<String> notLoaded = new ArrayList<>();
      for (String name : changed) {
        List<ReferenceType> types = vm.classesByName(name);
        if (types.isEmpty()) {
          notLoaded.add(name);
          continue;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(now.get(name)[0]));
        for (ReferenceType t : types) toRedefine.put(t, bytes);
      }
      System.out.println(
          "hotreload: " + changed.size() + " changed class(es), " + toRedefine.size() + " loaded, " + notLoaded.size()
              + " not loaded yet, " + added.size() + " new");
      if (!toRedefine.isEmpty()) {
        if (!vm.canRedefineClasses()) {
          System.err.println("hotreload: the JVM cannot redefine classes");
          return 3;
        }
        try {
          vm.redefineClasses(toRedefine);
        } catch (Throwable t) {
          // UnsupportedOperationException: the change adds, removes or reorders members, and this JVM has no DCEVM.
          System.err.println("hotreload: REFUSED by the JVM: " + t);
          return 3;
        }
        toRedefine.keySet().forEach(t -> System.out.println("  redefined " + t.name()));
      }
      write(manifest, now);
      return 0;
    } finally {
      vm.dispose();
    }
  }
}
