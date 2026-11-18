package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;

public class ClickGuiModule extends ClientModule {
    public static ClickGuiModule INSTANCE;
    private final ColorSetting accent = add(new ColorSetting("Accent", 0.46f, 0.9f, 0.75f));
    private final NumberSetting dim = add(new NumberSetting("Background Dim", 35, 0, 80, 1));

    private final BoolSetting winter = add(new BoolSetting("Winter Background", true));
    private final NumberSetting snowAmount = add(new NumberSetting("Snow Amount", 140, 0, 500, 5));
    private final NumberSetting snowSpeed = add(new NumberSetting("Snow Speed", 1.0, 0.2, 3.0, 0.1));
    private final BoolSetting aurora = add(new BoolSetting("Aurora", true));
    private final BoolSetting startupSound = add(new BoolSetting("Startup Sound", true));
    private final NumberSetting startupVol = add(new NumberSetting("Startup Volume", 80, 0, 100, 1));

    public ClickGuiModule() {
        super("ClickGUI", "GUI appearance", Category.CLIENT);
        INSTANCE = this;
        snowAmount.visibleWhen(winter::get);
        snowSpeed.visibleWhen(winter::get);
        aurora.visibleWhen(winter::get);
    }
    public static int accent() { return 0xFF000000 | INSTANCE.accent.rgb(); }
    public static boolean winter() { return INSTANCE == null || INSTANCE.winter.get(); }
    public static int snowAmount() { return INSTANCE == null ? 140 : INSTANCE.snowAmount.i(); }
    public static float snowSpeed() { return INSTANCE == null ? 1f : INSTANCE.snowSpeed.f(); }
    public static boolean aurora() { return INSTANCE == null || INSTANCE.aurora.get(); }
    public static boolean startupSoundEnabled() { return INSTANCE == null || INSTANCE.startupSound.get(); }
    public static float startupVolume() { return INSTANCE == null ? 0.8f : INSTANCE.startupVol.f() / 100f; }
    public static int dimARGB() { return ((int) (INSTANCE.dim.get() / 100.0 * 255)) << 24; }
}
