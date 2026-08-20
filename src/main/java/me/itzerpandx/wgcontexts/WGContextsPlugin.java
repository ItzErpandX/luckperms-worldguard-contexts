package me.itzerpandx.wgcontexts;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class WGContextsPlugin extends JavaPlugin {

    private me.itzerpandx.wgcontexts.WorldGuardContextCalculator contextCalculator;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LuckPerms luckPerms = LuckPermsProvider.get();

        this.contextCalculator = new me.itzerpandx.wgcontexts.WorldGuardContextCalculator();
        luckPerms.getContextManager().registerCalculator(contextCalculator);

        getLogger().info("Registered WorldGuard context calculator with LuckPerms.");
        getLogger().info("Contexts available: worldguard:region=<region-id>, worldguard:flag-<flag-name>=<value>");
    }

    @Override
    public void onDisable() {
        if (contextCalculator == null) {
            return;
        }
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            luckPerms.getContextManager().unregisterCalculator(contextCalculator);
        } catch (IllegalStateException ignored) {
        }
    }
}
