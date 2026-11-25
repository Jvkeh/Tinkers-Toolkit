package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class ChatKeybindsModule {

    public static final Logger LOGGER = LoggerFactory.getLogger("tinkers-toolkit/chat-keybindsbinds");
    static final List<String> CATEGORY_STRINGS = Arrays.asList("userbinds", "storyteller", "player");
    static HashMap<String, ArrayList<ChatKeybind>> chatKeybindMap = new HashMap<>();
    static KeyMapping.Category CHAT_KEYBBINDS_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tinkerstoolkit", "chatkeybinds"));
    static int currentCategoryIndex = 0;

    private static void sendMessage(String msg) {
        assert Minecraft.getInstance().player != null;
        if (msg.startsWith("/")) {
            Minecraft.getInstance().player.connection.sendCommand(msg.substring(1));
        } else
            Minecraft.getInstance().player.connection.sendChat(msg);
    }

    public void onInitialise() {
        KeyMapping menuKeybind = new KeyMapping("tinkerstoolkit.keybinds.key.menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CHAT_KEYBBINDS_CATEGORY);
        KeyMappingHelper.registerKeyMapping(menuKeybind);
        KeyMapping addChatKeybind = new KeyMapping("tinkerstoolkit.keybinds.key.add_chat_keybind",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CHAT_KEYBBINDS_CATEGORY);
        KeyMappingHelper.registerKeyMapping(addChatKeybind);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (menuKeybind.consumeClick()) {
                client.gui.setScreen(new BindMenuScreen(null));
            }
            if (addChatKeybind.consumeClick()) {
                client.gui.setScreen(new AddChatKeybindScreen(null));
            }

            for (Map.Entry<String, ArrayList<ChatKeybind>> category : chatKeybindMap.entrySet())
                for (ChatKeybind keybind : category.getValue()) {
                    if (keybind.keyMapping.consumeClick())
                        sendMessage(keybind.command);
                }
        });
        ConfigManager.loadConfig();
    }
}