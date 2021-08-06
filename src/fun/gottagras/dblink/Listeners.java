package fun.gottagras.dblink;

import fun.gottagras.mysql.GottaGrasMySQL;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Objects;

public class Listeners implements Listener {
    private Main main;
    public Listeners(Main main)
    {
        this.main = main;
    }

    @EventHandler
    public void onPlayerConnect(PostLoginEvent event)
    {
        ProxiedPlayer player = event.getPlayer();
        // GottaGras-MySQL
        Statement statement = GottaGrasMySQL.createStatement(main.connection);
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

        main.playersTimePlayed.put(player.getUniqueId().toString(), time_played);
        main.playersLastLogin.put(player.getUniqueId().toString(), date.getTime());
        GottaGrasMySQL.closeStatement(statement);
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event)
    {
        ProxiedPlayer player = event.getPlayer();
        Date date = new Date();

        String uuid = player.getUniqueId().toString();
        long playerLoginTime = main.playersLastLogin.get(uuid);
        long playerTimePlayed = main.playersTimePlayed.get(uuid);
        long currentTime = date.getTime();

        long currentTimePlayed = currentTime - playerLoginTime;
        long totalTimePlayed = currentTimePlayed + playerTimePlayed;

        // GottaGras-MySQL
        Statement statement = GottaGrasMySQL.createStatement(main.connection);
        GottaGrasMySQL.statementUpdate(statement, "UPDATE players SET time_played = "+totalTimePlayed+", connected = 0 WHERE uuid LIKE '"+player.getUniqueId().toString()+"'");
        GottaGrasMySQL.closeStatement(statement);
    }
}
