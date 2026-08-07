package top.zamc.autoaim.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import top.zamc.autoaim.client.command.AutoaimCommand;
import top.zamc.autoaim.client.config.AutoaimConfigManager;

public class AutoaimClient implements ClientModInitializer {

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        AutoaimConfigManager.load();
        KeyBindingHelper.registerKeyBinding(AimController.AIM_KEY);
        AutoaimCommand.register();

        AimController controller = new AimController(MinecraftClient.getInstance());
        WorldRenderEvents.LAST.register(context -> controller.onRenderFrame(context.tickCounter()));
        // 新 HUD API 的 addLast 在当前 Fabric 版本不渲染，改用经典 HudRenderCallback（必然触发）
        HudRenderCallback.EVENT.register(controller::renderHud);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> AutoaimConfigManager.save());
    }
}
