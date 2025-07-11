package net.woek.Hat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Hat extends JavaPlugin {

    private boolean isFolia = false;

    @Override
    public void onEnable() {
        // Detect Folia
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Detected Folia server implementation");
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        YamlConfiguration messages = validateYaml();
        boolean enabled = messages.getBoolean("messages-enabled");
        String set = (String) messages.get("set");
        String stacksize = (String) messages.get("stack-size");
        String nopermission = (String) messages.get("no-permission");
        String console = (String) messages.get("console");

        registerPermissions();

        HatHandler handler = new HatHandler(this, enabled, set, stacksize, nopermission, console);

        this.getCommand("hat").setExecutor(handler);
        this.getServer().getPluginManager().registerEvents(handler, this);

        Bukkit.getConsoleSender().sendMessage("Hat has been enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("Hat has been disabled.");
    }

    public boolean isFolia() {
        return isFolia;
    }

    private YamlConfiguration validateYaml() {
        Bukkit.getConsoleSender().sendMessage("[" + this.getName() + "] Validating " + "messages.yml");

        InputStream defaultFile = this.getResource("messages.yml");
        assert defaultFile != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(defaultFile));
        YamlConfiguration newconfig = YamlConfiguration.loadConfiguration(reader);

        YamlConfiguration oldconfig = new YamlConfiguration();
        try {
            File config = new File(getDataFolder(), "messages.yml");
            if(config.exists()) {
                oldconfig.load(config);
            } else {
                Bukkit.getConsoleSender().sendMessage("[" + this.getName() + "] " + "messages.yml" + " does not exist, creating it now.");
            }
        } catch(Throwable e) {
            Bukkit.getConsoleSender().sendMessage("[" + this.getName() + "] " + "messages.yml" + " does not contain a valid configuration, the default configuration will be used instead.");
            return newconfig;
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(String key : oldconfig.getKeys(true)) {
            if(newconfig.contains(key) && !(oldconfig.get(key) instanceof ConfigurationSection)){
                newconfig.set(key, oldconfig.get(key));
            }
        }

        try {
            newconfig.save(new File(this.getDataFolder(), "messages.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bukkit.getConsoleSender().sendMessage("[" + this.getName() + "] " + "messages.yml" + " has been validated.");

        return newconfig;
    }

    private void registerPermissions() {
        Permission basePerm = new Permission("hat.*", PermissionDefault.OP);
        Bukkit.getPluginManager().addPermission(basePerm);

        Permission blockPerm = new Permission("hat.blocks", PermissionDefault.FALSE);
        blockPerm.addParent(basePerm, true);
        Bukkit.getPluginManager().addPermission(blockPerm);

        Permission itemPerm = new Permission("hat.items", PermissionDefault.FALSE);
        itemPerm.addParent(basePerm, true);
        Bukkit.getPluginManager().addPermission(itemPerm);

        Material[] materials = Material.values();

        for(Material mat : materials) {
            Permission perm = new Permission("hat." + mat.name(), PermissionDefault.FALSE);

            if(mat.isBlock()) {
                perm.addParent(blockPerm, true);
            } else {
                perm.addParent(itemPerm, true);
            }

            Bukkit.getPluginManager().addPermission(perm);
        }
    }
}