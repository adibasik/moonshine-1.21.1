package moonshine.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.EntityColorEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.util.ColorUtil;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeeInvisible extends ModuleStructure {

    SliderSettings alphaSetting = new SliderSettings("Прозрачность", "Прозрачность игрока").setValue(0.5f).range(0.1F, 1);

    public SeeInvisible() {
        super("SeeInvisible", "See Invisible", ModuleCategory.RENDER);
        settings(alphaSetting);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        e.setColor(ColorUtil.multAlpha(e.getColor(), alphaSetting.getValue()));
        e.cancel();
    }

}
