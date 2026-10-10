package me.spunkyinsaan.heartindicator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

/**
 * Dwa elementy HUD (każdy można ukryć, przesunąć i przeskalować w GUI):
 *  - napis np. "[serce] 2/10" (serca / max serc, albo punkty zdrowia),
 *  - boss bar (czerwony; ze złotymi sercami cały żółty).
 * Pokazują gracza, którego ostatnio uderzyłeś (6 s), a gdy nie ma licznika - gracza, na którego patrzysz.
 * Złote serca są wliczone w liczbę (np. 12/10) i wtedy napis jest żółty.
 */
public final class HudRenderer {
    private static final Identifier CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier ABSORB = Identifier.ofVanilla("hud/heart/absorbing_full");

    public static final long SHOW_MS = 6000;
    public static final int BAR_W = 110;
    public static final int BAR_H = 11;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFD700;
    private static final int RED_BAR = 0xFFD62828;

    private static int targetId = -1;
    private static long lastHit = 0;

    private HudRenderer() {}

    public static void onAttack(Entity target) {
        if (target instanceof PlayerEntity && target != MinecraftClient.getInstance().player) {
            targetId = target.getId();
            lastHit = System.currentTimeMillis();
        }
    }

    // ---------------------------------------------------------------- patrzenie na gracza

    private static PlayerEntity lookedAt(MinecraftClient mc) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d end = eye.add(mc.player.getRotationVec(1.0f).multiply(Config.INSTANCE.range));
        BlockHitResult bhr = mc.world.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        double blockDist = bhr.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : bhr.getPos().distanceTo(eye);

        AbstractClientPlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Optional<Vec3d> hit = p.getBoundingBox().expand(0.15).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double dist = hit.get().distanceTo(eye);
            if (dist < bestDist && dist < blockDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- teksty

    public static String fmt(float v) {
        float r = Math.round(v * 2f) / 2f;           // do połowy serca
        return r == (int) r ? String.valueOf((int) r) : String.valueOf(r);
    }

    /** hp = punkty zdrowia; zwraca w sercach albo w punktach zależnie od ustawienia */
    public static String val(float hp) {
        return Config.INSTANCE.usePoints ? String.valueOf(Math.round(hp)) : fmt(hp / 2f);
    }

    public static String label(float health, float maxHealth, float absorption) {
        float total = health + (Config.INSTANCE.showAbsorption ? Math.max(0f, absorption) : 0f);
        return val(total) + "/" + val(maxHealth);
    }

    // ---------------------------------------------------------------- rozmiary (przy skali 1.0)

    public static int[] textSize(MinecraftClient mc, String name, String label) {
        int w = 12 + mc.textRenderer.getWidth(label);
        int h = 9;
        if (Config.INSTANCE.showName && name != null) {
            w = Math.max(w, mc.textRenderer.getWidth(name));
            h += 11;
        }
        return new int[]{w, h};
    }

    /** nick nad barem pokazujemy tylko, gdy napis jest ukryty (żeby się nie dublował) */
    private static boolean barName() {
        return Config.INSTANCE.showName && !Config.INSTANCE.showText;
    }

    public static int[] barSize(MinecraftClient mc, String name) {
        int h = BAR_H + (barName() && name != null ? 11 : 0);
        int w = BAR_W;
        if (barName() && name != null) w = Math.max(w, mc.textRenderer.getWidth(name));
        return new int[]{w, h};
    }

    // ---------------------------------------------------------------- rysowanie

    /** Napis z sercem; lewy górny róg w (x,y). */
    public static void drawText(DrawContext ctx, MinecraftClient mc, int x, int y, float scale,
                                String name, float health, float maxHealth, float absorption) {
        Config c = Config.INSTANCE;
        boolean gold = c.showAbsorption && absorption > 0.01f;
        String text = label(health, maxHealth, absorption);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        int ty = 0;
        if (c.showName && name != null) {
            ctx.drawText(mc.textRenderer, Text.literal(name), 0, 0, WHITE, true);
            ty = 11;
        }
        ctx.drawGuiTexture(RenderLayer::getGuiTextured, CONTAINER, 0, ty, 9, 9);
        ctx.drawGuiTexture(RenderLayer::getGuiTextured, gold ? ABSORB : FULL, 0, ty, 9, 9);
        ctx.drawText(mc.textRenderer, Text.literal(text), 12, ty + 1, gold ? GOLD : WHITE, true);
        ctx.getMatrices().pop();
    }

    /**
     * Boss bar: czerwony = życie / max. Gdy są złote serca, cały pasek jest żółty i liczy się
     * życie + złote jak zwykłe serca (np. 3 + 2 złote z 10 = 5/10 paska; od 10 wzwyż pasek pełny).
     */
    public static void drawBar(DrawContext ctx, MinecraftClient mc, int x, int y, float scale,
                               String name, float health, float maxHealth, float absorption) {
        Config c = Config.INSTANCE;
        float hp = Math.max(0f, health);
        float ab = c.showAbsorption ? Math.max(0f, absorption) : 0f;
        float denom = Math.max(maxHealth, 0.001f);
        boolean gold = ab > 0.01f;
        int fillW = Math.round(BAR_W * Math.min(1f, (hp + ab) / denom));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        int ty = 0;
        if (barName() && name != null) {
            ctx.drawText(mc.textRenderer, Text.literal(name), 0, 0, WHITE, true);
            ty = 11;
        }
        ctx.fill(-1, ty - 1, BAR_W + 1, ty + BAR_H + 1, 0xFF000000);   // obwódka
        ctx.fill(0, ty, BAR_W, ty + BAR_H, 0xFF3A3A3A);                 // tło
        ctx.fill(0, ty, fillW, ty + BAR_H, gold ? GOLD : RED_BAR);

        String text = label(health, maxHealth, absorption);
        ctx.drawText(mc.textRenderer, Text.literal(text), (BAR_W - mc.textRenderer.getWidth(text)) / 2, ty + 2, WHITE, true);
        ctx.getMatrices().pop();
    }

    // ---------------------------------------------------------------- HUD w grze

    public static void render(DrawContext ctx) {
        Config c = Config.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!c.renderingEnabled || mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        if (!c.showText && !c.showBar) return;

        PlayerEntity shown = null;
        // 1. aktywny licznik 6 s ma pierwszeństwo - nie zmienia się, gdy patrzysz na kogoś innego
        if (targetId != -1 && System.currentTimeMillis() - lastHit <= SHOW_MS) {
            Entity e = mc.world.getEntityById(targetId);
            if (e instanceof PlayerEntity p && !p.isRemoved()) shown = p;
        }
        // 2. brak licznika - pokazuj gracza, na którego patrzysz
        if (shown == null) shown = lookedAt(mc);
        if (shown == null) return;

        String name = shown.getName().getString();
        float hp = shown.getHealth(), max = shown.getMaxHealth(), ab = shown.getAbsorptionAmount();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        if (c.showBar) drawBar(ctx, mc, Math.round(c.barX * sw), Math.round(c.barY * sh), c.barScale, name, hp, max, ab);
        if (c.showText) drawText(ctx, mc, Math.round(c.posX * sw), Math.round(c.posY * sh), c.scale, name, hp, max, ab);
    }
}
