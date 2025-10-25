package cc.silk.module.events;

import cc.silk.SilkClient;
import cc.silk.event.impl.input.MouseClickEvent;
import cc.silk.module.Module;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class MouseModuleHandler {

    @EventHandler
    public void onMouseClick(MouseClickEvent event) {
        if (event.action() == GLFW.GLFW_PRESS) {
            for (Module module : SilkClient.INSTANCE.getModuleManager().getModules()) {
                if (event.button() == module.getKey()) {
                    module.toggle();
                    break;
                }
            }
        }
    }
}