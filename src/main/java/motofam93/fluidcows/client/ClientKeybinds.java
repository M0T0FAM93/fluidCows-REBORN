package motofam93.fluidcows.client;

import motofam93.fluidcows.FluidCows;
import motofam93.fluidcows.client.gui.FluidCowConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class ClientKeybinds {

    public static KeyMapping CONFIG_KEY;

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        CONFIG_KEY = new KeyMapping("key.fluidcows.config", GLFW.GLFW_KEY_F7, "key.categories.fluidcows");
        event.register(CONFIG_KEY);
    }

    @EventBusSubscriber(modid = FluidCows.MOD_ID, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (CONFIG_KEY != null && CONFIG_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.hasPermissions(2)) {
                    mc.setScreen(new FluidCowConfigScreen(null));
                } else if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§cYou need operator permissions to access Fluid Cows config."), false);
                }
            }
        }
    }
}
