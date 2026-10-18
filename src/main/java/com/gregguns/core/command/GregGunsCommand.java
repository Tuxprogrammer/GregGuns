package com.gregguns.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

/**
 * The {@code /gregguns} command: dispatches to registered subcommands. Op-only (permission level 2, which the console
 * always meets). Server side.
 */
// Spec: docs/gregguns-spec/01-core/index.md (dev tooling); CONTRIBUTING.md § Testing (in-game scenarios).
// Source: vanilla 1.7.10 net.minecraft.command.CommandBase (getCommandName, processCommand, addTabCompletionOptions,
// getRequiredPermissionLevel); no pack mod is involved, so there is no tag to pin.
public final class GregGunsCommand extends CommandBase {

    private static final int OP_LEVEL = 2;

    private final Map<String, GregGunsSubCommand> subCommands = new TreeMap<>();

    /**
     * Adds a subcommand.
     *
     * @param sub the subcommand
     * @throws IllegalArgumentException if the name is taken
     */
    public void register(GregGunsSubCommand sub) {
        if (subCommands.put(sub.name(), sub) != null) {
            throw new IllegalArgumentException("Duplicate /gregguns subcommand: " + sub.name());
        }
    }

    @Override
    public String getCommandName() {
        return "gregguns";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "gregguns.command.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return OP_LEVEL;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        GregGunsSubCommand sub = args.length == 0 ? null : subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.command.usage", join(subCommands.keySet())));
            return;
        }
        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String name : subCommands.keySet()) {
                if (name.startsWith(prefix)) {
                    out.add(name);
                }
            }
            return out;
        }
        GregGunsSubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        return sub == null ? new ArrayList<String>() : sub.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private static String join(Iterable<String> words) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(w);
        }
        return sb.toString();
    }
}
