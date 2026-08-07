package top.zamc.autoaim.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import top.zamc.autoaim.client.target.AimTargetCategory;

import java.util.ArrayList;
import java.util.List;

public final class AutoaimConfigScreen {
    private AutoaimConfigScreen() {
    }

    public static Screen create(Screen parent) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AutoAim 配置"));
        builder.setSavingRunnable(AutoaimConfigManager::save);

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("通用"));
        ConfigCategory targets = builder.getOrCreateCategory(Text.literal("目标类型"));
        ConfigCategory lists = builder.getOrCreateCategory(Text.literal("白名单 / 黑名单"));
        ConfigEntryBuilder entry = builder.entryBuilder();

        general.addEntry(entry.startBooleanToggle(Text.literal("总开关"), config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.enabled = v)
                .build());
        general.addEntry(entry.startDoubleField(Text.literal("最大锁定距离（格）"), config.aimRange)
                .setMin(1.0)
                .setMax(256.0)
                .setDefaultValue(100.0)
                .setSaveConsumer(v -> config.aimRange = v)
                .build());
        general.addEntry(entry.startFloatField(Text.literal("锁定角度（度）"), config.aimFov)
                .setMin(1.0f)
                .setMax(180.0f)
                .setDefaultValue(120.0f)
                .setSaveConsumer(v -> config.aimFov = v)
                .build());
        general.addEntry(entry.startDoubleField(Text.literal("瞄准响应速度"), config.rotationSpeed)
                .setMin(2.0)
                .setMax(30.0)
                .setDefaultValue(8.0)
                .setSaveConsumer(v -> config.rotationSpeed = v)
                .build());
        general.addEntry(entry.startDoubleField(Text.literal("瞄准抖动（度，0=关闭）"), config.aimNoise)
                .setMin(0.0)
                .setMax(3.0)
                .setDefaultValue(0.3)
                .setSaveConsumer(v -> config.aimNoise = v)
                .build());
        general.addEntry(entry.startBooleanToggle(Text.literal("瞄准头部"), config.aimAtHead)
                .setDefaultValue(false)
                .setSaveConsumer(v -> config.aimAtHead = v)
                .build());
        general.addEntry(entry.startBooleanToggle(Text.literal("方块视线检测"), config.checkLineOfSight)
                .setDefaultValue(false)
                .setSaveConsumer(v -> config.checkLineOfSight = v)
                .build());
        general.addEntry(entry.startBooleanToggle(Text.literal("显示 HUD 指示"), config.showHud)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.showHud = v)
                .build());

        for (AimTargetCategory category : AimTargetCategory.values()) {
            targets.addEntry(entry.startBooleanToggle(
                            Text.literal("瞄准: " + category.displayName),
                            config.targetCategories.contains(category))
                    .setDefaultValue(category == AimTargetCategory.PLAYER)
                    .setSaveConsumer(v -> {
                        if (v) {
                            config.targetCategories.add(category);
                        } else {
                            config.targetCategories.remove(category);
                        }
                    })
                    .build());
        }

        lists.addEntry(entry.startBooleanToggle(Text.literal("白名单模式"), config.whitelistEnabled)
                .setDefaultValue(false)
                .setSaveConsumer(v -> config.whitelistEnabled = v)
                .build());
        lists.addEntry(entry.startStrList(Text.literal("白名单"), config.whitelist)
                .setDefaultValue(List.of())
                .setSaveConsumer(v -> config.whitelist = new ArrayList<>(v))
                .build());
        lists.addEntry(entry.startStrList(Text.literal("黑名单"), config.blacklist)
                .setDefaultValue(List.of())
                .setSaveConsumer(v -> config.blacklist = new ArrayList<>(v))
                .build());

        return builder.build();
    }
}
