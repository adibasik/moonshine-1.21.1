package moonshine.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import moonshine.modules.impl.render.Hud;
import moonshine.util.render.Render2D;
import moonshine.util.render.font.Fonts;

import java.awt.*;

public class BackgroundRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        if (isDropDown()) {
            renderDropDown(bgX, bgY, alphaMultiplier);
            return;
        }
        if (isModern()) {
            renderModern(bgX, bgY, alphaMultiplier);
            return;
        }

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
        if (isDropDown()) {
            renderDropDownCategoryPanel(bgX, bgY, bgHeight, alphaMultiplier);
            return;
        }
        if (isModern()) {
            renderModernCategoryPanel(bgX, bgY, bgHeight, alphaMultiplier);
            return;
        }

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

    private void renderModern(float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        int panelAlpha = (int) (235 * alphaMultiplier);
        int softAlpha = (int) (70 * alphaMultiplier);
        int accentAlpha = (int) (210 * alphaMultiplier);

        Render2D.blur(bgX, bgY, 400, 250, 6, 12, new Color(0, 0, 0, (int) (135 * alphaMultiplier)).getRGB());
        Render2D.rect(bgX, bgY, 400, 250, new Color(13, 15, 20, panelAlpha).getRGB(), 8);
        Render2D.gradientRect(bgX, bgY, 400, 250,
                new int[]{
                        new Color(34, 45, 58, softAlpha).getRGB(),
                        new Color(12, 14, 19, alpha).getRGB(),
                        new Color(16, 22, 28, alpha).getRGB(),
                        new Color(10, 12, 17, alpha).getRGB()
                },
                8);
        Render2D.outline(bgX, bgY, 400, 250, 0.7f, new Color(86, 101, 116, (int) (130 * alphaMultiplier)).getRGB(), 8);
        Render2D.gradientRect(bgX + 1, bgY + 1, 398, 1.2f,
                new int[]{
                        new Color(98, 189, 255, 0).getRGB(),
                        new Color(98, 189, 255, accentAlpha).getRGB(),
                        new Color(150, 230, 190, accentAlpha).getRGB(),
                        new Color(98, 189, 255, 0).getRGB()
                },
                1);
    }

    private void renderModernCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int panelAlpha = (int) (145 * alphaMultiplier);
        int outlineAlpha = (int) (95 * alphaMultiplier);

        Render2D.rect(bgX + 8f, bgY + 8f, 78, bgHeight - 16, new Color(20, 24, 30, panelAlpha).getRGB(), 6);
        Render2D.outline(bgX + 8f, bgY + 8f, 78, bgHeight - 16, 0.5f, new Color(95, 112, 128, outlineAlpha).getRGB(), 6);

        Render2D.rect(bgX + 13f, bgY + 220.5f, 68, 17, new Color(16, 20, 25, (int) (160 * alphaMultiplier)).getRGB(), 4);
        Render2D.outline(bgX + 13f, bgY + 220.5f, 68, 17, 0.5f, new Color(70, 88, 104, outlineAlpha).getRGB(), 4);

        float textSize = 6f;
        String soonText = "Modern";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        Fonts.BOLD.draw(soonText, bgX + 13f + (68 - textWidth) / 2f, bgY + 226f, textSize,
                new Color(185, 198, 208, (int) (210 * alphaMultiplier)).getRGB());
    }

    private void renderDropDown(float bgX, float bgY, float alphaMultiplier) {
        int panelAlpha = (int) (226 * alphaMultiplier);
        int glowAlpha = (int) (95 * alphaMultiplier);

        Render2D.blur(bgX, bgY, 400, 250, 7, 10, new Color(0, 5, 9, (int) (125 * alphaMultiplier)).getRGB());
        Render2D.rect(bgX, bgY, 400, 250, new Color(9, 13, 17, panelAlpha).getRGB(), 7);
        Render2D.gradientRect(bgX, bgY, 400, 250,
                new int[]{
                        new Color(12, 29, 35, (int) (130 * alphaMultiplier)).getRGB(),
                        new Color(8, 12, 17, panelAlpha).getRGB(),
                        new Color(16, 20, 22, panelAlpha).getRGB(),
                        new Color(7, 10, 15, panelAlpha).getRGB()
                },
                7);
        Render2D.outline(bgX, bgY, 400, 250, 0.7f, new Color(91, 129, 132, (int) (118 * alphaMultiplier)).getRGB(), 7);
        Render2D.gradientRect(bgX + 92f, bgY + 36f, 298f, 1.2f,
                new int[]{
                        new Color(87, 229, 211, 0).getRGB(),
                        new Color(87, 229, 211, glowAlpha).getRGB(),
                        new Color(255, 210, 117, glowAlpha).getRGB(),
                        new Color(87, 229, 211, 0).getRGB()
                },
                1);
    }

    private void renderDropDownCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        int panelAlpha = (int) (132 * alphaMultiplier);
        int outlineAlpha = (int) (86 * alphaMultiplier);

        Render2D.rect(bgX + 8f, bgY + 8f, 78, bgHeight - 16, new Color(14, 20, 24, panelAlpha).getRGB(), 5);
        Render2D.outline(bgX + 8f, bgY + 8f, 78, bgHeight - 16, 0.5f, new Color(86, 124, 128, outlineAlpha).getRGB(), 5);

        Render2D.rect(bgX + 13f, bgY + 220.5f, 68, 17, new Color(13, 18, 22, (int) (155 * alphaMultiplier)).getRGB(), 4);
        Render2D.outline(bgX + 13f, bgY + 220.5f, 68, 17, 0.5f, new Color(90, 126, 116, outlineAlpha).getRGB(), 4);

        String label = "DropDown";
        float textWidth = Fonts.BOLD.getWidth(label, 6f);
        Fonts.BOLD.draw(label, bgX + 13f + (68 - textWidth) / 2f, bgY + 226f, 6f,
                new Color(196, 218, 210, (int) (215 * alphaMultiplier)).getRGB());
    }

    private boolean isModern() {
        Hud hud = Hud.getInstance();
        return hud == null || hud.clickGuiStyle == null || hud.clickGuiStyle.isSelected("Modern");
    }

    private boolean isDropDown() {
        Hud hud = Hud.getInstance();
        return hud != null && hud.clickGuiStyle != null && hud.clickGuiStyle.isSelected("DropDown");
    }
}
