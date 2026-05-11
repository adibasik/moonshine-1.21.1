package moonshine.screens.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import moonshine.IMinecraft;
import moonshine.Initialization;
import moonshine.modules.impl.render.Hud;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.setting.SettingComponentAdder;
import moonshine.screens.clickgui.impl.DragHandler;
import moonshine.screens.clickgui.impl.autobuy.autobuyui.AutoBuyRenderer;
import moonshine.screens.clickgui.impl.background.BackgroundComponent;
import moonshine.screens.clickgui.impl.configs.ConfigsRenderer;
import moonshine.screens.clickgui.impl.module.ModuleComponent;
import moonshine.screens.clickgui.impl.settingsrender.ColorComponent;
import moonshine.screens.clickgui.impl.settingsrender.MultiSelectComponent;
import moonshine.screens.clickgui.impl.settingsrender.SelectComponent;
import moonshine.screens.clickgui.impl.settingsrender.BindComponent;
import moonshine.screens.clickgui.impl.settingsrender.TextComponent;
import moonshine.util.animations.Direction;
import moonshine.util.animations.GuiAnimation;
import moonshine.util.interfaces.AbstractSettingComponent;
import moonshine.util.math.FrameRateCounter;
import moonshine.util.render.Render2D;
import moonshine.util.render.shader.Scissor;
import moonshine.util.render.font.Fonts;
import moonshine.util.render.gif.GifRender;
import moonshine.util.selfdestruct.SelfDestructManager;
import moonshine.util.string.KeyHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();
    private static final int FIXED_GUI_SCALE = 2;

    private final BackgroundComponent background = new BackgroundComponent();
    private final ModuleComponent moduleComponent = new ModuleComponent();
    private final AutoBuyRenderer autoBuyRenderer = new AutoBuyRenderer();
    private final ConfigsRenderer configsRenderer = new ConfigsRenderer();
    private final DragHandler dragHandler = new DragHandler();
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;

    private final GuiAnimation openAnimation = new GuiAnimation();
    private boolean closing = false;
    private boolean waitingForSlide = false;
    private boolean slideTriggered = false;

    private float hintAlphaAnimation = 0f;
    private long lastHintUpdateTime = System.currentTimeMillis();
    private static final float HINT_ANIM_SPEED = 6f;
    private static final float OFFSET_THRESHOLD = 5f;

    private int lastMouseX;
    private int lastMouseY;
    private float lastDelta;

    private static final ModuleCategory[] DROPDOWN_CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final float DROPDOWN_WIDTH = 132f;
    private static final float DROPDOWN_HEADER_HEIGHT = 21f;
    private static final float DROPDOWN_MODULE_HEIGHT = 18f;
    private static final float DROPDOWN_SETTING_HEIGHT = 16f;
    private static final float DROPDOWN_SETTING_SPACING = 2f;
    private static final float DROPDOWN_GAP = 7f;
    private static final float DROPDOWN_MAX_HEIGHT = 292f;

    private final Map<ModuleCategory, DropDownCategoryState> dropDownCategoryStates = new EnumMap<>(ModuleCategory.class);
    private final Set<String> dropDownExpandedModules = new HashSet<>();
    private final Set<ModuleCategory> dropDownCollapsedCategories = EnumSet.noneOf(ModuleCategory.class);
    private final Map<ModuleStructure, List<AbstractSettingComponent>> dropDownSettingComponents = new IdentityHashMap<>();
    private final Map<ModuleCategory, Float> dropDownScrollOffsets = new EnumMap<>(ModuleCategory.class);
    private final Map<ModuleCategory, Float> dropDownTargetScrollOffsets = new EnumMap<>(ModuleCategory.class);
    private ModuleCategory dropDownDraggingCategory = null;
    private float dropDownDragOffsetX = 0f;
    private float dropDownDragOffsetY = 0f;
    private int dropDownLayoutWidth = -1;
    private int dropDownLayoutHeight = -1;
    private AbstractSettingComponent dropDownActiveComponent = null;
    private boolean dropDownSearchActive = false;
    private String dropDownSearchText = "";
    private int dropDownSearchCursor = 0;
    private float dropDownSearchAnimation = 0f;
    private float dropDownSearchCursorBlink = 0f;

    private static final class DropDownCategoryState {
        private float x;
        private float y;

        private DropDownCategoryState(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public ClickGui() {
        super(Text.of("MenuScreen"));
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        waitingForSlide = false;
        slideTriggered = false;
        openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
        hintAlphaAnimation = 0f;
        lastHintUpdateTime = System.currentTimeMillis();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        background.setSearchActive(false);
        autoBuyRenderer.resetForClose();
        updateModules();
    }

    private void updateModules() {
        List<ModuleStructure> modules = new ArrayList<>();
        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure m : repo.modules()) {
                    if (m.getCategory() == selectedCategory) modules.add(m);
                }
            }
        } catch (Exception ignored) {}
        moduleComponent.updateModules(modules, selectedCategory);
    }

    public void openGui() {
        if (SelfDestructManager.isLocked()) {
            return;
        }
        if (mc.currentScreen == null) {
            closing = false;
            waitingForSlide = false;
            slideTriggered = false;
            openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
            mc.setScreen(this);
        }
    }

    @Override
    public void tick() {
        GifRender.tick();
        moduleComponent.tick();
        if (isDropDownStyle()) {
            dropDownSettingComponents.values().forEach(components -> components.forEach(AbstractSettingComponent::tick));
        }
        super.tick();
    }

    private float[] calculateBackground(float scale) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        float bgX = (vw - BackgroundComponent.BG_WIDTH) / 2f + dragHandler.getOffsetX();
        float bgY = (vh - BackgroundComponent.BG_HEIGHT) / 2f + dragHandler.getOffsetY();
        return new float[]{bgX, bgY, vw, vh};
    }

    private boolean isAnyBindListening() {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                return true;
            }
        }
        return false;
    }

    private void updateHintAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastHintUpdateTime) / 1000f, 0.1f);
        lastHintUpdateTime = currentTime;

        float offsetX = Math.abs(dragHandler.getOffsetX());
        float offsetY = Math.abs(dragHandler.getOffsetY());
        boolean shouldShow = (offsetX > OFFSET_THRESHOLD || offsetY > OFFSET_THRESHOLD);

        float target = shouldShow ? 1f : 0f;
        float diff = target - hintAlphaAnimation;

        if (Math.abs(diff) < 0.001f) {
            hintAlphaAnimation = target;
        } else {
            hintAlphaAnimation += diff * HINT_ANIM_SPEED * deltaTime;
            hintAlphaAnimation = Math.max(0f, Math.min(1f, hintAlphaAnimation));
        }
    }

    private boolean isModuleCategory(ModuleCategory category) {
        return category != ModuleCategory.AUTOBUY ;
    }

    private boolean isDropDownStyle() {
        Hud hud = Hud.getInstance();
        return hud != null && hud.clickGuiStyle != null && hud.clickGuiStyle.isSelected("DropDown");
    }

    private void renderDropDownGui(DrawContext context, float bgX, float bgY, float mouseX, float mouseY,
                                   float delta, int guiScale, float alphaMultiplier) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        initDropDownLayout(vw, vh);
        updateDropDownAnimations(delta);

        renderDropDownSearch(mouseX, mouseY, vw, alphaMultiplier);

        for (ModuleCategory category : DROPDOWN_CATEGORIES) {
            if (category != dropDownDraggingCategory) {
                renderDropDownCategory(context, category, mouseX, mouseY, delta, guiScale, alphaMultiplier);
            }
        }

        if (dropDownDraggingCategory != null) {
            renderDropDownCategory(context, dropDownDraggingCategory, mouseX, mouseY, delta, guiScale, alphaMultiplier);
        }
    }

    private void updateDropDownAnimations(float delta) {
        float frame = Math.min(delta / 20f, 0.1f);
        if (frame <= 0f) frame = 0.016f;

        float searchTarget = (dropDownSearchActive || !dropDownSearchText.isEmpty()) ? 1f : 0f;
        dropDownSearchAnimation += (searchTarget - dropDownSearchAnimation) * Math.min(1f, frame * 12f);
        if (dropDownSearchActive) {
            dropDownSearchCursorBlink += frame * 2f;
            if (dropDownSearchCursorBlink > 1f) dropDownSearchCursorBlink -= 1f;
        }

        for (ModuleCategory category : DROPDOWN_CATEGORIES) {
            float current = dropDownScrollOffsets.getOrDefault(category, 0f);
            float target = dropDownTargetScrollOffsets.getOrDefault(category, 0f);
            if (Math.abs(target - current) < 0.25f) {
                current = target;
            } else {
                current += (target - current) * Math.min(1f, frame * 14f);
            }
            dropDownScrollOffsets.put(category, current);
        }
    }

    private void renderDropDownSearch(float mouseX, float mouseY, int vw, float alphaMultiplier) {
        float anim = dropDownSearchAnimation * alphaMultiplier;
        if (anim <= 0.01f) return;

        float w = 188f;
        float h = 22f;
        float x = (vw - w) / 2f;
        float y = 10f - (1f - dropDownSearchAnimation) * 8f;
        boolean hovered = isInside(mouseX, mouseY, x, y, w, h);

        Render2D.blur(x, y, w, h, 5, 6, new Color(0, 0, 0, (int) (96 * anim)).getRGB());
        Render2D.rect(x, y, w, h, new Color(17, 21, 27, (int) (230 * anim)).getRGB(), 6);
        Render2D.outline(x, y, w, h, 0.6f,
                new Color(hovered ? 98 : 76, hovered ? 189 : 105, hovered ? 255 : 126, (int) ((hovered ? 170 : 112) * anim)).getRGB(), 6);
        Render2D.rect(x + 8f, y + h - 3f, w - 16f, 0.8f,
                new Color(98, 189, 255, (int) (95 * anim)).getRGB(), 1);

        Fonts.GUI_ICONS.draw("J", x + 8f, y + 6f, 8f,
                new Color(168, 188, 202, (int) (205 * anim)).getRGB());

        String text = dropDownSearchText.isEmpty() ? "Search modules..." : dropDownSearchText;
        Color textColor = dropDownSearchText.isEmpty()
                ? new Color(118, 132, 144, (int) (160 * anim))
                : new Color(220, 228, 234, (int) (225 * anim));
        Fonts.BOLD.draw(text, x + 22f, y + 7f, 5.5f, textColor.getRGB());

        if (dropDownSearchActive && !dropDownSearchText.isEmpty() && dropDownSearchCursorBlink < 0.5f) {
            String beforeCursor = dropDownSearchText.substring(0, Math.min(dropDownSearchCursor, dropDownSearchText.length()));
            float cursorX = x + 22f + Fonts.BOLD.getWidth(beforeCursor, 5.5f) + 1f;
            Render2D.rect(cursorX, y + 6f, 0.7f, 10f, new Color(220, 232, 240, (int) (210 * anim)).getRGB(), 0);
        }
    }

    private void initDropDownLayout(int vw, int vh) {
        if (!dropDownCategoryStates.isEmpty() && dropDownLayoutWidth == vw && dropDownLayoutHeight == vh) {
            return;
        }

        dropDownLayoutWidth = vw;
        dropDownLayoutHeight = vh;
        float totalWidth = DROPDOWN_CATEGORIES.length * DROPDOWN_WIDTH + (DROPDOWN_CATEGORIES.length - 1) * DROPDOWN_GAP;
        float startX = Math.max(8f, (vw - totalWidth) / 2f);
        float startY = Math.max(38f, (vh - DROPDOWN_MAX_HEIGHT) / 2f + 18f);

        for (int i = 0; i < DROPDOWN_CATEGORIES.length; i++) {
            ModuleCategory category = DROPDOWN_CATEGORIES[i];
            dropDownCategoryStates.putIfAbsent(category, new DropDownCategoryState(startX + i * (DROPDOWN_WIDTH + DROPDOWN_GAP), startY));
            dropDownScrollOffsets.putIfAbsent(category, 0f);
            dropDownTargetScrollOffsets.putIfAbsent(category, 0f);
        }
    }

    private void renderDropDownCategory(DrawContext context, ModuleCategory category, float mouseX, float mouseY,
                                        float delta, int guiScale, float alphaMultiplier) {
        DropDownCategoryState state = dropDownCategoryStates.get(category);
        if (state == null) return;

        List<ModuleStructure> modules = getDropDownModules(category);
        boolean collapsed = dropDownCollapsedCategories.contains(category);
        float contentHeight = collapsed ? 0f : calculateDropDownContentHeight(category, modules);
        float panelHeight = DROPDOWN_HEADER_HEIGHT + Math.min(contentHeight, DROPDOWN_MAX_HEIGHT - DROPDOWN_HEADER_HEIGHT);
        clampDropDownScroll(category, contentHeight, panelHeight - DROPDOWN_HEADER_HEIGHT);
        boolean dragging = category == dropDownDraggingCategory;

        int panelAlpha = (int) ((dragging ? 244 : 232) * alphaMultiplier);
        int outlineAlpha = (int) ((dragging ? 190 : 112) * alphaMultiplier);
        Render2D.blur(state.x, state.y, DROPDOWN_WIDTH, panelHeight, 5, 5,
                new Color(0, 0, 0, (int) ((dragging ? 140 : 102) * alphaMultiplier)).getRGB());
        Render2D.rect(state.x, state.y, DROPDOWN_WIDTH, panelHeight,
                new Color(13, 16, 21, panelAlpha).getRGB(), 7);
        Render2D.gradientRect(state.x, state.y, DROPDOWN_WIDTH, panelHeight,
                new int[]{
                        new Color(26, 33, 42, (int) (130 * alphaMultiplier)).getRGB(),
                        new Color(13, 16, 21, panelAlpha).getRGB(),
                        new Color(12, 16, 20, panelAlpha).getRGB(),
                        new Color(9, 12, 16, panelAlpha).getRGB()
                },
                7);
        Render2D.outline(state.x, state.y, DROPDOWN_WIDTH, panelHeight, 0.5f,
                new Color(86, 101, 116, outlineAlpha).getRGB(), 7);

        boolean headerHovered = isInside(mouseX, mouseY, state.x, state.y, DROPDOWN_WIDTH, DROPDOWN_HEADER_HEIGHT);
        Render2D.rect(state.x + 2, state.y + 2, DROPDOWN_WIDTH - 4, DROPDOWN_HEADER_HEIGHT - 3,
                new Color(headerHovered ? 31 : 22, headerHovered ? 39 : 28, headerHovered ? 49 : 36,
                        (int) ((headerHovered ? 188 : 158) * alphaMultiplier)).getRGB(), 5);
        Render2D.rect(state.x + 8f, state.y + DROPDOWN_HEADER_HEIGHT - 2.4f, DROPDOWN_WIDTH - 16f, 0.8f,
                new Color(98, 189, 255, (int) (92 * alphaMultiplier)).getRGB(), 1);

        Fonts.BOLD.draw(category.getReadableName(), state.x + 8, state.y + 7, 6.5f,
                new Color(226, 233, 238, (int) (232 * alphaMultiplier)).getRGB());
        float markerX = state.x + DROPDOWN_WIDTH - 16f;
        float markerY = state.y + 8f;
        int markerColor = new Color(170, 196, 214, (int) (210 * alphaMultiplier)).getRGB();
        if (collapsed) {
            Render2D.rect(markerX, markerY + 3f, 8f, 1f, markerColor, 0.5f);
            Render2D.rect(markerX + 3.5f, markerY, 1f, 8f, markerColor, 0.5f);
        } else {
            Render2D.rect(markerX, markerY + 3f, 8f, 1f, markerColor, 0.5f);
        }

        if (collapsed) return;

        float visibleContentHeight = panelHeight - DROPDOWN_HEADER_HEIGHT;
        renderDropDownScrollFades(category, state.x, state.y + DROPDOWN_HEADER_HEIGHT, DROPDOWN_WIDTH, visibleContentHeight, contentHeight, alphaMultiplier);

        Scissor.enable(state.x, state.y + DROPDOWN_HEADER_HEIGHT, DROPDOWN_WIDTH, visibleContentHeight, guiScale);
        float y = state.y + DROPDOWN_HEADER_HEIGHT + dropDownScrollOffsets.getOrDefault(category, 0f);
        if (modules.isEmpty()) {
            String emptyText = dropDownSearchText.isEmpty() ? "No modules" : "No results";
            float textWidth = Fonts.BOLD.getWidth(emptyText, 5.5f);
            Fonts.BOLD.draw(emptyText, state.x + (DROPDOWN_WIDTH - textWidth) / 2f, state.y + DROPDOWN_HEADER_HEIGHT + 12f, 5.5f,
                    new Color(128, 144, 156, (int) (160 * alphaMultiplier)).getRGB());
        }
        for (ModuleStructure module : modules) {
            String key = dropDownModuleKey(category, module);
            boolean expanded = dropDownExpandedModules.contains(key);
            renderDropDownModuleRow(category, module, state.x, y, mouseX, mouseY, alphaMultiplier, expanded);
            y += DROPDOWN_MODULE_HEIGHT;

            if (expanded) {
                y = renderDropDownSettings(context, module, state.x + 6f, y, DROPDOWN_WIDTH - 12f,
                        mouseX, mouseY, delta, alphaMultiplier);
            }

            if (y > state.y + panelHeight + 24f) break;
        }
        Scissor.disable();
    }

    private void renderDropDownModuleRow(ModuleCategory category, ModuleStructure module, float x, float y,
                                         float mouseX, float mouseY, float alphaMultiplier, boolean expanded) {
        boolean hovered = isInside(mouseX, mouseY, x, y, DROPDOWN_WIDTH, DROPDOWN_MODULE_HEIGHT);
        int rowAlpha = (int) ((hovered ? 96 : expanded ? 70 : 44) * alphaMultiplier);
        Render2D.rect(x + 4, y + 2, DROPDOWN_WIDTH - 8, DROPDOWN_MODULE_HEIGHT - 3,
                new Color(18, 23, 29, rowAlpha).getRGB(), 4);
        if (hovered || expanded) {
            Render2D.outline(x + 4, y + 2, DROPDOWN_WIDTH - 8, DROPDOWN_MODULE_HEIGHT - 3, 0.45f,
                    new Color(98, 189, 255, (int) ((hovered ? 80 : 48) * alphaMultiplier)).getRGB(), 4);
        }

        int arrowColor = new Color(158, 178, 194, (int) (185 * alphaMultiplier)).getRGB();
        float arrowX = x + 7f;
        float arrowY = y + 8f;
        if (expanded) {
            Render2D.rect(arrowX, arrowY, 6f, 1f, arrowColor, 0.5f);
            Render2D.rect(arrowX + 1f, arrowY + 2.5f, 4f, 1f, arrowColor, 0.5f);
        } else {
            Render2D.rect(arrowX + 2.5f, arrowY - 2f, 1f, 6f, arrowColor, 0.5f);
            Render2D.rect(arrowX, arrowY + 0.5f, 6f, 1f, arrowColor, 0.5f);
        }

        Fonts.BOLD.draw(module.getName(), x + 17, y + 6, 5.5f,
                new Color(module.isState() ? 232 : 168, module.isState() ? 238 : 178, module.isState() ? 242 : 188,
                        (int) (220 * alphaMultiplier)).getRGB());

        if (module.getKey() != GLFW.GLFW_KEY_UNKNOWN && module.getKey() != -1) {
            String bind = KeyHelper.getKeyName(module.getKey());
            float bindW = Fonts.BOLD.getWidth(bind, 4.5f);
            Fonts.BOLD.draw(bind, x + DROPDOWN_WIDTH - 31f - bindW, y + 6.5f, 4.5f,
                    new Color(128, 142, 143, (int) (165 * alphaMultiplier)).getRGB());
        }

        renderDropDownSwitch(x + DROPDOWN_WIDTH - 24f, y + 5f, module.isState(), alphaMultiplier);
        Render2D.rect(x + 6, y + DROPDOWN_MODULE_HEIGHT - 0.5f, DROPDOWN_WIDTH - 12, 0.5f,
                new Color(61, 72, 82, (int) (34 * alphaMultiplier)).getRGB(), 0);
    }

    private void renderDropDownSwitch(float x, float y, boolean enabled, float alphaMultiplier) {
        float w = 17f;
        float h = 8f;
        float knob = 6f;
        Color off = new Color(43, 50, 58, (int) (200 * alphaMultiplier));
        Color on = new Color(54, 137, 201, (int) (220 * alphaMultiplier));
        Render2D.rect(x, y, w, h, (enabled ? on : off).getRGB(), 4f);
        float knobX = x + 1f + (enabled ? w - knob - 2f : 0f);
        Render2D.rect(knobX, y + 1f, knob, knob, new Color(226, 235, 240, (int) (235 * alphaMultiplier)).getRGB(), 3f);
    }

    private float renderDropDownSettings(DrawContext context, ModuleStructure module, float x, float y, float width,
                                         float mouseX, float mouseY, float delta, float alphaMultiplier) {
        List<AbstractSettingComponent> components = getDropDownSettingComponents(module);
        float posY = y + 3f;
        for (AbstractSettingComponent component : components) {
            if (!component.getSetting().isVisible()) continue;

            float componentHeight = getDropDownSettingHeight(component);
            component.position(x, posY);
            component.size(width, DROPDOWN_SETTING_HEIGHT);
            component.setAlphaMultiplier(alphaMultiplier);

            Render2D.rect(x - 1, posY - 1, width + 2, componentHeight + 1,
                    new Color(10, 14, 19, (int) (122 * alphaMultiplier)).getRGB(), 4);
            Render2D.outline(x - 1, posY - 1, width + 2, componentHeight + 1, 0.35f,
                    new Color(70, 86, 101, (int) (52 * alphaMultiplier)).getRGB(), 4);
            context.getMatrices().pushMatrix();
            component.render(context, (int) mouseX, (int) mouseY, delta);
            context.getMatrices().popMatrix();
            posY += componentHeight + DROPDOWN_SETTING_SPACING;
        }
        return posY + 2f;
    }

    private List<ModuleStructure> getDropDownModules(ModuleCategory category) {
        List<ModuleStructure> modules = new ArrayList<>();
        String query = dropDownSearchText.trim().toLowerCase();
        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure module : repo.modules()) {
                    if (module.getCategory() == category
                            && (query.isEmpty() || module.getName().toLowerCase().contains(query))) {
                        modules.add(module);
                    }
                }
            }
        } catch (Exception ignored) {}
        return modules;
    }

    private List<AbstractSettingComponent> getDropDownSettingComponents(ModuleStructure module) {
        return dropDownSettingComponents.computeIfAbsent(module, key -> {
            List<AbstractSettingComponent> components = new ArrayList<>();
            new SettingComponentAdder().addSettingComponent(key.settings(), components);
            return components;
        });
    }

    private float calculateDropDownContentHeight(ModuleCategory category, List<ModuleStructure> modules) {
        float height = 0f;
        for (ModuleStructure module : modules) {
            height += DROPDOWN_MODULE_HEIGHT;
            if (dropDownExpandedModules.contains(dropDownModuleKey(category, module))) {
                for (AbstractSettingComponent component : getDropDownSettingComponents(module)) {
                    if (component.getSetting().isVisible()) {
                        height += getDropDownSettingHeight(component) + DROPDOWN_SETTING_SPACING;
                    }
                }
                height += 5f;
            }
        }
        return Math.max(DROPDOWN_MODULE_HEIGHT, height);
    }

    private float getDropDownSettingHeight(AbstractSettingComponent component) {
        if (component instanceof SelectComponent selectComponent) return Math.max(DROPDOWN_SETTING_HEIGHT, selectComponent.getTotalHeight());
        if (component instanceof MultiSelectComponent multiSelectComponent) return Math.max(DROPDOWN_SETTING_HEIGHT, multiSelectComponent.getTotalHeight());
        if (component instanceof ColorComponent colorComponent) return Math.max(DROPDOWN_SETTING_HEIGHT, colorComponent.getTotalHeight());
        return DROPDOWN_SETTING_HEIGHT;
    }

    private void renderDropDownScrollFades(ModuleCategory category, float x, float y, float width, float height,
                                           float contentHeight, float alphaMultiplier) {
        float maxScroll = Math.max(0f, contentHeight - height);
        if (maxScroll <= 0.5f) return;

        float scroll = dropDownScrollOffsets.getOrDefault(category, 0f);
        if (scroll < -0.5f) {
            for (int i = 0; i < 10; i++) {
                float alpha = 70 * alphaMultiplier * (1f - i / 10f);
                Render2D.rect(x + 3, y + i, width - 6, 1,
                        new Color(5, 8, 12, (int) alpha).getRGB(), 0);
            }
        }
        if (scroll > -maxScroll + 0.5f) {
            for (int i = 0; i < 10; i++) {
                float alpha = 70 * alphaMultiplier * (i / 10f);
                Render2D.rect(x + 3, y + height - 10 + i, width - 6, 1,
                        new Color(5, 8, 12, (int) alpha).getRGB(), 0);
            }
        }

        float thumbH = Math.max(18f, height * (height / contentHeight));
        float thumbRange = Math.max(1f, height - thumbH - 8f);
        float thumbY = y + 4f + thumbRange * (-scroll / maxScroll);
        Render2D.rect(x + width - 3.5f, thumbY, 1.5f, thumbH,
                new Color(98, 189, 255, (int) (100 * alphaMultiplier)).getRGB(), 1f);
    }

    private void clampDropDownScroll(ModuleCategory category, float contentHeight, float visibleHeight) {
        float maxScroll = Math.max(0f, contentHeight - visibleHeight);
        float target = Math.max(-maxScroll, Math.min(0f, dropDownTargetScrollOffsets.getOrDefault(category, 0f)));
        float current = Math.max(-maxScroll, Math.min(0f, dropDownScrollOffsets.getOrDefault(category, 0f)));
        dropDownTargetScrollOffsets.put(category, target);
        dropDownScrollOffsets.put(category, current);
    }

    private boolean handleDropDownClick(double mouseX, double mouseY, int button, float bgX, float bgY) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        initDropDownLayout(vw, vh);
        dropDownActiveComponent = null;

        float searchW = 188f;
        float searchH = 22f;
        float searchX = (vw - searchW) / 2f;
        float searchY = 10f - (1f - dropDownSearchAnimation) * 8f;
        if (dropDownSearchAnimation > 0.05f && isInside(mouseX, mouseY, searchX, searchY, searchW, searchH)) {
            if (button == 0) setDropDownSearchActive(true);
            return true;
        }

        for (int i = DROPDOWN_CATEGORIES.length - 1; i >= 0; i--) {
            ModuleCategory category = DROPDOWN_CATEGORIES[i];
            DropDownCategoryState state = dropDownCategoryStates.get(category);
            if (state == null) continue;

            List<ModuleStructure> modules = getDropDownModules(category);
            float panelHeight = DROPDOWN_HEADER_HEIGHT + Math.min(
                    dropDownCollapsedCategories.contains(category) ? 0f : calculateDropDownContentHeight(category, modules),
                    DROPDOWN_MAX_HEIGHT - DROPDOWN_HEADER_HEIGHT);

            if (!isInside(mouseX, mouseY, state.x, state.y, DROPDOWN_WIDTH, panelHeight)) {
                continue;
            }

            if (isInside(mouseX, mouseY, state.x, state.y, DROPDOWN_WIDTH, DROPDOWN_HEADER_HEIGHT)) {
                if (button == 0 && mouseX >= state.x + DROPDOWN_WIDTH - 24f) {
                    toggleDropDownCategory(category);
                    return true;
                }
                if (button == 1) {
                    toggleDropDownCategory(category);
                    return true;
                }
                if (button == 0) {
                    dropDownDraggingCategory = category;
                    dropDownDragOffsetX = (float) mouseX - state.x;
                    dropDownDragOffsetY = (float) mouseY - state.y;
                    return true;
                }
            }

            if (dropDownCollapsedCategories.contains(category)) {
                return true;
            }

            float y = state.y + DROPDOWN_HEADER_HEIGHT + dropDownScrollOffsets.getOrDefault(category, 0f);
            for (ModuleStructure module : modules) {
                String key = dropDownModuleKey(category, module);
                boolean expanded = dropDownExpandedModules.contains(key);

                if (isInside(mouseX, mouseY, state.x, y, DROPDOWN_WIDTH, DROPDOWN_MODULE_HEIGHT)) {
                    if (button == 0 && mouseX >= state.x + DROPDOWN_WIDTH - 30f) {
                        module.switchState();
                    } else if (button == 2) {
                        moduleComponent.setBindingModule(module);
                    } else if (button == 0 || button == 1) {
                        toggleDropDownModule(key);
                    }
                    return true;
                }
                y += DROPDOWN_MODULE_HEIGHT;

                if (expanded) {
                    for (AbstractSettingComponent component : getDropDownSettingComponents(module)) {
                        if (!component.getSetting().isVisible()) continue;
                        float componentHeight = getDropDownSettingHeight(component);
                        if (isInside(mouseX, mouseY, state.x + 5f, y + 2f, DROPDOWN_WIDTH - 10f, componentHeight + 2f)) {
                            dropDownActiveComponent = component;
                            component.mouseClicked(mouseX, mouseY, button);
                            return true;
                        }
                        y += componentHeight + DROPDOWN_SETTING_SPACING;
                    }
                    y += 5f;
                }

                if (y > state.y + panelHeight) break;
            }
            return true;
        }

        return false;
    }

    private boolean handleDropDownRelease(double mouseX, double mouseY, int button) {
        if (dropDownDraggingCategory != null) {
            dropDownDraggingCategory = null;
            return true;
        }

        for (List<AbstractSettingComponent> components : dropDownSettingComponents.values()) {
            for (AbstractSettingComponent component : components) {
                if (component.getSetting().isVisible() && component.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        dropDownActiveComponent = null;
        return true;
    }

    private boolean handleDropDownScroll(double mouseX, double mouseY, double vertical) {
        for (int i = DROPDOWN_CATEGORIES.length - 1; i >= 0; i--) {
            ModuleCategory category = DROPDOWN_CATEGORIES[i];
            DropDownCategoryState state = dropDownCategoryStates.get(category);
            if (state == null || dropDownCollapsedCategories.contains(category)) continue;

            List<ModuleStructure> modules = getDropDownModules(category);
            float contentHeight = calculateDropDownContentHeight(category, modules);
            float panelHeight = DROPDOWN_HEADER_HEIGHT + Math.min(contentHeight, DROPDOWN_MAX_HEIGHT - DROPDOWN_HEADER_HEIGHT);
            float contentVisibleHeight = panelHeight - DROPDOWN_HEADER_HEIGHT;

            if (isInside(mouseX, mouseY, state.x, state.y + DROPDOWN_HEADER_HEIGHT, DROPDOWN_WIDTH, contentVisibleHeight)) {
                float maxScroll = Math.max(0f, contentHeight - contentVisibleHeight);
                float currentTarget = dropDownTargetScrollOffsets.getOrDefault(category, 0f);
                float next = currentTarget + (float) vertical * 24f;
                next = Math.max(-maxScroll, Math.min(0f, next));
                dropDownTargetScrollOffsets.put(category, next);
                return true;
            }
        }
        return true;
    }

    private void updateDropDownDrag(float mouseX, float mouseY) {
        if (dropDownDraggingCategory == null) return;
        DropDownCategoryState state = dropDownCategoryStates.get(dropDownDraggingCategory);
        if (state == null) return;
        state.x = mouseX - dropDownDragOffsetX;
        state.y = mouseY - dropDownDragOffsetY;
    }

    private void toggleDropDownCategory(ModuleCategory category) {
        if (!dropDownCollapsedCategories.remove(category)) {
            dropDownCollapsedCategories.add(category);
        }
    }

    private void toggleDropDownModule(String key) {
        if (!dropDownExpandedModules.remove(key)) {
            dropDownExpandedModules.add(key);
        }
    }

    private String dropDownModuleKey(ModuleCategory category, ModuleStructure module) {
        return category.name() + "-" + module.getName();
    }

    private boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void setDropDownSearchActive(boolean active) {
        dropDownSearchActive = active;
        dropDownSearchCursorBlink = 0f;
        if (active) {
            dropDownSearchCursor = dropDownSearchText.length();
        }
    }

    private boolean isCtrlDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    private boolean handleDropDownSearchKey(int keyCode, int modifiers) {
        if (isCtrlDown(modifiers) && keyCode == GLFW.GLFW_KEY_F) {
            setDropDownSearchActive(true);
            return true;
        }

        if (!dropDownSearchActive) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (!dropDownSearchText.isEmpty()) {
                    dropDownSearchText = "";
                    dropDownSearchCursor = 0;
                    resetDropDownScroll();
                }
                setDropDownSearchActive(false);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (dropDownSearchCursor > 0 && !dropDownSearchText.isEmpty()) {
                    dropDownSearchText = dropDownSearchText.substring(0, dropDownSearchCursor - 1)
                            + dropDownSearchText.substring(dropDownSearchCursor);
                    dropDownSearchCursor--;
                    resetDropDownScroll();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (dropDownSearchCursor < dropDownSearchText.length()) {
                    dropDownSearchText = dropDownSearchText.substring(0, dropDownSearchCursor)
                            + dropDownSearchText.substring(dropDownSearchCursor + 1);
                    resetDropDownScroll();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (dropDownSearchCursor > 0) dropDownSearchCursor--;
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (dropDownSearchCursor < dropDownSearchText.length()) dropDownSearchCursor++;
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                dropDownSearchCursor = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                dropDownSearchCursor = dropDownSearchText.length();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER -> {
                setDropDownSearchActive(false);
                return true;
            }
        }
        return false;
    }

    private boolean handleDropDownSearchChar(char chr) {
        if (!dropDownSearchActive || Character.isISOControl(chr)) return false;
        dropDownSearchText = dropDownSearchText.substring(0, dropDownSearchCursor) + chr
                + dropDownSearchText.substring(dropDownSearchCursor);
        dropDownSearchCursor++;
        resetDropDownScroll();
        return true;
    }

    private void resetDropDownScroll() {
        for (ModuleCategory category : DROPDOWN_CATEGORIES) {
            dropDownTargetScrollOffsets.put(category, 0f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastDelta = delta;

        FrameRateCounter.INSTANCE.recordFrame();

        if (waitingForSlide && selectedCategory == ModuleCategory.AUTOBUY) {
            if (!slideTriggered) {
                autoBuyRenderer.triggerSlideOut();
                slideTriggered = true;
            }

            if (autoBuyRenderer.isSlideOutComplete()) {
                waitingForSlide = false;
                slideTriggered = false;
                startActualClose();
            }
        }

        if (closing && !waitingForSlide && openAnimation.isFinished(Direction.BACKWARDS)) {
            closing = false;
            TextComponent.typing = false;
            moduleComponent.setBindingModule(null);
            dragHandler.stopDrag();
            autoBuyRenderer.resetForClose();
            mc.currentScreen = null;
        }
    }

    public void renderOverlay(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.getWindow() == null) return;

        float delta = lastDelta;
        int mouseX = lastMouseX;
        int mouseY = lastMouseY;

        float scrollSpeed = Math.min(1f, 60f / Math.max(FrameRateCounter.INSTANCE.getFps(), 1));
        float animValue = openAnimation.getOutput().floatValue();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        context.createNewRootLayer();

        int dimAlpha = (int) (125 * animValue);
        if (dimAlpha > 0) {
            Render2D.rect(0, 0, 5000, 5000, new Color(0, 0, 0, dimAlpha).getRGB(), 0);
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;

        float mx = mouseX / scale, my = mouseY / scale;

        if ((!closing || waitingForSlide) && isDropDownStyle()) {
            updateDropDownDrag(mx, my);
        } else if (!closing || waitingForSlide) {
            dragHandler.update(mx, my);
        }

        updateHintAnimation();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float[] bg = calculateBackground(scale);
        float bgX = bg[0];
        float bgY = bg[1];
        int vw = (int) bg[2];
        int vh = (int) bg[3];

        float yOffset;
        if (closing && !waitingForSlide) {
            yOffset = (1f - animValue) * 30f;
        } else {
            yOffset = (1f - animValue) * -15f;
        }
        bgY += yOffset;

        float alphaMultiplier = animValue;

        context.getMatrices().pushMatrix();

        if (isDropDownStyle()) {
            renderDropDownGui(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier);
            Scissor.reset();
            context.getMatrices().popMatrix();
            context.getMatrices().popMatrix();
            return;
        }

        background.render(context, bgX, bgY, selectedCategory, delta, alphaMultiplier);
        background.renderCategoryPanel(bgX, bgY, alphaMultiplier);
        background.renderHeader(bgX, bgY, selectedCategory, alphaMultiplier);
        background.renderCategoryNames(bgX, bgY, selectedCategory, alphaMultiplier);

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 46f;
        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 46f;

        float normalAlpha = background.getNormalPanelAlpha();
        float searchAlpha = background.getSearchPanelAlpha();

        if (normalAlpha > 0.01f) {
            configsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);

            boolean isAutoBuySliding = autoBuyRenderer.isSliding();
            boolean shouldRenderModules = isModuleCategory(selectedCategory);
            boolean slidingToModuleCategory = isAutoBuySliding && isModuleCategory(selectedCategory);

            if (shouldRenderModules || slidingToModuleCategory) {
                moduleComponent.updateScroll(delta, scrollSpeed);
                moduleComponent.updateScrollFades(delta, scrollSpeed, mlH, spH);
                moduleComponent.renderModuleList(context, mlX, mlY, mlW, mlH, mx, my, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
                moduleComponent.renderSettingsPanel(context, spX, spY, spW, spH, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
            }

            autoBuyRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);
        }

        if (searchAlpha > 0.01f) {
            background.renderSearchResults(context, bgX, bgY, mx, my, FIXED_GUI_SCALE, alphaMultiplier);
        }

        Scissor.reset();

        context.getMatrices().popMatrix();

        float finalHintAlpha = hintAlphaAnimation * alphaMultiplier;
        if (finalHintAlpha > 0.01f) {
            int hintAlpha = (int) (255 * finalHintAlpha);
            float centerX = vw / 2f;
            float centerY = vh / 2f;
            float textY = centerY + BackgroundComponent.BG_HEIGHT / 2f + 10f;
//            Fonts.TEST.drawCentered("Press CTRL + ALT to reset position", centerX, textY + 65, 6, new Color(150, 150, 150, hintAlpha).getRGB());
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) return false;

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = click.x() / scale, my = click.y() / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (isDropDownStyle()) {
            return handleDropDownClick(mx, my, click.button(), bgX, bgY);
        }

        if (background.isSearchBoxHovered(mx, my, bgX, bgY) && click.button() == 0) {
            background.setSearchActive(true);
            return true;
        }

        if (background.isSearchActive()) {
            if (click.button() == 0) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    searchModule.switchState();
                    return true;
                }

                float panelX = bgX + 92f;
                float panelY = bgY + 38f;
                float panelW = BackgroundComponent.BG_WIDTH - 100f;
                float panelH = BackgroundComponent.BG_HEIGHT - 46f;

                if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                    return true;
                }

                if (!background.isSearchBoxHovered(mx, my, bgX, bgY)) {
                    background.setSearchActive(false);
                }
            } else if (click.button() == 1) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    background.setSearchActive(false);
                    selectedCategory = searchModule.getCategory();
                    moduleComponent.selectModuleFromSearch(searchModule);
                    updateModules();
                    return true;
                }
            }
            return true;
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
//                return true;
//            }
//        }

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;

        if (click.button() == 2) {
            if (isAnyBindListening()) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                        bindComponent.handleMiddleMouseBind();
                        return true;
                    }
                }
            }

            if (moduleComponent.getBindingModule() != null) {
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                moduleComponent.setBindingModule(module);
                return true;
            }

            if (dragHandler.startDrag(mx, my, bgX, bgY, BackgroundComponent.BG_WIDTH, BackgroundComponent.BG_HEIGHT)) {
                return true;
            }
        }

        ModuleCategory cat = background.getCategoryAtPosition(mx, my, bgX, bgY);
        if (cat != null) {
            selectedCategory = cat;
            updateModules();
            return true;
        }

        if (isModuleCategory(selectedCategory)) {
            ModuleStructure starModule = moduleComponent.getModuleForStarClick(mx, my, mlX, mlY, mlW, mlH);
            if (starModule != null && click.button() == 0) {
                moduleComponent.toggleFavorite(starModule);
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                if (click.button() == 0) module.switchState();
                else if (click.button() == 1) moduleComponent.selectModule(module);
                return true;
            }

            float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
            if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c.getSetting().isVisible() && c.mouseClicked(mx, my, click.button())) return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (closing) return false;

        if (isDropDownStyle()) {
            int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
            float scale = (float) FIXED_GUI_SCALE / guiScale;
            double mx = click.x() / scale, my = click.y() / scale;
            return handleDropDownRelease(mx, my, click.button());
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            autoBuyRenderer.mouseReleased(click.x(), click.y(), click.button());
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            configsRenderer.mouseReleased(click.x(), click.y(), click.button());
//        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.mouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) return false;

        if (isAnyBindListening()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    return true;
                }
            }
        }

        if (moduleComponent.getBindingModule() != null) {
            return true;
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = mouseX / scale, my = mouseY / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (isDropDownStyle()) {
            for (List<AbstractSettingComponent> components : dropDownSettingComponents.values()) {
                for (AbstractSettingComponent component : components) {
                    if (component.getSetting().isVisible() && component.isHover(mx, my)
                            && component.mouseScrolled(mx, my, vertical)) {
                        return true;
                    }
                }
            }
            return handleDropDownScroll(mx, my, vertical);
        }

        if (background.isSearchActive()) {
            float panelX = bgX + 92f;
            float panelY = bgY + 38f;
            float panelW = BackgroundComponent.BG_WIDTH - 100f;
            float panelH = BackgroundComponent.BG_HEIGHT - 46f;

            if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                background.handleSearchScroll(vertical, panelH);
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
//                return true;
//            }
//        }

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= mlX && mx <= mlX + mlW && my >= mlY && my <= mlY + mlH) {
            moduleComponent.handleModuleScroll(vertical, mlH);
            return true;
        }

        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
            moduleComponent.handleSettingScroll(vertical, spH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (isDropDownStyle() && handleDropDownSearchKey(input.key(), input.modifiers())) {
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (autoBuyRenderer.isEditing()) {
                return true;
            }
            if (configsRenderer.isEditing()) {
                return true;
            }
            if (background.isSearchActive()) {
                background.setSearchActive(false);
                return true;
            }
            close();
            return true;
        }

        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
//                return true;
//            }
//        }

        if (background.isSearchActive()) {
            if (background.handleSearchKey(input.key())) {
                return true;
            }
        }

        if (dragHandler.isResetNeeded(input.key(), input.modifiers())) {
            dragHandler.reset();
            return true;
        }

        ModuleStructure binding = moduleComponent.getBindingModule();
        if (binding != null) {
            binding.setKey(input.key() == GLFW.GLFW_KEY_DELETE ? GLFW.GLFW_KEY_UNKNOWN : input.key());
            moduleComponent.setBindingModule(null);
            return true;
        }

        if (isDropDownStyle()) {
            for (List<AbstractSettingComponent> components : dropDownSettingComponents.values()) {
                for (AbstractSettingComponent c : components) {
                    if (c.getSetting().isVisible() && c.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
                }
            }
            return super.keyPressed(input);
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (closing) return false;

        if (isDropDownStyle() && handleDropDownSearchChar((char) input.codepoint())) {
            return true;
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
//                return true;
//            }
//        }

        if (background.isSearchActive()) {
            if (background.handleSearchChar((char) input.codepoint())) {
                return true;
            }
        }

        if (isDropDownStyle()) {
            for (List<AbstractSettingComponent> components : dropDownSettingComponents.values()) {
                for (AbstractSettingComponent c : components) {
                    if (c.getSetting().isVisible() && c.charTyped((char) input.codepoint(), input.modifiers())) return true;
                }
            }
            return super.charTyped(input);
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.charTyped((char) input.codepoint(), input.modifiers())) return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void startActualClose() {
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;

        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        TextComponent.typing = false;
        moduleComponent.setBindingModule(null);
        background.setSearchActive(false);
        dragHandler.stopDrag();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;

            if (selectedCategory == ModuleCategory.AUTOBUY) {
                waitingForSlide = true;
                slideTriggered = false;
            } else {
                waitingForSlide = false;
                startActualClose();
            }
        }
    }
}
