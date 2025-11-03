package cc.silk.module.modules.misc;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.HandledScreenAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class HoverTotem extends Module {
    public static final NumberSetting maxMS = new NumberSetting("Max MS", 1, 1000, 400, 0.5);
    public static final NumberSetting minMS = new NumberSetting("Min MS", 1, 1000, 200, 0.5);
    public static final BooleanSetting ignoreOffhand = new BooleanSetting("Ignore Offhand", false);

    private final NumberSetting normalBaseDelay = new NumberSetting("Base Delay", 50, 1000, 200, 1);
    private final NumberSetting normalVariation = new NumberSetting("Variation", 10, 1000, 150, 1);
    private final NumberSetting normalSDThreshold = new NumberSetting("SD Threshold", 1, 500, 80, 1);

    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();

    private final List<Long> recentIntervals = new ArrayList<>();
    private final List<Double> recentStandardDeviations = new ArrayList<>();
    private int swapCount = 0;

    public HoverTotem() {
        super("Hover Totem", "Puts a totem in your offhand and hotbar once hovered", -1, Category.MISC);
        this.addSettings(maxMS, minMS, ignoreOffhand, normalBaseDelay, normalVariation, normalSDThreshold);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;

        if (minMS.getValueFloat() >= maxMS.getValueFloat()) {
            minMS.setValue(maxMS.getValueFloat() - 0.5);
        }

        totemHandler();
    }

    private void totemHandler() {
        if (isNull()) return;
        if (!(mc.currentScreen instanceof InventoryScreen inv)) return;

        Slot focusedSlot = ((HandledScreenAccessor) inv).getFocusedSlot();
        if (focusedSlot == null) return;

        ItemStack focusedItemStack = focusedSlot.getStack();
        if (focusedItemStack.getItem() != Items.TOTEM_OF_UNDYING) return;

        if (focusedSlot.getIndex() < 9) return;

        ItemStack offhandItem = mc.player.getOffHandStack();
        boolean needsOffhand = offhandItem.isEmpty() || (offhandItem.getItem() != Items.TOTEM_OF_UNDYING && !ignoreOffhand.getValue());
        boolean needsHotbar = !hasTotemInHotbar();

        if (!needsOffhand && !needsHotbar) return;

        long delay = generateNormalModeDelay();

        if (timer.hasElapsedTime(delay, true)) {
            assert mc.player != null;
            assert mc.interactionManager != null;

            if (needsOffhand) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        focusedSlot.getIndex(),
                        40,
                        SlotActionType.SWAP,
                        mc.player
                );
            } else {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        focusedSlot.getIndex(),
                        0,
                        SlotActionType.QUICK_MOVE,
                        mc.player
                );
            }
        }
    }

    private boolean hasTotemInHotbar() {
        if (isNull()) return false;
        return mc.player.getInventory().main.subList(0, 9).stream()
                .anyMatch(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING);
    }

    private long generateNormalModeDelay() {
        swapCount++;

        long baseDelay = normalBaseDelay.getValueInt() + random.nextInt(normalVariation.getValueInt());

        if (swapCount % 3 == 0) {
            baseDelay += (normalVariation.getValueInt() / 2) + random.nextInt(normalVariation.getValueInt());
        } else if (swapCount % 5 == 0) {
            baseDelay = Math.max(50, normalBaseDelay.getValueInt() / 3);
        }

        double gaussian = random.nextGaussian() * (normalSDThreshold.getValue() / 2);
        baseDelay += (long) Math.abs(gaussian);

        if (recentIntervals.size() >= 2) {
            double avgInterval = calculateDoubleAverage(recentIntervals);
            if (Math.abs(baseDelay - avgInterval) < normalSDThreshold.getValue() / 2) {
                baseDelay += random.nextBoolean() ?
                        normalVariation.getValueInt() / 2 + random.nextInt(normalVariation.getValueInt() / 2) :
                        -(normalBaseDelay.getValueInt() / 2 + random.nextInt(normalBaseDelay.getValueInt() / 2));
            }
        }

        updateStandardDeviationTracking();
        if (recentStandardDeviations.size() >= 3) {
            double avgSD = calculateAverage(recentStandardDeviations);
            if (avgSD < normalSDThreshold.getValue()) {
                baseDelay += random.nextBoolean() ? normalVariation.getValueInt() : -normalVariation.getValueInt() / 2;
            }
        }

        long finalDelay = Math.max(80, Math.min(2000, baseDelay));
        recentIntervals.add(finalDelay);
        if (recentIntervals.size() > 10) recentIntervals.remove(0);

        return finalDelay;
    }

    private double calculateDoubleAverage(List<Long> list) {
        return list.stream().mapToDouble(Long::doubleValue).average().orElse(0);
    }

    private double calculateAverage(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private void updateStandardDeviationTracking() {
        if (recentIntervals.size() < 2) return;

        double mean = calculateDoubleAverage(recentIntervals);
        double variance = 0.0;
        for (long interval : recentIntervals) {
            variance += Math.pow(interval - mean, 2);
        }
        variance /= recentIntervals.size();
        double stdDev = Math.sqrt(variance);

        recentStandardDeviations.add(stdDev);
        if (recentStandardDeviations.size() > 10) recentStandardDeviations.remove(0);
    }

    @Override
    public void onEnable() {
        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        timer.reset();
        super.onDisable();
    }
}
