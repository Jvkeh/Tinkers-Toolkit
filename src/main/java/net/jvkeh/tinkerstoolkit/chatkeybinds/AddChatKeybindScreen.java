package net.jvkeh.tinkerstoolkit.chatkeybinds;


import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.chatKeybinds;


public class AddChatKeybindScreen extends Screen {
    EditBox nameBox;
    EditBox commandBox;
    EditBox categoryBox;

    Button keyButton;
    Button addButton;
    Button cancelButton;

    Screen parent;

    String cmd;
    InputConstants.Key key = InputConstants.UNKNOWN;
    boolean listening;

    public AddChatKeybindScreen(String cmd_, Screen parent) {
        super(Component.empty());
        cmd = cmd_;
        this.parent = parent;
    }

    @Override
    protected void init() {
        StringWidget categoryText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.category"), minecraft.font);
        categoryText.setX(width / 4);
        categoryText.setY((height / 2) - 96);
        addRenderableOnly(categoryText);

        categoryBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        categoryBox.setPosition(width / 2 - (categoryBox.getWidth() / 2), categoryText.getY() + categoryText.getHeight());
        addRenderableWidget(categoryBox);

        StringWidget nameText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.name"), minecraft.font);
        nameText.setX(width / 4);
        nameText.setY(categoryBox.getY() + categoryBox.getHeight() + 4);
        addRenderableOnly(nameText);

        nameBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        nameBox.setPosition(width / 2 - (nameBox.getWidth() / 2), nameText.getY() + nameText.getHeight());
        addRenderableWidget(nameBox);

        StringWidget commandText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.command"), minecraft.font);
        commandText.setX(width / 4);
        commandText.setY(nameBox.getY() + nameBox.getHeight() + 4);
        addRenderableOnly(commandText);

        commandBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        categoryBox.setResponder(this::onCommandChanged);
        nameBox.setResponder(this::onCommandChanged);
        commandBox.setPosition(width / 2 - (commandBox.getWidth() / 2), commandText.getY() + commandText.getHeight());
        addRenderableWidget(commandBox);
        this.setInitialFocus(commandBox);

        int buttonWidth = 64;
        int buttonHeight = 16;

        keyButton = Button.builder(Component.empty(), this::onListenButton)
                .bounds(commandBox.getX() + commandBox.getWidth() - buttonWidth,
                        commandBox.getY() + commandBox.getHeight() + 6,
                        buttonWidth,
                        buttonHeight)
                .build();
        addRenderableWidget(keyButton);
        updateKeyText();

        StringWidget keyText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.keybind"), minecraft.font);
        keyText.setX(keyButton.getX() - keyText.getWidth() - 2);
        keyText.setY(keyButton.getY() + (keyButton.getHeight() / 2) - (keyText.getHeight() / 2));
        addRenderableOnly(keyText);

        addButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.addScreen.add"), this::onAdd)
                .bounds(keyButton.getX(),
                        keyButton.getY() + keyButton.getHeight() + 6,
                        buttonWidth,
                        buttonHeight)
                .build();
        addRenderableWidget(addButton);

        cancelButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.addScreen.cancel"), this::onCancel)
                .bounds(addButton.getX() - buttonWidth - 2,
                        addButton.getY(),
                        buttonWidth,
                        buttonHeight)
                .build();
        addRenderableWidget(cancelButton);
    }

    void onListenButton(Button b) {
        listening = true;
        updateKeyText();
    }

    public boolean keyPressed(@NonNull KeyEvent event) {
        if (listening) {
            if (event.isEscape()) {
                key = InputConstants.UNKNOWN;
            } else {
                key = InputConstants.getKey(event);
            }
            listening = false;
            updateKeyText();
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            add();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (listening) {
            key = InputConstants.Type.MOUSE.getOrCreate(event.button());
            listening = false;
            updateKeyText();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    void updateKeyText() {
        if (listening)
            keyButton.setMessage(Component.literal("..."));
        else
            keyButton.setMessage(key.getDisplayName());
    }

    void onCommandChanged(String c) {
        cmd = c;
    }

    void onAdd(Button b) {
        add();
    }

    void onCancel(Button b) {
        onClose();
    }

    void add() {
        String command = commandBox.getValue().strip();
        String name = nameBox.getValue().strip();
        String categoryString = categoryBox.getValue().strip().toLowerCase();
        KeyMapping.Category category;

        if (command.isEmpty())
            return;

        if (name.isEmpty())
            name = cmd;

        if (categoryString.isEmpty()) {
            category = new KeyMapping.Category(
                    Identifier.fromNamespaceAndPath("tinkerstoolkit", "userbinds"));
        } else {
            category = new KeyMapping.Category(
                    Identifier.fromNamespaceAndPath("tinkerstoolkit", categoryString));
            if (!KeyMapping.Category.SORT_ORDER.contains(category)) {
                int index = KeyMapping.Category.SORT_ORDER.indexOf(KeyMapping.Category.MOVEMENT);
                KeyMapping.Category.SORT_ORDER.add(index, category);
            }
        }
        chatKeybinds.add(new ChatKeybind(name, command, key, category).registerCommand());
        ConfigManager.saveConfig();
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
