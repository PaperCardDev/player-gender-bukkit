package cn.paper_card.player_gender;

import cn.paper_card.mc_command.TheMcCommand;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

class GenderCommand extends TheMcCommand.HasSub {

    private final @NotNull PlayerGender plugin;

    private final @NotNull Permission permission;

    GenderCommand(@NotNull PlayerGender plugin) {
        super("gender");
        this.plugin = plugin;

        final PluginCommand command = plugin.getCommand(this.getLabel());
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);

        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("gender.command"));

        this.addSubCommand(new SetX());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }


    class SetX extends TheMcCommand {

        private final @NotNull Permission permission;

        protected SetX() {
            super("set");
            this.permission = plugin.addPermission(GenderCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argPlayer = strings.length > 0 ? strings[0] : null;
            final String argGender = strings.length > 1 ? strings[1] : null;

            if (argPlayer == null) {
                plugin.sendError(commandSender, "你必须提供参数：玩家名或UUID");
                return true;
            }

            if (argGender == null) {
                plugin.sendError(commandSender, "你必须提供参数：性别");
                return true;
            }

            final UUID uuid = plugin.parseArgPlayer(argPlayer);

            if (uuid == null) {
                plugin.sendError(commandSender, "找不到该玩家: " + argPlayer);
                return true;
            }

            PlayerGenderApi.GenderType genderType = null;
            for (PlayerGenderApi.GenderType value : PlayerGenderApi.GenderType.values()) {
                if (value.name().equals(argGender)) {
                    genderType = value;
                    break;
                }
            }
            if (genderType == null) {
                plugin.sendError(commandSender, "%s 不是正确的性别".formatted(argGender));
                return true;
            }

            final PlayerGenderApi.GenderType genderType1 = genderType;

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final boolean added;

                try {
                    added = plugin.setGender(uuid, genderType1);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (name == null) name = uuid.toString();

                plugin.sendInfo(commandSender, "%s成功，已将玩家 %s 的性别设置为: %s".formatted(
                        added ? "添加" : "更新", name, genderType1.name()
                ));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (strings.length == 1) {
                final String argPlayer = strings[0];
                final LinkedList<String> list = new LinkedList<>();
                if (argPlayer.isEmpty()) {
                    list.add("<玩家名或UUID>");
                } else {
                    for (OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
                        final String name = offlinePlayer.getName();
                        if (name == null) continue;
                        if (name.startsWith(argPlayer)) list.add(name);
                    }
                }
                return list;
            }

            if (strings.length == 2) {
                final String argGender = strings[1];
                final LinkedList<String> list = new LinkedList<>();
                if (argGender.isEmpty()) list.add("<性别>");
                for (PlayerGenderApi.GenderType value : PlayerGenderApi.GenderType.values()) {
                    list.add(value.name());
                }
                return list;
            }

            return null;
        }
    }
}
