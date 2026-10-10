package me.spunkyinsaan.heartindicator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Bez suwaków: łapiesz napis albo pasek myszką i przesuwasz; kwadracik ze strzałką ↘ przy elemencie
 * ciągniesz w różne strony, żeby go powiększyć/pomniejszyć. Przyciski z okiem u góry ukrywają element.
 */
public class HeartScreen extends Screen {
    private enum El { TEXT, BAR }

    private static final int HANDLE = 10;
    private static final String NAME = "Nick";

    private El active = null;
    private boolean moving = false, resizing = false;
    private double grabDx, grabDy;

    private static final String[] EYE = {
            "...XXXXX...",
            ".XX.....XX.",
            "X...XXX...X",
            "X..XXXXX..X",
            "X...XXX...X",
            ".XX.....XX.",
            "...XXXXX..."
    };

    public HeartScreen() {
        super(Text.literal("Heart Indicator"));
    }

    // ------------------------------------------------------------ geometria

    private String previewText() {
        return HudRenderer.label(4f, 20f, 0f);          // 2/10 (albo 4/20 w punktach)
    }

    private String previewBarText() {
        return HudRenderer.label(10f, 20f, 4f);
    }

    private boolean enabled(El e) {
        return e == El.TEXT ? Config.INSTANCE.showText : Config.INSTANCE.showBar;
    }

    private float scaleOf(El e) {
        return e == El.TEXT ? Config.INSTANCE.scale : Config.INSTANCE.barScale;
    }

    private int[] base(El e) {
        return e == El.TEXT ? HudRenderer.textSize(client, NAME, previewText()) : HudRenderer.barSize(client, NAME);
    }

