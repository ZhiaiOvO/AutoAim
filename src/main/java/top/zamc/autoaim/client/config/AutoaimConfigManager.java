package top.zamc.autoaim.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.zamc.autoaim.client.target.AimTargetCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;

public final class AutoaimConfigManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("autoaim");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autoaim.json");
    private static AutoaimConfig config = new AutoaimConfig();

    private AutoaimConfigManager() {
    }

    public static AutoaimConfig getConfig() {
        return config;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            AutoaimConfig loaded = GSON.fromJson(Files.readString(CONFIG_PATH), AutoaimConfig.class);
            if (loaded != null) {
                config = loaded;
            }
            if (config.whitelist == null) {
                config.whitelist = new ArrayList<>();
            }
            if (config.blacklist == null) {
                config.blacklist = new ArrayList<>();
            }
            if (config.targetCategories == null) {
                config.targetCategories = EnumSet.of(AimTargetCategory.PLAYER);
            } else {
                // 确保是可变集合（Gson 可能反序列化为不可变/特殊实现）
                config.targetCategories = new HashSet<>(config.targetCategories);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("加载自动瞄准配置失败，使用默认配置", e);
            config = new AutoaimConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            LOGGER.error("保存自动瞄准配置失败", e);
        }
    }
}
