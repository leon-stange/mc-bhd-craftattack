package io.github.leonstange.bhdcraftattack;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PersonalElytra {
    public enum GrantResult { GIVEN, ALREADY_RECEIVED, INVENTORY_FULL }

    private final NamespacedKey ownerKey;
    private final NamespacedKey receivedKey;
    private final NamespacedKey equipKey;

    public PersonalElytra(JavaPlugin plugin) {
        ownerKey = new NamespacedKey(plugin, "elytra-owner");
        receivedKey = new NamespacedKey(plugin, "elytra-received");
        equipKey = new NamespacedKey(plugin, "equip-after-death");
    }

    public ItemStack create(UUID owner, String name) {
        ItemStack item = new ItemStack(Material.ELYTRA);
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.displayName(Component.text("Elytra - " + name, NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Persönliche Elytra von " + name, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
        });
        return item;
    }

    public boolean isPersonal(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return false;
        }
        String owner = item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner == null) {
            return false;
        }
        try {
            return UUID.fromString(owner).toString().equalsIgnoreCase(owner);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isOwned(ItemStack item, UUID owner) {
        return isPersonal(item) && owner.toString().equalsIgnoreCase(
                item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
    }

    public boolean hasReceived(Player player) {
        return player.getPersistentDataContainer().has(receivedKey, PersistentDataType.BYTE);
    }

    public boolean carriesOwn(Player player) {
        return findOwnSlot(player) >= 0;
    }

    public GrantResult grant(Player player, boolean manual) {
        if (!manual && hasReceived(player)) {
            return GrantResult.ALREADY_RECEIVED;
        }
        // Recover an existing personal item without issuing another one.
        if (!manual && carriesOwn(player)) {
            markReceived(player);
            return GrantResult.ALREADY_RECEIVED;
        }
        int slot = player.getInventory().firstEmpty();
        if (slot < 0 || slot >= player.getInventory().getStorageContents().length) {
            return GrantResult.INVENTORY_FULL;
        }
        player.getInventory().setItem(slot, create(player.getUniqueId(), player.getName()));
        markReceived(player);
        return GrantResult.GIVEN;
    }

    private void markReceived(Player player) {
        player.getPersistentDataContainer().set(receivedKey, PersistentDataType.BYTE, (byte) 1);
        player.saveData();
    }

    public void markForEquip(Player player, boolean pending) {
        if (pending) {
            player.getPersistentDataContainer().set(equipKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            player.getPersistentDataContainer().remove(equipKey);
        }
    }

    public void equipAfterDeath(Player player) {
        if (!player.getPersistentDataContainer().has(equipKey, PersistentDataType.BYTE) || player.isDead()) {
            return;
        }
        int slot = findOwnSlot(player);
        if (slot < 0) {
            markForEquip(player, false);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (!isOwned(inventory.getChestplate(), player.getUniqueId())) {
            ItemStack elytra = inventory.getItem(slot);
            ItemStack previousChestplate = inventory.getChestplate();
            // Swapping into the source slot is lossless even with a full inventory.
            inventory.setItem(slot, previousChestplate);
            inventory.setChestplate(elytra);
        }
        markForEquip(player, false);
        player.saveData();
    }

    private int findOwnSlot(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isOwned(inventory.getItem(slot), player.getUniqueId())) {
                return slot;
            }
        }
        return -1;
    }
}
