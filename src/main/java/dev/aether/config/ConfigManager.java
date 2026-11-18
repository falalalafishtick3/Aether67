package dev.aether.config;

import com.google.gson.*;
import dev.aether.module.*;
import dev.aether.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public final class ConfigManager {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aether.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (ClientModule m : ModuleManager.MODULES) {
                JsonObject o = new JsonObject(), s = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("key", m.key);
                for (Setting<?> st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                root.add(m.name, o);
            }
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            for (ClientModule m : ModuleManager.MODULES) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                m.enabled = o.get("enabled").getAsBoolean();
                if (o.has("key")) m.key = o.get("key").getAsInt();
                JsonObject s = o.getAsJsonObject("settings");
                for (Setting<?> st : m.settings) if (s.has(st.name)) st.load(s.get(st.name));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
