package jinzo.watdad.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "watdad")
public class WatdadConfig implements ConfigData {
    public boolean displayHighlightOres = true;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int oreRenderRange = 20;

    public boolean exposeOres = false;
    public boolean displayHighlightBlock = false;
    public boolean displayStaffSession = true;
}
