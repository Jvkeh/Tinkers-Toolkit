package net.jvkeh.tinkerstoolkit.chatkeybinds;


import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.KeybindCategory;
import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.updateChatKeybindMap;


public class AddChatKeybindScreen extends Screen {
    EditBox nameBox;
    EditBox commandBox;
    CycleButton<KeybindCategory> categoryButton;
    CycleButton<ChatKeybind.Translatable> translatableButton;
    Button addButton;
    Screen parent;
    private ChatKeybind.Translatable translatable = ChatKeybind.Translatable.NO;
    private int widgetX;

    public AddChatKeybindScreen(Screen parent) {
        super(Component.translatable("tinkerstoolkit.keybinds.addScreen.title"));
        this.parent = parent;
    }

    public ChatKeybind.Translatable getTranslatable() {
        return translatable;
    }

    public void setTranslatable(ChatKeybind.Translatable translatable) {
        this.translatable = translatable;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        // TITLE
        graphics.centeredText(this.font, this.title, this.width / 2, 17, 0xFFFFFFFF);
        // NAME TEXT
        graphics.text(this.font, Component.translatable("tinkerstoolkit.keybinds.addScreen.name"), widgetX + 1, 53, 0xFFA0A0A0);
        // COMMAND TEXT
        graphics.text(this.font, Component.translatable("tinkerstoolkit.keybinds.addScreen.command"), widgetX + 1, 118, 0xFFA0A0A0);
    }

    @Override
    protected void init() {
        widgetX = this.width / 2 - 100;
        int widgetWidth = 200;
        int widgetHeight = 20;
        nameBox = new EditBox(this.font, widgetX, 66, widgetWidth, widgetHeight, Component.empty());
        addRenderableWidget(nameBox);
        translatableButton = CycleButton.builder(ChatKeybind.Translatable::getName, getTranslatable())
                .withValues(ChatKeybind.Translatable.values())
                .create(
                        widgetX,
                        nameBox.getY() + 24,
                        widgetWidth,
                        widgetHeight,
                        Component.translatable("tinkerstoolkit.keybinds.addScreen.translatable"),
                        (_, value) -> this.setTranslatable(value)
                );

        addRenderableWidget(translatableButton);
        commandBox = new EditBox(this.font, widgetX, translatableButton.getY() + 40, widgetWidth, widgetHeight, Component.empty());
        commandBox.setResponder(this::onAddButtonUpdate);
        addRenderableWidget(commandBox);
        this.setInitialFocus(commandBox);
        categoryButton = CycleButton.builder(KeybindCategory::getName, ChatKeybindsModule.getKeybindCategory())
                .withValues(KeybindCategory.values())
                .create(
                        widgetX,
                        commandBox.getY() + 24,
                        widgetWidth,
                        widgetHeight,
                        Component.translatable("tinkerstoolkit.keybinds.addScreen.category"),
                        (_, value) -> ChatKeybindsModule.setKeybindCategory(value)
                );
        addRenderableWidget(categoryButton);
        this.addButton = this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, _ -> this.onAdd())
                        .bounds(widgetX, categoryButton.getY() + 30, widgetWidth, widgetHeight).build()
        );
        this.addButton.active = false;
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, _ -> this.onClose())
                        .bounds(widgetX, addButton.getY() + 24, widgetWidth, widgetHeight)
                        .build()
        );
    }

    private void onAddButtonUpdate(String s) {
        this.addButton.active = !this.commandBox.getValue().isEmpty();
    }

    void onAdd() {
        String command = commandBox.getValue().strip();
        String name = nameBox.getValue().strip();
        KeybindCategory category = categoryButton.getValue();
        if (command.isEmpty())
            return;

        if (name.isEmpty())
            name = command;

        updateChatKeybindMap(category, name, command, InputConstants.UNKNOWN, translatable);
        ConfigManager.saveConfig();
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
