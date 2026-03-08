package gg.pufferfish.pufferfish;

import gg.pufferfish.pufferfish.metrics.PufferfishMetrics;
import gg.pufferfish.pufferfish.metrics.WorldTickLoadService;
import gg.pufferfish.pufferfish.metrics.WorldTickLoadSnapshot;
import gg.pufferfish.pufferfish.util.WorldTickLoadTrackerService;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class PufferfishCommand extends Command {

    public PufferfishCommand() {
        super("pufferfish");
        this.description = "Pufferfish related commands";
        this.usageMessage = "/pufferfish [reload | version | worldload [limit]]";
        this.setPermission("bukkit.command.pufferfish");
    }
    
    public static void init() {
        MinecraftServer.getServer().server.getCommandMap().register("pufferfish", "Pufferfish", new PufferfishCommand());
        PufferfishMetrics.setWorldTickLoadService(WorldTickLoadTrackerService.INSTANCE);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length == 1) {
            return Stream.of("reload", "version", "worldload")
              .filter(arg -> arg.startsWith(args[0].toLowerCase()))
              .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;
        String prefix = ChatColor.of("#12fff6") + "" + ChatColor.BOLD + "Pufferfish » " + ChatColor.of("#e8f9f9");

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(prefix + "Usage: " + usageMessage);
            args = new String[]{"version"};
        }

        if (args[0].equalsIgnoreCase("reload")) {
            MinecraftServer console = MinecraftServer.getServer();
            try {
                PufferfishConfig.load();
            } catch (IOException e) {
                sender.sendMessage(Component.text("Failed to reload.", NamedTextColor.RED));
                e.printStackTrace();
                return true;
            }
            console.server.reloadCount++;

            Command.broadcastCommandMessage(sender, prefix + "Pufferfish configuration has been reloaded.");
        } else if (args[0].equalsIgnoreCase("version")) {
            Command.broadcastCommandMessage(sender, prefix + "This server is running " + Bukkit.getName() + " version " + Bukkit.getVersion() + " (Implementing API version " + Bukkit.getBukkitVersion() + ")");
        } else if (args[0].equalsIgnoreCase("worldload")) {
            final WorldTickLoadService service = PufferfishMetrics.worldTickLoadService();
            if (service == null) {
                sender.sendMessage(prefix + "World tick load service is not available.");
                return true;
            }

            int limit = 5;
            if (args.length == 2) {
                try {
                    limit = Math.max(1, Math.min(20, Integer.parseInt(args[1])));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(prefix + "Limit must be a number from 1 to 20.");
                    return true;
                }
            }

            final List<WorldTickLoadSnapshot> snapshots = service.snapshotTop(limit);
            if (snapshots.isEmpty()) {
                sender.sendMessage(prefix + "No world tick load samples yet.");
                return true;
            }

            sender.sendMessage(prefix + "Top " + snapshots.size() + " worlds by avg100 share:");
            for (int index = 0; index < snapshots.size(); index++) {
                final WorldTickLoadSnapshot snapshot = snapshots.get(index);
                sender.sendMessage(prefix + String.format(
                    "#%d %s [%s | key=%s | uid=%s] avg100=%.3fms share100=%.2f%% max100=%.3fms ent=%d be=%d chunks=%d/%d blockTicks=%d fluidTicks=%d chunkTasks=%d samples=%d",
                    index + 1,
                    snapshot.worldName(),
                    snapshot.dimensionKey(),
                    snapshot.bukkitWorldKey(),
                    snapshot.worldUid(),
                    snapshot.averageMs100(),
                    snapshot.averageShare100(),
                    snapshot.maxMs100(),
                    snapshot.loadedEntityCount(),
                    snapshot.blockEntityTickerCount(),
                    snapshot.loadedChunkCount(),
                    snapshot.tickingChunkCount(),
                    snapshot.pendingBlockTickCount(),
                    snapshot.pendingFluidTickCount(),
                    snapshot.pendingChunkTaskCount(),
                    snapshot.samples()
                ));
            }
        }

        return true;
    }
}