    /** x, y, szerokość, wysokość po przeskalowaniu */
    private int[] box(El e) {
        Config c = Config.INSTANCE;
        int[] s = base(e);
        float sc = scaleOf(e);
        float fx = e == El.TEXT ? c.posX : c.barX;
        float fy = e == El.TEXT ? c.posY : c.barY;
        return new int[]{Math.round(fx * this.width), Math.round(fy * this.height),
                Math.round(s[0] * sc), Math.round(s[1] * sc)};
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean onHandle(El e, double mx, double my) {
        int[] b = box(e);
        return in(mx, my, b[0] + b[2], b[1] + b[3], HANDLE + 6, HANDLE + 6);
    }

    private boolean onBox(El e, double mx, double my) {
        int[] b = box(e);
        return in(mx, my, b[0] - 3, b[1] - 3, b[2] + 6, b[3] + 6);
    }

    // ------------------------------------------------------------ widgety

    @Override
    protected void init() {
        int cx = this.width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Napis"), b -> {
            Config.INSTANCE.showText = !Config.INSTANCE.showText;
            Config.save();
        }).dimensions(cx - 78, 20, 76, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Pasek"), b -> {
            Config.INSTANCE.showBar = !Config.INSTANCE.showBar;
            Config.save();
        }).dimensions(cx + 2, 20, 76, 20).build());

        addDrawableChild(ButtonWidget.builder(unitLabel(), b -> {
            Config.INSTANCE.usePoints = !Config.INSTANCE.usePoints;
            Config.save();
            clearAndInit();
        }).dimensions(cx - 90, this.height - 28, 180, 20).build());
    }

    private Text unitLabel() {
        return Text.literal(Config.INSTANCE.usePoints ? "Liczenie: punkty zdrowia (1 serce = 2)" : "Liczenie: serca");
    }

    // ------------------------------------------------------------ rysowanie

    private void drawEye(DrawContext ctx, int x, int y, boolean visible) {
        int col = visible ? 0xFFFFFFFF : 0xFF777777;
        for (int r = 0; r < EYE.length; r++)
            for (int c = 0; c < EYE[r].length(); c++)
                if (EYE[r].charAt(c) == 'X') ctx.fill(x + c, y + r, x + c + 1, y + r + 1, col);
        if (!visible) {
            for (int i = 0; i < 12; i++) {      // przekreślenie
                int yy = y + 8 - Math.round(i * 9f / 11f);
                ctx.fill(x + i - 1, yy, x + i + 1, yy + 2, 0xFFFF5555);
            }
        }
    }

    private void drawHandle(DrawContext ctx, int[] b, boolean hot) {
        int hx = b[0] + b[2] + 2, hy = b[1] + b[3] + 2;
        ctx.fill(hx, hy, hx + HANDLE, hy + HANDLE, hot ? 0xFF55AAFF : 0xFFCCCCCC);
        ctx.fill(hx, hy, hx + HANDLE, hy + 1, 0xFF000000);
        ctx.fill(hx, hy + HANDLE - 1, hx + HANDLE, hy + HANDLE, 0xFF000000);
        ctx.fill(hx, hy, hx + 1, hy + HANDLE, 0xFF000000);
        ctx.fill(hx + HANDLE - 1, hy, hx + HANDLE, hy + HANDLE, 0xFF000000);
        for (int i = 2; i <= 7; i++) ctx.fill(hx + i, hy + i, hx + i + 1, hy + i + 1, 0xFF000000);
        for (int i = 3; i <= 7; i++) {
            ctx.fill(hx + 7, hy + i, hx + 8, hy + i + 1, 0xFF000000);
            ctx.fill(hx + i, hy + 7, hx + i + 1, hy + 8, 0xFF000000);
        }
    }

    private void drawElement(DrawContext ctx, El e) {
        Config c = Config.INSTANCE;
        int[] b = box(e);
        boolean on = enabled(e);
        boolean grabbed = active == e && (moving || resizing);
        ctx.fill(b[0] - 3, b[1] - 3, b[0] + b[2] + 3, b[1] + b[3] + 3, grabbed ? 0x6655AAFF : (on ? 0x44FFFFFF : 0x22FFFFFF));
        if (on) {
            if (e == El.TEXT) HudRenderer.drawText(ctx, client, b[0], b[1], c.scale, NAME, 4f, 20f, 0f);
            else HudRenderer.drawBar(ctx, client, b[0], b[1], c.barScale, NAME, 10f, 20f, 4f);
        } else {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ukryty").formatted(Formatting.DARK_GRAY),
                    b[0] + b[2] / 2, b[1] + Math.max(0, b[3] / 2 - 4), 0xFFFFFF);
        }
        drawHandle(ctx, b, active == e && resizing);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawElement(ctx, El.BAR);
        drawElement(ctx, El.TEXT);

        int cx = this.width / 2;
        drawEye(ctx, cx - 78 + 5, 20 + 6, Config.INSTANCE.showText);
        drawEye(ctx, cx + 2 + 5, 20 + 6, Config.INSTANCE.showBar);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Złap element i przesuń • ciągnij kwadracik ↘ = rozmiar • oko = ukryj • Esc = gotowe")
                        .formatted(Formatting.GRAY),
                cx, 6, 0xFFFFFF);
    }

    // ------------------------------------------------------------ mysz

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        for (El e : new El[]{El.TEXT, El.BAR}) {
            if (onHandle(e, mx, my)) {
                active = e;
                resizing = true;
                return true;
            }
        }
        for (El e : new El[]{El.TEXT, El.BAR}) {
            if (onBox(e, mx, my)) {
                int[] b = box(e);
                active = e;
                moving = true;
                grabDx = mx - b[0];
                grabDy = my - b[1];
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        Config c = Config.INSTANCE;
        if (active != null && resizing) {
            // od lewego górnego rogu elementu: w prawo/dół = większy, w lewo/górę = mniejszy
            int[] s = base(active);
            int[] b = box(active);
            double ns = (((mx - b[0]) / s[0]) + ((my - b[1]) / s[1])) / 2.0;
            float v = (float) Math.max(0.5, Math.min(6.0, ns));
            if (active == El.TEXT) c.scale = v; else c.barScale = v;
            return true;
        }
        if (active != null && moving) {
            int[] b = box(active);
            double nx = Math.max(0, Math.min(this.width - b[2] - HANDLE - 4, mx - grabDx));
            double ny = Math.max(0, Math.min(this.height - b[3] - HANDLE - 4, my - grabDy));
            if (active == El.TEXT) {
                c.posX = (float) (nx / this.width);
                c.posY = (float) (ny / this.height);
            } else {
                c.barX = (float) (nx / this.width);
                c.barY = (float) (ny / this.height);
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (moving || resizing) {
            moving = false;
            resizing = false;
            Config.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    /** kółko myszy zmienia rozmiar elementu pod kursorem */
    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        Config c = Config.INSTANCE;
        float d = (float) Math.signum(vertical) * 0.25f;
        if (onBox(El.TEXT, mx, my)) c.scale = Math.max(0.5f, Math.min(6f, c.scale + d));
        else if (onBox(El.BAR, mx, my)) c.barScale = Math.max(0.5f, Math.min(6f, c.barScale + d));
        else return super.mouseScrolled(mx, my, horizontal, vertical);
        Config.save();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
