package net.woek.Hat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class HatHandler implements CommandExecutor, Listener {

    private final boolean messagesEnabled;
    private final String setMessage;
    private final String stackSizeMessage;
    private final String noPermissionMessage;
    private final String consoleMessage;
    private final Hat instance;

    public HatHandler(Hat instance, boolean enabled, String set, String stacksize, String nopermission, String console) {
        this.instance = instance;
        this.messagesEnabled = enabled;
        this.setMessage = ChatColor.translateAlternateColorCodes('&', set);
        this.stackSizeMessage = ChatColor.translateAlternateColorCodes('&', stacksize);
        this.noPermissionMessage = ChatColor.translateAlternateColorCodes('&', nopermission);
        this.consoleMessage = ChatColor.translateAlternateColorCodes('&', console);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(consoleMessage);
            return true;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack handItem = inventory.getItemInMainHand();
        ItemStack helmet = inventory.getHelmet();

        if (checkValidHat(player, handItem)) {
            inventory.setHelmet(handItem);
            inventory.setItemInMainHand(helmet);
            player.updateInventory();
            if (messagesEnabled) {
                player.sendMessage(setMessage);
            }
        }
        return true;
    }

    @EventHandler
    public void onClickInHelmetSlot(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) ||
                event.getInventory().getType() != InventoryType.CRAFTING ||
                event.getRawSlot() != 5 ||
                event.getCurrentItem() == null ||
                event.getCurrentItem().getType() == Material.AIR ||
                event.getCurrentItem().getType().getEquipmentSlot() == EquipmentSlot.HEAD) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem().clone();
        ItemStack currentHelmet = player.getInventory().getHelmet() != null ?
                player.getInventory().getHelmet().clone() : new ItemStack(Material.AIR);

        if (checkValidHat(player, clickedItem)) {
            event.setCancelled(true);
            scheduleHatUpdate(player, clickedItem, currentHelmet);
        }
    }

    private void scheduleHatUpdate(Player player, ItemStack newHelmet, ItemStack oldItem) {
        Runnable updateTask = () -> {
            try {
                player.getInventory().setHelmet(newHelmet);
                player.setItemOnCursor(oldItem);
                if (messagesEnabled) {
                    player.sendMessage(setMessage);
                }
            } catch (Exception e) {
                instance.getLogger().warning("Ошибка при обновлении шляпы: " + e.getMessage());
            }
        };

        if (instance.isFolia()) {
            try {
                Bukkit.getScheduler().runTask(instance, updateTask);
            } catch (Exception e) {
                updateTask.run();
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateTask.run();
                }
            }.runTaskLater(instance, 1L);
        }
    }

    private boolean checkValidHat(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }

        if (!player.hasPermission("hat." + item.getType().name().toLowerCase())) {
            if (messagesEnabled) {
                player.sendMessage(noPermissionMessage);
            }
            return false;
        }

        if (item.getAmount() != 1) {
            if (messagesEnabled) {
                player.sendMessage(stackSizeMessage);
            }
            return false;
        }

        return true;
    }
}