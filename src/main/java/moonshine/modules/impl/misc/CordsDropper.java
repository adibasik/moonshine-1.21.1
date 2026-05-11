package moonshine.modules.impl.misc;

import moonshine.events.api.EventHandler;
import moonshine.events.impl.InputEvent;
import moonshine.events.impl.KeyEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.BindSetting;

public class CordsDropper extends ModuleStructure {
    public CordsDropper() {
        super("CordsDropper", ModuleCategory.MISC);
    }
    final BindSetting bind = new BindSetting("Кнопка", "Кнопка для отправки координат");

    @EventHandler
    public void onInput(KeyEvent e) {
        if (mc.player == null) return;
        if (e.key() == bind.getKey()) {
            if (mc.player != null) {
                String message = String.format("! %.0f %.0f !!!", mc.player.getX(), mc.player.getZ());
                mc.player.networkHandler.sendChatMessage(message);
            }
        }
    }
}

