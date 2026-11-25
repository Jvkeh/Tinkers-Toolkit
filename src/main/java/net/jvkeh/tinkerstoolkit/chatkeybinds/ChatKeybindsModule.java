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
    static final KeyMapping.Category CHAT_KEYBBINDS_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tinkerstoolkit", "chatkeybinds"));

    static final KeyMapping.Category USERBINDS_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tinkerstoolkit", "userbinds"));
    static final KeyMapping.Category PLAYER_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("tinkerstoolkit", "player"));
    static final KeyMapping.Category STORYTELLER_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("tinkerstoolkit", "storyteller"));

    static SortedMap<String, ArrayList<ChatKeybind>> chatKeybindMap = new TreeMap<>();
    static int currentCategoryIndex = 0;

    public ChatKeybindsModule() {
        int index = KeyMapping.Category.SORT_ORDER.indexOf(KeyMapping.Category.MOVEMENT) + 1;
        KeyMapping.Category.SORT_ORDER.add(index, PLAYER_CATEGORY);
        KeyMapping.Category.SORT_ORDER.add(index, STORYTELLER_CATEGORY);
    }

    private static void sendMessage(String msg) {
        assert Minecraft.getInstance().player != null;
        if (msg.startsWith("/")) {
            Minecraft.getInstance().player.connection.sendCommand(msg.substring(1));
        } else
            Minecraft.getInstance().player.connection.sendChat(msg);
    }

    public static ChatKeybind updateChatKeybindMap(String categoryString, String name, String command) {
        ChatKeybind newBind;
        if (categoryString.equals("player")) {
            newBind = new ChatKeybind(name, command, InputConstants.UNKNOWN, PLAYER_CATEGORY);
        } else if (categoryString.equals("storyteller")) {
            newBind = new ChatKeybind(name, command, InputConstants.UNKNOWN, STORYTELLER_CATEGORY);
        } else {
            newBind = new ChatKeybind(name, command, InputConstants.UNKNOWN, USERBINDS_CATEGORY);
        }
        chatKeybindMap.computeIfAbsent(categoryString, k -> new ArrayList<>()).add(newBind);
        return newBind;
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