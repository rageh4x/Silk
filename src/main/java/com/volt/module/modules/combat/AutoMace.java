package com.volt.module.modules.combat;

import com.volt.event.impl.player.TickEvent;
import com.volt.mixin.MinecraftClientAccessor;
import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.modules.misc.Teams;
import com.volt.module.setting.BooleanSetting;
import com.volt.module.setting.NumberSetting;
import com.volt.utils.friend.FriendManager;
import com.volt.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class AutoMace extends Module {

    private final NumberSetting minFallDistance = new NumberSetting("Min Fall Distance", 1.0, 10.0, 3.0, 0.5);
    private final NumberSetting attackDelay = new NumberSetting("Attack Delay", 50, 500, 100, 10);
    private final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Target Mobs", false);
    private final BooleanSetting stunSlam = new BooleanSetting("Stun Slam", false);
    
    private final TimerUtil attackTimer = new TimerUtil();
    private int savedSlot = -1;
    private double fallStartY = -1;
    private boolean isFalling = false;
    private boolean slamExecuted = false;

    public AutoMace() {
        super("Auto Mace", "Automatically attacks with mace", -1, Category.COMBAT);
        this.addSettings(minFallDistance, attackDelay, targetPlayers, targetMobs, stunSlam);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;

        updateFall();
        attack();
    }

    private void updateFall() {
        boolean onGround = mc.player.isOnGround();
        boolean falling = mc.player.getVelocity().y < -0.1;
        double currentY = mc.player.getY();

        if (onGround) {
            if (isFalling) {
                resetFall();
            }
            if (savedSlot != -1) {
                switchToSlot(savedSlot);
                savedSlot = -1;
            }
            return;
        }

        if (!isFalling) {
            isFalling = true;
            fallStartY = currentY;
            slamExecuted = false;
        } else if (falling && fallStartY != -1 && currentY > fallStartY) {
            fallStartY = currentY;
        }
    }

    private void attack() {
        if (!isFalling || mc.player.getVelocity().y >= -0.1) return;
        
        double fallDist = fallStartY == -1 ? 0 : Math.max(0, fallStartY - mc.player.getY());
        if (fallDist < minFallDistance.getValueFloat()) return;

        Entity target = mc.targetedEntity;
        if (target == null || !isValidTarget(target) || FriendManager.isFriend(target.getUuid())) return;

        if (stunSlam.getValue()) {
            handleSlam(target, fallDist);
        }
        
        handleMaceAttack(target);
    }

    private void handleSlam(Entity target, double fallDist) {
        boolean targetBlocking = target instanceof PlayerEntity player && 
                                player.isHolding(Items.SHIELD) && 
                                player.isBlocking();
        
        if (targetBlocking && fallDist > minFallDistance.getValueFloat() && !slamExecuted) {
            if (savedSlot == -1) savedSlot = mc.player.getInventory().selectedSlot;
            
            if (switchToAxe()) {
                ((MinecraftClientAccessor) mc).invokeDoAttack();
                slamExecuted = true;
            }
        }
        
        if (slamExecuted) {
            switchToMace();
        }
    }

    private void handleMaceAttack(Entity target) {
        if (!hasMace()) {
            if (savedSlot == -1) savedSlot = mc.player.getInventory().selectedSlot;
            switchToMace();
        }

        if (hasMace() && attackTimer.hasElapsedTime((long) attackDelay.getValue(), true)) {
            ((MinecraftClientAccessor) mc).invokeDoAttack();
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || entity == mc.cameraEntity) return false;
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (!livingEntity.isAlive() || livingEntity.isDead()) return false;
        if (Teams.isTeammate(entity)) return false;

        if (entity instanceof PlayerEntity) {
            return targetPlayers.getValue();
        } else {
            if (!targetMobs.getValue()) return false;
            if (entity instanceof PassiveEntity || entity instanceof Tameable) return false;
        }

        return true;
    }

    private boolean switchToAxe() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    private boolean hasMace() {
        return mc.player.getMainHandStack().getItem() == Items.MACE;
    }

    private void switchToMace() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MACE) {
                mc.player.getInventory().selectedSlot = i;
                return;
            }
        }
    }

    private void switchToSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    private void resetFall() {
        isFalling = false;
        fallStartY = -1;
        slamExecuted = false;
    }

    @Override
    public void onEnable() {
        savedSlot = -1;
        fallStartY = -1;
        isFalling = false;
        slamExecuted = false;
        attackTimer.reset();
    }

    @Override
    public void onDisable() {
        if (savedSlot != -1) {
            switchToSlot(savedSlot);
        }
        resetAll();
    }

    private void resetAll() {
        savedSlot = -1;
        fallStartY = -1;
        isFalling = false;
        slamExecuted = false;
        attackTimer.reset();
    }
}