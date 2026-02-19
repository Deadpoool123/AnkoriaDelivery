package online.ankoria.delivery;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnkoriaDeliveryPlugin extends JavaPlugin {

    private Connection conn;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        connect();
        startPoller();
        getLogger().info("AnkoriaDelivery enabled.");
    }

    @Override
    public void onDisable() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        getLogger().info("AnkoriaDelivery disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("deliveryreload")) {
            reloadConfig();
            connect();
            sender.sendMessage("AnkoriaDelivery config reloaded.");
            return true;
        }
        return false;
    }

    private void connect() {
        try {
            if (conn != null && !conn.isClosed()) return;

            String host = getConfig().getString("mysql.host", "localhost");
            int port = getConfig().getInt("mysql.port", 3306);
            String db = getConfig().getString("mysql.database", "loginsystem");
            String user = getConfig().getString("mysql.user", "root");
            String pass = getConfig().getString("mysql.password", "");
            boolean useSSL = getConfig().getBoolean("mysql.useSSL", false);

            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useUnicode=true&characterEncoding=utf8&useSSL=" + useSSL + "&serverTimezone=UTC";
            conn = DriverManager.getConnection(url, user, pass);
            getLogger().info("MySQL connected: " + host + ":" + port + "/" + db);
        } catch (Exception e) {
            getLogger().severe("MySQL connection failed: " + e.getMessage());
            conn = null;
        }
    }

    private void startPoller() {
        int interval = getConfig().getInt("poll-interval-ticks", 20);
        int maxPer = getConfig().getInt("max-per-poll", 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (conn == null) {
                    connect();
                    return;
                }
                List<Delivery> list = fetchPending(maxPer);
                if (list.isEmpty()) return;

                // Komutları main thread'de çalıştır
                Bukkit.getScheduler().runTask(AnkoriaDeliveryPlugin.this, () -> {
                    for (Delivery d : list) {
                        try {
                            for (String cmd : d.commands) {
                                String c = cmd.replace("{player}", d.username);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
                            }
                            markDelivered(d.id);
                        } catch (Exception ex) {
                            markFailed(d.id, ex.getMessage());
                        }
                    }
                });
            }
        }.runTaskTimerAsynchronously(this, interval, interval);
    }

    private List<Delivery> fetchPending(int limit) {
        List<Delivery> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, username, commands_json FROM deliveries WHERE status='pending' ORDER BY id ASC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String user = rs.getString("username");
                    String json = rs.getString("commands_json");
                    List<String> cmds = JsonMini.parseStringArray(json);
                    out.add(new Delivery(id, user, cmds));
                }
            }
        } catch (Exception e) {
            getLogger().warning("Fetch failed: " + e.getMessage());
            try { conn.close(); } catch (Exception ignored) {}
            conn = null;
        }
        return out;
    }

    private void markDelivered(long id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE deliveries SET status='delivered', delivered_at=NOW(), last_error=NULL WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void markFailed(long id, String err) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE deliveries SET status='failed', last_error=?, updated_at=NOW() WHERE id=?")) {
            ps.setString(1, err != null ? err.substring(0, Math.min(500, err.length())) : "error");
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    static class Delivery {
        long id;
        String username;
        List<String> commands;
        Delivery(long id, String username, List<String> commands) {
            this.id = id;
            this.username = username;
            this.commands = commands;
        }
    }
}
