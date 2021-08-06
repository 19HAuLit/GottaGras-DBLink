package fun.gottagras.dblink;

import fun.gottagras.mysql.GottaGrasMySQL;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;

public class Main extends Plugin
{
    public Connection connection = null;
    public Configuration config = null;
    public HashMap<String, Long> playersTimePlayed = new HashMap<String, Long>();
    public HashMap<String, Long> playersLastLogin = new HashMap<String, Long>();

    @Override
    public void onEnable()
    {
        ProxyServer.getInstance().getPluginManager().registerListener(this, new Listeners(this));

        if (!getDataFolder().exists()) getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists())
        {
            try (InputStream in = getResourceAsStream("config.yml"))
            {
                Files.copy(in, file.toPath());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            // Load config
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
            // Save config
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(getDataFolder(), "config.yml"));
        }
        catch (IOException e)
        {
            GottaGrasMySQL.log(e.getMessage());
        }

        // GottaGras-MySQL
        connection = GottaGrasMySQL.connect(config.getString("mysql.ip"), config.getString("mysql.port"), config.getString("mysql.database"), config.getString("mysql.login"), config.getString("mysql.password"));
        System.out.println(connection);
        Statement statement = GottaGrasMySQL.createStatement(connection);
        GottaGrasMySQL.statementExecute(statement, "CREATE TABLE IF NOT EXISTS players(uuid text, name text, time_played long, connected boolean)");
        GottaGrasMySQL.closeStatement(statement);
    }

    @Override
    public void onDisable()
    {
        // GottaGras-MySQL
        GottaGrasMySQL.disconnect(connection);
    }
}
