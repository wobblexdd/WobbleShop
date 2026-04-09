package me.wobble.wobbleshop.manager;

import me.wobble.wobbleshop.WobbleShopPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyManager {

    private final WobbleShopPlugin plugin;
    private Economy economy;

    public EconomyManager(WobbleShopPlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    public void refresh() {
        hook();
    }

    private void hook() {
        RegisteredServiceProvider<Economy> provider = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        if (provider != null) {
            this.economy = provider.getProvider();
        } else {
            this.economy = null;
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public EconomyResponse withdraw(Player player, double amount) {
        if (economy == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Vault economy unavailable");
        }
        return economy.withdrawPlayer(player, amount);
    }

    public EconomyResponse deposit(Player player, double amount) {
        if (economy == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Vault economy unavailable");
        }
        return economy.depositPlayer(player, amount);
    }

    public String format(double amount) {
        if (economy == null) {
            return String.format("%.2f", amount);
        }
        return economy.format(amount);
    }
}
