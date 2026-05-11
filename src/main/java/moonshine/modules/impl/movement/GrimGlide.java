package moonshine.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.util.math.Vec3d;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.PacketEvent;
import moonshine.events.impl.TickEvent;
import moonshine.modules.impl.combat.Aura;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.util.Instance;
import moonshine.util.timer.StopWatch;
import net.minecraft.world.tick.Tick;

import java.util.Random;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GrimGlide extends ModuleStructure {
    StopWatch timer = new StopWatch();
    @NonFinal
    Vec3d targetPosition = null;
    @NonFinal
    Random random = new Random();
    @NonFinal double rotationAngle = 0.0;

    public GrimGlide() {
        super("GrimGlide", "Elytra Motion", ModuleCategory.MOVEMENT);
        this.settings();
    }
    private final StopWatch ticks = new StopWatch();

    @EventHandler
    private void onMotion(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        Vec3d pos = mc.player.getEntityPos();
        float yaw = mc.player.getYaw();
        double forward = 0.085;
        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        mc.player.setVelocity(dx * 1.25, mc.player.getVelocity().y - 0.01, dz * 1.25);

        if (ticks.finished(45)) {
            mc.player.setPos(
                    pos.getX() + dx,
                    pos.getY(),
                    pos.getZ() + dz
            );
            ticks.reset();
        }

        mc.player.setVelocity(dx * 1.25, mc.player.getVelocity().y + 0.015, dz * 1.25);
    }

    @Override
    public void activate() {
        super.activate();
        ticks.reset();
    }
}
