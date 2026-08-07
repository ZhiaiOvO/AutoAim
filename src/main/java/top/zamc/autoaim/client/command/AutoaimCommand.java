package top.zamc.autoaim.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import top.zamc.autoaim.client.config.AutoaimConfig;
import top.zamc.autoaim.client.config.AutoaimConfigManager;
import top.zamc.autoaim.client.target.AimTargetCategory;

import java.util.List;

/**
 * /autoaim 客户端命令，联机时在本地执行，修改本地配置。
 */
public final class AutoaimCommand {
    private AutoaimCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("autoaim")
                        .executes(ctx -> showHelp(ctx.getSource()))
                        .then(ClientCommandManager.literal("help").executes(ctx -> showHelp(ctx.getSource())))
                        .then(ClientCommandManager.literal("status").executes(ctx -> showStatus(ctx.getSource())))
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> toggle(ctx.getSource())))
                        .then(targetCommand())
                        .then(nameListCommand("whitelist", true))
                        .then(nameListCommand("blacklist", false))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> targetCommand() {
        return ClientCommandManager.literal("target")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                .executes(ctx -> addCategory(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                .executes(ctx -> removeCategory(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))
                .then(ClientCommandManager.literal("list").executes(ctx -> listCategories(ctx.getSource())));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> nameListCommand(
            String root, boolean isWhitelist) {
        return ClientCommandManager.literal(root)
                .then(ClientCommandManager.literal("on").executes(ctx -> setWhitelistMode(ctx.getSource(), true)))
                .then(ClientCommandManager.literal("off").executes(ctx -> setWhitelistMode(ctx.getSource(), false)))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> addName(ctx.getSource(), isWhitelist,
                                        StringArgumentType.getString(ctx, "name")))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> removeName(ctx.getSource(), isWhitelist,
                                        StringArgumentType.getString(ctx, "name")))))
                .then(ClientCommandManager.literal("list").executes(ctx -> listNames(ctx.getSource(), isWhitelist)))
                .then(ClientCommandManager.literal("clear").executes(ctx -> clearList(ctx.getSource(), isWhitelist)));
    }

    private static int showHelp(FabricClientCommandSource source) {
        String help = """
                §e--- AutoAim 帮助 ---
                §f/autoaim toggle§7  切换总开关
                §f/autoaim status§7  查看当前状态
                §f/autoaim target add|remove <类型>§7  添加/移除目标类型 (player, hostile, passive, water, ambient, other)
                §f/autoaim target list§7  查看已选目标类型
                §f/autoaim whitelist on|off§7  启用/关闭白名单模式
                §f/autoaim whitelist add|remove <名字>§7  增删白名单
                §f/autoaim whitelist list|clear§7  查看/清空白名单
                §f/autoaim blacklist add|remove <名字>§7  增删黑名单
                §f/autoaim blacklist list|clear§7  查看/清空黑名单
                §f/autoaim help§7  显示本帮助
                """;
        source.sendFeedback(Text.literal(help));
        return 1;
    }

    private static int showStatus(FabricClientCommandSource source) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        String whitelistMode = config.whitelistEnabled ? "§a开启" : "§c关闭";
        source.sendFeedback(Text.literal(String.format(
                "§e[AutoAim] 总开关: %s | 白名单模式: %s | 白名单 %d 人 | 黑名单 %d 人 | 锁定距离 %.0f | 响应速度 %.1f | 抖动 %.1f | 目标类型: %s",
                config.enabled ? "§a开启" : "§c关闭",
                whitelistMode,
                config.whitelist.size(),
                config.blacklist.size(),
                config.aimRange,
                config.rotationSpeed,
                config.aimNoise,
                selectedCategoriesText(config))));
        return 1;
    }

    private static int addCategory(FabricClientCommandSource source, String name) {
        AimTargetCategory category = AimTargetCategory.fromName(name);
        if (category == null) {
            source.sendError(Text.literal("§c[AutoAim] 未知类型: " + name + "，可选: " + categoryOptions()));
            return 0;
        }
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        if (!config.targetCategories.add(category)) {
            source.sendError(Text.literal("§c[AutoAim] " + category.displayName + " 已在目标类型中"));
            return 0;
        }
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal("§a[AutoAim] 已添加目标类型: " + category.displayName));
        return 1;
    }

    private static int removeCategory(FabricClientCommandSource source, String name) {
        AimTargetCategory category = AimTargetCategory.fromName(name);
        if (category == null) {
            source.sendError(Text.literal("§c[AutoAim] 未知类型: " + name + "，可选: " + categoryOptions()));
            return 0;
        }
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        if (!config.targetCategories.remove(category)) {
            source.sendError(Text.literal("§c[AutoAim] " + category.displayName + " 不在目标类型中"));
            return 0;
        }
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal("§a[AutoAim] 已移除目标类型: " + category.displayName));
        return 1;
    }

    private static int listCategories(FabricClientCommandSource source) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        if (config.targetCategories.isEmpty()) {
            source.sendFeedback(Text.literal("§e[AutoAim] 未选择任何目标类型"));
            return 1;
        }
        source.sendFeedback(Text.literal("§e[AutoAim] 当前目标类型: " + selectedCategoriesText(config)));
        return 1;
    }

    private static String selectedCategoriesText(AutoaimConfig config) {
        StringBuilder sb = new StringBuilder();
        for (AimTargetCategory category : AimTargetCategory.values()) {
            if (config.targetCategories.contains(category)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("§a").append(category.displayName);
            }
        }
        return sb.length() == 0 ? "§c无" : sb.toString();
    }

    private static String categoryOptions() {
        StringBuilder sb = new StringBuilder();
        for (AimTargetCategory category : AimTargetCategory.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(category.commandName);
        }
        return sb.toString();
    }

    private static int toggle(FabricClientCommandSource source) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        config.enabled = !config.enabled;
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal(config.enabled
                ? "§a[AutoAim] 已开启，按住 §fR§a 键瞄准最近的玩家"
                : "§c[AutoAim] 已关闭"));
        return 1;
    }

    private static int setWhitelistMode(FabricClientCommandSource source, boolean enabled) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        config.whitelistEnabled = enabled;
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal(enabled
                ? "§a[AutoAim] 白名单模式已开启，只锁定白名单中的玩家"
                : "§c[AutoAim] 白名单模式已关闭，锁定所有非黑名单玩家"));
        return 1;
    }

    private static int addName(FabricClientCommandSource source, boolean isWhitelist, String name) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        List<String> list = isWhitelist ? config.whitelist : config.blacklist;
        String listName = isWhitelist ? "白名单" : "黑名单";
        if (list.contains(name)) {
            source.sendError(Text.literal("§c[AutoAim] " + name + " 已在" + listName + "中"));
            return 0;
        }
        list.add(name);
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal("§a[AutoAim] 已将 §f" + name + "§a 添加到" + listName));
        return 1;
    }

    private static int removeName(FabricClientCommandSource source, boolean isWhitelist, String name) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        List<String> list = isWhitelist ? config.whitelist : config.blacklist;
        String listName = isWhitelist ? "白名单" : "黑名单";
        if (!list.remove(name)) {
            source.sendError(Text.literal("§c[AutoAim] " + name + " 不在" + listName + "中"));
            return 0;
        }
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal("§a[AutoAim] 已将 §f" + name + "§a 从" + listName + "移除"));
        return 1;
    }

    private static int listNames(FabricClientCommandSource source, boolean isWhitelist) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        List<String> list = isWhitelist ? config.whitelist : config.blacklist;
        String listName = isWhitelist ? "白名单" : "黑名单";
        if (list.isEmpty()) {
            source.sendFeedback(Text.literal("§e[AutoAim] " + listName + "为空"));
            return 1;
        }
        source.sendFeedback(Text.literal("§e[AutoAim] " + listName + " (" + list.size() + "): §f" + String.join(", ", list)));
        return 1;
    }

    private static int clearList(FabricClientCommandSource source, boolean isWhitelist) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        List<String> list = isWhitelist ? config.whitelist : config.blacklist;
        String listName = isWhitelist ? "白名单" : "黑名单";
        list.clear();
        AutoaimConfigManager.save();
        source.sendFeedback(Text.literal("§a[AutoAim] 已清空" + listName));
        return 1;
    }
}
