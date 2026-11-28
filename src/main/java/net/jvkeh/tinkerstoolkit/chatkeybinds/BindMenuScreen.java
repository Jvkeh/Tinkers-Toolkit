package net.jvkeh.tinkerstoolkit.chatkeybinds;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Map;

import static net.jvkeh.tinkerstoolkit.chatkeybinds.ChatKeybindsModule.chatKeybindMap;


public class BindMenuScreen extends Screen implements ModMenuApi {
    public BindList.Entry bindKeySelected;
    HeaderAndFooterLayout layout;
    Screen parent;
    BindList list;
    Button removeButton;

    public BindMenuScreen(final Screen parent) {
        super(Component.translatable("tinkerstoolkit.keybinds.menuScreen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        layout = new HeaderAndFooterLayout(this, 33, 60);
        this.layout.addTitleHeader(this.title, this.font);

        list = this.layout.addToContents(new BindList(this, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight(), 32));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
        footer.defaultCellSetting().alignHorizontallyCenter();
        LinearLayout topFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(4));
        LinearLayout bottomFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(4));

        topFooterButtons.addChild(Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.new"), this::onAdd)
                .width(150)
                .build());
        removeButton = topFooterButtons.addChild(Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.remove"), this::onRemove)
                .width(150)
                .build());
        removeButton.active = false;

        bottomFooterButtons.addChild(Button.builder(Component.translatable("tinkerstoolkit.keybinds.menuScreen.reload"), this::onReload)
                .width(150)
                .build());

        bottomFooterButtons.addChild(Button.builder(CommonComponents.GUI_DONE, this::onClose)
                .width(150)
                .build());

        this.layout.arrangeElements();
        this.layout.visitWidgets(this::addRenderableWidget);
    }

    void onAdd(Button w) {
        this.minecraft.gui.setScreen(new AddChatKeybindScreen(this));
    }

    void onRemove(Button w) {
        BindList.Entry entry = list.getSelected();
        if (entry != null) {
            entry.chatKeybind.removeCommand();
            chatKeybindMap.forEach((_, array) -> array.removeIf(keybind ->
                    keybind.name.equals(entry.chatKeybind.name) &&
                            keybind.command.equals(entry.chatKeybind.command) &&
                            keybind.keyMapping.same(entry.chatKeybind.keyMapping)));
            ConfigManager.saveConfig();
            list.updateEntries();
            removeButton.active = false;
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

        public BindList(BindMenuScreen parent, int width, int height, int y, int itemHeight) {
            super(parent.minecraft, width, height, y, itemHeight);
            this.setY(parent.layout.getY() + parent.layout.getHeaderHeight() + padding);
            this.parent = parent;
            updateEntries();
        }

        @Override
        public int getRowWidth() {
            return 300 + padding * 2;
        }

        public void updateEntries() {
            this.clearEntries();
            for (Map.Entry<ChatKeybindsModule.KeybindCategory, ArrayList<ChatKeybind>> category : chatKeybindMap.entrySet()) {
                for (ChatKeybind keybind : category.getValue()) {
                    Entry e = new Entry(minecraft, keybind, this);
                    addEntry(e);
                }
            }
        }

        public static class Entry extends ObjectSelectionList.Entry<Entry> implements GuiEventListener {
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
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                String category = Component.translatable("key.category.tinkerstoolkit." + chatKeybind.keyMapping.getCategory().id().getPath()).getString();
                if (chatKeybind.translatable.equals(ChatKeybind.Translatable.NO)) {
                    graphics.text(client.font, Component.literal(chatKeybind.name), getX() + padding, getY() + 2, 0xFFFFFFFF);
                } else {
                    graphics.text(client.font, Component.translatable(chatKeybind.name), getX() + padding, getY() + 2, 0xFFFFFFFF);
                }
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
