package moonshine.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.KeyEvent;
import moonshine.events.impl.WorldRenderEvent;
import moonshine.modules.impl.combat.Aura;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.BindSetting;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.util.ColorUtil;
import moonshine.util.Instance;
import moonshine.util.render.Render3D;
import moonshine.util.sounds.SoundManager;

import java.awt.Color;

public class ElytraTarget extends ModuleStructure {

    public static ElytraTarget getInstance() {
        return Instance.get(ElytraTarget.class);
    }

    public SliderSettings elytraFindRange = new SliderSettings("Дистанция наводки", "Дальность поиска цели во время полета на элитре")
            .setValue(32).range(6F, 64F);

    public SliderSettings elytraForward = new SliderSettings("Значение перегона", "заебался")
            .setValue(3).range(0F, 6F);

    final BindSetting forward = new BindSetting("Кнопка вкл/выкл перегона", "");

    public static boolean shouldElytraTarget = false;

    public ElytraTarget() {
        super("ElytraTarget", "Elytra Target", ModuleCategory.MOVEMENT);
        settings(elytraFindRange, elytraForward, forward);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void onEventKey(KeyEvent e) {
        if (e.isKeyDown(forward.getKey())) {
            shouldElytraTarget = !shouldElytraTarget;
            SoundManager.playSound(shouldElytraTarget ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, 1, 1.0f);
        }
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!isState() || !shouldElytraTarget || !mc.player.isGliding()) return;

        LivingEntity target = Aura.target;
        if (target == null || !target.isAlive() || !target.isGliding()) return;

        Box predictedBox = getPredictedBox(target, elytraForward.getValue());
        if (predictedBox == null) return;

        int lineColor = new Color(130, 190, 255, 210).getRGB();
        int fillColor = ColorUtil.multAlpha(lineColor, 0.18f);
        Render3D.drawBoxWithCross(predictedBox, lineColor, fillColor, 2.2f);
    }

    public static Box getPredictedBox(LivingEntity target, float leadTicks) {
        if (target == null) return null;

        Vec3d velocity = target.getVelocity();
        if (velocity.lengthSquared() < 0.001) return target.getBoundingBox();

        Vec3d offset = velocity.multiply(Math.max(0, leadTicks));
        return target.getBoundingBox().offset(offset);
    }
}
