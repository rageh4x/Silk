package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;

import java.awt.*;

public class OutlineESP extends Module {

    private static OutlineESP instance;

    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final BooleanSetting teamCheck = new BooleanSetting("Team Check", false);
    private final NumberSetting range = new NumberSetting("Range", 10, 200, 100, 5);
    private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 255, 255));

    private final java.util.Set<Integer> handledEntities = new java.util.HashSet<>();

    public OutlineESP() {
        super("Outline ESP", "Uses Minecraft's glowing effect for player outlines", Category.RENDER);
        instance = this;
        addSettings(showSelf, teamCheck, range, playerColor);
    }

    public static OutlineESP getInstance() {
        return instance;
    }

    public boolean shouldEntityGlow(Entity entity) {
        if (!isEnabled() || isNull())
            return false;
        return shouldRender(entity);
    }

    public boolean wasHandledByModule(Entity entity) {
        return handledEntities.contains(entity.getId());
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull())
            return;

        for (Entity entity : mc.world.getEntities()) {
            if (shouldRender(entity)) {
                applyColor(entity);
            } else {
                if (handledEntities.contains(entity.getId())) {
                    handledEntities.remove(entity.getId());
                    Team team = entity.getScoreboardTeam();
                    if (team != null && team.getName().startsWith("outlineESP_")) {
                        mc.world.getScoreboard().removeScoreHolderFromTeam(entity.getNameForScoreboard(), team);
                        if (team.getPlayerList().isEmpty()) {
                            mc.world.getScoreboard().removeTeam(team);
                        }
                    }
                }
            }
        }
    }

    private boolean shouldRender(Entity entity) {
        if (!(entity instanceof PlayerEntity player))
            return false;
        if (mc.player.distanceTo(entity) > range.getValue())
            return false;
        if (player == mc.player && !showSelf.getValue())
            return false;
        if (teamCheck.getValue() && isTeammate(player))
            return false;
        return true;
    }

    private boolean isTeammate(PlayerEntity player) {
        if (mc.player.getScoreboardTeam() == null || player.getScoreboardTeam() == null) {
            return false;
        }
        return mc.player.getScoreboardTeam().equals(player.getScoreboardTeam());
    }

    private void applyColor(Entity entity) {
        handledEntities.add(entity.getId());

        Team team = entity.getScoreboardTeam();
        if (team == null) {
            String teamName = "outlineESP_" + entity.getId();
            team = mc.world.getScoreboard().getTeam(teamName);
            if (team == null) {
                team = mc.world.getScoreboard().addTeam(teamName);
            }
            mc.world.getScoreboard().addScoreHolderToTeam(entity.getNameForScoreboard(), team);
        }

        Color color = getColorForEntity(entity);
        team.setColor(getClosestMinecraftColor(color));
    }

    private Color getColorForEntity(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (FriendManager.isFriend(player.getUuid())) {
                return new Color(128, 0, 128);
            }
        }
        return playerColor.getValue();
    }

    private net.minecraft.util.Formatting getClosestMinecraftColor(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        if (r > 200 && g < 100 && b < 100)
            return net.minecraft.util.Formatting.RED;
        if (r < 100 && g > 200 && b < 100)
            return net.minecraft.util.Formatting.GREEN;
        if (r < 100 && g < 100 && b > 200)
            return net.minecraft.util.Formatting.BLUE;
        if (r > 200 && g > 200 && b < 100)
            return net.minecraft.util.Formatting.YELLOW;
        if (r > 200 && g < 100 && b > 200)
            return net.minecraft.util.Formatting.LIGHT_PURPLE;
        if (r < 100 && g > 200 && b > 200)
            return net.minecraft.util.Formatting.AQUA;
        if (r > 200 && g > 200 && b > 200)
            return net.minecraft.util.Formatting.WHITE;
        if (r < 100 && g < 100 && b < 100)
            return net.minecraft.util.Formatting.DARK_GRAY;

        return net.minecraft.util.Formatting.WHITE;
    }

    @Override
    public void onDisable() {
        if (!isNull()) {
            for (Entity entity : mc.world.getEntities()) {
                if (handledEntities.contains(entity.getId())) {
                    Team team = entity.getScoreboardTeam();
                    if (team != null && team.getName().startsWith("outlineESP_")) {
                        mc.world.getScoreboard().removeScoreHolderFromTeam(entity.getNameForScoreboard(), team);
                        mc.world.getScoreboard().removeTeam(team);
                    }
                }
            }
            handledEntities.clear();
        }
        super.onDisable();
    }
}
