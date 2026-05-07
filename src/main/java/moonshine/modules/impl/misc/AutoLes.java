package moonshine.modules.impl.misc;

import moonshine.events.api.EventHandler;
import moonshine.events.impl.TickEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.BooleanSetting;
import moonshine.modules.module.setting.implement.SelectSetting;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.modules.module.setting.implement.TextSetting;
import moonshine.util.timer.StopWatch;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoLes extends ModuleStructure {

    public SelectSetting mode = new SelectSetting("Режим", "Тип добычи").value("Default", "Fast").selected("Fast");
    public BooleanSetting inf = new BooleanSetting("Чудеса", "Для баг. дерева").setValue(false);
    public SliderSettings pps = new SliderSettings("Скорость", "Пакеты в секунду рекомендую 35").range(1.0f, 100.0f).setValue(60.0f);
    public SliderSettings range = new SliderSettings("Радиус", "Дальность").range(1.0f, 6.0f).setValue(4.5f);
    public BooleanSetting swing = new BooleanSetting("Махи", "Визуальные взмахи хз зач").setValue(true);

    public BooleanSetting sell = new BooleanSetting("Авто-прод.", "SellWood").setValue(true);
    public BooleanSetting pay = new BooleanSetting("Авто-пей", "Кидать деньги").setValue(false);

    public TextSetting payName = new TextSetting("Ник", "Кому").setText("name").visible(pay::isValue);
    public SliderSettings paySum = new SliderSettings("Сумма", "Сколько").range(500, 25000).setValue(1000).visible(pay::isValue);
    public SliderSettings delay = new SliderSettings("Задержка", "Сек").range(1, 60).setValue(20).visible(pay::isValue);

    BlockPos cur;
    StopWatch tSell = new StopWatch();
    StopWatch tPay = new StopWatch();
    StopWatch tBreak = new StopWatch();
    StopWatch tPPS = new StopWatch();
    int sent = 0;

    public AutoLes() {
        super("AutoLes", "Ultimate Fast Nuker", ModuleCategory.PLAYER);
        settings(mode, inf, pps, range, swing, sell, pay, payName, paySum, delay);
    }

    @Override
    public void activate() {
        cur = null;
        sent = 0;
        tPPS.reset();
        super.activate();
    }

    @Override
    public void deactivate() {
        cur = null;
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        runSell();
        runPay();
        runNuker();
    }

    void runSell() {
        if (sell.isValue() && tSell.finished((long) (delay.getValue() * 500))) {
            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendChatCommand("sellwood");
            }
            tSell.reset();
        }
    }

    void runPay() {
        if (pay.isValue() && tPay.finished((long) ((delay.getValue() * 500) + 200))) {
            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendChatCommand("pay " + payName.getText() + " " + (int)paySum.getValue());
            }
            tPay.reset();
        }
    }

    void runNuker() {
        if (tPPS.finished(1000)) {
            sent = 0;
            tPPS.reset();
        }

        if (mode.isSelected("Default")) {
            cur = find();
            if (cur != null && tBreak.finished(150)) {
                if (mc.interactionManager != null) {
                    mc.interactionManager.attackBlock(cur, Direction.UP);
                    if (swing.isValue()) mc.player.swingHand(Hand.MAIN_HAND);
                }
                tBreak.reset();
            }
        }
        else if (mode.isSelected("Fast")) {
            int max = (int) pps.getValue();
            int ops = Math.max(1, max / 20);
            boolean act = false;

            for (int i = 0; i < ops; i++) {
                if (sent >= max) break;

                if (inf.isValue()) {
                    if (cur == null || !check(cur)) {
                        cur = find();
                    }
                    if (cur != null) {
                        packet(cur);
                        sent++;
                        act = true;
                    }
                } else {
                    BlockPos t = find();
                    if (t != null) {
                        packet(t);
                        mc.world.setBlockState(t, Blocks.AIR.getDefaultState());
                        sent++;
                        act = true;
                    } else {
                        break;
                    }
                }
            }

            if (act && swing.isValue()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    void packet(BlockPos p) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, p, Direction.UP));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, p, Direction.UP));
    }

    BlockPos find() {
        if (mc.player == null || mc.world == null) return null;
        List<BlockPos> list = new ArrayList<>();
        int r = (int) range.getValue();
        BlockPos pp = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (valid(p) && check(p)) {
                        list.add(p);
                    }
                }
            }
        }
        return list.stream()
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(Vec3d.ofCenter(p))))
                .orElse(null);
    }

    boolean check(BlockPos p) {
        if (mc.player == null) return false;
        double r = range.getValue();
        return mc.player.squaredDistanceTo(Vec3d.ofCenter(p)) <= (r * r);
    }

    boolean valid(BlockPos p) {
        if (mc.world == null) return false;
        BlockState s = mc.world.getBlockState(p);
        String n = s.getBlock().getTranslationKey().toLowerCase();
        return n.contains("log") || n.contains("wood") || n.contains("stem") || s.isIn(BlockTags.LOGS);
    }
}
