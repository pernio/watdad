package jinzo.watdad.client.utils;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import jinzo.watdad.client.WatdadConfig;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(WatdadConfig.class, parent).get();
    }
}
