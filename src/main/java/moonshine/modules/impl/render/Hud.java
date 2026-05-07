package moonshine.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.setting.implement.*;
import moonshine.util.Instance;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends ModuleStructure {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
            .value("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "test",
                    "TargetHud",
//                    "CoolDowns",
//                    "Inventory",
                    "Info",
                    "Notifications")

            .selected("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "TargetHud",
//                    "CoolDowns",
//                    "Inventory",
                    "Info",
                    "Notifications");

    public BooleanSetting showBps = new BooleanSetting("Show BPS", "Показывать блоки в секунду")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Info"));

    public BooleanSetting showTps = new BooleanSetting("Show TPS", "Показывать TPS в Watermark")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public ColorSetting clientPrimaryColor = new ColorSetting("Client Color 1", "Primary client interface color")
            .value(new Color(52, 52, 52, 255).getRGB());

    public ColorSetting clientSecondaryColor = new ColorSetting("Client Color 2", "Secondary client interface color")
            .value(new Color(32, 32, 32, 255).getRGB());

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        settings(interfaceSettings, showBps, showTps, clientPrimaryColor, clientSecondaryColor);
    }
}
