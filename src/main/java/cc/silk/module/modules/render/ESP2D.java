package cc.silk.module.modules.render;

import cc.silk.event.impl.render.EventRender2D;
import cc.silk.mixin.WorldRendererAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.render.W2SUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;

import java.awt.*;

public class ESP2D extends Module {

        private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Passives", "Hostiles",
                        "All");
        private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
        private final NumberSetting range = new NumberSetting("Range", 10, 200, 64, 5);
        private final ModeSetting boxMode = new ModeSetting("Box Mode", "Full", "Full", "Corners");
        private final BooleanSetting boxSetting = new BooleanSetting("Box", true);
        private final BooleanSetting boxFill = new BooleanSetting("Box Fill", false);
        private final NumberSetting fillOpacity = new NumberSetting("Fill Opacity", 0, 255, 80, 5);
        private final BooleanSetting gradientFill = new BooleanSetting("Gradient Fill", false);
        private final ColorSetting fillColor1 = new ColorSetting("Fill Color 1", new Color(255, 0, 0, 80));
        private final ColorSetting fillColor2 = new ColorSetting("Fill Color 2", new Color(0, 0, 255, 80));
        private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
        private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 255, 255));
        private final ColorSetting passiveColor = new ColorSetting("Passive Color", new Color(0, 255, 0));
        private final ColorSetting hostileColor = new ColorSetting("Hostile Color", new Color(255, 0, 0));

        public ESP2D() {
                super("2D ESP", "Draws 2D boxes around entities", -1, Category.RENDER);
                addSettings(targets, showSelf, range, boxMode, boxSetting, boxFill, fillOpacity, gradientFill,
                                fillColor1,
                                fillColor2, healthBar, playerColor, passiveColor, hostileColor);
        }

        @EventHandler
        private void onRender2D(EventRender2D event) {
                if (isNull() || mc.world == null || mc.player == null)
                        return;

                cc.silk.utils.render.NanoVGRenderer.beginFrame();

                for (Entity entity : mc.world.getEntities()) {
                        if (!(entity instanceof LivingEntity))
                                continue;

                        if (!shouldRender(entity))
                                continue;

                        Box box = entity.getBoundingBox();

                        if (!((WorldRendererAccessor) mc.worldRenderer).getFrustum().isVisible(box))
                                continue;

                        double x = entity.prevX + (entity.getX() - entity.prevX)
                                        * mc.getRenderTickCounter().getTickDelta(false);
                        double y = entity.prevY + (entity.getY() - entity.prevY)
                                        * mc.getRenderTickCounter().getTickDelta(false);
                        double z = entity.prevZ + (entity.getZ() - entity.prevZ)
                                        * mc.getRenderTickCounter().getTickDelta(false);

                        Box expandedBox = new Box(
                                        box.minX - entity.getX() + x - 0.05,
                                        box.minY - entity.getY() + y,
                                        box.minZ - entity.getZ() + z - 0.05,
                                        box.maxX - entity.getX() + x + 0.05,
                                        box.maxY - entity.getY() + y + 0.1,
                                        box.maxZ - entity.getZ() + z + 0.05);

                        Vec3d[] vectors = new Vec3d[] {
                                        new Vec3d(expandedBox.minX, expandedBox.minY, expandedBox.minZ),
                                        new Vec3d(expandedBox.minX, expandedBox.maxY, expandedBox.minZ),
                                        new Vec3d(expandedBox.maxX, expandedBox.minY, expandedBox.minZ),
                                        new Vec3d(expandedBox.maxX, expandedBox.maxY, expandedBox.minZ),
                                        new Vec3d(expandedBox.minX, expandedBox.minY, expandedBox.maxZ),
                                        new Vec3d(expandedBox.minX, expandedBox.maxY, expandedBox.maxZ),
                                        new Vec3d(expandedBox.maxX, expandedBox.minY, expandedBox.maxZ),
                                        new Vec3d(expandedBox.maxX, expandedBox.maxY, expandedBox.maxZ),
                        };

                        Vector4d position = null;

                        for (Vec3d vector : vectors) {
                                Vec3d vectorToScreen = W2SUtil.getCoords(vector);

                                if (vectorToScreen.z > 0 && vectorToScreen.z < 1) {
                                        if (position == null) {
                                                position = new Vector4d(vectorToScreen.x, vectorToScreen.y,
                                                                vectorToScreen.z, 0);
                                        }

                                        position.x = Math.min(vectorToScreen.x, position.x);
                                        position.y = Math.min(vectorToScreen.y, position.y);
                                        position.z = Math.max(vectorToScreen.x, position.z);
                                        position.w = Math.max(vectorToScreen.y, position.w);
                                }
                        }

                        if (position != null) {
                                float posX = (float) position.x;
                                float posY = (float) position.y;
                                float endPosX = (float) position.z;
                                float endPosY = (float) position.w;

                                float lineThickness = 1f;

                                Color entityColor = getColorForEntity(entity);

                                if (boxFill.getValue()) {
                                        if (gradientFill.getValue()) {
                                                int opacity = fillOpacity.getValueInt();
                                                Color color1 = new Color(fillColor1.getValue().getRed(),
                                                                fillColor1.getValue().getGreen(),
                                                                fillColor1.getValue().getBlue(), opacity);
                                                Color color2 = new Color(fillColor2.getValue().getRed(),
                                                                fillColor2.getValue().getGreen(),
                                                                fillColor2.getValue().getBlue(), opacity);
                                                cc.silk.utils.render.NanoVGRenderer.drawRoundedRectGradient(posX, posY,
                                                                endPosX - posX,
                                                                endPosY - posY, 0f, color1, color2);
                                        } else {
                                                Color fillColor = new Color(entityColor.getRed(),
                                                                entityColor.getGreen(),
                                                                entityColor.getBlue(), fillOpacity.getValueInt());
                                                cc.silk.utils.render.NanoVGRenderer.drawRect(posX, posY, endPosX - posX,
                                                                endPosY - posY,
                                                                fillColor);
                                        }
                                }

                                if (boxSetting.getValue()) {
                                        if (boxMode.getMode().equals("Full")) {
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY, posX,
                                                                endPosY - 1, lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY, endPosX, posY,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX, posY, endPosX,
                                                                endPosY - 1, lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, endPosY - 1, endPosX,
                                                                endPosY - 1,
                                                                lineThickness,
                                                                Color.BLACK);

                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY, posX,
                                                                endPosY - 1, lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY, endPosX, posY,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX, posY, endPosX,
                                                                endPosY - 1, lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, endPosY - 1, endPosX,
                                                                endPosY - 1,
                                                                lineThickness,
                                                                entityColor);
                                        } else {
                                                float cornerLength = (endPosX - posX) * 0.25f;

                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY, posX,
                                                                posY + cornerLength,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY,
                                                                posX + cornerLength, posY,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX, posY, endPosX,
                                                                posY + cornerLength,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX - cornerLength,
                                                                posY, endPosX, posY,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX,
                                                                endPosY - cornerLength, posX, endPosY,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, endPosY,
                                                                posX + cornerLength, endPosY,
                                                                lineThickness,
                                                                Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX,
                                                                endPosY - cornerLength, endPosX, endPosY,
                                                                lineThickness, Color.BLACK);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX - cornerLength,
                                                                endPosY, endPosX, endPosY,
                                                                lineThickness, Color.BLACK);

                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY, posX,
                                                                posY + cornerLength,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, posY,
                                                                posX + cornerLength, posY,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX, posY, endPosX,
                                                                posY + cornerLength,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX - cornerLength,
                                                                posY, endPosX, posY,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX,
                                                                endPosY - cornerLength, posX, endPosY,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(posX, endPosY,
                                                                posX + cornerLength, endPosY,
                                                                lineThickness,
                                                                entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX,
                                                                endPosY - cornerLength, endPosX, endPosY,
                                                                lineThickness, entityColor);
                                                cc.silk.utils.render.NanoVGRenderer.drawLine(endPosX - cornerLength,
                                                                endPosY, endPosX, endPosY,
                                                                lineThickness, entityColor);
                                        }
                                }

                                if (healthBar.getValue() && entity instanceof LivingEntity livingEntity) {
                                        float health = livingEntity.getHealth();
                                        float maxHealth = livingEntity.getMaxHealth();
                                        float healthPercent = Math.min(health / maxHealth, 1f);

                                        float boxWidth = endPosX - posX;
                                        float barWidth = Math.max(2f, Math.min(3f, boxWidth * 0.05f));
                                        float barHeight = endPosY - posY;
                                        float barX = posX - barWidth - 2;
                                        float barY = posY;

                                        Color barBg = new Color(0, 0, 0, 150);
                                        cc.silk.utils.render.NanoVGRenderer.drawRect(barX, barY, barWidth, barHeight,
                                                        barBg);

                                        float healthBarHeight = barHeight * healthPercent;
                                        float healthBarY = barY + (barHeight - healthBarHeight);

                                        int red = (int) ((1f - healthPercent) * 255);
                                        int green = (int) (healthPercent * 255);
                                        Color healthColor = new Color(red, green, 0, 255);

                                        cc.silk.utils.render.NanoVGRenderer.drawRect(barX, healthBarY, barWidth,
                                                        healthBarHeight,
                                                        healthColor);
                                }
                        }
                }

                cc.silk.utils.render.NanoVGRenderer.endFrame();
        }

        private Color getColorForEntity(Entity entity) {
                if (entity instanceof PlayerEntity)
                        return playerColor.getValue();
                if (entity instanceof PassiveEntity)
                        return passiveColor.getValue();
                if (entity instanceof HostileEntity)
                        return hostileColor.getValue();
                return Color.WHITE;
        }

        private boolean shouldRender(Entity entity) {
                if (entity == mc.player && !showSelf.getValue())
                        return false;

                if (mc.player.distanceTo(entity) > range.getValue())
                        return false;

                return switch (targets.getMode()) {
                        case "Players" -> entity instanceof PlayerEntity;
                        case "Passives" -> entity instanceof PassiveEntity;
                        case "Hostiles" -> entity instanceof HostileEntity;
                        case "All" ->
                                entity instanceof PlayerEntity || entity instanceof PassiveEntity
                                                || entity instanceof HostileEntity;
                        default -> false;
                };
        }
}
