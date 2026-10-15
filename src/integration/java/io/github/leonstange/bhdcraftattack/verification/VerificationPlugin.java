package io.github.leonstange.bhdcraftattack.verification;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.github.leonstange.bhdcraftattack.BhdCraftAttackPlugin;
import io.github.leonstange.bhdcraftattack.CraftAttackCommands;
import io.github.leonstange.bhdcraftattack.DeathCounter;
import io.github.leonstange.bhdcraftattack.ElytraProtection;
import io.github.leonstange.bhdcraftattack.PersonalElytra;
import io.github.leonstange.bhdcraftattack.TeamNames;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.chat.ChatRenderer;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;

public final class VerificationPlugin extends JavaPlugin {
    private BhdCraftAttackPlugin main;
    private PersonalElytra elytras;
    private ElytraProtection protection;
    private int passed;
    private int failed;

    @Override
    public void onEnable() {
        main = (BhdCraftAttackPlugin) Bukkit.getPluginManager().getPlugin("BHD-CraftAttack");
        elytras = new PersonalElytra(main);
        protection = new ElytraProtection(elytras);
        Bukkit.getScheduler().runTaskLater(this, this::runTests, 20L);
    }

    private void runTests() {
        test("metadata and registration", () -> {
            require(main.isEnabled(), "Plugin enabled");
            require(main.getPluginMeta().getVersion().equals("0.0.1"), "Version");
            require(main.getCommand("bhd-craftattack") != null, "Command registered");
        });
        test("dark red unbreakable named elytra with lore and persistent owner", () -> {
            var player = new TestPlayer("Leon", false);
            ItemStack item = own(player);
            require(item.getItemMeta().isUnbreakable(), "Unbreakable");
            require(plain(item.getItemMeta().displayName()).equals("Elytra - Leon"), "Name");
            require(item.getItemMeta().displayName().color().equals(NamedTextColor.DARK_RED), "Dark red");
            require(plain(item.getItemMeta().lore().getFirst()).equals("Persönliche Elytra von Leon"), "Lore");
            require(elytras.isOwned(ItemStack.deserializeBytes(item.serializeAsBytes()), player.id), "Owner survives serialization");
        });
        test("ordinary renamed or invalid-owner elytra is not personal", () -> {
            ItemStack item = new ItemStack(Material.ELYTRA);
            item.editMeta(meta -> meta.displayName(Component.text("Elytra - Leon")));
            require(!elytras.isPersonal(item), "Name does not grant protection");
            item.editMeta(meta -> meta.getPersistentDataContainer().set(new NamespacedKey(main, "elytra-owner"), PersistentDataType.STRING, "invalid"));
            require(!elytras.isPersonal(item), "Malformed owner rejected");
            require(!elytras.isPersonal(null), "Empty slot");
        });
        test("first grant occurs once and state survives serialization", () -> {
            var player = new TestPlayer("First", false);
            require(elytras.grant(player.player, false) == PersonalElytra.GrantResult.GIVEN, "Initial grant");
            require(elytras.grant(player.player, false) == PersonalElytra.GrantResult.ALREADY_RECEIVED, "No duplicate");
            require(player.elytras() == 1 && player.saves == 1, "Saved together");
            var restored = new TestPlayer("First", false);
            restored.data.readFromBytes(player.data.serializeToBytes(), true);
            require(elytras.grant(restored.player, false) == PersonalElytra.GrantResult.ALREADY_RECEIVED, "Persisted first grant marker");
            require(restored.elytras() == 0, "No replacement on rejoin");
        });
        test("existing owned elytra repairs missing grant marker without duplicate", () -> {
            var player = new TestPlayer("Existing", false);
            player.slots[5] = own(player);
            elytras.grant(player.player, false);
            require(player.elytras() == 1 && elytras.hasReceived(player.player), "Existing item recognized");
        });
        test("full inventory defers initial grant safely", () -> {
            var player = new TestPlayer("Full", false);
            player.fill();
            require(elytras.grant(player.player, false) == PersonalElytra.GrantResult.INVENTORY_FULL, "No room");
            require(!elytras.hasReceived(player.player) && player.elytras() == 0, "No item or marker lost");
            player.slots[8] = null;
            require(elytras.grant(player.player, false) == PersonalElytra.GrantResult.GIVEN, "Grant after space freed");
        });
        for (int slot : new int[]{4, 38, 40}) {
            test("death retains own elytra in slot " + slot + " and respawn equips", () -> {
                var player = new TestPlayer("Death" + slot, false);
                player.slots[slot] = own(player);
                player.slots[1] = new ItemStack(Material.DIAMOND, 3);
                PlayerDeathEvent event = death(player);
                protection.onDeath(event);
                require(event.getItemsToKeep().size() == 1, "Only Elytra kept");
                require(event.getDrops().size() == 1 && event.getDrops().getFirst().getType() == Material.DIAMOND, "Other loot still drops");
                simulatePaperRetention(player, event);
                protection.onRespawn(new PlayerPostRespawnEvent(player.player, Bukkit.getWorlds().getFirst().getSpawnLocation(),
                        false, false, false, PlayerRespawnEvent.RespawnReason.DEATH));
                require(elytras.isOwned(player.slots[38], player.id) && player.elytras() == 1, "Equipped exactly once");
                elytras.equipAfterDeath(player.player);
                require(player.elytras() == 1, "Idempotent respawn");
            });
        }
        test("foreign and ordinary elytra follow normal death loot", () -> {
            var player = new TestPlayer("Foreign", false);
            player.slots[0] = elytras.create(UUID.randomUUID(), "Other");
            player.slots[1] = new ItemStack(Material.ELYTRA);
            PlayerDeathEvent event = death(player);
            protection.onDeath(event);
            require(event.getItemsToKeep().isEmpty() && event.getDrops().size() == 2, "No foreign items retained");
            Arrays.fill(player.slots, null);
            elytras.equipAfterDeath(player.player);
            require(player.elytras() == 0, "No replacement generated");
        });
        test("no carried elytra means no replacement or chest retrieval", () -> {
            var player = new TestPlayer("Stored", false);
            PlayerDeathEvent event = death(player);
            protection.onDeath(event);
            elytras.equipAfterDeath(player.player);
            require(player.elytras() == 0 && event.getItemsToKeep().isEmpty(), "No unsolicited item");
        });
        test("keepInventory does not duplicate and swaps chestplate with full storage", () -> {
            var player = new TestPlayer("Keep", false);
            player.fill();
            player.slots[7] = own(player);
            player.slots[38] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            PlayerDeathEvent event = death(player);
            event.setKeepInventory(true);
            protection.onDeath(event);
            require(event.getItemsToKeep().isEmpty(), "No additional kept copy");
            elytras.equipAfterDeath(player.player);
            require(elytras.isOwned(player.slots[38], player.id), "Auto-equipped");
            require(player.slots[7].getType() == Material.DIAMOND_CHESTPLATE && player.elytras() == 1, "Chestplate safely swapped");
        });
        test("identical replacements and already-kept item retain exact multiplicity", () -> {
            var player = new TestPlayer("Duplicates", false);
            player.slots[0] = own(player);
            player.slots[1] = player.slots[0].clone();
            PlayerDeathEvent event = death(player);
            event.getItemsToKeep().add(player.slots[0].clone());
            protection.onDeath(event);
            require(event.getItemsToKeep().size() == 2 && event.getDrops().isEmpty(), "Exactly two retained, none dropped");
            simulatePaperRetention(player, event);
            elytras.equipAfterDeath(player.player);
            require(player.elytras() == 2 && elytras.isOwned(player.slots[38], player.id), "No extra or lost replacement");
        });
        test("manual drop cancelled with exact message; ordinary item allowed", () -> {
            var player = new TestPlayer("Drop", false);
            Item drop = TestPlayer.proxy(Item.class, (method, args) -> own(player));
            var event = new PlayerDropItemEvent(player.player, drop);
            protection.onDrop(event);
            require(event.isCancelled(), "Drop cancelled");
            require(player.messages.equals(List.of(BhdCraftAttackPlugin.PREFIX + ElytraProtection.DROP_MESSAGE)), "Exact chat message");
            Item ordinary = TestPlayer.proxy(Item.class, (method, args) -> new ItemStack(Material.ELYTRA));
            var allowed = new PlayerDropItemEvent(player.player, ordinary);
            protection.onDrop(allowed);
            require(!allowed.isCancelled(), "Ordinary Elytra may drop");
        });
        test("outside inventory drop blocked and chest moves allowed", () -> {
            var player = new TestPlayer("Click", false);
            Inventory top = Bukkit.createInventory(null, 27);
            var view = view(player, top, own(player));
            var drop = new InventoryClickEvent(view, InventoryType.SlotType.OUTSIDE, -999, ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR);
            protection.onClick(drop);
            require(drop.isCancelled(), "Cursor drop blocked");
            var move = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PLACE_ALL);
            protection.onClick(move);
            require(!move.isCancelled(), "Chest placement allowed");
        });
        test("grindstone and anvil consumption protected including result click", () -> {
            var player = new TestPlayer("Process", false);
            Inventory grinder = processingInventory(GrindstoneInventory.class, InventoryType.GRINDSTONE);
            grinder.setItem(0, own(player));
            var grind = new PrepareGrindstoneEvent(view(player, grinder, null), new ItemStack(Material.ELYTRA));
            protection.onGrindstone(grind);
            require(grind.getResult() == null, "Grindstone has no result");
            var take = new InventoryClickEvent(view(player, grinder, null), InventoryType.SlotType.RESULT, 2, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
            protection.onClick(take);
            require(take.isCancelled(), "Shift result blocked");
            Inventory anvil = processingInventory(AnvilInventory.class, InventoryType.ANVIL);
            anvil.setItem(1, own(player));
            var repair = new PrepareAnvilEvent((AnvilView) view(player, anvil, null), new ItemStack(Material.ELYTRA));
            protection.onAnvil(repair);
            require(repair.getResult() == null, "Personal Elytra not consumed as material");
            var takeAnvil = new InventoryClickEvent(view(player, anvil, null), InventoryType.SlotType.RESULT,
                    2, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
            protection.onClick(takeAnvil);
            require(takeAnvil.isCancelled(), "Anvil result also blocked");
            anvil.setItem(1, new ItemStack(Material.PHANTOM_MEMBRANE));
            var ordinary = new PrepareAnvilEvent((AnvilView) view(player, anvil, null), new ItemStack(Material.ELYTRA));
            protection.onAnvil(ordinary);
            require(ordinary.getResult() != null, "Ordinary repair unaffected");
        });
        test("crafting personal Elytra is blocked without affecting other recipes", () -> {
            var player = new TestPlayer("Craft", false);
            ItemStack[] matrix = {own(player)};
            ItemStack[] result = {new ItemStack(Material.ELYTRA)};
            CraftingInventory crafting = TestPlayer.proxy(CraftingInventory.class, (method, args) -> switch (method) {
                case "getMatrix" -> matrix;
                case "setResult" -> { result[0] = (ItemStack) args[0]; yield null; }
                default -> null;
            });
            var view = view(player, crafting, null);
            protection.onPrepareCraft(new PrepareItemCraftEvent(crafting, view, true));
            require(result[0] == null, "Craft preview blocked");
            var recipe = new ShapelessRecipe(new NamespacedKey(this, "test"), new ItemStack(Material.ELYTRA)).addIngredient(Material.ELYTRA);
            var craft = new CraftItemEvent(recipe, view, InventoryType.SlotType.RESULT, 0, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
            protection.onCraft(craft);
            require(craft.isCancelled(), "Shift crafting blocked");
            matrix[0] = new ItemStack(Material.ELYTRA);
            result[0] = new ItemStack(Material.ELYTRA);
            protection.onPrepareCraft(new PrepareItemCraftEvent(crafting, view, true));
            require(result[0] != null, "Ordinary recipe unaffected");
        });
        commandTests();
        teamAndDeathTests();
        test("automatic crafter cannot consume personal Elytra", () -> {
            var player = new TestPlayer("AutoCraft", false);
            Inventory storage = Bukkit.createInventory(null, 9);
            storage.setItem(0, own(player));
            Crafter state = TestPlayer.proxy(Crafter.class, (method, args) -> storage);
            Block block = TestPlayer.proxy(Block.class, (method, args) -> state);
            var recipe = new ShapelessRecipe(new NamespacedKey(this, "automatic"), new ItemStack(Material.ELYTRA)).addIngredient(Material.ELYTRA);
            var blocked = new CrafterCraftEvent(block, recipe, new ItemStack(Material.ELYTRA));
            protection.onAutoCraft(blocked);
            require(blocked.isCancelled(), "Automatic personal repair blocked");
            storage.setItem(0, new ItemStack(Material.ELYTRA));
            var allowed = new CrafterCraftEvent(block, recipe, new ItemStack(Material.ELYTRA));
            protection.onAutoCraft(allowed);
            require(!allowed.isCancelled(), "Ordinary automatic recipe allowed");
        });
        test("optional death counter uses persistent vanilla stats and restores prior tab objective", () -> {
            var player = new TestPlayer("Stats", false);
            var previous = player.scoreboard.registerNewObjective("previous", Criteria.DUMMY, Component.text("Previous"));
            previous.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            var counter = new DeathCounter();
            player.deaths = 7;
            counter.update(List.of(player.player));
            require(player.scoreboard.getObjective(DisplaySlot.PLAYER_LIST).getScore(player.name).getScore() == 7, "Vanilla statistic shown");
            player.deaths = 8;
            counter.update(List.of(player.player));
            require(player.scoreboard.getObjective(DisplaySlot.PLAYER_LIST).getScore(player.name).getScore() == 8, "New death shown");
            counter.close();
            require(player.scoreboard.getObjective(DisplaySlot.PLAYER_LIST).getName().equals(previous.getName()), "Previous display restored");
        });
        var joining = new TestPlayer("Join", false);
        test("death counter resumes an objective left by a hard server stop", () -> {
            var player = new TestPlayer("RestartStats", false);
            var existing = player.scoreboard.registerNewObjective("bhd_ca_deaths", Criteria.DUMMY, Component.text("Tode"));
            existing.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            existing.getScore(player.name).setScore(2);
            player.deaths = 9;
            var counter = new DeathCounter();
            counter.update(List.of(player.player));
            require(existing.getScore(player.name).getScore() == 9, "Existing objective refreshed");
            counter.close();
        });
        main.onJoin(new PlayerJoinEvent(joining.player, Component.empty()));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            test("every join greets exactly and repeat joins do not duplicate Elytra", () -> {
                require(joining.messages.equals(List.of(BhdCraftAttackPlugin.PREFIX + "Willkommen auf dem Server!")), "First welcome");
                require(joining.elytras() == 1, "First join item");
                main.onJoin(new PlayerJoinEvent(joining.player, Component.empty()));
            });
            Bukkit.getScheduler().runTaskLater(this, () -> {
                test("repeat join welcome and no second grant", () -> {
                    require(joining.messages.size() == 2 && joining.elytras() == 1, "Every join greeted; grant once");
                });
                if (failed == 0) getLogger().info("BHD_CRAFTATTACK_TESTS_OK " + passed);
                else getLogger().severe("BHD_CRAFTATTACK_TEST_FAILED total=" + failed);
                Bukkit.shutdown();
            }, 2L);
        }, 2L);
    }

