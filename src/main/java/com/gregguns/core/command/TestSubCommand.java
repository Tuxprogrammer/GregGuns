package com.gregguns.core.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioRequest;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.ScenarioRun;
import com.gregguns.core.testing.ScenarioRunner;
import com.gregguns.core.testing.ScenarioSummary;
import com.gregguns.core.testing.ScenarioTicker;
import com.gregguns.core.testing.WorldScenarioContext;

/**
 * {@code /gregguns test <scenario|all|list>}: runs scenarios from the {@link ScenarioRegistry}. Works from a player
 * (origin at the player) and from the dedicated-server console (origin at the overworld spawn). Server side.
 */
public final class TestSubCommand implements GregGunsSubCommand {

    private final ScenarioRegistry registry;
    private final ScenarioTicker ticker = new ScenarioTicker();

    /**
     * @param registry the scenarios to run
     */
    public TestSubCommand(ScenarioRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "test";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        ScenarioRequest request = ScenarioRequest.parse(args, registry);
        switch (request.kind()) {
            case LIST:
                list(sender);
                break;
            case ALL:
                run(sender, registry.forAll());
                break;
            case NAMED:
                run(sender, Collections.singletonList(request.scenario()));
                break;
            case UNKNOWN:
                sender.addChatMessage(new ChatComponentTranslation("gregguns.test.unknown", request.argument()));
                break;
            default:
                sender.addChatMessage(new ChatComponentTranslation("gregguns.test.usage"));
                break;
        }
    }

    @Override
    public List<String> complete(ICommandSender sender, String[] args) {
        return ScenarioRequest.complete(args, registry);
    }

    private void list(ICommandSender sender) {
        List<Scenario> all = registry.all();
        sender.addChatMessage(new ChatComponentTranslation("gregguns.test.list.header", all.size()));
        for (Scenario s : all) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.test.list.entry", s.name(), s.description()));
        }
    }

    private void run(ICommandSender sender, List<Scenario> scenarios) {
        if (ticker.isBusy()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.test.busy"));
            return;
        }
        World overworld = MinecraftServer.getServer()
            .worldServerForDimension(0);
        EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;
        ScenarioRunner runner = new ScenarioRunner(s -> WorldScenarioContext.create(s, overworld, player));
        ScenarioRun run = runner.begin(scenarios);
        if (run.isDone()) {
            report(sender, run.summary());
        } else {
            // Some scenario waits for server ticks: the report comes once every one of them has finished.
            ticker.track(run, summary -> report(sender, summary));
        }
    }

    private void report(ICommandSender sender, ScenarioSummary summary) {
        for (String line : summary.reportLines()) {
            GregGuns.LOG.info(line);
        }
        for (ScenarioResult r : summary.results()) {
            sender.addChatMessage(
                new ChatComponentTranslation(
                    r.passed() ? "gregguns.test.result.pass" : "gregguns.test.result.fail",
                    r.scenario()));
            for (String m : r.messages()) {
                sender.addChatMessage(new ChatComponentTranslation("gregguns.test.message", m));
            }
        }
        sender
            .addChatMessage(new ChatComponentTranslation("gregguns.test.summary", summary.passed(), summary.failed()));
    }
}
