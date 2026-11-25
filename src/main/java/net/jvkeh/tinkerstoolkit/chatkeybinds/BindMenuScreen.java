package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Map;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.chatKeybindMap;


public class BindMenuScreen extends Screen implements ModMenuApi {
    public BindList.Entry bindKeySelected;
    Screen parent;
    BindList list;
    StringWidget screenTitle;
    Button doneButton;
    Button reloadButton;
    Button addButton;
    Button removeButton;

    public BindMenuScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int padding = 8;
        int buttonHeight = 20;
        int buttonWidth = 150;

        Component title = Component.translatable("tinkerstoolkit.keybinds.menuScreen.title");
        int textWidth = this.font.width(title);
        screenTitle = new StringWidget(title, minecraft.font);
        screenTitle.setX(width / 2 - (textWidth / 2));
        screenTitle.setY(12);
        addRenderableOnly(screenTitle);

        list = new BindList(this, width, height - 64 - padding * 2 - screenTitle.getHeight());
        list.updateEntries();
        addRenderableWidget(list);

        addButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.new"), this::onAdd)
                .bounds(list.getX() + list.getWidth() / 2 - (buttonWidth + padding), list.getHeight() + list.getY() + padding, buttonWidth, buttonHeight)
                .build();
        addRenderableWidget(addButton);

        reloadButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.reload"), this::onReload)
                .bounds(addButton.getX(), addButton.getY() + addButton.getHeight() + padding / 2, addButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(reloadButton);

        removeButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.remove"), this::onRemove)
                .bounds(list.getX() + list.getWidth() / 2 + padding / 2, addButton.getY(), reloadButton.getWidth(), buttonHeight)
                .build();
        removeButton.active = false;
        addRenderableWidget(removeButton);

        doneButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.done"), this::onClose)
                .bounds(removeButton.getX(), reloadButton.getY(), removeButton.getWidth(), buttonHeight)
                .build();

        addRenderableWidget(doneButton);


    }

    void onAdd(Button w) {
        this.minecraft.gui.setScreen(new AddChatKeybindScreen(this));
    }

    void onRemove(Button w) {
        BindList.Entry entry = list.getSelected();
        if (entry != null) {
            entry.chatKeybind.removeCommand();
            chatKeybindMap.forEach((key, array) -> {
                array.removeIf(keybind ->
                        keybind.name.equals(entry.chatKeybind.name) &&
                                keybind.command.equals(entry.chatKeybind.command) &&
                                keybind.keyMapping.same(entry.chatKeybind.keyMapping));
            });
            ConfigManager.saveConfig();
            list.updateEntries();
        }
    }

    void onReload(Button w) {
        ConfigManager.loadConfig();
        init();
    }

    void onClose(Button w) {
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (bindKeySelected != null) {
            if (event.isEscape()) {
                bindKeySelected.chatKeybind.keyMapping.setKey(InputConstants.UNKNOWN);
            } else {
                bindKeySelected.chatKeybind.keyMapping.setKey(InputConstants.getKey(event));
            }
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();

            bindKeySelected.update();
            bindKeySelected = null;

            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (bindKeySelected != null) {
            bindKeySelected.chatKeybind.keyMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            KeyMapping.resetMapping();
            bindKeySelected.update();
            bindKeySelected = null;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    static class BindList extends ObjectSelectionList<BindList.Entry> {
        static int padding = 8;
        public BindMenuScreen parent;

        public BindList(BindMenuScreen parent, int w, int h) {
            super(parent.minecraft, w, h, 0, 32);
            this.setY(parent.screenTitle.getY() + parent.screenTitle.getHeight() + padding);
            this.parent = parent;
            updateEntries();
        }

        @Override
        public int getRowWidth() {
            return 300 + padding * 2;
        }

        public void updateEntries() {
            this.clearEntries();
            for (Map.Entry<String, ArrayList<ChatKeybind>> category : chatKeybindMap.entrySet()) {
                for (ChatKeybind keybind : category.getValue()) {
                    Entry e = new Entry(minecraft, keybind, this);
                    addEntry(e);
                }
            }
        }

        static class Entry extends ObjectSelectionList.Entry<Entry> implements GuiEventListener {
            public ChatKeybind chatKeybind;
            Minecraft client;
            BindList parent;
            Button keyButton;

            Entry(Minecraft client, ChatKeybind keybind, BindList parent) {
                super();
                this.client = client;
                this.chatKeybind = keybind;
                this.parent = parent;
                keyButton = Button.builder(Component.empty(), this::onButton).build();
                update();
            }

            public void update() {
                keyButton.setMessage(chatKeybind.keyMapping.getTranslatedKeyMessage());
            }

            void onButton(Button b) {
                parent.parent.bindKeySelected = this;
                keyButton.setMessage(Component.literal("..."));
            }

            @Override
            public @NonNull Component getNarration() {
                return Component.empty();
            }

            @Override
            public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
                parent.setSelected(this);
                parent.parent.removeButton.active = true;
                keyButton.mouseClicked(event, doubleClick);
                return true;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                String category = Component.translatable("key.category.tinkerstoolkit." + chatKeybind.keyMapping.getCategory().id().getPath()).getString();
                graphics.text(client.font, Component.literal(chatKeybind.name), getX() + padding, getY() + 2, 0xFFFFFFFF);
                graphics.text(client.font, Component.literal(chatKeybind.command), getX() + padding, getY() + 12, 0xFF888888);
                graphics.text(client.font, Component.literal(category), getX() + padding, getY() + 22, 0xFF888888);

                keyButton.setWidth(64);
                keyButton.setHeight(24);

                keyButton.setX(getX() + this.getContentWidth() - keyButton.getWidth());
                keyButton.setY(getY() + 4);

                keyButton.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
    }
}
