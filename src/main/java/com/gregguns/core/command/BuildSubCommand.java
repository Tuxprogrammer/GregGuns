package com.gregguns.core.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.gregguns.machine.DevStructureBuilder;

/**
 * {@code /gregguns build <x> <y> <z> [count] [bench]}: builds and forms the GregGuns multiblock whose controller stands
 * at the
 * block position (Ammunition Works, Armory Gantry or Rampart): casings through the controller's hologram routine,
 * the required hatches, then a structure check. The word {@code bench} adds the Ammunition Works' optional filling
 * bench and its two buses. Dev environment only (registered next to {@code test}); the console
 * has no player to sneak-click a trigger item, so this is the way to get a formed machine from a script. Server side.
 */
public final class BuildSubCommand implements GregGunsSubCommand {

    @Override
    public String name() {
        return "build";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        BuildArgs parsed;
        try {
            parsed = BuildArgs.parse(args);
        } catch (IllegalArgumentException e) {
            sender.addChatMessage(new ChatComponentText(e.getMessage()));
            return;
        }
        Object result = DevStructureBuilder
            .build(sender.getEntityWorld(), parsed.x, parsed.y, parsed.z, parsed.count, parsed.bench);
        sender.addChatMessage(new ChatComponentText(String.valueOf(result)));
    }

    @Override
    public List<String> complete(ICommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
