package com.ma4z.bettertpa;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BetterTPA extends JavaPlugin implements Listener, TabExecutor {

    private FileConfiguration cfg;
    private final Map<UUID, TPARequest> activeRequests = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.cfg = getConfig();

        getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("tpa")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpahere")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpcancel")).setExecutor(this);
        Objects.requireNonNull(getCommand("better-tpa")).setExecutor(this);

        getLogger().info("Hewlo from BetterTPA, Is this actually better than donutsmp one?");
    }

    @Override
    public void onDisable() {
        getLogger().info("BetterTPA disabled, i think so not :( atleast thanks for trying !");
        activeRequests.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "tpa":
                return handleTpa(player, args, RequestType.TO_TARGET);
            case "tpahere":
                return handleTpa(player, args, RequestType.TO_REQUESTER);
            case "tpaccept":
                return handleTpAccept(player);
            case "tpcancel":
                return handleCancel(player);
            case "better-tpa":
                return handleAdmin(player, args);
            default:
                return false;
        }
    }

    private boolean handleTpa(Player requester, String[] args, RequestType type) {
        if (args.length == 0) {
            requester.sendMessage(colorize("&cUsage: /" + (type == RequestType.TO_TARGET ? "tpa <player>" : "tpahere <player>")));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(colorize(cfg.getString("messages.player-not-online")));
            return true;
        }
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(colorize("&cYou cannot send a teleport request to yourself."));
            return true;
        }

        TPARequest req = new TPARequest(requester.getUniqueId(), target.getUniqueId(), type);
        activeRequests.put(requester.getUniqueId(), req);

        requester.sendMessage(colorize(cfg.getString("messages.request-sent")).replace("%target%", target.getName()));
        target.sendMessage(colorize(cfg.getString("messages.request-received")).replace("%player%", requester.getName()));

        // timeout
        int timeout = cfg.getInt("request-timeout", 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeRequests.get(requester.getUniqueId()) == req) {
                    activeRequests.remove(requester.getUniqueId());
                    Player reqPlayer = Bukkit.getPlayer(req.requester);
                    if (reqPlayer != null)
                        reqPlayer.sendMessage(colorize("&cYour teleport request expired."));
                }
            }
        }.runTaskLater(this, timeout * 20L);

        return true;
    }

    private boolean handleTpAccept(Player target) {
        UUID requesterId = findRequesterForTarget(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage(colorize(cfg.getString("messages.no-pending")));
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            target.sendMessage(colorize("&cThe requester is no longer online."));
            activeRequests.remove(requesterId);
            return true;
        }
        openAcceptMenu(target, requester);
        return true;
    }

    private boolean handleCancel(Player player) {
        if (activeRequests.remove(player.getUniqueId()) != null) {
            player.sendMessage(colorize(cfg.getString("messages.request-cancelled")));
        } else {
            player.sendMessage(colorize(cfg.getString("messages.no-pending")));
        }
        return true;
    }

    private boolean handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("bettertpa.reload") && !player.isOp()) {
            player.sendMessage(colorize("&cNo permission."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            this.cfg = getConfig();
            player.sendMessage(colorize("&aBetterTPA config reloaded."));
            return true;
        }
        player.sendMessage(colorize("&cUsage: /better-tpa reload"));
        return true;
    }

    private void openAcceptMenu(Player target, Player requester) {
        Inventory inv = Bukkit.createInventory(null, 9, "Teleport Request");

        ItemStack accept = createNamed(Material.LIME_WOOL, "[ACCEPT]");
        ItemStack deny = createNamed(Material.RED_WOOL, "[DENY]");

        inv.setItem(3, accept);
        inv.setItem(5, deny);

        target.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (title == null) return;

        if (title.equals("Teleport Request")) {
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem();
            if (it == null || !it.hasItemMeta()) return;

            UUID requesterId = findRequesterForTarget(clicker.getUniqueId());
            if (requesterId == null) {
                clicker.closeInventory();
                clicker.sendMessage(colorize(cfg.getString("messages.no-pending")));
                return;
            }
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester == null) {
                clicker.closeInventory();
                clicker.sendMessage(colorize("&cRequester went offline."));
                activeRequests.remove(requesterId);
                return;
            }

            String name = it.getItemMeta().getDisplayName();
            if (name.contains("[ACCEPT]")) {
                clicker.closeInventory();
                clicker.sendMessage(colorize(cfg.getString("messages.request-accepted")));
                requester.sendMessage(colorize("&e%player% accepted your teleport request.").replace("%player%", clicker.getName()));
                startCountdownAndTeleport(requester, clicker, activeRequests.get(requesterId));
            } else if (name.contains("[DENY]")) {
                clicker.closeInventory();
                requester.sendMessage(colorize(cfg.getString("messages.request-denied")));
                clicker.sendMessage(colorize(cfg.getString("messages.request-denied")));
                activeRequests.remove(requester.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID quitting = e.getPlayer().getUniqueId();
        List<UUID> toRemove = activeRequests.values().stream()
                .filter(r -> r.target.equals(quitting) || r.requester.equals(quitting))
                .map(r -> r.requester)
                .collect(Collectors.toList());
        toRemove.forEach(activeRequests::remove);
    }

    private void startCountdownAndTeleport(Player requester, Player target, TPARequest r) {
        int seconds = cfg.getInt("countdown-seconds", 5);

        new BukkitRunnable() {
            int time = seconds;

            @Override
            public void run() {
                if (!requester.isOnline() || !target.isOnline()) {
                    cancel();
                    if (requester.isOnline()) requester.sendMessage(colorize(cfg.getString("messages.teleport-cancelled")));
                    activeRequests.remove(r.requester);
                    return;
                }
                String action = colorize(cfg.getString("messages.countdown-start")).replace("%n%", String.valueOf(time));
                requester.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.stripColor(action)));

                if (time <= 0) {
                    requester.teleport(target.getLocation());
                    requester.sendMessage(colorize(cfg.getString("messages.teleport-complete")));
                    target.sendMessage(colorize("&a%player% teleported to you.").replace("%player%", requester.getName()));
                    activeRequests.remove(r.requester);
                    cancel();
                }
                time--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private UUID findRequesterForTarget(UUID target) {
        for (TPARequest r : activeRequests.values()) {
            if (r.target.equals(target)) return r.requester;
        }
        return null;
    }

    private ItemStack createNamed(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            it.setItemMeta(m);
        }
        return it;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static class TPARequest {
        final UUID requester;
        final UUID target;
        final RequestType type;

        TPARequest(UUID requester, UUID target, RequestType type) {
            this.requester = requester;
            this.target = target;
            this.type = type;
        }
    }

    private enum RequestType {
        TO_TARGET, TO_REQUESTER
    }
}