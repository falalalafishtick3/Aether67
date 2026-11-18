package dev.aether.gui;

import dev.aether.config.ConfigManager;
import dev.aether.module.*;
import dev.aether.module.impl.ClickGuiModule;
import dev.aether.setting.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;               // VERSION-SENSITIVE (1.21.9+ input events)
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int W = 112, HH = 18, MH = 16, SH = 14, BAR = 8;

    private static class Panel {
        final Category cat; int x, y; boolean open = true;
        Panel(Category c, int x, int y) { cat = c; this.x = x; this.y = y; }
    }
    private interface Action { void run(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, Action click, Action drag) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private static List<Panel> panels;
    private final List<Hit> hits = new ArrayList<>();
    private Hit activeDrag; private int dragBtn;
    private Panel draggingPanel; private double dragX, dragY;
    private double scroll;
    private ClientModule binding;

    public ClickGuiScreen() {
        super(Text.literal("Aether"));
        if (panels == null) {
            panels = new ArrayList<>();
            int i = 0;
            for (Category c : Category.values()) panels.add(new Panel(c, 12 + i++ * (W + 10), 64));
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { ConfigManager.save(); super.close(); }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        hits.clear();
        g.fill(0, 0, width, height, ClickGuiModule.dimARGB());
        if (ClickGuiModule.winter()) drawWinter(g);
        drawBanner(g);
        for (Panel p : panels) drawPanel(g, p, mx, my);
    }

    // ---------------- winter background ----------------
    private static final class Flake { float x, y, vy, drift, size, phase; int layer; }
    private static final List<Flake> FLAKES = new ArrayList<>();
    private static final java.util.Random RNG = new java.util.Random();
    private static long lastNs = System.nanoTime();
    private static int lastW = -1, lastH = -1;

    private Flake newFlake(boolean anywhere) {
        Flake f = new Flake();
        f.layer = RNG.nextInt(3);                       // 0 far, 1 mid, 2 near
        f.x = RNG.nextFloat() * width;
        f.y = anywhere ? RNG.nextFloat() * height : -6 - RNG.nextFloat() * 20;
        f.vy = 14f + f.layer * 12f + RNG.nextFloat() * 8f;
        f.drift = 6f + RNG.nextFloat() * 10f;
        f.size = 1 + f.layer;
        f.phase = RNG.nextFloat() * 6.28f;
        return f;
    }

    private void drawWinter(DrawContext g) {
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastNs) / 1e9f);
        lastNs = nowNs;
        double t = Util.getMeasuringTimeMs() / 1000.0;

        if (width != lastW || height != lastH) { FLAKES.clear(); lastW = width; lastH = height; }

        // icy sky tint
        g.fillGradient(0, 0, width, height, 0x6A0B1F3A, 0x8A1C3F63);

        // stars
        for (int i = 0; i < 40; i++) {
            int sx = (int) ((i * 97L * 31 % 1000) / 1000.0 * width);
            int sy = (int) ((i * 53L * 17 % 1000) / 1000.0 * height * 0.55);
            float tw = (float) (0.5 + 0.5 * Math.sin(t * 1.5 + i));
            g.fill(sx, sy, sx + 1, sy + 1, ((int) (60 + 150 * tw) << 24) | 0xDDEEFF);
        }

        // aurora curtains
        if (ClickGuiModule.aurora()) {
            int step = 4, top = 0, mid = (int) (height * 0.28), bot = (int) (height * 0.62);
            for (int x = 0; x < width; x += step) {
                double w1 = Math.sin(x * 0.012 + t * 0.55), w2 = Math.sin(x * 0.027 - t * 0.8), w3 = Math.sin(x * 0.005 + t * 0.2);
                double v = 0.5 + 0.5 * (0.55 * w1 + 0.3 * w2 + 0.15 * w3);
                int a = (int) (70 * v * v);
                if (a < 4) continue;
                float hue = 0.40f + 0.22f * (float) (0.5 + 0.5 * Math.sin(x * 0.006 + t * 0.3));   // green -> teal -> violet
                int rgb = MathHelper.hsvToRgb(hue, 0.65f, 1f) & 0xFFFFFF;
                int y0 = (int) (mid - 40 + 25 * w1), y1 = (int) (bot - 40 + 30 * w2);
                g.fillGradient(x, top + y0 / 2, x + step, y0 + (y1 - y0) / 3, rgb, (a << 24) | rgb);
                g.fillGradient(x, y0 + (y1 - y0) / 3, x + step, y1, (a << 24) | rgb, rgb);
            }
        }

        // snowy hills at the bottom
        for (int x = 0; x < width; x += 2) {
            double hh = 14 + 9 * Math.sin(x * 0.011 + 1.3) + 6 * Math.sin(x * 0.027) + 4 * Math.sin(x * 0.06 + 2);
            int top = height - (int) hh;
            g.fillGradient(x, top, x + 2, height, 0xB0EAF4FF, 0xD0BFD8EE);
        }
        for (int x = 0; x < width; x += 2) {
            double hh = 6 + 5 * Math.sin(x * 0.02 + 4) + 3 * Math.sin(x * 0.05);
            int top = height - (int) hh;
            g.fillGradient(x, top, x + 2, height, 0xE0FFFFFF, 0xE0DCEBFA);
        }

        // snow
        int target = ClickGuiModule.snowAmount();
        while (FLAKES.size() < target) FLAKES.add(newFlake(true));
        while (FLAKES.size() > target) FLAKES.remove(FLAKES.size() - 1);
        float speed = ClickGuiModule.snowSpeed();
        for (int i = 0; i < FLAKES.size(); i++) {
            Flake f = FLAKES.get(i);
            f.y += f.vy * speed * dt;
            f.x += (float) Math.sin(t * 0.9 + f.phase) * f.drift * dt + 3f * dt;
            if (f.y > height + 4 || f.x > width + 8) { FLAKES.set(i, newFlake(false)); continue; }
            if (f.x < -8) f.x = width + 4;
            int a = 90 + f.layer * 55;
            int c = (a << 24) | 0xFFFFFF;
            int x = (int) f.x, y = (int) f.y, sz = (int) f.size;
            g.fill(x, y, x + sz, y + sz, c);
            if (f.layer == 2) {            // big flakes get a little cross sparkle
                int soft = (60 << 24) | 0xFFFFFF;
                g.fill(x - 1, y + 1, x, y + 2, soft); g.fill(x + sz, y + 1, x + sz + 1, y + 2, soft);
                g.fill(x + 1, y - 1, x + 2, y, soft); g.fill(x + 1, y + sz, x + 2, y + sz + 1, soft);
            }
        }

        // frosty vignette on the edges
        int fw = 26;
        for (int i = 0; i < fw; i += 2) {
            int a = (int) (70 * (1 - i / (float) fw) * (1 - i / (float) fw));
            int col = (a << 24) | 0xCFE8FF;
            g.fill(i, 0, i + 2, height, col);
            g.fill(width - i - 2, 0, width - i, height, col);
        }
    }

    // ---------------- title banner ----------------
    private static int rgbArgb(float h, float sat, float br, int a) {
        return (a << 24) | (MathHelper.hsvToRgb(h - (float) Math.floor(h), sat, br) & 0xFFFFFF);
    }
    private static int mix(int rgb, int to, float k) {
        int r = (int) (((rgb >> 16 & 255) * (1 - k)) + ((to >> 16 & 255) * k));
        int gr = (int) (((rgb >> 8 & 255) * (1 - k)) + ((to >> 8 & 255) * k));
        int b = (int) (((rgb & 255) * (1 - k)) + ((to & 255) * k));
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
    private void scaledText(DrawContext g, String str, float x, float y, float scale, int argb, boolean shadow) {
        var m = g.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        g.drawText(textRenderer, str, 0, 0, argb, shadow);
        m.popMatrix();
    }
    /** Horizontal line that fades out toward both ends (alpha ramps), drawn in short segments. */
    private void fadeLine(DrawContext g, int x0, int x1, int y, int h, float hue, boolean fadeLeft) {
        int n = Math.max(1, x1 - x0);
        for (int i = 0; i < n; i += 2) {
            float t = i / (float) n;
            float a = fadeLeft ? t : 1f - t;
            g.fill(x0 + i, y, Math.min(x1, x0 + i + 2), y + h, rgbArgb(hue + t * 0.08f, 0.55f, 1f, (int) (210 * a * a)));
        }
    }

    private void drawBanner(DrawContext g) {
        double t = Util.getMeasuringTimeMs() / 1000.0;
        final String title = "AETHER";
        final float scale = 3.2f;
        final int spacing = 3;
        int[] cw = new int[title.length()];
        int total = 0;
        for (int i = 0; i < title.length(); i++) { cw[i] = textRenderer.getWidth(String.valueOf(title.charAt(i))); total += cw[i] + spacing; }
        total -= spacing;
        float tw = total * scale;
        float x = (width - tw) / 2f, y = 9f;

        // soft backdrop glow behind the title
        for (int i = 0; i < 6; i++) {
            int pad = 6 + i * 5;
            g.fill((int) (x - pad), (int) (y - 2 - i), (int) (x + tw + pad), (int) (y + 26 + i), (6 - i) << 24 | 0x0B2A33);
        }

        // side lines
        int cx = width / 2;
        int lineY = (int) (y + 12);
        float baseHue = 0.46f + (float) Math.sin(t * 0.6) * 0.03f;
        fadeLine(g, Math.max(8, cx - 230), (int) (x - 14), lineY, 1, baseHue, true);
        fadeLine(g, (int) (x + tw + 14), Math.min(width - 8, cx + 230), lineY, 1, baseHue, false);
        g.fill((int) (x - 10), lineY - 2, (int) (x - 8), lineY + 3, rgbArgb(baseHue, 0.5f, 1f, 255));
        g.fill((int) (x + tw + 8), lineY - 2, (int) (x + tw + 10), lineY + 3, rgbArgb(baseHue, 0.5f, 1f, 255));

        // letters: animated teal -> cyan -> violet wave, glow pass, then a white shimmer sweeping across
        float shimmer = (float) ((t * 5.0) % (title.length() + 8)) - 3f;
        float cursor = x;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            float hue = 0.47f + 0.13f * (float) (0.5 + 0.5 * Math.sin(t * 1.4 + i * 0.75));
            int rgb = MathHelper.hsvToRgb(hue, 0.62f, 1f);
            float k = Math.max(0f, 1f - Math.abs(i - shimmer) / 2.2f);
            int col = mix(rgb, 0xFFFFFF, k * 0.85f);
            float bob = (float) Math.sin(t * 2.0 + i * 0.5) * 0.8f;
            // glow
            int glow = (0x38 << 24) | (rgb & 0xFFFFFF);
            for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}})
                scaledText(g, ch, cursor + o[0] * 1.1f, y + bob + o[1] * 1.1f, scale, glow, false);
            scaledText(g, ch, cursor, y + bob, scale, col, true);
            cursor += (cw[i] + spacing) * scale;
        }

        // subtitle
        final String sub = "C L I E N T";
        float ss = 1.25f;
        float sw = textRenderer.getWidth(sub) * ss;
        float sx = (width - sw) / 2f, sy = y + 28f;
        for (int i = 0; i < sub.length(); i++) {
            String ch = String.valueOf(sub.charAt(i));
            float h = 0.5f + 0.1f * (float) Math.sin(t * 1.2 + i * 0.4);
            scaledText(g, ch, sx, sy, ss, rgbArgb(h, 0.35f, 1f, 235), true);
            sx += textRenderer.getWidth(ch) * ss;
        }

        // underline accent + version
        int ux = (int) ((width - 90) / 2f);
        for (int i = 0; i < 90; i += 2) {
            float tt = i / 90f;
            float edge = 1f - Math.abs(tt - 0.5f) * 2f;
            g.fill(ux + i, (int) (sy + 13), ux + i + 2, (int) (sy + 14), rgbArgb(baseHue + tt * 0.1f, 0.6f, 1f, (int) (255 * edge)));
        }
        String ver = "1.21.11";
        g.drawText(textRenderer, ver, width - textRenderer.getWidth(ver) - 8, 8, 0x66FFFFFF, false);
    }

    private void drawPanel(DrawContext g, Panel p, int mx, int my) {
        int ac = ClickGuiModule.accent();
        int x = p.x, y = p.y + (int) scroll;

        g.fill(x, y, x + W, y + HH, ac);
        g.drawText(textRenderer, p.cat.label, x + (W - textRenderer.getWidth(p.cat.label)) / 2, y + 5, 0xFFFFFFFF, true);
        hits.add(new Hit(x, y, W, HH, (a, b, btn) -> {
            if (btn == 0) { draggingPanel = p; dragX = a - p.x; dragY = b - (p.y + scroll); }
            else if (btn == 1) p.open = !p.open;
        }, null));
        if (!p.open) return;

        int cy = y + HH + 2;
        for (ClientModule m : ModuleManager.in(p.cat)) {
            boolean hover = mx >= x && mx < x + W && my >= cy && my < cy + MH;
            g.fill(x, cy, x + W, cy + MH, hover ? 0xE0202028 : 0xD8101014);
            if (m.enabled) g.fill(x, cy, x + 2, cy + MH, ac);
            g.drawText(textRenderer, m.enabled ? "-" : "+", x + 7, cy + 4, m.enabled ? ac : 0xFF8A8A8A, false);
            g.drawText(textRenderer, m.name, x + 20, cy + 4, m.enabled ? 0xFFFFFFFF : 0xFF9A9A9A, false);
            if (!m.settings.isEmpty())
                g.drawText(textRenderer, m.expanded ? "v" : ">", x + W - 10, cy + 4, 0xFFAAAAAA, false);
            String kl = binding == m ? "..." : (m.key >= 0 ? "[" + keyName(m.key) + "]" : null);
            if (kl != null) g.drawText(textRenderer, kl, x + W - 14 - textRenderer.getWidth(kl), cy + 4, 0xFF777788, false);
            hits.add(new Hit(x, cy, W, MH, (a, b, btn) -> {
                if (btn == 0) m.toggle(); else if (btn == 1) m.expanded = !m.expanded;
                else if (btn == 2) binding = binding == m ? null : m;
            }, null));
            cy += MH;

            if (m.expanded)
                for (Setting<?> s : m.settings) if (s.isVisible()) cy = drawSetting(g, s, x, cy, ac);
        }
    }

    private int drawSetting(DrawContext g, Setting<?> s, int x, int y, int ac) {
        g.fill(x, y, x + W, y + SH, 0xE0181820);
        int ty = y + 3;

        if (s instanceof BoolSetting b) {
            g.drawText(textRenderer, b.name, x + 8, ty, 0xFFCCCCCC, false);
            g.fill(x + W - 16, y + 3, x + W - 7, y + 12, 0xFF444450);
            if (b.get()) g.fill(x + W - 15, y + 4, x + W - 8, y + 11, ac);
            hits.add(new Hit(x, y, W, SH, (a, c, btn) -> { if (btn == 0) b.set(!b.get()); }, null));
            return y + SH;
        }
        if (s instanceof NumberSetting n) {
            double f = (n.get() - n.min) / (n.max - n.min);
            g.fill(x, y, x + (int) (W * f), y + SH, (ac & 0xFFFFFF) | 0x70000000);
            g.drawText(textRenderer, n.name, x + 8, ty, 0xFFCCCCCC, false);
            String v = n.step >= 1 ? String.valueOf(n.i()) : String.format("%.2f", n.get());
            g.drawText(textRenderer, v, x + W - 6 - textRenderer.getWidth(v), ty, 0xFFFFFFFF, false);
            Action a = (mx, my, btn) -> n.set(n.min + MathHelper.clamp((mx - x) / W, 0, 1) * (n.max - n.min));
            hits.add(new Hit(x, y, W, SH, (mx, my, btn) -> { if (btn == 0) a.run(mx, my, btn); }, a));
            return y + SH;
        }
        if (s instanceof ModeSetting md) {
            g.drawText(textRenderer, md.name, x + 8, ty, 0xFFCCCCCC, false);
            g.drawText(textRenderer, md.get(), x + W - 6 - textRenderer.getWidth(md.get()), ty, ac, false);
            hits.add(new Hit(x, y, W, SH, (a, c, btn) -> md.cycle(btn == 1 ? -1 : 1), null));
            return y + SH;
        }
        if (s instanceof ColorSetting c) {
            g.drawText(textRenderer, c.name, x + 8, ty, 0xFFCCCCCC, false);
            g.fill(x + W - 26, y + 3, x + W - 6, y + 11, 0xFF000000 | c.rgb());
            int by = y + SH;
            g.fill(x, by, x + W, by + BAR * 3, 0xE0181820);
            for (int i = 0; i < 3; i++) drawBar(g, c, i, x + 6, by + i * BAR + 1, W - 12);
            return by + BAR * 3;
        }
        return y;
    }

    private void drawBar(DrawContext g, ColorSetting c, int type, int bx, int by, int bw) {
        final int seg = 28;
        for (int k = 0; k < seg; k++) {
            float t = k / (float) seg;
            int col = switch (type) {
                case 0 -> MathHelper.hsvToRgb(t, c.s, c.b);
                case 1 -> MathHelper.hsvToRgb(c.h, t, c.b);
                default -> MathHelper.hsvToRgb(c.h, c.s, t);
            };
            g.fill(bx + k * bw / seg, by, bx + (k + 1) * bw / seg, by + BAR - 2, 0xFF000000 | col);
        }
        float val = type == 0 ? c.h : type == 1 ? c.s : c.b;
        int mxp = bx + (int) (val * bw);
        g.fill(mxp - 1, by - 1, mxp + 1, by + BAR - 1, 0xFFFFFFFF);

        Action a = (mx, my, btn) -> {
            float v = MathHelper.clamp((float) (mx - bx) / bw, 0f, 1f);
            if (type == 0) c.h = Math.min(v, 0.999f); else if (type == 1) c.s = v; else c.b = v;
        };
        hits.add(new Hit(bx, by, bw, BAR, (mx, my, btn) -> { if (btn == 0) a.run(mx, my, btn); }, a));
    }

    // ---- VERSION-SENSITIVE: 1.21.9+ passes a Click record instead of (x, y, button) ----
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y(); int btn = click.button();
        if (binding != null && btn != 2) binding = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                h.click().run(mx, my, btn);
                if (h.drag() != null) { activeDrag = h; dragBtn = btn; }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingPanel != null) {
            draggingPanel.x = (int) (click.x() - dragX);
            draggingPanel.y = (int) (click.y() - dragY - scroll);
            return true;
        }
        if (activeDrag != null && dragBtn == 0) { activeDrag.drag().run(click.x(), click.y(), dragBtn); return true; }
        return super.mouseDragged(click, dx, dy);
    }
    @Override public boolean mouseReleased(Click click) {
        draggingPanel = null; activeDrag = null;
        return super.mouseReleased(click);
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.min(0, scroll + v * 20);
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (binding != null) {
            int k = input.getKeycode();
            if (k == GLFW.GLFW_KEY_ESCAPE) binding = null;
            else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) { binding.key = -1; binding = null; }
            else { binding.key = k; binding = null; }
            return true;
        }
        return super.keyPressed(input);
    }

    private static String keyName(int k) {
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(k, 0);
        if (n != null) return n.toUpperCase();
        return switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT"; case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL"; case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT"; case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_UP -> "UP"; case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT"; case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE"; case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "#" + k;
        };
    }
}