    private void commandTests() {
        test("public rules, version permission and tab completion", () -> {
            var guest = new TestPlayer("Guest", false);
            var op = new TestPlayer("Operator", true);
            var commands = commands(List.of(guest, op));
            execute(commands, guest, "regeln");
            require(guest.messages.size() == 9 && guest.messages.getFirst().endsWith("Serverregeln"), "All rules public");
            guest.messages.clear();
            execute(commands, guest, "version");
            require(guest.messages.equals(List.of(BhdCraftAttackPlugin.PREFIX + "keine Berechtigung.")), "Version protected");
            execute(commands, op, "version");
            require(op.messages.getLast().equals(BhdCraftAttackPlugin.PREFIX + "Version v0.0.1"), "Version from plugin metadata");
            require(commands.onTabComplete(guest.player, main.getCommand("bhd-craftattack"), "bhd-craftattack", new String[]{""}).equals(List.of("team", "regeln")), "Only public suggestions");
        });
        test("operator grant targets receiver, warns on duplicate and never regrants on join", () -> {
            var target = new TestPlayer("Target", false);
            var op = new TestPlayer("Operator", true);
            var commands = commands(List.of(target, op));
            execute(commands, target, "elytra", target.name);
            require(target.elytras() == 0, "Non-operator blocked");
            execute(commands, op, "elytra", target.name);
            require(target.elytras() == 1 && elytras.isOwned(target.slots[0], target.id), "Receiver owns grant");
            require(!elytras.isOwned(target.slots[0], op.id), "Not operator's item");
            execute(commands, op, "elytra", target.name);
            require(op.messages.stream().anyMatch(line -> line.contains("bereits")), "Duplicate warning");
            require(target.elytras() == 2, "Explicit manual replacement still granted");
            elytras.grant(target.player, false);
            require(target.elytras() == 2, "Manual grant records first receipt");
        });
        test("manual grant rejects full, offline and dead targets", () -> {
            var target = new TestPlayer("FullTarget", false);
            var op = new TestPlayer("Operator", true);
            var commands = commands(List.of(target, op));
            target.fill();
            execute(commands, op, "elytra", target.name);
            require(target.elytras() == 0 && !elytras.hasReceived(target.player), "Full target unchanged");
            execute(commands, op, "elytra", "Missing");
            require(op.messages.getLast().contains("nicht online"), "Offline message");
            target.dead = true;
            execute(commands, op, "elytra", target.name);
            require(op.messages.getLast().contains("respawnen"), "Dead target rejected");
        });
        test("configured rules are used rather than hardcoded messages", () -> {
            var target = new TestPlayer("Rules", false);
            var original = main.getConfig().getStringList("rules");
            try {
                main.getConfig().set("rules", List.of("Unsere Testregel"));
                execute(commands(List.of(target)), target, "regeln");
                require(target.messages.getLast().equals("1. Unsere Testregel"), "Config read");
            } finally {
                main.getConfig().set("rules", original);
            }
        });
    }

