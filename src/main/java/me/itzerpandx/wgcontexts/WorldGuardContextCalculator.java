package me.itzerpandx.wgcontexts;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class WorldGuardContextCalculator implements ContextCalculator<Player> {

    private static final String REGION_CONTEXT_KEY = "worldguard:region";
    private static final String FLAG_CONTEXT_PREFIX = "worldguard:flag-";

    @Override
    public void calculate(@NotNull Player player, @NotNull ContextConsumer consumer) {
        Location location = player.getLocation();

        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) {
            return;
        }

        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet applicableRegions = query.getApplicableRegions(BukkitAdapter.adapt(location));

        Set<ProtectedRegion> regions = applicableRegions.getRegions();
        if (regions.isEmpty()) {
            return;
        }

        for (ProtectedRegion region : regions) {
            consumer.accept(REGION_CONTEXT_KEY, region.getId());
        }

        // IMPORTANT: some flags (e.g. "build") are special-cased by WorldGuard
        // and REQUIRE a non-null RegionAssociable subject, or they throw an NPE.
        // Wrap the Bukkit player into a WorldGuard LocalPlayer so we always pass
        // a real subject instead of null.
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
        for (Flag<?> flag : flagRegistry) {
            Object value;
            try {
                value = applicableRegions.queryValue(localPlayer, flag);
            } catch (Exception e) {
                // Some flags may still refuse resolution in edge cases (e.g. no
                // handler registered, unusual server state). Skip rather than
                // crash the whole context calculation for this player.
                continue;
            }

            if (value == null) {
                continue;
            }

            String flagName = flag.getName();
            String contextValue = value.toString().trim();

            if (flagName == null || flagName.trim().isEmpty() || contextValue.isEmpty()) {
                continue;
            }

            String contextKey = FLAG_CONTEXT_PREFIX + flagName.trim().toLowerCase(java.util.Locale.ROOT);
            consumer.accept(contextKey, contextValue.toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();

        try {
            for (var world : org.bukkit.Bukkit.getWorlds()) {
                RegionManager rm = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                if (rm == null) {
                    continue;
                }
                for (String regionId : rm.getRegions().keySet()) {
                    builder.add(REGION_CONTEXT_KEY, regionId);
                }
            }
        } catch (Exception ignored) {
            // Region container may not be ready yet during early startup
        }

        FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
        for (Flag<?> flag : flagRegistry) {
            String contextKey = FLAG_CONTEXT_PREFIX + flag.getName().toLowerCase();
            builder.add(contextKey, "allow");
            builder.add(contextKey, "deny");
        }

        return builder.build();
    }
}