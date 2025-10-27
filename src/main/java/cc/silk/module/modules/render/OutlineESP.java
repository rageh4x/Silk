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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;

import java.awt.*;

public class OutlineESP extends Module {

    private static OutlineESP instance;

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final BooleanSetting teamCheck = new BooleanSetting("Team Check", false);
    private final NumberSetting range = new NumberSetting("Range", 10, 200, 100, 5);
    private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 255, 255));
    private final ColorSetting mobColor = new ColorSetting("Mob Color", new Color(255, 0, 0));
    private final ColorSetting animalColor = new ColorSetting("Animal Color", new Color(0, 255, 0));

    public OutlineESP() {
        super("Outline ESP", "Uses Minecraft's glowing effect for entity outlines", Category.RENDER);
        instance = this;
        addSettings(players, mobs, animals, showSelf, teamCheck, range, playerColor, mobColor, animalColor);
    }

    public static OutlineESP getInstance() {
        return instance;
    }

    public boolean shouldEntityGlow(Entity entity) {
        if (!isEnabled() || isNull()) return false;
        return shouldRender(entity);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (shouldRender(entity)) {
                applyColor(entity);
            }
        }
    }

    private boolean shouldRender(Entity entity) {
        if (mc.player.distanceTo(entity) > range.getValue()) return false;

        if (entity instanceof PlayerEntity player) {
            if (!players.getValue()) return false;
            if (player == mc.player && !showSelf.getValue()) return false;
            if (teamCheck.getValue() && isTeammate(player)) return false;
            return true;
        }

        if (entity instanceof HostileEntity) {
            return mobs.getValue();
        }

        if (entity instanceof PassiveEntity) {
            return animals.getValue();
        }

        return false;
    }

    private boolean isTeammate(PlayerEntity player) {
        if (mc.player.getScoreboardTeam() == null || player.getScoreboardTeam() == null) {
            return false;
        }
        return mc.player.getScoreboardTeam().equals(player.getScoreboardTeam());
    }

    private void applyColor(Entity entity) {
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
            return playerColor.getValue();
        }

        if (entity instanceof HostileEntity) {
            return mobColor.getValue();
        }

        if (entity instanceof PassiveEntity) {
            return animalColor.getValue();
        }

        return playerColor.getValue();
    }

    private net.minecraft.util.Formatting getClosestMinecraftColor(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        if (r > 200 && g < 100 && b < 100) return net.minecraft.util.Formatting.RED;
        if (r < 100 && g > 200 && b < 100) return net.minecraft.util.Formatting.GREEN;
        if (r < 100 && g < 100 && b > 200) return net.minecraft.util.Formatting.BLUE;
        if (r > 200 && g > 200 && b < 100) return net.minecraft.util.Formatting.YELLOW;
        if (r > 200 && g < 100 && b > 200) return net.minecraft.util.Formatting.LIGHT_PURPLE;
        if (r < 100 && g > 200 && b > 200) return net.minecraft.util.Formatting.AQUA;
        if (r > 200 && g > 200 && b > 200) return net.minecraft.util.Formatting.WHITE;
        if (r < 100 && g < 100 && b < 100) return net.minecraft.util.Formatting.DARK_GRAY;

        return net.minecraft.util.Formatting.WHITE;
    }

    @Override
    public void onDisable() {
        if (!isNull()) {
            for (Entity entity : mc.world.getEntities()) {
                Team team = entity.getScoreboardTeam();
                if (team != null && team.getName().startsWith("outlineESP_")) {
                    mc.world.getScoreboard().removeTeam(team);
                }
            }
        }
        super.onDisable();
    }
}
