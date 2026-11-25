package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.*;


public class ConfigManager {

    static Path configDir = FabricLoader.getInstance().getConfigDir().resolve("tinkers-toolkit");
    static Path keybindsConfig = configDir.resolve("chat_keybinds.json");

    static void loadConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (chatKeybindMap != null && mc.options != null) {
            ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
            for (Map.Entry<String, ArrayList<ChatKeybind>> category : chatKeybindMap.entrySet()) {
                for (ChatKeybind keybind : category.getValue()) {
                    keys.remove(keybind.keyMapping);
                }
            }
            mc.options.keyMappings = keys.toArray(new KeyMapping[0]);
        }
        chatKeybindMap = new TreeMap<>();

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(keybindsConfig)) {
                return;
            }
            String str = Files.readString(keybindsConfig);
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            addCategoryToMap("player", jsonObject);
            addCategoryToMap("storyteller", jsonObject);
            addCategoryToMap("userbinds", jsonObject);

        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    private static void addCategoryToMap(String categoryString, JsonObject jsonObject) {
        JsonArray array = jsonObject.getAsJsonArray(categoryString);
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (element instanceof JsonObject jo) {
                String name = jo.get("name").getAsString();
                String command = jo.get("command").getAsString();
                updateChatKeybindMap(categoryString, name, command);
            }
        }
    }

    static void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(ChatKeybind.class, new KeybindSerializer()).create();
        String jsonString = gson.toJson(chatKeybindMap);
        JsonObject jsonObject = new JsonObject();
        try {
            Files.createDirectories(configDir);
            Files.writeString(keybindsConfig, jsonString);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}

class KeybindSerializer implements JsonSerializer<ChatKeybind> {

    @Override
    public JsonElement serialize(ChatKeybind keybind, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("name", new JsonPrimitive(keybind.name));
        jsonObject.add("command", new JsonPrimitive(keybind.command));
        return jsonObject;
    }
}
