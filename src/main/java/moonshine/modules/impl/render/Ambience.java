package moonshine.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.PacketEvent;
import moonshine.events.impl.TickEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.util.Instance;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class Ambience extends ModuleStructure {

    public static Ambience getInstance() {
        return Instance.get(Ambience.class);
    }

    public SliderSettings time = new SliderSettings("Время", "Время суток (0-24000)")
            .range(0, 24000)
            .setValue(1000);

    private double animatedTime = 1000;

    public Ambience() {
        super("Ambience", "Изменяет время мира", ModuleCategory.RENDER);
        settings(time);
    }

    @Override
    public void activate() {
        animatedTime = time.getValue();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        double targetTime = time.getValue();
        double speed = 150 / 1000.0;
        double diff = targetTime - animatedTime;
        animatedTime += diff * speed;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
        }
    }

    public long getCustomTime() {
        return (long) animatedTime;
    }
}