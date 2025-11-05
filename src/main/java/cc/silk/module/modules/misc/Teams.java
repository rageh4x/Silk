package cc.silk.module.modules.misc;

import cc.silk.SilkClient;
import cc.silk.module.Category;
import cc.silk.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.Objects;

public class Teams extends Module {

    public Teams() {
        super("Teams", "Stops you from targeting teammates", Category.MISC);
    }

    public static boolean isTeammate(Entity entity) {
        Teams teamsModule = SilkClient.INSTANCE.getModuleManager().getModule(Teams.class).get();
        if (teamsModule == null || !teamsModule.isEnabled()) {
            return false;
        }

        // Null checks
        if (entity == null || entity.getName() == null || !(entity instanceof LivingEntity)) {
            return false;
        }

        try {
            // Ensure player and scoreboard exist before checking
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.player.getScoreboardTeam() == null) {
                return false;
            }

            // Safely call isTeammate
            return client.player.isTeammate(entity);
        } catch (IllegalStateException e) {
            // Prevent crash caused by desynced team data
            return false;
        } catch (Exception e) {
            // Any unexpected errors — also ignore teammate check
            return false;
        }
    }
}
