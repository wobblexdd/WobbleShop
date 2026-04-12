package me.wobble.wobbleshop;

import me.wobble.wobbleshop.command.ShopCommand;
import me.wobble.wobbleshop.command.ShopTabCompleter;
import me.wobble.wobbleshop.config.ConfigManager;
import me.wobble.wobbleshop.config.MessageManager;
import me.wobble.wobbleshop.config.ShopCatalogLoader;
import me.wobble.wobbleshop.listener.InventoryListener;
import me.wobble.wobbleshop.manager.EconomyManager;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.repository.CategoryRepository;
import me.wobble.wobbleshop.repository.ShopItemRepository;
import me.wobble.wobbleshop.service.RestockService;
import me.wobble.wobbleshop.service.ShopService;
import me.wobble.wobbleshop.storage.Database;
import me.wobble.wobbleshop.storage.SQLiteDatabase;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WobbleShopPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private Database database;
    private EconomyManager economyManager;
    private ShopService shopService;
    private RestockService restockService;
    private GuiManager guiManager;
    private ShopCatalogLoader catalogLoader;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.database = new SQLiteDatabase(this, configManager);
        this.database.initialize();
        this.catalogLoader = new ShopCatalogLoader(this);
        this.catalogLoader.ensureDefaults();

        CategoryRepository categoryRepository = new CategoryRepository(database);
        ShopItemRepository shopItemRepository = new ShopItemRepository(database);
        this.economyManager = new EconomyManager(this);
        this.shopService = new ShopService(this, categoryRepository, shopItemRepository, economyManager, catalogLoader);
        this.shopService.bootstrap();

        this.restockService = new RestockService(this, shopService, configManager);
        this.restockService.start();
        this.guiManager = new GuiManager(this, shopService, messageManager);

        PluginCommand command = getCommand("shop");
        if (command != null) {
            ShopCommand shopCommand = new ShopCommand(this);
            command.setExecutor(shopCommand);
            command.setTabCompleter(new ShopTabCompleter(this));
        }

        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
    }

    @Override
    public void onDisable() {
        if (restockService != null) {
            restockService.stop();
        }
        if (database != null) {
            database.close();
        }
    }

    public void reloadPlugin() {
        configManager.reload();
        messageManager.reload();
        shopService.reload();
        restockService.restart();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopService getShopService() {
        return shopService;
    }

    public RestockService getRestockService() {
        return restockService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
