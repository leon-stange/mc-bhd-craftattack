package io.github.leonstange.bhdcraftattack;

import java.util.Objects;
import org.bukkit.OfflinePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class BhdCraftAttackPlugin extends JavaPlugin implements Listener {
    public static final String PREFIX = "BHD - Craft Attack: ";
    private PersonalElytra elytras;
    private DeathCounter deathCounter;
    private TeamNames teams;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        elytras = new PersonalElytra(this);
        teams = new TeamNames(this);
        var commands = new CraftAttackCommands(this, elytras, teams, getServer()::getPlayerExact,
                this::findKnownPlayer, getServer()::getOnlinePlayers);
        var command = Objects.requireNonNull(getCommand("bhd-craftattack"));
        command.setExecutor(commands);
        command.setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ElytraProtection(elytras), this);
        getServer().getPluginManager().registerEvents(teams, this);
        getServer().getOnlinePlayers().forEach(teams::load);
        if (getConfig().getBoolean("death-counter.enabled", true)) {
            deathCounter = new DeathCounter();
        }
        // Retry deferred first grants; also refresh vanilla death statistics in the tab list.
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                maintain(player);
            }
            if (deathCounter != null) {
                deathCounter.update(getServer().getOnlinePlayers());
            }
        }, 1L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teams.load(player);
        message(player, "Willkommen auf dem Server!");
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline() && !player.isDead()) {
                if (elytras.grant(player, false) == PersonalElytra.GrantResult.INVENTORY_FULL) {
                    message(player, "Dein Inventar ist voll. Deine persönliche Elytra erhältst du, sobald ein Platz frei ist.");
                }
                elytras.equipAfterDeath(player);
            }
        });
    }

    private void maintain(Player player) {
        if (player.isOnline() && !player.isDead()) {
            elytras.grant(player, false);
            elytras.equipAfterDeath(player);
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (deathCounter != null) {
            deathCounter.close();
        }
        if (teams != null) {
            teams.close(getServer().getOnlinePlayers());
        }
    }

    private OfflinePlayer findKnownPlayer(String name) {
        Player online = getServer().getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer cached = getServer().getOfflinePlayerIfCached(name);
        if (cached != null && cached.hasPlayedBefore()) return cached;
        for (OfflinePlayer player : getServer().getOfflinePlayers()) {
            if (name.equalsIgnoreCase(player.getName())) return player;
        }
        return null;
    }

    public static void message(CommandSender receiver, String text) {
        receiver.sendMessage(Component.text(PREFIX + text));
    }
}
