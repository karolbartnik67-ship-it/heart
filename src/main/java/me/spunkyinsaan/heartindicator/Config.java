package me.spunkyinsaan.heartindicator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config INSTANCE = new Config();

    /** włącz/wyłącz wskaźnik na ekranie */
    public boolean renderingEnabled = true;
    /** pozycja lewego górnego rogu jako ułamek ekranu (0..1) */
    public float posX = 0.5f;
    public float posY = 0.62f;
    /** rozmiar (mnożnik) */
    public float scale = 1.5f;
    public boolean showName = true;
    public boolean showSelf = false;
    public boolean showAbsorption = true;
    /** napis (np. 2/10) widoczny / ukryty */
    public boolean showText = true;
    /** boss bar widoczny / ukryty */
    public boolean showBar = true;
    public float barX = 0.5f;
    public float barY = 0.08f;
    public float barScale = 1.5f;
    /** true = punkty zdrowia (1 serce = 2 pkt), false = serca */
    public boolean usePoints = false;
    /** zasięg wykrywania gracza pod celownikiem (bloki) */
    public double range = 32.0;

    public boolean getRenderingEnabled() { return renderingEnabled; }

    public void setRenderingEnabled(boolean v) { renderingEnabled = v; }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("heartindicator.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        INSTANCE.scale = Math.max(0.5f, Math.min(6f, INSTANCE.scale));
        INSTANCE.barScale = Math.max(0.5f, Math.min(6f, INSTANCE.barScale));
        INSTANCE.barX = Math.max(0f, Math.min(1f, INSTANCE.barX));
        INSTANCE.barY = Math.max(0f, Math.min(1f, INSTANCE.barY));
        INSTANCE.posX = Math.max(0f, Math.min(1f, INSTANCE.posX));
        INSTANCE.posY = Math.max(0f, Math.min(1f, INSTANCE.posY));
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(INSTANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
