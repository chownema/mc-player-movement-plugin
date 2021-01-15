package de.gerolmed.doublejump;

import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import java.lang.Math.*;

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
    private final ArrayList<Player> movers;
    private final HashMap<UUID,Vector> playerVectorMap;
    private final Doublejump plugin;

    /**
     * Creates a new instance of this plugin.
     * Always use a no-args constructor.
     */
    public Doublejump() {
        this.plugin = this;
        this.LOGGER = Bukkit.getLogger();
        this.movers = new ArrayList<>();
        this.playerVectorMap = new HashMap<>();
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

    private static boolean isUnsafeVelocity(final Vector vel) {
        final double x = vel.getX();
        final double y = vel.getY();
        final double z = vel.getZ();
        return x > 4.0 || x < -4.0 || y > 4.0 || y < -4.0 || z > 4.0 || z < -4.0;
    }

     @EventHandler
     public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
         Player player = event.getPlayer();
         Vector existingMovingPlayerInstanceVector = playerVectorMap.get(player.getUniqueId());

         Location belowPlayer = player.getLocation().subtract(0,0.01,0);
         Block block = belowPlayer.getBlock();
        if(block.isLiquid())
            return;
         if(isNonGroundMaterial(block.getType()))
             return;

         if (player.isSneaking() && existingMovingPlayerInstanceVector != null) {
             if(block.isEmpty()) {
                 int distance = 6;
                 Block targetBlock = player.getTargetBlock((HashSet<Byte>) null, distance);
                 if (!targetBlock.isEmpty()) {
                     Vector v = player.getEyeLocation().getDirection().multiply(10);
                     player.setVelocity(v);
                 }
             } else {
                 Vector v = existingMovingPlayerInstanceVector.multiply(5);
                 if(isUnsafeVelocity(v)) {
                     LOGGER.info("Player is reaching unsafe velocity");
                     return;
                 }
                 player.setVelocity(v);
             }
         }
     }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location belowPlayer = event.getPlayer().getLocation().subtract(0,0.01,0);
        Block block = belowPlayer.getBlock();
        if(isNonGroundMaterial(block.getType()))
            return;

        // Get possible vector player is moving to
        Vector vector = event.getTo().toVector().subtract(event.getFrom().toVector()).multiply(1.5);

        playerVectorMap.put(event.getPlayer().getUniqueId(), vector);
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
        if(
               //  jumpers.contains(player.getUniqueId()) ||
                !player.hasPermission("doublejump.jump") ||
                !event.isFlying() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR)
            return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);//Disable to prevent wobbling

        // Vector direction = player.getEyeLocation().getDirection();
        // if(direction.getY() <= 0)
        //     direction.setY(2.0);

        // Player existingMovingPlayerInstance = movers.stream()
        //         .filter(mover -> player.getUniqueId().equals(mover.getUniqueId()))
        //         .findAny()
        //         .orElse(null);
        Vector existingMovingPlayerInstanceVector = playerVectorMap.get(player.getUniqueId());

        if (existingMovingPlayerInstanceVector != null) {
        // if (existingMovingPlayerInstance != null) {
            // Location toLocation = player.getLocation();
            // Location fromLocation = existingMovingPlayerInstance.getLocation();
            // 
            // // double dX = toLocation.getX() - fromLocation.getX();
            // // double dZ = toLocation.getZ() - fromLocation.getZ();
            // // double dY = toLocation.getY() - fromLocation.getY();
            // // 
            // // double yaw = Math.atan2(dZ, dX);
            // // double pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI;

            // // double X = Math.sin(pitch) * Math.cos(yaw);
            // // double Y = Math.sin(pitch) * Math.sin(yaw);
            // // double Z = Math.cos(pitch);

            // // Vector newDirection = new Vector(X, Z, Y);

            // Vector vector = toLocation.toVector().subtract(fromLocation.toVector()).multiply(3);
            

            Vector direction = player.getEyeLocation().getDirection();
            player.setVelocity(direction.multiply(0.95));
            // player.getLocation().getWorld().spawnArrow(player.getLocation(), direction, (float) 2, (float) 0);
            // player.getLocation().getWorld().spawnArrow(player.getLocation(), direction, (float) 3, (float) 0);
            // player.getLocation().getWorld().spawnArrow(player.getLocation(), direction, (float) 4, (float) 0);
            // player.getLocation().getWorld().spawnArrow(player.getLocation(), direction, (float) 5, (float) 0);
            movers.add(player);
        // } else {
        //     Vector direction = player.getLocation().getDirection();
        //     player.setVelocity(direction);
        //     movers.add(player);
        }

        // player.getLocation().getWorld().playEffect(player.getLocation(), Effect.EXPLOSION_LARGE,0, 20);
        //TODO Rework effect and add sound
    }

    /**
     * Block fall damage of double jump
     *
     * @param event the event
     */
    @EventHandler
    public void damageFall(EntityDamageEvent event) {
        // if(!(event.getEntity() instanceof Player))
        //     return;
        // // if(!jumpers.contains(event.getEntity().getUniqueId()) || event.getCause() != EntityDamageEvent.DamageCause.FALL)
        //     // return;
        // event.setCancelled(true);
    }

    /**
     * Listens to player movement
     *
     * @param event the event
     */
    @EventHandler
    public void refresh(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        movers.add(player);
        if (movers.stream()
                .filter(mover -> player.getUniqueId().equals(mover.getUniqueId()))
                .findAny()
                .orElse(null) == null) {
            return;
        }

        Location belowPlayer = player.getLocation().subtract(0,0.1,0);
        Block block = belowPlayer.getBlock();

        // Player definitely not grounded so no refresh in sight
        if(block.isEmpty() || block.isLiquid())
            return;

        // No normal block below
        if(isNonGroundMaterial(block.getType()))
            return;
        player.setAllowFlight(true);
        movers.remove(
                movers.stream()
                .filter(mover -> player.getUniqueId().equals(mover.getUniqueId()))
                );
    }

    /**
     * Called when player switches his game mode
     *
     * @param event the event
     */
    @EventHandler
    public void switchGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if(!player.hasPermission("doublejump.jump") ||
                event.getNewGameMode() == GameMode.CREATIVE ||
                event.getNewGameMode() == GameMode.SPECTATOR)
            return;
        player.setAllowFlight(true);
        player.setFlying(false);
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

    @EventHandler
    public void onShoot(ProjectileLaunchEvent event) {
        if(event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if(arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                if(shooter.getItemInHand().getType() == Material.BOW) {
                    Vector arrowVelocity = arrow.getVelocity().multiply(10);
                    arrow.setVelocity(arrowVelocity);
                    // Location shooterHeadLocation = shooter.getLocation().add(0, 1.5, 0);
                    // FixedMetadataValue arrowMetaData = new FixedMetadataValue(
                    //         plugin,
                    //         shooter.getItemInHand().getEnchantments());
                    // 
                    // Arrow newArrow = shooter.getLocation().
                    //     getWorld().spawnArrow(
                    //             shooterHeadLocation,
                    //             arrowVelocity.multiply(10),
                    //             (float) 3, (float) 0);
                    // newArrow.setShooter(shooter);
                    // newArrow.setMetadata("enchant", arrowMetaData); 

                    // Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
                    //     public void run()
                    //     {
                    //         Arrow arrow = shooter.getLocation().
                    //             getWorld().spawnArrow(
                    //             shooterHeadLocation,
                    //             arrowVelocity.multiply(10),
                    //                     (float) 3, (float) 0);
                    //         newArrow.setShooter(shooter);
                    //         arrow.setMetadata("enchant", arrowMetaData); 
                    //     }
                    // }, 2L);

                    // Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
                    //     public void run()
                    //     {
                    //         Arrow arrow = shooter.getLocation().
                    //             getWorld().spawnArrow(
                    //             shooterHeadLocation,
                    //             arrowVelocity.multiply(10),
                    //                     (float) 3, (float) 0);
                    //         newArrow.setShooter(shooter);
                    //         arrow.setMetadata("enchant", arrowMetaData); 
                    //     }
                    // }, 3L);
                    // event.setCancelled(true);

                    // shooter.launchProjectile(Arrow.class).setVelocity(arrow.getVelocity().multiply(2));
                }
                
            }
            
        }
        
    }
}
