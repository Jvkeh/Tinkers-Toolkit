package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.LOGGER;
import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.chatKeybinds;


public class ConfigManager {

    static Path configDir = FabricLoader.getInstance().getConfigDir().resolve("tinkers-toolkit");
    static Path keybindsConfig = configDir.resolve("chat_keybinds.json");

    static void loadConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (chatKeybinds != null && mc.options != null) {
            ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
            for (ChatKeybind chatKeybind : chatKeybinds)
                keys.remove(chatKeybind.keyMapping);
            mc.options.keyMappings = keys.toArray(new KeyMapping[0]);
        }
        chatKeybinds = new ArrayList<>();

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(keybindsConfig)) {
                return;
            }

            String str = Files.readString(keybindsConfig);
            JsonArray ja = (JsonArray) JsonParser.parseString(str);

            for (JsonElement je : ja) {
                if (je instanceof JsonObject jo) {

                    String name = jo.get("name").getAsString();
                    String command = jo.get("command").getAsString();

                    KeyMapping.Category category = new KeyMapping.Category(
                            Identifier.fromNamespaceAndPath(
                                    "tinkerstoolkit",
                                    jo.get("category").getAsString()
                            )
                    );
                    if (!KeyMapping.Category.SORT_ORDER.contains(category)) {
                        int index = KeyMapping.Category.SORT_ORDER.indexOf(KeyMapping.Category.MULTIPLAYER);
                        KeyMapping.Category.SORT_ORDER.add(index, category);
                    }
                    chatKeybinds.add(new ChatKeybind(name, command, InputConstants.UNKNOWN, category).registerCommand());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    static void saveConfig() {
        JsonArray ja = new JsonArray();
        for (ChatKeybind chatKeybind : chatKeybinds) {
            JsonObject jo = new JsonObject();
            jo.add("category", new JsonPrimitive(chatKeybind.keyMapping.getCategory().id().getPath()));
            jo.add("name", new JsonPrimitive(chatKeybind.name));
            jo.add("command", new JsonPrimitive(chatKeybind.command));
            ja.add(jo);
        }

        String json = new Gson().toJson(ja);

        try {
            Files.createDirectories(configDir);
            Files.writeString(keybindsConfig, json);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
