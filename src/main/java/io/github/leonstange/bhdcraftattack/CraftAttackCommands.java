package io.github.leonstange.bhdcraftattack;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftAttackCommands implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final PersonalElytra elytras;
    private final TeamNames teams;
    private final Function<String, Player> findPlayer;
    private final Function<String, OfflinePlayer> findKnownPlayer;
    private final Supplier<? extends Collection<? extends Player>> onlinePlayers;

    public CraftAttackCommands(JavaPlugin plugin, PersonalElytra elytras, TeamNames teams,
                               Function<String, Player> findPlayer,
                               Function<String, OfflinePlayer> findKnownPlayer,
                               Supplier<? extends Collection<? extends Player>> onlinePlayers) {
        this.plugin = plugin;
        this.elytras = elytras;
        this.teams = teams;
        this.findPlayer = findPlayer;
        this.findKnownPlayer = findKnownPlayer;
        this.onlinePlayers = onlinePlayers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("regeln") && args.length == 1) {
            BhdCraftAttackPlugin.message(sender, "Serverregeln");
            List<String> rules = plugin.getConfig().getStringList("rules");
            for (int i = 0; i < rules.size(); i++) {
                sender.sendMessage(net.kyori.adventure.text.Component.text((i + 1) + ". " + rules.get(i)));
            }
            return true;
        }
        if (subcommand.equals("team")) {
            return teamCommand(sender, args);
        }
        if (!sender.isOp()) {
            BhdCraftAttackPlugin.message(sender, "keine Berechtigung.");
            return true;
        }
        if (subcommand.equals("version") && args.length == 1) {
            BhdCraftAttackPlugin.message(sender, "Version v" + plugin.getPluginMeta().getVersion());
        } else if (subcommand.equals("death") && args.length == 3 && args[1].equalsIgnoreCase("reset")) {
            OfflinePlayer target = findKnownPlayer.apply(args[2]);
            if (target == null) {
                BhdCraftAttackPlugin.message(sender, "Unbekannter Spieler: " + args[2]);
            } else {
                target.setStatistic(Statistic.DEATHS, 0);
                if (target instanceof Player online) online.saveData();
                BhdCraftAttackPlugin.message(sender, "Todeszähler von " + target.getName() + " auf 0 zurückgesetzt.");
            }
        } else if (subcommand.equals("elytra") && args.length == 2) {
            Player target = findPlayer.apply(args[1]);
            if (target == null || !target.isOnline()) {
                BhdCraftAttackPlugin.message(sender, "Spieler nicht online: " + args[1]);
                return true;
            }
            if (target.isDead()) {
                BhdCraftAttackPlugin.message(sender, "Der Spieler muss zuerst respawnen.");
                return true;
            }
            if (elytras.carriesOwn(target)) {
                BhdCraftAttackPlugin.message(sender, "Hinweis: " + target.getName() + " hat bereits eine persönliche Elytra dabei.");
            }
            if (elytras.grant(target, true) == PersonalElytra.GrantResult.INVENTORY_FULL) {
                BhdCraftAttackPlugin.message(sender, "Das Inventar von " + target.getName() + " ist voll. Keine Elytra vergeben.");
            } else {
                BhdCraftAttackPlugin.message(sender, "Persönliche Elytra an " + target.getName() + " vergeben.");
            }
        } else {
            BhdCraftAttackPlugin.message(sender, "Verwendung: /bhd-craftattack <elytra|death reset|team|version|regeln>");
        }
        return true;
    }

    private boolean teamCommand(CommandSender sender, String[] args) {
        boolean quotedSelf = args.length >= 2 && args[1].startsWith("\"")
                && args[args.length - 1].endsWith("\"");
        if (args.length == 2 || quotedSelf) {
            if (!(sender instanceof Player player)) {
                BhdCraftAttackPlugin.message(sender, "Die Konsole muss einen Spielernamen angeben.");
                return true;
            }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            if (quotedSelf) name = name.length() >= 2 ? name.substring(1, name.length() - 1) : "";
            return setTeam(sender, player, name);
        }
        if (args.length >= 3) {
            if (!sender.isOp()) {
                BhdCraftAttackPlugin.message(sender, "keine Berechtigung.");
                return true;
            }
            Player target = findPlayer.apply(args[1]);
            if (target == null || !target.isOnline()) {
                BhdCraftAttackPlugin.message(sender, "Spieler nicht online: " + args[1]);
                return true;
            }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            if (args.length == 3 && name.equalsIgnoreCase("reset")) {
                teams.clear(target);
                BhdCraftAttackPlugin.message(sender, "Teamname von " + target.getName() + " gelöscht.");
                return true;
            }
            if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1);
            }
            return setTeam(sender, target, name);
        }
        BhdCraftAttackPlugin.message(sender, "Verwendung: /bhd-craftattack team <Teamname>"
                + (sender.isOp() ? " oder /bhd-craftattack team <Spielername> <Teamname|reset>" : ""));
        return true;
    }

    private boolean setTeam(CommandSender sender, Player player, String name) {
        try {
            teams.set(player, name);
            BhdCraftAttackPlugin.message(sender, "Teamname von " + player.getName() + " auf [" + name + "] gesetzt.");
        } catch (IllegalArgumentException exception) {
            BhdCraftAttackPlugin.message(sender, exception.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return (sender.isOp() ? List.of("elytra", "death", "team", "version", "regeln") : List.of("team", "regeln"))
                    .stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (sender.isOp() && args.length == 2 && args[0].equalsIgnoreCase("death")) {
            return "reset".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("reset") : List.of();
        }
        boolean playerArgument = args.length == 2 && (args[0].equalsIgnoreCase("elytra") || args[0].equalsIgnoreCase("team"))
                || args.length == 3 && args[0].equalsIgnoreCase("death") && args[1].equalsIgnoreCase("reset");
        if (sender.isOp() && playerArgument) {
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            return onlinePlayers.get().stream()
                    .filter(player -> !(sender instanceof Player viewer) || viewer.canSee(player))
                    .map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted().toList();
        }
        if (sender.isOp() && args.length == 3 && args[0].equalsIgnoreCase("team")) {
            return "reset".startsWith(args[2].toLowerCase(Locale.ROOT)) ? List.of("reset") : List.of();
        }
        return List.of();
    }
}
