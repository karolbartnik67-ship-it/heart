package me.spunkyinsaan.heartindicator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class HeartIndicatorMod implements ClientModInitializer {
    public static final String MOD_ID = "heartindicator";
    public static KeyBinding TOGGLE_KEY_BINDING;
    public static KeyBinding OPEN_KEY_BINDING;

    @Override
    public void onInitializeClient() {
        Config.load();

        // H = włącz/wyłącz, J = ustawienia (zmienisz w Sterowaniu)
        TOGGLE_KEY_BINDING = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.heartindicator.toggle", InputUtil.Type.KEYSYM, 72, "key.categories.heartindicator"));
        OPEN_KEY_BINDING = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.heartindicator.open_menu", InputUtil.Type.KEYSYM, 74, "key.categories.heartindicator"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY_BINDING.wasPressed()) {
                Config.INSTANCE.setRenderingEnabled(!Config.INSTANCE.getRenderingEnabled());
                Config.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Heart Indicator: "
                            + (Config.INSTANCE.getRenderingEnabled() ? "Enabled" : "Disabled")), true);
                }
            }
            while (OPEN_KEY_BINDING.wasPressed()) {
                if (client.currentScreen == null && client.player != null) client.setScreen(new HeartScreen());
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) HudRenderer.onAttack(entity);
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> HudRenderer.render(ctx));
    }
}
