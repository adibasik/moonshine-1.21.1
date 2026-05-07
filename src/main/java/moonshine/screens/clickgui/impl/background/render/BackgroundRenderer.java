package moonshine.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import moonshine.util.render.Render2D;
import moonshine.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int baseAlpha = (int) (255 * alphaMultiplier);
        int[] gradientColors = {
                moonshine.util.color.ClientColors.primary(baseAlpha),
                moonshine.util.color.ClientColors.secondary(baseAlpha),
                moonshine.util.color.ClientColors.primary(baseAlpha),
                moonshine.util.color.ClientColors.secondary(baseAlpha),
                moonshine.util.color.ClientColors.primary(baseAlpha)
        };

        Render2D.gradientRect(bgX, bgY, 400, 250, gradientColors, 15);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);
        int blurAlpha = (int) (155 * alphaMultiplier);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, moonshine.util.color.ClientColors.primary(panelAlpha), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, bgHeight - 15, 0.5f, moonshine.util.color.ClientColors.primary(outlineAlpha), 10);

        Render2D.outline(bgX + 12.5f, bgY + 220.5f, 70, 17, 0.5f, moonshine.util.color.ClientColors.primary(outlineAlpha), 5);

        Fonts.GUI_ICONS.draw("X", bgX + 21.15f, bgY + 217.5f, 19, moonshine.util.color.ClientColors.primary(outlineAlpha));
        Fonts.GUI_ICONS.draw("Y", bgX + 40f, bgY + 217f, 20, moonshine.util.color.ClientColors.primary(outlineAlpha));
        Fonts.GUI_ICONS.draw("Z", bgX + 60f, bgY + 217f, 20, moonshine.util.color.ClientColors.primary(outlineAlpha));

        Render2D.blur(bgX + 12.5f, bgY + 220.5f, 70, 17, 4, 5, moonshine.util.color.ClientColors.secondary(blurAlpha));

        float textSize = 6f;
        String soonText = "Soon...";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = bgX + 12.5f + (70 - textWidth) / 2f;
        float centerY = bgY + 220.5f + (17 - textHeight) / 2f;
        Fonts.BOLD.draw(soonText, centerX, centerY, textSize, new Color(150, 150, 150, (int) (200 * alphaMultiplier)).getRGB());
    }
}