package cc.silk.module.modules.render;

import cc.silk.event.impl.render.EventRender2D;
import cc.silk.mixin.WorldRendererAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.utils.render.DrawUtils;
import cc.silk.utils.render.W2SUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ESP2D extends Module {

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Passives", "Hostiles", "All");
    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final ModeSetting boxMode = new ModeSetting("Box Mode", "Full", "Full", "Corners");
    private final BooleanSetting boxSetting = new BooleanSetting("Box", true);
    private final BooleanSetting boxFill = new BooleanSetting("Box Fill", false);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting nameTags = new BooleanSetting("Name Tags", false);
    private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 255, 255));
    private final ColorSetting passiveColor = new ColorSetting("Passive Color", new Color(0, 255, 0));
    private final ColorSetting hostileColor = new ColorSetting("Hostile Color", new Color(255, 0, 0));



    public ESP2D() {
        super("2D ESP", "Draws 2D boxes around entities", -1, Category.RENDER);
        addSettings(targets, showSelf, boxMode, boxSetting, boxFill, healthBar, nameTags, playerColor, passiveColor, hostileColor);
    }



    @EventHandler
    private void onRender2D(EventRender2D event) {
        if (isNull() || mc.world == null || mc.player == null)
            return;

        Matrix4f matrix = event.getContext().getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        for (Entity entity : mc.world.getEntities()) {
            BufferBuilder bufferBuilder = null;

            if (!(entity instanceof LivingEntity))
                continue;

            if (!shouldRender(entity))
                continue;

            Box box = entity.getBoundingBox();

            if (!((WorldRendererAccessor) mc.worldRenderer).getFrustum().isVisible(box))
                continue;

            double x = entity.prevX + (entity.getX() - entity.prevX) * mc.getRenderTickCounter().getTickDelta(false);
            double y = entity.prevY + (entity.getY() - entity.prevY) * mc.getRenderTickCounter().getTickDelta(false);
            double z = entity.prevZ + (entity.getZ() - entity.prevZ) * mc.getRenderTickCounter().getTickDelta(false);

            Box expandedBox = new Box(
                    box.minX - entity.getX() + x - 0.05,
                    box.minY - entity.getY() + y,
                    box.minZ - entity.getZ() + z - 0.05,
                    box.maxX - entity.getX() + x + 0.05,
                    box.maxY - entity.getY() + y + 0.1,
                    box.maxZ - entity.getZ() + z + 0.05
            );

            Vec3d[] vectors = new Vec3d[]{
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
                        position = new Vector4d(vectorToScreen.x, vectorToScreen.y, vectorToScreen.z, 0);
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

                Color color = getColorForEntity(entity);

                bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                if (boxSetting.getValue()) {
                    if (boxMode.getMode().equals("Full")) {
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, posY, posX + 0.5f, endPosY + 0.5f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, posY - 0.5f, endPosX + 0.5f, posY + 1.0f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - 1.0f, posY, endPosX + 0.5f, endPosY + 0.5f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, endPosY - 1.0f, endPosX + 0.5f, endPosY + 0.5f, Color.BLACK);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, posY, posX, endPosY, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX, endPosY - 0.5f, endPosX, endPosY, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, posY, endPosX, posY + 0.5f, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - 0.5f, posY, endPosX, endPosY, color);
                    } else {
                        float cornerLength = (endPosX - posX) * 0.25f;

                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, posY, posX + 0.5f, posY + cornerLength + 0.5f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, posY - 0.5f, posX + cornerLength + 0.5f, posY + 1.0f, Color.BLACK);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - 1.0f, posY, endPosX + 0.5f, posY + cornerLength + 0.5f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - cornerLength - 1.0f, posY - 0.5f, endPosX + 0.5f, posY + 1.0f, Color.BLACK);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, endPosY - cornerLength - 1.0f, posX + 0.5f, endPosY + 0.5f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 1.0f, endPosY - 1.0f, posX + cornerLength + 0.5f, endPosY + 0.5f, Color.BLACK);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - 1.0f, endPosY - cornerLength - 1.0f, endPosX + 0.5f, endPosY + 0.5f, Color.BLACK);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - cornerLength - 1.0f, endPosY - 1.0f, endPosX + 0.5f, endPosY + 0.5f, Color.BLACK);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, posY, posX, posY + cornerLength, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, posY, posX + cornerLength, posY + 0.5f, color);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - 0.5f, posY, endPosX, posY + cornerLength, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - cornerLength, posY, endPosX, posY + 0.5f, color);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, endPosY - cornerLength, posX, endPosY, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, endPosY - 0.5f, posX + cornerLength, endPosY, color);

                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - 0.5f, endPosY - cornerLength, endPosX, endPosY, color);
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX - cornerLength, endPosY - 0.5f, endPosX, endPosY, color);
                    }

                    if (boxFill.getValue()) {
                        DrawUtils.rectPoint4(bufferBuilder, matrix, endPosX, endPosY, posX, posY, new Color(0, 0, 0, 80));
                    }
                }

                if (healthBar.getValue() && entity instanceof LivingEntity living) {
                    float healthRatio = living.getHealth() / living.getMaxHealth();
                    float healthBarTop = endPosY + (posY - endPosY) * healthRatio;
                    float yOffset = 0.3f;

                    DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 5.5f, posY, posX - 2.5f, endPosY, Color.BLACK);
                    DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 5.0f, healthBarTop - yOffset, posX - 3f, healthBarTop, Color.BLACK);
                    DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 5.0f, endPosY, posX - 3f, endPosY + yOffset, Color.BLACK);

                    DrawUtils.rectPoint4VerticalGradient(bufferBuilder, matrix, posX - 5.0f, healthBarTop, posX - 3.0f, endPosY, color, color.darker().darker());
                }

                if (nameTags.getValue()) {
                    DrawUtils.rectPoint4(bufferBuilder, matrix, posX - 0.5f, posY - 6f, endPosX, posY - 1f, new Color(0, 0, 0, 120));
                }
            }

            if (bufferBuilder != null && (boxSetting.getValue() || boxFill.getValue() || healthBar.getValue() || nameTags.getValue())) {
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            }
        }

        RenderSystem.disableBlend();
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

        return switch (targets.getMode()) {
            case "Players" -> entity instanceof PlayerEntity;
            case "Passives" -> entity instanceof PassiveEntity;
            case "Hostiles" -> entity instanceof HostileEntity;
            case "All" -> entity instanceof PlayerEntity || entity instanceof PassiveEntity || entity instanceof HostileEntity;
            default -> false;
        };
    }
}
