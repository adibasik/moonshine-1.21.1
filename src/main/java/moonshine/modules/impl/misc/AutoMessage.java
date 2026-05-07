package moonshine.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.TickEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.modules.module.setting.implement.TextSetting;
import moonshine.util.timer.TimerUtil;

public class AutoMessage extends ModuleStructure {

    private final TextSetting message = new TextSetting("Сообщение", "Текст для отправки в чат")
            .setText("Привет всем!");

    private final SliderSettings delay = new SliderSettings("Задержка", "Интервал отправки (мс)")
            .setValue(5000F).range(1000F, 30000F);

    private final TimerUtil timer = TimerUtil.create();

    public AutoMessage() {
        super("AutoMessage", "Auto Message", ModuleCategory.MISC);
        settings(message, delay);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        timer.resetCounter();
        super.activate();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.player.networkHandler == null) return;

        if (timer.hasTimeElapsed((long) delay.getValue())) {
            sendChatMessage(message.getText());
            timer.resetCounter();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendChatMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        mc.player.networkHandler.sendChatMessage(text);
    }
}