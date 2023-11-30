package cn.paper_card.player_gender;

import cn.paper_card.database.api.DatabaseApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerGender extends JavaPlugin implements PlayerGenderApi {

    private DatabaseApi.MySqlConnection mySqlConnection;

    private Connection connection = null;
    private Table table = null;

    private final @NotNull TextComponent prefix;

    private final @NotNull ConcurrentMap<UUID, GenderType> genderCache;

    private final @NotNull TaskScheduler taskScheduler;

    public PlayerGender() {
        this.genderCache = new ConcurrentHashMap<>();
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.GOLD))
                .append(Component.text("性别").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("]").color(NamedTextColor.GOLD))
                .build();

        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    private @NotNull Table getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRawConnection();

        if (this.connection != null && this.connection == newCon) return this.table;

        if (this.table != null) this.table.close();
        this.table = new Table(newCon);
        this.connection = newCon;
        return this.table;
    }

    @Override
    public void onLoad() {
        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);
        if (api == null) throw new RuntimeException("无法连接到" + DatabaseApi.class.getSimpleName());

        this.mySqlConnection = api.getRemoteMySQL().getConnectionImportant();
    }

    @Override
    public void onEnable() {
        new GenderCommand(this);
    }

    @Override
    public void onDisable() {
        synchronized (this.mySqlConnection) {
            if (this.table != null) {
                try {
                    this.table.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.table = null;
                this.connection = null;
            }
        }

        this.taskScheduler.cancelTasks(this);
    }


    private boolean setGenderDb(@NotNull UUID uuid, @NotNull GenderType genderType) throws Exception {
        synchronized (this.mySqlConnection) {

            try {
                final Table t = this.getTable();
                final int updated = t.updateGender(uuid, genderType.getInt());
                this.mySqlConnection.setLastUseTime();

                if (updated == 1) return false;
                if (updated == 0) {
                    final int inserted = t.insert(uuid, genderType.getInt());
                    this.mySqlConnection.setLastUseTime();

                    if (inserted != 1) throw new Exception("插入了%d条数据".formatted(inserted));
                    return true;
                }
                throw new Exception("更新了%d条数据！".formatted(updated));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }


    private @NotNull GenderType queryGenderDb(@NotNull UUID uuid) throws Exception {
        synchronized (this.mySqlConnection) {

            try {
                final Table t = this.getTable();
                final Integer gender = t.queryGender(uuid);
                if (gender == null) return GenderType.UNKNOWN;

                final GenderType genderType = GenderType.fromInt(gender);
                if (genderType == null) throw new Exception("数据库中存储了不正确的性别: %d".formatted(gender));

                return genderType;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean setGender(@NotNull UUID uuid, @NotNull GenderType genderType) throws Exception {
        final boolean added = this.setGenderDb(uuid, genderType);
        // 清除缓存
        this.genderCache.remove(uuid);
        return added;
    }

    @Override
    public @NotNull GenderType queryGender(@NotNull UUID uuid) throws Exception {
        final GenderType genderType = this.genderCache.get(uuid);
        if (genderType != null) return genderType;

        // 从数据库中取
        final GenderType genderType1 = this.queryGenderDb(uuid);

        // 放入缓存
        this.genderCache.put(uuid, genderType1);

        return genderType1;
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build()
        );
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull String info) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(info).color(NamedTextColor.GREEN))
                .build()
        );
    }

    @Nullable UUID parseArgPlayer(@NotNull String argPlayer) {
        try {
            return UUID.fromString(argPlayer);
        } catch (IllegalArgumentException ignored) {
        }

        for (final OfflinePlayer offlinePlayer : this.getServer().getOfflinePlayers()) {
            if (argPlayer.equals(offlinePlayer.getName())) return offlinePlayer.getUniqueId();
        }
        return null;
    }


    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }
}
