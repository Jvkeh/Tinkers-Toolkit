package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.*;


public class ConfigManager {

    static Path configDir = FabricLoader.getInstance().getConfigDir().resolve("tinkers-toolkit");
    static Path keybindsConfig = configDir.resolve("chat_keybinds.json");

    static void loadConfig() {
        Minecraft mc = Minecraft.getInstance();
        ArrayList<ChatKeybind> removedBinds = new ArrayList<>();
        if (chatKeybindMap != null && mc.options != null) {
            ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
            for (Map.Entry<KeybindCategory, ArrayList<ChatKeybind>> category : chatKeybindMap.entrySet()) {
                for (ChatKeybind keybind : category.getValue()) {
                    if (keys.remove(keybind.keyMapping)) {
                        removedBinds.add(keybind);
                    }
                }
            }
            mc.options.keyMappings = keys.toArray(new KeyMapping[0]);
            mc.options.save();
        }
        chatKeybindMap = new TreeMap<>();

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(keybindsConfig)) {
                return;
            }
            String str = Files.readString(keybindsConfig);
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            for (KeybindCategory category : KeybindCategory.values()) {
                addCategoryToMap(category, jsonObject, removedBinds);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    private static void addCategoryToMap(KeybindCategory category, JsonObject jsonObject, ArrayList<ChatKeybind> removedBinds) {
        JsonArray array = jsonObject.getAsJsonArray(category.name());
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (element instanceof JsonObject jo) {
                String name = jo.get("name").getAsString();
                String command = jo.get("command").getAsString();
                Optional<ChatKeybind> match = removedBinds.stream()
                        .filter(keybind -> keybind.name.equals(name) && keybind.command.equals(command))
                        .findFirst();
                if (match.isPresent()) {
                    updateChatKeybindMap(category, name, command, match.get().keyMapping);
                } else {
                    updateChatKeybindMap(category, name, command, InputConstants.UNKNOWN);
                }
            }
        }
    }

    static void saveConfig() {

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(ChatKeybind.class, new KeybindSerializer()).create();
        String jsonString = gson.toJson(chatKeybindMap);
        try {
            Files.createDirectories(configDir);
            Files.writeString(keybindsConfig, jsonString);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static class KeybindSerializer implements JsonSerializer<ChatKeybind> {

        @Override
        public JsonElement serialize(ChatKeybind keybind, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("name", new JsonPrimitive(keybind.name));
            jsonObject.add("command", new JsonPrimitive(keybind.command));
            return jsonObject;
        }
    }
}


