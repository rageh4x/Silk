package cc.silk.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.EventRender3D;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public final class ShaderTest extends Module {

    private static ShaderTest instance;

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Living", "All");
    private final NumberSetting outlineWidth = new NumberSetting("Outline Width", 1.0, 10.0, 2.0, 0.5);
    private final NumberSetting glowMultiplier = new NumberSetting("Glow Multiplier", 0.0, 10.0, 3.5, 0.1);
    private final NumberSetting fillAlpha = new NumberSetting("Fill Alpha", 0, 255, 255, 1);
    private final BooleanSetting ignoreSelf = new BooleanSetting("Ignore Self", true);
    private final BooleanSetting showPlayers = new BooleanSetting("Show Players", true);
    private final BooleanSetting showAnimals = new BooleanSetting("Show Animals", false);
    private final BooleanSetting showMonsters = new BooleanSetting("Show Monsters", false);
    private final BooleanSetting showAmbient = new BooleanSetting("Show Ambient", false);
    private final BooleanSetting showMisc = new BooleanSetting("Show Misc", false);
    private final ColorSetting playersColor = new ColorSetting("Players Color", new Color(255, 255, 255, 255));
    private final ColorSetting animalsColor = new ColorSetting("Animals Color", new Color(25, 255, 25, 255));
    private final ColorSetting monstersColor = new ColorSetting("Monsters Color", new Color(255, 25, 25, 255));
    private final ColorSetting ambientColor = new ColorSetting("Ambient Color", new Color(25, 25, 25, 255));
    private final ColorSetting miscColor = new ColorSetting("Misc Color", new Color(200, 200, 200, 255));

    public ShaderTest() {
        super("Shader Test", "Shader ESP", -1, Category.RENDER);
        instance = this;
        addSettings(targets, outlineWidth, glowMultiplier, fillAlpha, ignoreSelf,
                showPlayers, showAnimals, showMonsters, showAmbient, showMisc,
                playersColor, animalsColor, monstersColor, ambientColor, miscColor);
    }

    public static ShaderTest getInstance() {
        return instance;
    }

    public boolean shouldEntityGlow(Entity entity) {
        if (!isEnabled()) {
            return false;
        }
        return shouldRender(entity);
    }

    @EventHandler
    private void onRender3D(EventRender3D event) {
        if (isNull())
            return;

        MatrixStack matrices = event.getMatrixStack();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        OutlineVertexConsumerProvider outlines = mc.getBufferBuilders().getOutlineVertexConsumers();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float width = Math.max(1.0f, Math.min(10.0f, outlineWidth.getValueFloat()));

        for (Entity e : mc.world.getEntities()) {
            if (!shouldRender(e))
                continue;

            Color entityColor = getEntityColor(e);
            if (entityColor != null) {
                float glowMult = (float) glowMultiplier.getValue();
                glowMult = Math.max(0.0f, Math.min(10.0f, glowMult));

                float fillAlphaValue = fillAlpha.getValueFloat() / 255.0f;
                RenderSystem.setShaderColor(
                        entityColor.getRed() / 255.0f,
                        entityColor.getGreen() / 255.0f,
                        entityColor.getBlue() / 255.0f,
                        fillAlphaValue);
                renderEntityWithProvider(e, tickDelta, matrices, immediate, 15728880);

                int outlineAlpha = (int) Math.min(255, entityColor.getAlpha() * glowMult / 3.5f);
                outlineAlpha = Math.max(1, outlineAlpha);

                float brightnessMultiplier = 1.0f + (glowMult - 1.0f) * 0.3f;
                int r = Math.min(255, (int) (entityColor.getRed() * brightnessMultiplier));
                int g = Math.min(255, (int) (entityColor.getGreen() * brightnessMultiplier));
                int b = Math.min(255, (int) (entityColor.getBlue() * brightnessMultiplier));

                float widthValue = Math.max(1.0f, Math.min(10.0f, width));
                int passCount = Math.max(1, (int) (widthValue * 2));
                float scaleStep = widthValue * 0.002f;

                for (int pass = 0; pass < passCount; pass++) {
                    matrices.push();
                    float currentScale = 1.0f + (scaleStep * pass);
                    matrices.scale(currentScale, currentScale, currentScale);

                    int passAlpha = (int) (outlineAlpha * (1.0f - (pass / (float) passCount) * 0.5f));
                    passAlpha = Math.max(30, passAlpha);

                    outlines.setColor(r, g, b, passAlpha);
                    renderEntityWithProvider(e, tickDelta, matrices, outlines, 15728880);
                    matrices.pop();
                }
            }
        }

        immediate.draw();
        outlines.draw();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private boolean shouldRender(Entity entity) {
        if (ignoreSelf.getValue() && entity == mc.player)
            return false;

        if (targets.isMode("Players")) {
            return entity instanceof PlayerEntity && showPlayers.getValue();
        } else if (targets.isMode("Living")) {
            if (!(entity instanceof LivingEntity))
                return false;
            if (entity instanceof PlayerEntity)
                return showPlayers.getValue();
        }

        if (entity instanceof PlayerEntity) {
            return showPlayers.getValue();
        }

        SpawnGroup spawnGroup = entity.getType().getSpawnGroup();
        return switch (spawnGroup) {
            case CREATURE -> showAnimals.getValue();
            case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> showAnimals.getValue();
            case MONSTER -> showMonsters.getValue();
            case AMBIENT -> showAmbient.getValue();
            default -> showMisc.getValue();
        };
    }

    public NumberSetting getOutlineWidth() {
        return outlineWidth;
    }

    public NumberSetting getGlowMultiplier() {
        return glowMultiplier;
    }

    public NumberSetting getFillAlpha() {
        return fillAlpha;
    }

    public Color getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return playersColor.getValue();
        }

        SpawnGroup spawnGroup = entity.getType().getSpawnGroup();
        return switch (spawnGroup) {
            case CREATURE -> animalsColor.getValue();
            case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> animalsColor.getValue();
            case MONSTER -> monstersColor.getValue();
            case AMBIENT -> ambientColor.getValue();
            default -> miscColor.getValue();
        };
    }

    private void renderEntityWithProvider(Entity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider provider, int light) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta - cam.x;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta - cam.y;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta - cam.z;
        mc.getEntityRenderDispatcher().render(entity, x, y, z, entity.getYaw(tickDelta), tickDelta, matrices, provider,
                light);
    }
}
