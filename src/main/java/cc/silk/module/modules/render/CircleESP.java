package cc.silk.module.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.EventRender3D;
import cc.silk.module.Category;
import cc.silk.module.Module;
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

public final class CircleESP extends Module {

    private static final Identifier CIRCLE_TEXTURE = Identifier.of("silk", "imgs/circle.png");
    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Living");
    private final NumberSetting heightMin = new NumberSetting("Height Min", 0.1, 2.5, 0.2, 0.05);
    private final NumberSetting heightMax = new NumberSetting("Height Max", 0.2, 3.0, 1.4, 0.05);
    private final NumberSetting radius = new NumberSetting("Radius", 0.2, 2.0, 0.7, 0.05);
    private final NumberSetting speed = new NumberSetting("Speed", 0.1, 5.0, 1.6, 0.1);
    private final ColorSetting color = new ColorSetting("Color", new Color(100, 255, 255, 200));

    public CircleESP() {
        super("Circle ESP", "Animated ring around players", -1, Category.RENDER);
        addSettings(targets, heightMin, heightMax, radius, speed, color);
    }

    @EventHandler
    private void onRender3D(EventRender3D event) {
        if (isNull()) return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, CIRCLE_TEXTURE);
        CompatShaders.usePositionTexColor();

        float minH = heightMin.getValueFloat();
        float maxH = heightMax.getValueFloat();
        float radius = this.radius.getValueFloat();
        Color c = color.getValue();

        double time = (System.currentTimeMillis() / 1000.0) * speed.getValue();
        double phase = (Math.sin(time) * 0.5) + 0.5;
        float y = (float) (minH + (maxH - minH) * phase);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);

        boolean hasVertices = false;

        for (var entity : mc.world.getEntities()) {
            if (!should(entity)) continue;

            hasVertices = true;

            double cx = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta - cam.x;
            double cy = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta - cam.y + y;
            double cz = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta - cam.z;

            int circleColor = c.getRGB();

            MatrixStack matrices = new MatrixStack();
            matrices.translate(cx, cy, cz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            buffer.vertex(matrix, -radius, -radius, 0).texture(0f, 0f).color(circleColor);
            buffer.vertex(matrix, -radius, radius, 0).texture(0f, 1f).color(circleColor);
            buffer.vertex(matrix, radius, radius, 0).texture(1f, 1f).color(circleColor);
            buffer.vertex(matrix, radius, -radius, 0).texture(1f, 0f).color(circleColor);
        }

        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private boolean should(net.minecraft.entity.Entity entity) {
        if (entity == mc.player) return entity instanceof PlayerEntity && targets.isMode("Players");
        if (targets.isMode("Players")) return entity instanceof PlayerEntity;
        return entity instanceof LivingEntity;
    }
}