    private void teamAndDeathTests() {
        test("death reset is operator-only and changes only the target statistic", () -> {
            var op = new TestPlayer("DeathOperator", true);
            var target = new TestPlayer("DeathTarget", false);
            op.deaths = 4;
            target.deaths = 12;
            var commands = commands(List.of(op, target));
            execute(commands, target, "death", "reset", target.name);
            require(target.deaths == 12 && target.saves == 0, "Non-operator cannot reset");
            execute(commands, op, "death", "reset", target.name);
            require(target.deaths == 0 && target.saves == 1 && op.deaths == 4, "Only requested target reset and saved");
            var counter = new DeathCounter();
            counter.update(List.of(op.player, target.player));
            require(target.scoreboard.getObjective(DisplaySlot.PLAYER_LIST).getScore(target.name).getScore() == 0, "Tab shows reset");
            counter.close();
            execute(commands, op, "death", "reset", "Unknown");
            require(op.messages.getLast().contains("Unbekannter Spieler"), "Unknown target rejected");
        });
        test("death reset persists in actual offline Minecraft statistics", () -> {
            var op = new TestPlayer("OfflineOperator", true);
            UUID id = UUID.randomUUID();
            var offline = Bukkit.getOfflinePlayer(id);
            offline.setStatistic(org.bukkit.Statistic.DEATHS, 6);
            var commands = new CraftAttackCommands(main, elytras, new TeamNames(main), name -> null,
                    name -> name.equals("OfflineTarget") ? offline : null, () -> List.of(op.player));
            execute(commands, op, "death", "reset", "OfflineTarget");
            require(Bukkit.getOfflinePlayer(id).getStatistic(org.bukkit.Statistic.DEATHS) == 0, "Saved offline statistic is zero");
        });
        test("players choose their own team, prefix is literal text", () -> {
            var player = new TestPlayer("TeamPlayer", false);
            var teams = new TeamNames(main);
            execute(commands(List.of(player), teams), player, "team", "<red>Ä&4%[X]");
            require(teams.get(player.player).equals("<red>Ä&4%[X]"), "Free team string");
            require(plain(player.listName).equals("[<red>Ä&4%[X]] TeamPlayer"), "Exact tab prefix, no formatting injection");
            require(player.saves == 1, "Team persisted");
        });
        test("quoted multiword self team and empty names", () -> {
            var player = new TestPlayer("Multiword", false);
            var teams = new TeamNames(main);
            var commands = commands(List.of(player), teams);
            execute(commands, player, "team", "\"Die", "Baumeister\"");
            require(teams.get(player.player).equals("Die Baumeister"), "Quoted self name with spaces");
            execute(commands, player, "team", "\"\"");
            execute(commands, player, "team", "\"");
            require(teams.get(player.player).equals("Die Baumeister"), "Invalid empty names leave previous team intact");
            execute(commands, player, "team", "reset");
            require(teams.get(player.player).equals("reset"), "Self name reset is allowed");
        });
        test("non-operators cannot change or reset another player's team", () -> {
            var alice = new TestPlayer("Alice", false);
            var bob = new TestPlayer("Bob", false);
            var teams = new TeamNames(main);
            teams.set(bob.player, "Blau");
            var commands = commands(List.of(alice, bob), teams);
            execute(commands, alice, "team", bob.name, "Rot");
            execute(commands, alice, "team", bob.name, "reset");
            require(teams.get(bob.player).equals("Blau") && teams.get(alice.player) == null, "No unauthorized change");
            require(alice.messages.stream().allMatch(line -> line.endsWith("keine Berechtigung.")), "Permission error");
        });
        test("operator assigns and resets target team without altering own team", () -> {
            var op = new TestPlayer("TeamOperator", true);
            var target = new TestPlayer("TeamTarget", false);
            var teams = new TeamNames(main);
            teams.set(op.player, "Operatoren");
            var commands = commands(List.of(op, target), teams);
            execute(commands, op, "team", target.name, "Die", "Bauer");
            require(plain(target.listName).equals("[Die Bauer] TeamTarget"), "Target tab updated immediately");
            require(teams.get(op.player).equals("Operatoren"), "Operator team unchanged");
            execute(commands, op, "team", target.name, "reset");
            require(teams.get(target.player) == null && plain(target.listName).equals(target.name), "Reset restores normal name");
            execute(commands, op, "team", "Missing", "Rot");
            require(op.messages.getLast().contains("nicht online"), "Offline team target rejected");
        });
        test("team survives PDC serialization, rejoin and respawn without duplicate prefix", () -> {
            var player = new TestPlayer("PersistentTeam", false);
            var teams = new TeamNames(main);
            teams.set(player.player, "BHD");
            byte[] saved = player.data.serializeToBytes();
            teams.unload(player.player);
            teams.load(player.player);
            teams.load(player.player);
            require(plain(player.listName).equals("[BHD] PersistentTeam"), "Rejoin without nested prefix");
            teams.onRespawn(new PlayerPostRespawnEvent(player.player, Bukkit.getWorlds().getFirst().getSpawnLocation(),
                    false, false, false, PlayerRespawnEvent.RespawnReason.DEATH));
            require(plain(player.listName).equals("[BHD] PersistentTeam"), "Respawn restores prefix");
            var restored = new TestPlayer("PersistentTeam", false);
            restored.data.readFromBytes(saved, true);
            new TeamNames(main).load(restored.player);
            require(plain(restored.listName).equals("[BHD] PersistentTeam"), "Reload saved player data");
        });
        test("chat prefixes sender name and preserves original renderer and message", () -> {
            var player = new TestPlayer("ChatPlayer", false);
            var teams = new TeamNames(main);
            teams.set(player.player, "Baumeister");
            ChatRenderer renderer = (source, displayName, message, viewer) ->
                    Component.text("Chat: ").append(displayName).append(Component.text(": ")).append(message);
            var message = Component.text("Hallo <red>Welt");
            var event = new AsyncChatEvent(false, player.player, new java.util.HashSet<>(), renderer, message, message,
                    net.kyori.adventure.chat.SignedMessage.system("Hallo <red>Welt", message));
            teams.onChat(event);
            var rendered = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                    event.renderer().render(player.player, Component.text(player.name), message, player.player))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
            require(plain(rendered).equals("Chat: [Baumeister] ChatPlayer: Hallo <red>Welt"), "Prefix around original renderer");
            require(event.message().equals(message) && !event.isCancelled(), "Chat content and delivery unchanged");
        });
        test("cleared or absent team leaves chat renderer untouched", () -> {
            var player = new TestPlayer("PlainChat", false);
            var teams = new TeamNames(main);
            teams.set(player.player, "Old");
            teams.clear(player.player);
            ChatRenderer renderer = ChatRenderer.defaultRenderer();
            var message = Component.text("Hallo");
            var event = new AsyncChatEvent(false, player.player, new java.util.HashSet<>(), renderer, message, message,
                    net.kyori.adventure.chat.SignedMessage.system("Hallo", message));
            teams.onChat(event);
            require(event.renderer() == renderer && plain(player.listName).equals(player.name), "No stale prefix");
        });
        test("team and death command suggestions respect permissions", () -> {
            var op = new TestPlayer("SuggestOperator", true);
            var guest = new TestPlayer("SuggestGuest", false);
            var commands = commands(List.of(op, guest));
            require(commands.onTabComplete(op.player, main.getCommand("bhd-craftattack"), "bhd-craftattack", new String[]{"death", ""}).equals(List.of("reset")), "Death subcommand");
            require(commands.onTabComplete(guest.player, main.getCommand("bhd-craftattack"), "bhd-craftattack", new String[]{"team", ""}).isEmpty(), "No administrative target suggestions");
            require(commands.onTabComplete(op.player, main.getCommand("bhd-craftattack"), "bhd-craftattack", new String[]{"team", guest.name, "r"}).equals(List.of("reset")), "Reset suggestion");
        });
    }

    private CraftAttackCommands commands(List<TestPlayer> players) {
        return commands(players, new TeamNames(main));
    }

    private CraftAttackCommands commands(List<TestPlayer> players, TeamNames teams) {
        return new CraftAttackCommands(main, elytras, teams,
                name -> players.stream().filter(p -> p.name.equalsIgnoreCase(name)).map(p -> p.player).findFirst().orElse(null),
                name -> players.stream().filter(p -> p.name.equalsIgnoreCase(name)).map(p -> p.player).findFirst().orElse(null),
                () -> players.stream().map(p -> p.player).toList());
    }

    private void execute(CraftAttackCommands commands, TestPlayer sender, String... args) {
        require(commands.onCommand(sender.player, main.getCommand("bhd-craftattack"), "bhd-craftattack", args), "Handled command");
    }

    private ItemStack own(TestPlayer player) { return elytras.create(player.id, player.name); }

    private PlayerDeathEvent death(TestPlayer player) {
        return new PlayerDeathEvent(player.player, DamageSource.builder(DamageType.GENERIC).build(),
                new ArrayList<>(Arrays.stream(player.slots).filter(item -> item != null).map(ItemStack::clone).toList()),
                0, (Component) null, true);
    }

    private void simulatePaperRetention(TestPlayer player, PlayerDeathEvent event) {
        Arrays.fill(player.slots, null);
        for (int i = 0; i < event.getItemsToKeep().size(); i++) player.slots[i] = event.getItemsToKeep().get(i).clone();
    }

    private InventoryView view(TestPlayer player, Inventory top, ItemStack cursor) {
        Class<? extends InventoryView> type = top instanceof AnvilInventory ? AnvilView.class : InventoryView.class;
        return TestPlayer.proxy(type, (method, args) -> switch (method) {
            case "getTopInventory" -> top;
            case "getBottomInventory" -> player.inventory;
            case "getPlayer" -> player.player;
            case "getCursor" -> cursor;
            case "convertSlot" -> args[0];
            case "getInventory" -> (int) args[0] < 0 ? null : top;
            default -> null;
        });
    }

    private <T extends Inventory> T processingInventory(Class<T> type, InventoryType inventoryType) {
        Inventory storage = Bukkit.createInventory(null, 9);
        return TestPlayer.proxy(type, (method, args) -> switch (method) {
            case "getItem" -> storage.getItem((int) args[0]);
            case "setItem" -> { storage.setItem((int) args[0], (ItemStack) args[1]); yield null; }
            case "getResult" -> storage.getItem(2);
            case "setResult" -> { storage.setItem(2, (ItemStack) args[0]); yield null; }
            case "getContents" -> storage.getContents();
            case "getSize" -> 3;
            case "getType" -> inventoryType;
            default -> null;
        });
    }

    private static String plain(Component text) { return PlainTextComponentSerializer.plainText().serialize(text); }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private interface Test { void run() throws Exception; }
    private void test(String name, Test test) {
        try {
            test.run();
            passed++;
            getLogger().info("BHD_TEST: OK " + name);
        } catch (Throwable error) {
            failed++;
            getLogger().log(java.util.logging.Level.SEVERE, "BHD_CRAFTATTACK_TEST_FAILED " + name, error);
        }
    }
}
