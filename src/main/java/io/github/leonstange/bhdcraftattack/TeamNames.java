package io.github.leonstange.bhdcraftattack;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeamNames implements Listener {
    private final NamespacedKey key;
    private final Map<UUID, Component> prefixes = new ConcurrentHashMap<>();
    private final Map<UUID, Component> originalListNames = new HashMap<>();
    private final Map<UUID, Component> appliedListNames = new HashMap<>();

    public TeamNames(JavaPlugin plugin) {
        key = new NamespacedKey(plugin, "team-name");
    }

    public String get(Player player) {
        return player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public void set(Player player, String name) {
        if (name.isBlank() || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Bitte einen nicht leeren Teamnamen ohne Zeilenumbrüche eingeben.");
        }
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, name);
        load(player);
        player.saveData();
    }

    public void clear(Player player) {
        player.getPersistentDataContainer().remove(key);
        unload(player);
        player.saveData();
    }

    public void load(Player player) {
        String name = get(player);
        if (name == null) {
            unload(player);
            return;
        }
        UUID id = player.getUniqueId();
        if (!originalListNames.containsKey(id)) {
            originalListNames.put(id, player.playerListName());
        }
        Component prefix = Component.text("[" + name + "] ");
        Component listName = prefix.append(Component.text(player.getName()));
        prefixes.put(id, prefix);
        appliedListNames.put(id, listName);
        player.playerListName(listName);
    }

    public void unload(Player player) {
        UUID id = player.getUniqueId();
        prefixes.remove(id);
        Component applied = appliedListNames.remove(id);
        if (applied != null && applied.equals(player.playerListName())) {
            player.playerListName(originalListNames.get(id));
        }
        originalListNames.remove(id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // Only immutable cached components are read on the asynchronous chat thread.
        Component prefix = prefixes.get(event.getPlayer().getUniqueId());
        if (prefix != null) {
            var original = event.renderer();
            event.renderer((source, displayName, message, viewer) ->
                    original.render(source, prefix.append(displayName), message, viewer));
        }
    }

    @EventHandler
    public void onRespawn(PlayerPostRespawnEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unload(event.getPlayer());
    }

    public void close(Collection<? extends Player> players) {
        players.forEach(this::unload);
        prefixes.clear();
        originalListNames.clear();
        appliedListNames.clear();
    }
}
