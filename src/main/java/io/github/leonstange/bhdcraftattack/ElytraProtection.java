package io.github.leonstange.bhdcraftattack;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;

public final class ElytraProtection implements Listener {
    public static final String DROP_MESSAGE = "Du kannst deine Elytra in einer Kiste ablegen, aber nicht droppen.";
    private final PersonalElytra elytras;

    public ElytraProtection(PersonalElytra elytras) {
        this.elytras = elytras;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> own = Arrays.stream(player.getInventory().getContents())
                .filter(item -> elytras.isOwned(item, player.getUniqueId())).toList();
        elytras.markForEquip(player, !own.isEmpty());
        if (own.isEmpty()) {
            return;
        }
        event.getDrops().removeIf(item -> elytras.isOwned(item, player.getUniqueId()));
        if (!event.getKeepInventory()) {
            // Match multiplicities so identical manual replacements cannot duplicate or vanish.
            List<ItemStack> alreadyKept = new ArrayList<>(event.getItemsToKeep());
            for (ItemStack item : own) {
                if (!alreadyKept.remove(item)) {
                    event.getItemsToKeep().add(item.clone());
                }
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerPostRespawnEvent event) {
        elytras.equipAfterDeath(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (elytras.isPersonal(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            BhdCraftAttackPlugin.message(event.getPlayer(), DROP_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE && elytras.isPersonal(event.getCursor())) {
            event.setCancelled(true);
            BhdCraftAttackPlugin.message(event.getWhoClicked(), DROP_MESSAGE);
            return;
        }
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        var inventory = event.getView().getTopInventory();
        if (inventory instanceof GrindstoneInventory
                && (elytras.isPersonal(inventory.getItem(0)) || elytras.isPersonal(inventory.getItem(1)))
                || inventory instanceof AnvilInventory && elytras.isPersonal(inventory.getItem(1))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrindstone(PrepareGrindstoneEvent event) {
        if (elytras.isPersonal(event.getInventory().getItem(0))
                || elytras.isPersonal(event.getInventory().getItem(1))) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        if (elytras.isPersonal(event.getInventory().getItem(1))) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (Arrays.stream(event.getInventory().getMatrix()).anyMatch(elytras::isPersonal)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (Arrays.stream(event.getInventory().getMatrix()).anyMatch(elytras::isPersonal)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAutoCraft(CrafterCraftEvent event) {
        if (event.getBlock().getState() instanceof Crafter crafter
                && Arrays.stream(crafter.getInventory().getContents()).anyMatch(elytras::isPersonal)) {
            event.setCancelled(true);
        }
    }
}
