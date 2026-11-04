package com.enchantmentevent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;

public final class EnchantmentEventPlugin extends org.bukkit.plugin.java.JavaPlugin implements Listener {
    private static final String UNIQUE_ENCHANTMENTS_PATH = "unique-enchantments";
    private static final String REMOVED_ENCHANTMENTS_PATH = "removed-enchantments";
    private static final String SWEEPING_EDGE = "SWEEPING_EDGE";
    private static final String PUNCH = "ARROW_KNOCKBACK";
    private static final String INFINITY = "ARROW_INFINITE";

    private final Random random = new Random();
    private final Set<String> uniqueEnchantments = new HashSet<String>();
    private final Set<String> removedEnchantments = new HashSet<String>();
    private final Set<Integer> unstableItems = new HashSet<Integer>();
    private boolean applyingSweepDamage;
    private BukkitTask startRollTask;
    private boolean eventRunning;
    private boolean killRollArmed;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        uniqueEnchantments.addAll(getConfig().getStringList(UNIQUE_ENCHANTMENTS_PATH));
        removedEnchantments.addAll(getConfig().getStringList(REMOVED_ENCHANTMENTS_PATH));
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("enchantmentevent").setExecutor((sender, command, label, args) -> handleCommand(sender, args));
        discoverExistingEnchantments();
        getLogger().info("Enchantment Event enabled with " + getAvailableEnchantments().size() + " available enchantments.");
    }

    @Override
    public void onDisable() {
        if (startRollTask != null) {
            startRollTask.cancel();
        }
        persistUniqueEnchantments();
    }

    private boolean handleCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!sender.hasPermission("enchantmentevent.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to manage this event.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /enchantmentevent <start|stop|killroll|worldroll|status>");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if ("start".equals(subcommand)) {
            startEvent(sender);
        } else if ("stop".equals(subcommand)) {
            stopEvent(sender);
        } else if ("killroll".equals(subcommand)) {
            killRollArmed = true;
            sender.sendMessage(ChatColor.GREEN + "The next player kill will grant an available enchantment.");
        } else if ("worldroll".equals(subcommand)) {
            int granted = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (grantRandomEnchantment(player)) {
                    granted++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "World roll complete. Enchantments granted: " + granted + ".");
        } else if ("status".equals(subcommand)) {
            sender.sendMessage(ChatColor.GOLD + "Event: " + (eventRunning ? "running" : "stopped")
                    + "; kill roll: " + (killRollArmed ? "armed" : "not armed")
                    + "; available enchantments: " + getAvailableEnchantments().size() + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /enchantmentevent <start|stop|killroll|worldroll|status>");
        }
        return true;
    }

    private void startEvent(org.bukkit.command.CommandSender sender) {
        if (eventRunning) {
            sender.sendMessage(ChatColor.RED + "The event is already running.");
            return;
        }
        eventRunning = true;
        int min = Math.max(0, getConfig().getInt("start-roll-minutes", 5));
        int max = Math.max(min, getConfig().getInt("start-roll-max-minutes", 15));
        int delayMinutes = min + random.nextInt(max - min + 1);
        startRollTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!eventRunning) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                grantRandomEnchantment(player);
            }
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "The Enchantment Event has begun! Online players received their rolls.");
            startRollTask = null;
        }, delayMinutes * 60L * 20L);
        sender.sendMessage(ChatColor.GREEN + "Event started. The opening roll is scheduled in " + delayMinutes + " minutes.");
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "The Enchantment Event is now active!");
    }

    private void stopEvent(org.bukkit.command.CommandSender sender) {
        if (startRollTask != null) {
            startRollTask.cancel();
            startRollTask = null;
        }
        eventRunning = false;
        killRollArmed = false;
        sender.sendMessage(ChatColor.GREEN + "The event and any pending rolls have been stopped.");
    }

    private boolean grantRandomEnchantment(Player player) {
        List<Enchantment> available = getAvailableEnchantments();
        if (available.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No unique enchantments remain available.");
            return false;
        }
        Enchantment enchantment = available.get(random.nextInt(available.size()));
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && heldItem.getType() != Material.AIR) {
            heldItem.addUnsafeEnchantment(enchantment, enchantment.getStartLevel());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You received " + displayName(enchantment) + " " + enchantment.getStartLevel() + "!");
        } else {
            giveOrDrop(player, createEnchantmentBook(enchantment, enchantment.getStartLevel()));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You received an enchanted book: " + displayName(enchantment) + " " + enchantment.getStartLevel() + ".");
        }
        uniqueEnchantments.add(enchantment.getName());
        persistUniqueEnchantments();
        return true;
    }

    private ItemStack createEnchantmentBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private List<Enchantment> getAvailableEnchantments() {
        List<Enchantment> available = new ArrayList<Enchantment>();
        for (String name : getConfig().getStringList("available-enchantments")) {
            Enchantment enchantment = Enchantment.getByName(name);
            if (enchantment != null && !uniqueEnchantments.contains(name) && !removedEnchantments.contains(name)) {
                available.add(enchantment);
            }
        }
        return available;
    }

    private String displayName(Enchantment enchantment) {
        return enchantment.getName().toLowerCase().replace('_', ' ');
    }

    private void persistUniqueEnchantments() {
        getConfig().set(UNIQUE_ENCHANTMENTS_PATH, new ArrayList<String>(uniqueEnchantments));
        getConfig().set(REMOVED_ENCHANTMENTS_PATH, new ArrayList<String>(removedEnchantments));
        saveConfig();
    }

    private void discoverExistingEnchantments() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            discoverInventory(player.getInventory());
        }
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    discoverItem(((Item) entity).getItemStack());
                }
            }
        }
        persistUniqueEnchantments();
    }

    private void discoverInventory(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            discoverItem(item);
        }
    }

    private void discoverItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
                registerDiscovered(enchantment);
            }
        }
        for (Enchantment enchantment : item.getEnchantments().keySet()) {
            registerDiscovered(enchantment);
        }
    }

    private void registerDiscovered(Enchantment enchantment) {
        if (isManagedEnchantment(enchantment) && !removedEnchantments.contains(enchantment.getName())) {
            uniqueEnchantments.add(enchantment.getName());
        }
    }

    private boolean isManagedEnchantment(Enchantment enchantment) {
        return enchantment != null && getConfig().getStringList("available-enchantments").contains(enchantment.getName());
    }

    private Set<Enchantment> getItemEnchantments(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Collections.emptySet();
        }
        Set<Enchantment> enchantments = new HashSet<Enchantment>(item.getEnchantments().keySet());
        if (item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta) {
            for (Enchantment enchantment : ((EnchantmentStorageMeta) item.getItemMeta()).getStoredEnchants().keySet()) {
                if (isManagedEnchantment(enchantment)) {
                    enchantments.add(enchantment);
                }
            }
        }
        enchantments.removeIf(enchantment -> !isManagedEnchantment(enchantment));
        return enchantments;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        discoverInventory(event.getPlayer().getInventory());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        discoverInventory(event.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() == null
                || event.getClickedInventory() instanceof PlayerInventory
                || event.getInventory().getType() == InventoryType.ANVIL) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        for (Enchantment enchantment : getItemEnchantments(clicked)) {
            if (playerAlreadyHas(enchantment)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "That enchantment already exists on the server.");
                return;
            }
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        discoverItem(event.getEntity().getItemStack());
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        Set<Enchantment> enchantments = getItemEnchantments(item);
        for (Enchantment enchantment : enchantments) {
            if (playerAlreadyHas(enchantment)) {
                event.setCancelled(true);
                if (item.getType() == Material.ENCHANTED_BOOK) {
                    makeUnstable(event.getItem());
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "That enchantment already exists on the server.");
                }
                return;
            }
        }
        for (Enchantment enchantment : enchantments) {
            registerDiscovered(enchantment);
        }
    }

    private boolean playerAlreadyHas(Enchantment enchantment) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inventory = player.getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (getItemEnchantments(item).contains(enchantment)) {
                    return true;
                }
            }
            for (ItemStack item : inventory.getArmorContents()) {
                if (getItemEnchantments(item).contains(enchantment)) {
                    return true;
                }
            }
            if (getItemEnchantments(inventory.getItemInOffHand()).contains(enchantment)) {
                return true;
            }
        }
        return false;
    }

    private void makeUnstable(Item item) {
        if (!item.isValid() || !unstableItems.add(item.getEntityId())) {
            return;
        }
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        int seconds = Math.max(1, getConfig().getInt("duplicate-countdown-seconds", 5));
        item.getWorld().playEffect(item.getLocation(), org.bukkit.Effect.ENDER_SIGNAL, 0);
        Bukkit.broadcastMessage(ChatColor.RED + "A duplicate enchantment is destabilizing!");
        final org.bukkit.scheduler.BukkitTask[] countdownTask = new org.bukkit.scheduler.BukkitTask[1];
        countdownTask[0] = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int remaining = seconds;

            @Override
            public void run() {
                if (!item.isValid()) {
                    unstableItems.remove(item.getEntityId());
                    countdownTask[0].cancel();
                    return;
                }
                item.getWorld().spigot().playEffect(item.getLocation(), org.bukkit.Effect.FLAME,
                        0, 0, 0.25F, 0.25F, 0.25F, 0.02F, 8, 32);
                if (--remaining <= 0) {
                    item.getWorld().createExplosion(item.getLocation().getX(), item.getLocation().getY(),
                        item.getLocation().getZ(), 1.0F, false, false);
                    item.remove();
                    unstableItems.remove(item.getEntityId());
                    countdownTask[0].cancel();
                }
            }
        }, 0L, 20L);
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack book = inventory.getItem(1);
        if (base == null || book == null || book.getType() != Material.ENCHANTED_BOOK
                || !(book.getItemMeta() instanceof EnchantmentStorageMeta)) {
            return;
        }
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
        for (Enchantment enchantment : bookMeta.getStoredEnchants().keySet()) {
            if (!isManagedEnchantment(enchantment)) {
                event.setResult(null);
                return;
            }
        }
        ItemStack result = base.clone();
        boolean changed = false;
        for (Enchantment enchantment : bookMeta.getStoredEnchants().keySet()) {
            int level = bookMeta.getStoredEnchantLevel(enchantment);
            result.addUnsafeEnchantment(enchantment, Math.max(level, base.getEnchantmentLevel(enchantment)));
            changed = true;
        }
        if (changed) {
            event.setResult(result);
            inventory.setRepairCost(Math.max(1, bookMeta.getStoredEnchants().size() * 2));
        }
    }

    @EventHandler
    public void onAnvilTake(InventoryClickEvent event) {
        if (event.getSlot() != 2 || !(event.getInventory() instanceof AnvilInventory)) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        if (result != null && result.getType() != Material.AIR) {
            discoverItem(result);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Map<Enchantment, Integer> dropped = new HashMap<Enchantment, Integer>();
        PlayerInventory inventory = event.getEntity().getInventory();
        for (ItemStack item : inventory.getContents()) {
            collectEnchantments(item, dropped);
            removePluginEnchantments(item);
        }
        for (ItemStack item : inventory.getArmorContents()) {
            collectEnchantments(item, dropped);
            removePluginEnchantments(item);
        }
        ItemStack offHand = inventory.getItemInOffHand();
        collectEnchantments(offHand, dropped);
        removePluginEnchantments(offHand);

        for (ItemStack drop : event.getDrops()) {
            collectEnchantments(drop, dropped);
            removePluginEnchantments(drop);
        }
        for (Map.Entry<Enchantment, Integer> entry : dropped.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (!removedEnchantments.contains(enchantment.getName())) {
                event.getDrops().add(createEnchantmentBook(enchantment, entry.getValue()));
                uniqueEnchantments.add(enchantment.getName());
            }
        }
        persistUniqueEnchantments();
    }

    private void collectEnchantments(ItemStack item, Map<Enchantment, Integer> destination) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            if (!isManagedEnchantment(entry.getKey())) {
                continue;
            }
            Integer oldLevel = destination.get(entry.getKey());
            if (oldLevel == null || oldLevel < entry.getValue()) {
                destination.put(entry.getKey(), entry.getValue());
            }
        }
        if (item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
                if (!isManagedEnchantment(enchantment)) {
                    continue;
                }
                int level = meta.getStoredEnchantLevel(enchantment);
                Integer oldLevel = destination.get(enchantment);
                if (oldLevel == null || oldLevel < level) {
                    destination.put(enchantment, level);
                }
            }
        }
    }

    private void removePluginEnchantments(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        boolean changed = false;
        for (Enchantment enchantment : getItemEnchantments(item)) {
            if (meta instanceof EnchantmentStorageMeta) {
                ((EnchantmentStorageMeta) meta).removeStoredEnchant(enchantment);
            } else {
                meta.removeEnchant(enchantment);
            }
            changed = true;
        }
        if (changed) {
            item.setItemMeta(meta);
            if (item.getType() == Material.ENCHANTED_BOOK) {
                item.setType(Material.BOOK);
            }
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (!killRollArmed) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            killRollArmed = false;
            grantRandomEnchantment(killer);
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + killer.getName() + " claimed the kill-roll enchantment!");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (applyingSweepDamage) {
            return;
        }
        Player owner = null;
        if (event.getDamager() instanceof Player) {
            owner = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (shooter instanceof Player) {
                owner = (Player) shooter;
            }
        }
        if (owner == null || event.getEntity() == owner) {
            return;
        }
        ItemStack held = owner.getInventory().getItemInMainHand();
        if (held == null) {
            return;
        }
        if (held.containsEnchantment(Enchantment.getByName(PUNCH))) {
            Vector knockback = event.getEntity().getLocation().toVector().subtract(owner.getLocation().toVector()).normalize();
            event.getEntity().setVelocity(knockback.multiply(0.8).setY(0.35));
        }
        if (held.containsEnchantment(Enchantment.getByName(SWEEPING_EDGE)) && event.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            double splashDamage = event.getFinalDamage() * 0.5;
            applyingSweepDamage = true;
            try {
                for (org.bukkit.entity.Entity nearby : event.getEntity().getNearbyEntities(2.0, 1.0, 2.0)) {
                    if (!(nearby instanceof org.bukkit.entity.LivingEntity) || nearby == owner || nearby == event.getEntity()) {
                        continue;
                    }
                    ((org.bukkit.entity.LivingEntity) nearby).damage(splashDamage, owner);
                }
            } finally {
                applyingSweepDamage = false;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInfiniteConsume(PlayerItemConsumeEvent event) {
        ItemStack consumed = event.getItem();
        if (!consumed.containsEnchantment(Enchantment.getByName(INFINITY))) {
            return;
        }
        ItemStack replacement = consumed.clone();
        replacement.setAmount(1);
        Bukkit.getScheduler().runTask(this, () -> giveOrDrop(event.getPlayer(), replacement));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInfiniteTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack totem = mainHand.getType() == Material.TOTEM ? mainHand : offHand;
        if (totem.getType() != Material.TOTEM
                || !totem.containsEnchantment(Enchantment.getByName(INFINITY))) {
            return;
        }
        ItemStack replacement = totem.clone();
        replacement.setAmount(1);
        Bukkit.getScheduler().runTask(this, () -> giveOrDrop(player, replacement));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInfinitePlace(BlockPlaceEvent event) {
        ItemStack placed = event.getItemInHand();
        if (!placed.containsEnchantment(Enchantment.getByName(INFINITY))) {
            return;
        }
        ItemStack replacement = placed.clone();
        replacement.setAmount(1);
        Bukkit.getScheduler().runTask(this, () -> giveOrDrop(event.getPlayer(), replacement));
    }

    @EventHandler
    public void onInfiniteDurability(PlayerItemDamageEvent event) {
        if (event.getItem().getType() == Material.SHIELD
                && event.getItem().containsEnchantment(Enchantment.getByName(INFINITY))) {
            event.setCancelled(true);
        }
    }
}