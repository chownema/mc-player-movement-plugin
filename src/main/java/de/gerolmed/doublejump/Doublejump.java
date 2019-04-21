package de.gerolmed.doublejump;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Main class of this project. This is just a simple one class project!
 */
public final class Doublejump extends JavaPlugin implements Listener {

    /**
     * Logger used for Logging console messages
     */
    private final Logger LOGGER;

    /**
     * A list of all players that have double jumped
     */
    private final ArrayList<UUID> jumpers;

    /**
     * Creates a new instance of this plugin.
     * Always use a no-args constructor.
     */
    public Doublejump() {
        this.LOGGER = Bukkit.getLogger();
        this.jumpers = new ArrayList<>();
    }

    /**
     * Called upon plugin start
     */
    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        LOGGER.info("========[DoubleJump]========\n"+
                "  Successfully booted\n"+
                "============================");
    }

    /**
     * Called upon plugin stop
     */
    @Override
    public void onDisable() {
        LOGGER.info("========[DoubleJump]========\n"+
                "  Successfully stopped\n"+
                "============================");
    }

    /**
     * Called when a player joins
     *
     * @param event the event
     */
    @EventHandler
    private void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(!player.hasPermission("doublejump.jump"))
            return;

        player.setAllowFlight(true); // Allow flight cause we will double jump on flight attempt.
    }

    /**
     * Called when a player quits the game
     *
     * @param event the event
     */
    @EventHandler
    public void quit(PlayerQuitEvent event) {
        leave(event.getPlayer());
    }

    /**
     * Called when a player gets kicked off the game
     *
     * @param event the event
     */
    @EventHandler
    public void quit(PlayerKickEvent event) {
        leave(event.getPlayer());
    }

    /**
     * Calls actions required when a player leaves.
     * @param player the leaving player
     */
    private void leave(Player player) {
        if(player == null)
            return;

        if(player.hasPermission("doublejump.jump") &&
                player.getGameMode() != GameMode.CREATIVE) //This line might cause issue's with other plugins
            player.setAllowFlight(false);
    }

    /**
     * Called when a player is trying to double jump
     *
     * @param event the event
     */
    @EventHandler
    public void attemptDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        //Don't double jump in these cases
        if(jumpers.contains(player.getUniqueId()) ||
                !player.hasPermission("doublejump.jump") ||
                !event.isFlying() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR)
            return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);//Disable to prevent wobbling

        Vector direction = player.getEyeLocation().getDirection();
        if(direction.getY() <= 0)
            direction.setY(0.6);

        player.setVelocity(direction);
        jumpers.add(player.getUniqueId());
        player.getLocation().getWorld().playEffect(player.getLocation(), Effect.EXPLOSION_LARGE,0, 20);
        //TODO Rework effect and add sound
    }

    /**
     * Block fall damage of double jump
     *
     * @param event the event
     */
    @EventHandler
    public void damageFall(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player))
            return;
        if(!jumpers.contains(event.getEntity().getUniqueId()) || event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        event.setCancelled(true);
    }

    /**
     * Listens to player movement
     *
     * @param event the event
     */
    @EventHandler
    public void refresh(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if(!jumpers.contains(player.getUniqueId()))
            return;

        Location belowPlayer = player.getLocation().subtract(0,0.1,0);
        Block block = belowPlayer.getBlock();

        // Player definitely not grounded so no refresh in sight
        if(block.isEmpty() || block.isLiquid())
            return;

        // No normal block below
        if(isNonGroundMaterial(block.getType()))
            return;
        player.setAllowFlight(true);
        jumpers.remove(player.getUniqueId());
    }

    /**
     * Checks a material if it is a block the players double jump should not be refreshed on
     *
     * @param type Material to check
     * @return is non ground material
     */
    private boolean isNonGroundMaterial(Material type) {
        return type == Material.LADDER ||
                type == Material.VINE ||
                type == Material.LONG_GRASS ||
                type == Material.DOUBLE_PLANT ||
                type == Material.YELLOW_FLOWER ||
                type == Material.RED_ROSE ||
                type == Material.COBBLE_WALL ||
                type == Material.TORCH ||
                type == Material.WALL_BANNER ||
                type == Material.WALL_SIGN ||
                type.toString().contains("FENCE") || // Filters out all fences and gates
                type.toString().contains("DOOR"); // Filters out doors and trapdoors
    }
}
