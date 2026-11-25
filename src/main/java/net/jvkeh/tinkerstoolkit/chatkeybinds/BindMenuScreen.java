package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
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
    public BindList.Entry selectedEntry;
    Screen parent;
    BindList list;
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
        int sideBarW = 128;
        int sideBarX = width - 128;
        int padding = 8;
        int buttonHeight = 32;

        list = new BindList(this, width - sideBarW, height - 4);
        list.updateEntries();
        addRenderableWidget(list);

        doneButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.done"), this::onClose)
                .bounds(sideBarX + padding, height - buttonHeight - padding, sideBarW - (padding * 2), buttonHeight)
                .build();

        addRenderableWidget(doneButton);

        reloadButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.reload"), this::onReload)
                .bounds(doneButton.getX(), doneButton.getY() - buttonHeight - padding, doneButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(reloadButton);

        addButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.new"), this::onAdd)
                .bounds(reloadButton.getX(), reloadButton.getY() - buttonHeight, reloadButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(addButton);

        removeButton = Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.remove"), this::onRemove)
                .bounds(addButton.getX(), addButton.getY() - buttonHeight - padding, addButton.getWidth(), buttonHeight)
                .build();
        addRenderableWidget(removeButton);
    }

    void onAdd(Button w) {
        this.minecraft.gui.setScreen(new AddChatKeybindScreen(this));
    }

    void onRemove(Button w) {
        BindList.Entry entry = list.getSelected();
        if (entry != null) {
            entry.chatKeybind.removeCommand();
            chatKeybindMap.get(entry.chatKeybind.keyMapping.getCategory().id().getPath()).remove(entry.chatKeybind);
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
        if (selectedEntry != null) {
            if (event.isEscape()) {
                selectedEntry.chatKeybind.keyMapping.setKey(InputConstants.UNKNOWN);
            } else {
                selectedEntry.chatKeybind.keyMapping.setKey(InputConstants.getKey(event));
            }
            KeyMapping.resetMapping();

            selectedEntry.update();
            selectedEntry = null;

            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (selectedEntry != null) {
            selectedEntry.chatKeybind.keyMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            KeyMapping.resetMapping();
            selectedEntry.update();
            selectedEntry = null;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    static class BindList extends ObjectSelectionList<BindList.Entry> {
        public BindMenuScreen parent;

        public BindList(BindMenuScreen parent, int w, int h) {
            super(parent.minecraft, w, h, 2, 32);
            this.parent = parent;
            updateEntries();
        }

        public void updateEntries() {
            this.clearEntries();
            for (Map.Entry<String, ArrayList<ChatKeybind>> category : chatKeybindMap.entrySet()) {
                for (ChatKeybind keybind : category.getValue()) {
                    Entry e = new Entry(minecraft, keybind, this);
                    addEntry(e);
                    setSelected(e);
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
                parent.parent.selectedEntry = this;
                keyButton.setMessage(Component.literal("..."));
            }

            @Override
            public @NonNull Component getNarration() {
                return Component.empty();
            }

            @Override
            public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
                parent.setSelected(this);
                keyButton.mouseClicked(event, doubleClick);
                return true;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                String category = Component.translatable("key.category.tinkerstoolkit." + chatKeybind.keyMapping.getCategory().id().getPath()).getString();
                graphics.text(client.font, Component.literal(chatKeybind.name), getX() + 8, getY() + 2, 0xFFFFFFFF);
                graphics.text(client.font, Component.literal(chatKeybind.command), getX() + 8, getY() + 12, 0xFF888888);
                graphics.text(client.font, Component.literal(category).withStyle(ChatFormatting.BOLD), getX() + 8, getY() + 22, 0xFF888888);

                keyButton.setX(getX() + this.getContentWidth() - 64 - 4);
                keyButton.setY(getY() + 2);
                keyButton.setWidth(64);
                keyButton.setHeight(32);
                keyButton.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
    }
}
