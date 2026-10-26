package com.gregguns.client.autotest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.MinecraftForge;

import com.gregguns.GregGuns;
import com.gregguns.client.gun.GunInputHandler;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.testing.AutotestArgs;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.gun.GunClientEvents;
import com.gregguns.gun.GunClientHooks;
import com.gregguns.gun.logic.GunStats;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dev-only client hooks that let a script collect multiplayer and visual evidence without a human at the screen.
 * Active only when {@link DevEnvironment#testHooksEnabled()} (a dev workspace, or {@code -Dgregguns.testHooks=true})
 * and the JVM property {@code -Dgregguns.autotest=<label>} is set;
 * otherwise {@link #init} returns without registering anything, so a released pack never runs any of it unless
 * {@code -Dgregguns.testHooks=true} is also set.
 *
 * <h2>What it does</h2>
 * <ul>
 * <li><b>Logs</b> greppable lines, all starting {@code [gregguns] AUTOTEST}: {@code joined}, {@code client X saw shot
 * from Y (predicted|remote)}, {@code saw reload start}, {@code saw reload cancel}, {@code sound <name>} and
 * {@code screenshot <label> <file>}.</li>
 * <li><b>Obeys the server's chat signals</b> ({@link AutotestSignal}): switch the camera, screenshot, hold the fire
 * button through the real input path, open GUIs. The signal lines are hidden from the chat.</li>
 * <li><b>Screenshots</b> use vanilla {@code ScreenShotHelper} at the end of the render tick into
 * {@code screenshots/}, renamed to {@code <player>-<label>.png}.</li>
 * </ul>
 * The commands are documented on {@link AutotestCommands}. Auto-connecting to a server is done with the launch
 * arguments, see {@code docs/testing/README.md}. Client only.
 */
@SideOnly(Side.CLIENT)
public final class ClientAutotest {

    /** The JVM property that switches the hooks on; its value is only a label for the log. */
    public static final String PROPERTY = "gregguns.autotest";

    /**
     * Optional JVM property {@code -Dgregguns.autotest.connect=host:port}: once the main menu has been up for
     * {@link #MENU_SETTLE_TICKS} ticks, join that server. The vanilla {@code --server} launch argument connects while
     * the client is still finishing its start-up and loses an FML handshake race (NullPointerException in
     * {@code FMLClientHandler.waitForPlayClient}), so this waits for the menu instead.
     */
    public static final String CONNECT_PROPERTY = "gregguns.autotest.connect";

    private static final int MENU_SETTLE_TICKS = 100;
    private static final int CONNECT_RETRY_TICKS = 300;

    private static final String LOG_PREFIX = "AUTOTEST ";

    private final GunInputHandler input;
    private final TooltipProbe tooltips = new TooltipProbe();
    private final List<Task> tasks = new ArrayList<>();
    private final Map<String, Armed> armed = new HashMap<>();
    private final java.util.ArrayDeque<String[]> queue = new java.util.ArrayDeque<>();
    private String pendingShot;
    private AutotestCommands.Wait holdWait;
    private int shotFrames;
    private boolean joinLogged;
    private int fireClock = -1;
    private int fireRemaining;
    private int menuTicks;
    private boolean connectStarted;
    private int connectTicks;
    private int retries;

    private ClientAutotest(GunInputHandler input) {
        this.input = input;
    }

    /** @return true if the test hooks are enabled and the JVM runs with {@code -Dgregguns.autotest} set */
    public static boolean enabled() {
        return DevEnvironment.testHooksEnabled() && System.getProperty(PROPERTY) != null;
    }

    /**
     * Installs the hooks when {@link #enabled()}.
     *
     * @param input the gun input handler, so the script can hold the fire button
     */
    public static void init(GunInputHandler input) {
        if (!enabled()) {
            return;
        }
        ClientAutotest autotest = new ClientAutotest(input);
        FMLCommonHandler.instance()
            .bus()
            .register(autotest);
        MinecraftForge.EVENT_BUS.register(autotest);
        MinecraftForge.EVENT_BUS.register(autotest.tooltips);
        GunClientHooks.install(autotest.new Logging(GunClientHooks.events()));
        GregGuns.LOG.info("[gregguns] AUTOTEST hooks active, label {}", System.getProperty(PROPERTY));
    }

    private static String localName() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        return player == null ? "?" : player.getCommandSenderName();
    }

    private static void log(String message) {
        GregGuns.LOG.info("[gregguns] {}{}", LOG_PREFIX, message);
    }

    private void schedule(int ticks, Runnable action) {
        tasks.add(new Task(ticks, action));
    }

    // ----- events -------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        tickConnect(mc);
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        // Commands run strictly in order, and nothing runs while a screenshot is waiting for its frames: a GUI that
        // stalls the client for a second must not be closed before it has been drawn and photographed.
        if (holdWait != null && holdWait.tick(mc.thePlayer)) {
            holdWait = null;
        }
        while (!queue.isEmpty() && pendingShot == null && holdWait == null) {
            execute(queue.poll());
        }
        if (!joinLogged) {
            joinLogged = true;
            log("joined " + localName());
        }
        for (Iterator<Task> it = tasks.iterator(); it.hasNext();) {
            Task task = it.next();
            if (--task.ticks <= 0) {
                it.remove();
                task.action.run();
            }
        }
        tickFire();
    }

    private void tickConnect(Minecraft mc) {
        String target = System.getProperty(CONNECT_PROPERTY);
        if (target == null) {
            return;
        }
        if (connectStarted) {
            // About one run in five the first attempt is silently lost (the server never sees a handshake), so retry
            // once the connection has had CONNECT_RETRY_TICKS without producing a world.
            if (mc.theWorld == null && ++connectTicks >= CONNECT_RETRY_TICKS && retries < 2) {
                retries++;
                connectTicks = 0;
                log("connect retry " + retries + " (no world after " + CONNECT_RETRY_TICKS + " ticks)");
                connect(mc, target);
            }
            return;
        }
        if (!(mc.currentScreen instanceof GuiMainMenu) || ++menuTicks < MENU_SETTLE_TICKS) {
            return;
        }
        connectStarted = true;
        log("connecting to " + target);
        connect(mc, target);
    }

    private void connect(Minecraft mc, String target) {
        // Source: FML 1.7.10 cpw.mods.fml.client.FMLClientHandler.connectToServer: it shows GuiConnecting first and
        // only
        // then creates playClientBlock, so a fast server's handshake reaches waitForPlayClient (on the netty thread)
        // while the field is still null and throws a NullPointerException. It also needs serverDataTag, which only the
        // server-list screen fills. So prime the latch ourselves, then show GuiConnecting directly.
        try {
            ReflectionHelper.findField(FMLClientHandler.class, "playClientBlock")
                .set(FMLClientHandler.instance(), new CountDownLatch(1));
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("[gregguns] AUTOTEST could not prime playClientBlock", e);
        }
        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, new ServerData("autotest", target)));
    }

    /** Presses the fire button for two ticks every six ticks, {@code fireRemaining} times. */
    private void tickFire() {
        if (fireClock < 0) {
            return;
        }
        int phase = fireClock % 6;
        input.setAutotestAttack(phase < 2);
        fireClock++;
        if (fireClock >= fireRemaining * 6) {
            fireClock = -1;
            input.setAutotestAttack(false);
        }
    }

    @SubscribeEvent
    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingShot == null) {
            return;
        }
        if (++shotFrames < 4) {
            return; // let the screen draw a few frames first
        }
        String label = pendingShot;
        pendingShot = null;
        shotFrames = 0;
        Screenshots.take(localName(), label);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String[] parts = AutotestSignal.parse(event.message.getUnformattedText());
        if (parts == null) {
            return;
        }
        event.setCanceled(true);
        log("signal " + String.join(" ", parts));
        queue.add(parts);
    }

    private void execute(String[] parts) {
        try {
            run(parts);
        } catch (RuntimeException e) {
            GregGuns.LOG.error("[gregguns] AUTOTEST command failed: {}", String.join(" ", parts), e);
        }
    }

    @SubscribeEvent
    public void onSound(PlaySoundEvent17 event) {
        // The event's name field is the bare event name; the resource location carries the namespace.
        String location = event.sound == null || event.sound.getPositionedSoundLocation() == null ? null
            : event.sound.getPositionedSoundLocation()
                .toString();
        String name = location != null ? location : event.name;
        if (name != null && (name.startsWith("gregguns:") || (event.name != null && event.name.contains("pistol")))) {
            log("sound " + name + " (client " + localName() + ")");
        }
    }

    // ----- commands -----------------------------------------------------------------------------------------

    private void run(String[] parts) {
        String[] args = AutotestSignal.args(parts);
        switch (parts[0]) {
            case "view" -> Minecraft.getMinecraft().gameSettings.thirdPersonView = Integer.parseInt(args[0]);
            // hud off hides the GUI layer (hotbar, hands, crosshair) for clean worn-model screenshots (F1).
            case "hud" -> Minecraft.getMinecraft().gameSettings.hideGUI = "off".equals(args[0]);
            case "shot" -> {
                pendingShot = args[0];
                shotFrames = 0;
            }
            case "arm" -> armed.put(args[0], new Armed(args[1], Integer.parseInt(args[2])));
            case "fire" -> {
                fireRemaining = Integer.parseInt(args[0]);
                fireClock = 0;
                log("fire x" + args[0] + " (client " + localName() + ")");
            }
            case "gui" -> AutotestCommands.openGui(args);
            case "look" -> AutotestCommands.look(args);
            case "use" -> {
                AutotestArgs.requireNone(args);
                AutotestCommands.use();
            }
            case "usesneak" -> {
                AutotestArgs.requireNone(args);
                holdWait = new AutotestCommands.SneakUseWait();
            }
            case "see" -> holdWait = new AutotestCommands.SeeWait(args[0]);
            case "sync" -> AutotestCommands.syncAck();
            case "hold" -> holdWait = new AutotestCommands.HoldWait(args[0]);
            case "scope" -> com.gregguns.client.render.GunRenderers
                .setDebugScope("off".equals(args[0]) ? null : args[0]);
            case "tip" -> tooltips.setSlot(args[0]);
            case "shift" -> tooltips.setShift("on".equals(args[0]));
            case "creativegive" -> AutotestCommands.creativeGive(args[0]);
            case "slots" -> AutotestCommands.reportSlots(args[0]);
            case "quit" -> {
                log("quit (client " + localName() + ")");
                Minecraft.getMinecraft()
                    .shutdown();
            }
            case "close" -> {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                }
            }
            default -> log("unknown command " + parts[0]);
        }
    }

    private void shotEvent(String kind) {
        Armed arm = armed.remove(kind);
        if (arm != null) {
            schedule(arm.delay, () -> {
                if (pendingShot == null) {
                    pendingShot = arm.label;
                    shotFrames = 3;
                }
            });
        }
    }

    // ----- helpers ------------------------------------------------------------------------------------------

    private static final class Task {

        int ticks;
        final Runnable action;

        Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }

    private static final class Armed {

        final String label;
        final int delay;

        Armed(String label, int delay) {
            this.label = label;
            this.delay = delay;
        }
    }

    /** Wraps the installed gun events: logs each one, then passes it on unchanged. */
    private final class Logging implements GunClientEvents {

        private final GunClientEvents next;

        Logging(GunClientEvents next) {
            this.next = next;
        }

        private String who(EntityLivingBase entity) {
            String name = entity.getCommandSenderName();
            return "client " + localName()
                + " saw %s from "
                + name
                + (entity == Minecraft.getMinecraft().thePlayer ? " (predicted)" : " (remote)");
        }

        @Override
        public void onShotFired(EntityLivingBase entity, GunStats stats, int attackType) {
            log(String.format(who(entity), "shot"));
            shotEvent("shot");
            next.onShotFired(entity, stats, attackType);
        }

        @Override
        public void onDryFire(EntityLivingBase entity, GunStats stats) {
            log(String.format(who(entity), "dry fire"));
            next.onDryFire(entity, stats);
        }

        @Override
        public void onReloadStarted(EntityLivingBase entity, GunStats stats, int ticks) {
            log(String.format(who(entity), "reload start"));
            shotEvent("reload");
            next.onReloadStarted(entity, stats, ticks);
        }

        @Override
        public void onReloadCanceled(EntityLivingBase entity) {
            log(String.format(who(entity), "reload cancel"));
            next.onReloadCanceled(entity);
        }

        @Override
        public void setZoom(EntityLivingBase entity, boolean zoomed, double fovMultiplier) {
            next.setZoom(entity, zoomed, fovMultiplier);
        }
    }

    /** Package access for the helpers that log in the same format. */
    static void logLine(String message) {
        log(message);
    }
}
