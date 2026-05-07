package moonshine.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.TickEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.SelectSetting;
import moonshine.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemPhysic extends ModuleStructure {
    public static ItemPhysic getInstance() {
        return Instance.get(ItemPhysic.class);
    }

    public SelectSetting mode = new SelectSetting("Физика", "").value("Обычная").selected("Обычная");

    public ItemPhysic() {
        super("ItemPhysic", "Item Physic", ModuleCategory.RENDER);
//        setup(mode);
    }

    @EventHandler
    public void onTick(TickEvent e) {
    }
}