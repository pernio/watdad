package jinzo.watdad.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class PunishmentData {
    public static class PunishmentType {
        public List<String> durations;
        public List<String> reasons;
    }

    public static Map<String, PunishmentType> load() {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, PunishmentType>>(){}.getType();
        return gson.fromJson(
                new InputStreamReader(PunishmentData.class.getResourceAsStream("/data/watdad/punishments.json")),
                type
        );
    }
}
