package moonshine.modules.impl.misc;

import moonshine.events.api.EventHandler;
import moonshine.events.impl.TickEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.BooleanSetting;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.util.timer.StopWatch;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoSnow extends ModuleStructure {

    // Настройки
    public SliderSettings range = new SliderSettings("Радиус", "Дальность поиска").range(1.0f, 8.0f).setValue(3.0f);
    public BooleanSetting golemOnly = new BooleanSetting("Только у голема", "Работать только если рядом снеговик").setValue(true);
    public SliderSettings pps = new SliderSettings("Скорость", "Пакетов в секунду").range(5.0f, 60.0f).setValue(20.0f);
    public BooleanSetting swing = new BooleanSetting("Махи", "Визуальные взмахи руки").setValue(true);

    // Состояние
    StopWatch tPPS = new StopWatch();
    int sent = 0;

    public AutoSnow() {
        super("AutoSnow", "Авто-уборка снега у снеговиков", ModuleCategory.PLAYER);
        settings(range, golemOnly, pps, swing);
    }

    @Override
    public void activate() {
        sent = 0;
        tPPS.reset();
        super.activate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Если включено требование голема, проверяем его наличие
        if (golemOnly.isValue() && !isSnowGolemNearby()) return;

        runNuker();
    }

    private boolean isSnowGolemNearby() {
        double r = range.getValue() + 2.0; // небольшой запас, т.к. голем может отойти
        for (Entity e : mc.world.getEntities()) {
            if (e.getType() == EntityType.SNOW_GOLEM && mc.player.squaredDistanceTo(e) <= r * r) {
                return true;
            }
        }
        return false;
    }

    void runNuker() {
        // Сброс счётчика пакетов каждую секунду
        if (tPPS.finished(1000)) {
            sent = 0;
            tPPS.reset();
        }

        int max = (int) pps.getValue();
        int ops = Math.max(1, max / 20); // операций за тик
        boolean acted = false;

        for (int i = 0; i < ops; i++) {
            if (sent >= max) break;

            BlockPos target = findSnow();
            if (target == null) break; // снег не найден

            // Отправляем пакеты ломания
            packet(target);
            // Синхронизируем клиент (как в Fast-режиме оригинала)
            mc.world.setBlockState(target, Blocks.AIR.getDefaultState());
            sent++;
            acted = true;
        }

        if (acted && swing.isValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    void packet(BlockPos p) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, p, Direction.UP));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, p, Direction.UP));
    }

    BlockPos findSnow() {
        if (mc.player == null || mc.world == null) return null;
        List<BlockPos> list = new ArrayList<>();
        int r = (int) range.getValue();
        BlockPos pp = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (isSnow(p)) {
                        list.add(p);
                    }
                }
            }
        }

        // Возвращаем ближайший снежный блок
        return list.stream()
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(Vec3d.ofCenter(p))))
                .orElse(null);
    }

    boolean isSnow(BlockPos p) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(p);
        // Снеговики оставляют именно SNOW (слой), но на всякий случай проверяем и SNOW_BLOCK
        return state.isOf(Blocks.SNOW) || state.isOf(Blocks.SNOW_BLOCK);
    }
}