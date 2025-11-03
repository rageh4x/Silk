package cc.silk.module.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.EventRender3D;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import cc.silk.utils.render.CompatShaders;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public final class SquareESP extends Module {

    private static final Identifier LEGEND_TEXTURE = Identifier.of("silk", "imgs/legend.png");

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Living");
    private final NumberSetting size = new NumberSetting("Size", 0.1, 2.0, 0.8, 0.05);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 0.5, 10.0, 3.0, 0.1);
    private final NumberSetting heightOffset = new NumberSetting("Height Offset", 0.0, 2.0, 1.0, 0.1);
    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255, 255));

    public SquareESP() {
        super("Square ESP", "Rotating square texture on players", -1, Category.RENDER);
        addSettings(targets, size, rotationSpeed, heightOffset, showSelf, color);
    }

    @EventHandler
    private void onRender3D(EventRender3D e) {
        if (isNull())
            return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, LEGEND_TEXTURE);
        CompatShaders.usePositionTexColor();

        float squareSize = size.getValueFloat();
        float yOff = heightOffset.getValueFloat();
        Color c = color.getValue();
        int colorInt = c.getRGB();

        float rotation = (System.currentTimeMillis() % 100000) / 50.0f * rotationSpeed.getValueFloat();

        for (var entity : mc.world.getEntities()) {
            if (!shouldRender(entity))
                continue;

            double tPosX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta - cam.x;
            double tPosY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta - cam.y + yOff;
            double tPosZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta - cam.z;

            if (mc.player.canSee(entity)) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.disableDepthTest();
            }

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR);

            MatrixStack matrices = new MatrixStack();
            matrices.translate(tPosX, tPosY, tPosZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            buffer.vertex(matrix, -squareSize, squareSize, 0).texture(0f, 1f).color(colorInt);
            buffer.vertex(matrix, squareSize, squareSize, 0).texture(1f, 1f).color(colorInt);
            buffer.vertex(matrix, squareSize, -squareSize, 0).texture(1f, 0).color(colorInt);
            buffer.vertex(matrix, -squareSize, -squareSize, 0).texture(0, 0).color(colorInt);

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            if (mc.player.canSee(entity)) {
                RenderSystem.depthMask(true);
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private boolean shouldRender(net.minecraft.entity.Entity entity) {
        if (entity == mc.player && !showSelf.getValue())
            return false;
        if (targets.isMode("Players"))
            return entity instanceof PlayerEntity;
        return entity instanceof LivingEntity;
    }
}
