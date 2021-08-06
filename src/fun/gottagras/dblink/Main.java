package fun.gottagras.dblink;

import fun.gottagras.mysql.GottaGrasMySQL;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class Main extends JavaPlugin
{
    public Connection connection = null;
    public HashMap<String, Long> playersTimePlayed = new HashMap<String, Long>();
    public HashMap<String, Long> playersLastLogin = new HashMap<String, Long>();

    @Override
    public void onEnable()
    {
        // CONFIG
        saveDefaultConfig();

        // GottaGras-MySQL
        connection = GottaGrasMySQL.connect(getConfig().getString("mysql.ip"), getConfig().getString("mysql.port"), getConfig().getString("mysql.database"), getConfig().getString("mysql.login"), getConfig().getString("mysql.password"));

        Statement statement = GottaGrasMySQL.createStatement(connection);
        GottaGrasMySQL.statementExecute(statement, "CREATE TABLE IF NOT EXISTS players(uuid text, name text, time_played long, connected boolean)");
        GottaGrasMySQL.closeStatement(statement);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        // GottaGras-MySQL
        Statement statement = GottaGrasMySQL.createStatement(connection);
        ResultSet resultSet = GottaGrasMySQL.statementQuery(statement, "SELECT uuid, time_played from players");
        boolean inDB = false;
        long time_played = 0;
        try
        {
            while (resultSet.next())
            {
                // CHECK IF PLAYER IS IN DB
                if (Objects.equals(resultSet.getString("uuid"), player.getUniqueId().toString()))
                {
                    inDB = true;
                    time_played = resultSet.getLong("time_played");
                }
            }
        }
        catch (SQLException e)
        {
            GottaGrasMySQL.log(e.getMessage());
        }

        // ADD OR UPDATE PLAYERS DATA
        if (!inDB)
        {
            GottaGrasMySQL.statementUpdate(statement, "INSERT INTO players (uuid, name, time_played, connected) VALUES ('"+player.getUniqueId().toString()+"', '"+player.getName()+"', 0, 1)");
        }
        else
        {
            GottaGrasMySQL.statementUpdate(statement, "UPDATE players SET name = '"+player.getName()+"', connected = 1 WHERE uuid LIKE '"+player.getUniqueId().toString()+"'");
        }

        Date date = new Date();

        playersTimePlayed.put(player.getUniqueId().toString(), time_played);
        playersLastLogin.put(player.getUniqueId().toString(), date.getTime());
        GottaGrasMySQL.closeStatement(statement);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        Date date = new Date();

        String uuid = player.getUniqueId().toString();
        long playerLoginTime = playersLastLogin.get(uuid);
        long playerTimePlayed = playersTimePlayed.get(uuid);
        long currentTime = date.getTime();

        long currentTimePlayed = currentTime - playerLoginTime;
        long totalTimePlayed = currentTimePlayed + playerTimePlayed;

        // GottaGras-MySQL
        Statement statement = GottaGrasMySQL.createStatement(connection);
        GottaGrasMySQL.statementUpdate(statement, "UPDATE players SET time_played = "+totalTimePlayed+", connected = 0 WHERE uuid LIKE '"+player.getUniqueId().toString()+"'");
        GottaGrasMySQL.closeStatement(statement);
    }

    @Override
    public void onDisable()
    {
        // GottaGras-MySQL
        GottaGrasMySQL.disconnect(connection);
    }
}
