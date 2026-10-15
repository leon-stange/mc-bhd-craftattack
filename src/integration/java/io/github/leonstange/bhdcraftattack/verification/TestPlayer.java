package io.github.leonstange.bhdcraftattack.verification;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scoreboard.Scoreboard;

final class TestPlayer {
    final UUID id = UUID.randomUUID();
    final String name;
    final ItemStack[] slots = new ItemStack[41];
    final List<String> messages = new ArrayList<>();
    final PersistentDataContainer data = Bukkit.getItemFactory().getItemMeta(Material.ELYTRA).getPersistentDataContainer();
    final PlayerInventory inventory;
    final Player player;
    final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    boolean op;
    boolean dead;
    int deaths;
    int saves;
    Component listName;

    TestPlayer(String name, boolean op) {
        this.name = name;
        this.op = op;
        this.listName = Component.text(name);
        inventory = proxy(PlayerInventory.class, (method, args) -> switch (method) {
            case "getContents" -> slots.clone();
            case "getStorageContents" -> Arrays.copyOf(slots, 36);
            case "getArmorContents" -> Arrays.copyOfRange(slots, 36, 40);
            case "getSize" -> 41;
            case "getItem" -> slots[(int) args[0]];
            case "setItem" -> { slots[(int) args[0]] = (ItemStack) args[1]; yield null; }
            case "getChestplate" -> slots[38];
            case "setChestplate" -> { slots[38] = (ItemStack) args[0]; yield null; }
            case "firstEmpty" -> {
                int free = -1;
                for (int i = 0; i < 36; i++) if (slots[i] == null || slots[i].isEmpty()) { free = i; break; }
                yield free;
            }
            case "getHolder" -> null;
            default -> throw new UnsupportedOperationException("Inventory: " + method);
        });
        player = proxy(Player.class, (method, args) -> switch (method) {
            case "getUniqueId" -> id;
            case "getName" -> name;
            case "getInventory" -> inventory;
            case "getPersistentDataContainer" -> data;
            case "isOnline", "canSee" -> true;
            case "isDead" -> dead;
            case "isOp", "hasPermission", "isPermissionSet" -> this.op;
            case "getStatistic" -> deaths;
            case "setStatistic" -> { deaths = (int) args[1]; yield null; }
            case "hasPlayedBefore" -> true;
            case "playerListName" -> {
                if (args.length == 0) yield listName;
                listName = (Component) args[0];
                yield null;
            }
            case "getScoreboard" -> scoreboard;
            case "getServer" -> Bukkit.getServer();
            case "saveData" -> { saves++; yield null; }
            case "sendMessage" -> {
                for (Object value : args) {
                    if (value instanceof Component text) messages.add(PlainTextComponentSerializer.plainText().serialize(text));
                    else if (value instanceof String text) messages.add(text);
                    else if (value instanceof String[] texts) messages.addAll(List.of(texts));
                }
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player: " + method);
        });
    }

    void fill() {
        for (int i = 0; i < 36; i++) slots[i] = new ItemStack(Material.STONE, 64);
    }

    long elytras() {
        return Arrays.stream(slots).filter(item -> item != null && item.getType() == Material.ELYTRA).count();
    }

    interface Invocation { Object call(String method, Object[] args) throws Throwable; }

    static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object, method, args) -> {
            return switch (method.getName()) {
                case "toString" -> "Test" + type.getSimpleName();
                case "hashCode" -> System.identityHashCode(object);
                case "equals" -> object == args[0];
                default -> invocation.call(method.getName(), args == null ? new Object[0] : args);
            };
        }));
    }
}
