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
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.*;


public class AddChatKeybindScreen extends Screen {
    EditBox nameBox;
    EditBox commandBox;
    Button categoryButton;

    Button keyButton;
    Button addButton;
    Button cancelButton;

    Screen parent;

    InputConstants.Key key = InputConstants.UNKNOWN;
    boolean listening;

    public AddChatKeybindScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    static void addCategoryToScreen(String categoryString, KeyMapping.Category category) {
        if (!KeyMapping.Category.SORT_ORDER.contains(category)) {
            int index;
            if (categoryString.equals("userbinds")) {
                index = KeyMapping.Category.SORT_ORDER.indexOf(CHAT_KEYBBINDS_CATEGORY) + 1;
            } else {
                index = KeyMapping.Category.SORT_ORDER.indexOf(KeyMapping.Category.MOVEMENT) + 1;
            }
            KeyMapping.Category.SORT_ORDER.add(index, category);

        }
    }

    @Override
    protected void init() {
        StringWidget nameText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.name"), minecraft.font);
        nameText.setX(width / 4);
        nameText.setY((height / 2) - 96);
        addRenderableOnly(nameText);

        nameBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        nameBox.setPosition(width / 2 - (nameBox.getWidth() / 2), nameText.getY() + nameText.getHeight());
        addRenderableWidget(nameBox);

        StringWidget commandText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.command"), minecraft.font);
        commandText.setX(width / 4);
        commandText.setY(nameBox.getY() + nameBox.getHeight() + 4);
        addRenderableOnly(commandText);

        commandBox = new EditBox(minecraft.font, width / 2, 24, Component.empty());
        commandBox.setPosition(width / 2 - (commandBox.getWidth() / 2), commandText.getY() + commandText.getHeight());
        addRenderableWidget(commandBox);
        this.setInitialFocus(commandBox);

        StringWidget categoryText = new StringWidget(Component.translatable("tinkerstoolkit.keybinds.addScreen.category"), minecraft.font);
        categoryText.setX(width / 4);
        categoryText.setY(commandBox.getY() + commandBox.getHeight() + 4);
        addRenderableOnly(categoryText);

        categoryButton = Button.builder(Component.translatable("key.category.tinkerstoolkit." + CATEGORY_STRINGS.get(currentCategoryIndex)), this::onListenCategoryInput)
                .bounds(commandBox.getX(),
                        categoryText.getY() + categoryText.getHeight(),
                        width / 2,
                        24)
                .build();
        addRenderableWidget(categoryButton);

        int buttonWidth = 64;
        int buttonHeight = 16;

        keyButton = Button.builder(Component.empty(), this::onListenKeyInput)
                .bounds(categoryButton.getX() + categoryButton.getWidth() - buttonWidth,
                        categoryButton.getY() + categoryButton.getHeight() + 6,
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

    void onListenKeyInput(Button b) {
        listening = true;
        updateKeyText();
    }

    void onListenCategoryInput(Button b) {
        cycleCategory();
    }

    private void cycleCategory() {
        if (currentCategoryIndex == 2) {
            currentCategoryIndex = 0;
        } else {
            currentCategoryIndex++;
        }
        categoryButton.setMessage(Component.translatable("key.category.tinkerstoolkit." + CATEGORY_STRINGS.get(currentCategoryIndex)));
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

    void onAdd(Button b) {
        add();
    }

    void onCancel(Button b) {
        onClose();
    }

    void add() {
        String command = commandBox.getValue().strip();
        String name = nameBox.getValue().strip();
        String categoryString = "userbinds";
        ComponentContents categoryContents = categoryButton.getMessage().getContents();
        if (categoryContents instanceof TranslatableContents) {
            String[] splitCategory = ((TranslatableContents) categoryContents).getKey().split("\\.");
            categoryString = splitCategory[splitCategory.length - 1];
        }
        KeyMapping.Category category;

        if (command.isEmpty())
            return;

        if (name.isEmpty())
            name = command;

        category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath("tinkerstoolkit", categoryString));

        addCategoryToScreen(categoryString, category);
        chatKeybindMap.computeIfAbsent(categoryString, k -> new ArrayList<>()).add(new ChatKeybind(name, command, key, category));
        ConfigManager.saveConfig();
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
