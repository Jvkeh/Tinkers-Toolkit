package net.jvkeh.tinkerstoolkit;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.jvkeh.tinkerstoolkit.chatkeybinds.BindMenuScreen;


public class ModMenuAPIImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BindMenuScreen::new;
    }
}
