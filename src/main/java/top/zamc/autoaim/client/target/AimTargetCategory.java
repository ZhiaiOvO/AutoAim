package top.zamc.autoaim.client.target;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 可瞄准的实体类型分类。实体可能同时属于多个分类（例如守卫既是水生又是敌对），
 * 目标选择时只要命中任意一个已选分类即可。
 */
public enum AimTargetCategory {
    PLAYER("player", "玩家"),
    HOSTILE("hostile", "怪物"),
    PASSIVE("passive", "动物"),
    WATER("water", "水生生物"),
    AMBIENT("ambient", "环境生物"),
    OTHER("other", "其他");

    /** 命令中使用的名称 */
    public final String commandName;
    /** 界面显示名 */
    public final String displayName;

    AimTargetCategory(String commandName, String displayName) {
        this.commandName = commandName;
        this.displayName = displayName;
    }

    public boolean matches(LivingEntity entity) {
        switch (this) {
            case PLAYER:
                return entity instanceof PlayerEntity;
            case HOSTILE:
                return entity instanceof HostileEntity;
            case PASSIVE:
                return entity instanceof PassiveEntity;
            case WATER:
                return entity instanceof WaterCreatureEntity;
            case AMBIENT:
                return entity instanceof AmbientEntity;
            case OTHER:
                return !(entity instanceof PlayerEntity
                        || entity instanceof HostileEntity
                        || entity instanceof PassiveEntity
                        || entity instanceof WaterCreatureEntity
                        || entity instanceof AmbientEntity);
            default:
                return false;
        }
    }

    public static AimTargetCategory fromName(String name) {
        for (AimTargetCategory category : values()) {
            if (category.commandName.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }
}
