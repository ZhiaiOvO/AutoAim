package top.zamc.autoaim.client.config;

import top.zamc.autoaim.client.target.AimTargetCategory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 自动瞄准配置，字段使用 Gson 序列化到 config/autoaim.json。
 * 新增字段时请在默认值处给出合理初始值，缺失字段时 Gson 会保留 Java 默认值。
 */
public class AutoaimConfig {
    /** 总开关（可通过 /autoaim toggle 切换） */
    public boolean enabled = true;
    /** 可瞄准的实体类型（可多选），默认仅玩家 */
    public Set<AimTargetCategory> targetCategories = EnumSet.of(AimTargetCategory.PLAYER);
    /** 最大锁定距离（格） */
    public double aimRange = 100.0;
    /** 与准星方向的夹角上限（度，0~180），超出则不锁定 */
    public float aimFov = 120.0f;
    /** 瞄准响应速度（临界阻尼弹簧的自然频率，越高越跟手、越低越平滑） */
    public double rotationSpeed = 8.0;
    /** 瞄准抖动（度），模拟手抖让转向更自然，0 表示关闭 */
    public double aimNoise = 0.3;
    /** true 瞄准头部，false 瞄准身体中心 */
    public boolean aimAtHead = false;
    /** 是否做方块视线检测（目标被墙挡住则不锁定） */
    public boolean checkLineOfSight = false;
    /** 白名单模式开关：开启后仅锁定白名单中的玩家 */
    public boolean whitelistEnabled = false;
    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>();
    /** 是否显示 HUD 指示 */
    public boolean showHud = true;
}
