package top.zamc.autoaim.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import top.zamc.autoaim.client.config.AutoaimConfig;
import top.zamc.autoaim.client.config.AutoaimConfigManager;
import top.zamc.autoaim.client.target.AimTargetCategory;

import java.util.Set;

/**
 * 自动瞄准核心逻辑。视角按渲染帧更新（由 WorldRenderEvents.LAST 每帧调用），
 * 相比按 tick（20次/秒）更新转向更平滑、不卡顿。
 */
public class AimController {
    /** 1.21.9+ 起按键分类不再是字符串，而是 KeyBinding.Category（翻译键形如 key.category.<命名空间>.<路径>） */
    public static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of("autoaim", "autoaim"));
    public static final String KEY_AIM = "key.autoaim.aim";
    public static final KeyBinding AIM_KEY =
            new KeyBinding(KEY_AIM, InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_R, KEY_CATEGORY);

    private final MinecraftClient client;
    /** 当前锁定目标，供 HUD 显示，同时用于目标保持（不来回切换） */
    private LivingEntity currentTarget;
    /** 弹簧模型的角速度状态（度/秒），跨帧连续 → 转向平滑无步进 */
    private double aimVelYaw;
    private double aimVelPitch;
    /** 上一帧 tick 进度，用于计算真实帧间隔 */
    private float lastTickProgress = -1.0f;
    /** 一次性 HUD 诊断输出，仅首次锁定目标时打印一次 */
    private static boolean hudDiagnosed = false;

    public AimController(MinecraftClient client) {
        this.client = client;
    }

    /** 每个渲染帧调用一次（世界渲染阶段） */
    public void onRenderFrame(RenderTickCounter tickCounter) {
        AutoaimConfig config = AutoaimConfigManager.getConfig();
        ClientPlayerEntity player = client.player;
        if (!config.enabled || player == null || client.world == null
                || client.currentScreen != null || !AIM_KEY.isPressed()) {
            currentTarget = null;
            resetAimState();
            return;
        }

        // 目标保持：当前目标仍有效就继续锁定，避免在多个目标间来回跳动
        LivingEntity target = findTarget(config, player, currentTarget);
        currentTarget = target;
        if (target == null) {
            resetAimState();
            return;
        }
        rotateTowards(config, tickCounter, player, target);
    }

    /** 松开按键/无目标时复位弹簧状态，避免再次触发时从旧状态“飞”起来 */
    private void resetAimState() {
        lastTickProgress = -1.0f;
        aimVelYaw = 0.0;
        aimVelPitch = 0.0;
    }

    /** 每渲染帧调用一次（HUD 阶段），绘制锁定指示 */
    public void renderHud(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!AutoaimConfigManager.getConfig().showHud || currentTarget == null) {
            return;
        }
        String name = currentTarget.getName().getString();
        String prefix = "[AutoAim] ";
        int h = client.textRenderer.fontHeight;
        int prefixW = client.textRenderer.getWidth(prefix);
        int w = prefixW + client.textRenderer.getWidth(name);
        drawContext.fill(2, 2, 4 + w + 2, 4 + h + 2, 0x80000000);
        if (!hudDiagnosed) {
            hudDiagnosed = true;
            var drawable = client.textRenderer.prepare(prefix + name, 4.0f, 4.0f, 0xFFFFFF55, true, 0);
            System.out.println("[AutoAim] HUD renderHud 触发，target=" + name
                    + "，glyphScreenRect=" + drawable.getScreenRect()
                    + "，textRenderer=" + client.textRenderer);
        }
        drawContext.drawTextWithShadow(client.textRenderer, prefix, 4, 4, 0xFFFFFF55);
        drawContext.drawTextWithShadow(client.textRenderer, name, 4 + prefixW, 4, 0xFFFF5555);
    }

    private void rotateTowards(AutoaimConfig config, RenderTickCounter tickCounter,
                               LivingEntity player, LivingEntity target) {
        Vec3d aimPoint = aimPoint(config, target);
        Vec3d dir = aimPoint.subtract(player.getEyePos());
        double dx = dir.x;
        double dy = dir.y;
        double dz = dir.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4) {
            return;
        }
        double targetYaw = Math.toDegrees(MathHelper.atan2(-dx, dz));
        double targetPitch = Math.toDegrees(MathHelper.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
        // 叠加小幅随机抖动模拟手抖，避免每次都精确锁定同一位置
        if (config.aimNoise > 0) {
            targetYaw += (client.world.random.nextDouble() * 2 - 1) * config.aimNoise;
            targetPitch += (client.world.random.nextDouble() * 2 - 1) * config.aimNoise;
        }

        double dt = frameDelta(tickCounter);

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        double yawError = MathHelper.wrapDegrees(targetYaw - yaw);
        double pitchError = MathHelper.wrapDegrees(targetPitch - pitch);

        // 临界阻尼弹簧：速度连续、先加速后减速，无逐帧步进 → 平滑且不机械
        double omega = config.rotationSpeed;
        double omega2 = omega * omega;
        aimVelYaw += (-2.0 * omega * aimVelYaw + omega2 * yawError) * dt;
        aimVelPitch += (-2.0 * omega * aimVelPitch + omega2 * pitchError) * dt;
        float newYaw = (float) (yaw + aimVelYaw * dt);
        float newPitch = MathHelper.clamp((float) (pitch + aimVelPitch * dt), -90.0f, 90.0f);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
        player.setHeadYaw(newYaw);
        player.setBodyYaw(newYaw);
    }

    /** 计算两帧间的真实时间间隔（秒），帧率无关；钳制上限避免卡顿瞬间角度大跳 */
    private double frameDelta(RenderTickCounter tickCounter) {
        float progress = tickCounter.getTickProgress(true);
        double dt;
        if (lastTickProgress >= 0) {
            dt = (progress - lastTickProgress + 1.0) % 1.0 / 20.0;
        } else {
            dt = 0.016;
        }
        lastTickProgress = progress;
        return Math.min(dt, 0.05);
    }

    /** preferred 为当前已锁定的目标：只要它仍满足所有条件就优先返回它，不再抢最近的 */
    private LivingEntity findTarget(AutoaimConfig config, ClientPlayerEntity player, LivingEntity preferred) {
        ClientWorld world = client.world;
        if (world == null) {
            return null;
        }
        LivingEntity best = null;
        double bestDistSq = config.aimRange * config.aimRange;
        Vec3d eye = player.getEyePos();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living == player || !living.isAlive() || living.isSpectator()) {
                continue;
            }
            if (!matchesSelectedCategories(config, living)) {
                continue;
            }
            if (!isAllowed(config, living)) {
                continue;
            }
            double distSq = player.squaredDistanceTo(living);
            if (distSq > bestDistSq) {
                continue;
            }
            if (!isWithinFov(config, eye, player, living)) {
                continue;
            }
            if (config.checkLineOfSight && !hasLineOfSight(world, player, aimPoint(config, living))) {
                continue;
            }
            if (preferred != null && preferred == living) {
                return preferred;
            }
            best = living;
            bestDistSq = distSq;
        }
        return best;
    }

    private boolean matchesSelectedCategories(AutoaimConfig config, LivingEntity entity) {
        Set<AimTargetCategory> categories = config.targetCategories;
        if (categories == null || categories.isEmpty()) {
            return false;
        }
        for (AimTargetCategory category : categories) {
            if (category.matches(entity)) {
                return true;
            }
        }
        return false;
    }

    /** 白名单/黑名单仅对玩家生效；其他实体由分类过滤决定是否锁定 */
    private boolean isAllowed(AutoaimConfig config, LivingEntity entity) {
        if (entity instanceof PlayerEntity p) {
            String name = p.getName().getString();
            if (config.blacklist.contains(name)) {
                return false;
            }
            if (config.whitelistEnabled) {
                return config.whitelist.contains(name);
            }
        }
        return true;
    }

    private boolean isWithinFov(AutoaimConfig config, Vec3d eye, LivingEntity player, LivingEntity target) {
        float fov = config.aimFov;
        if (fov >= 180.0f) {
            return true;
        }
        Vec3d look = player.getRotationVector();
        Vec3d toTarget = aimPoint(config, target).subtract(eye).normalize();
        return look.dotProduct(toTarget) >= Math.cos(Math.toRadians(fov));
    }

    private boolean hasLineOfSight(ClientWorld world, LivingEntity player, Vec3d aimPoint) {
        Vec3d from = player.getEyePos();
        RaycastContext context = new RaycastContext(
                from, aimPoint, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
        BlockHitResult hit = world.raycast(context);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        return from.squaredDistanceTo(hit.getPos()) >= from.squaredDistanceTo(aimPoint);
    }

    private Vec3d aimPoint(AutoaimConfig config, LivingEntity target) {
        if (config.aimAtHead) {
            return target.getEyePos();
        }
        Box box = target.getBoundingBox();
        return box != null ? box.getCenter() : target.getEntityPos();
    }
}
