package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;


public class ChatKeybind {
    public final String name;
    public final String command;
    public KeyMapping keyMapping;

    public ChatKeybind(String name, String command, InputConstants.Key key, KeyMapping.Category category) {
        this.name = name;
        this.command = command;
        this.keyMapping = new KeyMapping(name,
                InputConstants.Type.KEYBOARD,
                key.getValue(),
                category);
    }

    ChatKeybind removeCommand() {

        Minecraft mc = Minecraft.getInstance();
        ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
        keys.remove(this.keyMapping);
        mc.options.keyMappings = keys.toArray(new KeyMapping[0]);
        return this;
    }

    public ChatKeybind registerCommand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            ArrayList<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
            keys.add(this.keyMapping);
            mc.options.keyMappings = keys.toArray(new KeyMapping[0]);
        } else
            KeyMappingHelper.registerKeyMapping(this.keyMapping);
        return this;
    }

}
