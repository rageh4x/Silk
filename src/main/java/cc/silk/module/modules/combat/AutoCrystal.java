package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.ItemUseEvent;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.KeybindSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.keybinding.KeyUtils;
import cc.silk.utils.math.TimerUtil;
import cc.silk.utils.mc.InventoryUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * @author koi
 */
public final class AutoCrystal extends Module {

    private final KeybindSetting crystalKey = new KeybindSetting("Crystal Key", GLFW.GLFW_MOUSE_BUTTON_4, false);

    private final NumberSetting minBreakDelay = new NumberSetting("Min Break Delay (MS)", 1, 500, 10, 1);
    private final NumberSetting maxBreakDelay = new NumberSetting("Max Break Delay (MS)", 1, 500, 50, 1);
    private final NumberSetting minPlaceDelay = new NumberSetting("Min Place Delay (MS)", 1, 500, 10, 1);
    private final NumberSetting maxPlaceDelay = new NumberSetting("Max Place Delay (MS)", 1, 500, 50, 1);

    private final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide", true);

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private final BooleanSetting antiWeakness = new BooleanSetting("Anti Weakness", true);

    private final TimerUtil breakTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private final Random random = new Random();

    private int originalSlot = -1;
    private long currentBreakDelay;
    private long currentPlaceDelay;

    public AutoCrystal() {
        super("Auto Crystal", "Hold key to spam crystals", -1, Category.COMBAT);
        this.addSettings(crystalKey, minBreakDelay, maxBreakDelay, minPlaceDelay, maxPlaceDelay, antiSuicide, autoSwitch, switchBack, antiWeakness);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(crystalKey));
        resetDelays();
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;
        if (mc.currentScreen != null) return;

        if (minBreakDelay.getValueFloat() >= maxBreakDelay.getValueFloat())
            minBreakDelay.setValue(maxBreakDelay.getValueFloat() - 1);
        if (minPlaceDelay.getValueFloat() >= maxPlaceDelay.getValueFloat())
            minPlaceDelay.setValue(maxPlaceDelay.getValueFloat() - 1);

        if (!KeyUtils.isKeyPressed(crystalKey.getKeyCode())) return;

        processCrystal();
    }

    private void resetDelays() {
        breakTimer.reset();
        placeTimer.reset();

        int minBreak = minBreakDelay.getValueInt();
        int maxBreak = maxBreakDelay.getValueInt();
        int minPlace = minPlaceDelay.getValueInt();
        int maxPlace = maxPlaceDelay.getValueInt();

        if (minBreak >= maxBreak) {
            maxBreak = minBreak + 1;
            maxBreakDelay.setValue(maxBreak);
        }
        if (minPlace >= maxPlace) {
            maxPlace = minPlace + 1;
            maxPlaceDelay.setValue(maxPlace);
        }

        currentBreakDelay = random.nextLong(minBreak, maxBreak);
        currentPlaceDelay = random.nextLong(minPlace, maxPlace);
    }

    private void processCrystal() {
        if (antiSuicide.getValue() && !mc.player.isOnGround()) return;

        if (mc.crosshairTarget instanceof EntityHitResult entityHit && breakTimer.hasElapsedTime(currentBreakDelay)) {
            if (entityHit.getEntity() instanceof EndCrystalEntity crystal) {
                if (!crystal.isRemoved() && crystal.isAlive() && mc.world.getEntityById(crystal.getId()) != null) {
                    if (mc.player.getPos().distanceTo(crystal.getPos()) <= 4.5) {
                        if (antiWeakness.getValue() && mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.WEAKNESS)) {
                            InventoryUtil.swapToWeapon(SwordItem.class);
                        }
                        ((MinecraftClientAccessor) mc).invokeDoAttack();
                        breakTimer.reset();
                        currentBreakDelay = random.nextLong(minBreakDelay.getValueInt(), maxBreakDelay.getValueInt());
                    }
                }
                return;
            }
        }

        if (mc.crosshairTarget instanceof BlockHitResult blockHit && placeTimer.hasElapsedTime(currentPlaceDelay)) {
            BlockPos targetBlock = blockHit.getBlockPos();
            BlockPos placementPos = targetBlock.offset(blockHit.getSide());

            if (isObsidianOrBedrock(targetBlock) && isValidCrystalPosition(placementPos)) {
                if (autoSwitch.getValue() && hasItemInHotbar()) {
                    InventoryUtil.swapToSlot(Items.END_CRYSTAL);
                }

                if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    placeTimer.reset();
                    currentPlaceDelay = random.nextLong(minPlaceDelay.getValueInt(), maxPlaceDelay.getValueInt());
                }
            }
        }
    }

    private boolean hasItemInHotbar() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.END_CRYSTAL) return true;
        }
        return false;
    }

    private boolean isObsidianOrBedrock(BlockPos pos) {
        if (mc.world == null) return false;
        var block = mc.world.getBlockState(pos).getBlock();
        return block == net.minecraft.block.Blocks.OBSIDIAN || block == net.minecraft.block.Blocks.BEDROCK;
    }

    private boolean isValidCrystalPosition(BlockPos pos) {
        if (mc.world == null) return false;
        if (mc.player.getPos().distanceTo(Vec3d.ofCenter(pos)) > 4.5) return false;

        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;

        BlockPos playerPos = mc.player.getBlockPos();
        return !pos.equals(playerPos) && !pos.equals(playerPos.up());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (autoSwitch.getValue()) originalSlot = mc.player.getInventory().selectedSlot;
        resetDelays();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (switchBack.getValue() && originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        originalSlot = -1;
    }

    @EventHandler
    public void onItemUse(ItemUseEvent event) {
        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
            if (mc.crosshairTarget instanceof BlockHitResult hit &&
                    mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {

                var blockPos = hit.getBlockPos();
                var blockState = mc.world.getBlockState(blockPos);
                var block = blockState.getBlock();

                if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                    event.cancel();
                }
            }
        }
    }


    @Override
    public int getKey() {
        return -1;
    }
}
