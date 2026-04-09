package app.netlify.jinzo.watdad.client.compat;

import app.netlify.jinzo.watdad.client.screen.WatdadConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class WatdadModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WatdadConfigScreen::new;
    }
}
