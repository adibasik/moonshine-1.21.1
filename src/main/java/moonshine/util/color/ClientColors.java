package moonshine.util.color;

import moonshine.modules.impl.render.Hud;

import java.awt.Color;

public final class ClientColors {

    private static final int PRIMARY_DEFAULT = new Color(52, 52, 52, 255).getRGB();
    private static final int SECONDARY_DEFAULT = new Color(32, 32, 32, 255).getRGB();

    private ClientColors() {
    }

    public static int primary(int alpha) {
        return withAlpha(primaryBase(), alpha);
    }

    public static int secondary(int alpha) {
        return withAlpha(secondaryBase(), alpha);
    }

    private static int primaryBase() {
        try {
            Hud hud = Hud.getInstance();
            if (hud != null && hud.clientPrimaryColor != null) {
                return hud.clientPrimaryColor.getColor();
            }
        } catch (Exception ignored) {
        }
            return PRIMARY_DEFAULT;
    }

    private static int secondaryBase() {
        try {
            Hud hud = Hud.getInstance();
            if (hud != null && hud.clientSecondaryColor != null) {
                return hud.clientSecondaryColor.getColor();
            }
        } catch (Exception ignored) {
        }
            return SECONDARY_DEFAULT;
    }

    private static int withAlpha(int color, int alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int finalAlpha = Math.round(baseAlpha * clamp(alpha) / 255f);
        return (finalAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static int clamp(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }
}
