package moonshine.modules.impl.misc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.TickEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.SliderSettings;
import moonshine.util.inventory.InventoryUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RwJoiner extends ModuleStructure {

    final SliderSettings slot1 = new SliderSettings("RW1 Слот", "").range(0, 100).setValue(21);
    final SliderSettings delay1 = new SliderSettings("RW1 Delay", "").range(0, 2000).setValue(400);

    final SliderSettings slot2 = new SliderSettings("RW2 Слот", "").range(0, 100).setValue(0);
    final SliderSettings delay2 = new SliderSettings("RW2 Delay", "").range(0, 2000).setValue(400);

    final SliderSettings slot3 = new SliderSettings("RW3 Слот", "").range(0, 100).setValue(0);
    final SliderSettings delay3 = new SliderSettings("RW3 Delay", "").range(0, 2000).setValue(400);

    final SliderSettings slot4 = new SliderSettings("RW4 Слот", "").range(0, 100).setValue(0);
    final SliderSettings delay4 = new SliderSettings("RW4 Delay", "").range(0, 2000).setValue(400);

    final SliderSettings slot5 = new SliderSettings("RW5 Слот", "").range(0, 100).setValue(0);
    final SliderSettings delay5 = new SliderSettings("RW5 Delay", "").range(0, 2000).setValue(400);

    final SliderSettings globalDelay = new SliderSettings("Закрытие Delay", "").range(0, 2000).setValue(500);

    boolean isProcessing = false;
    boolean menuOpened = false;

    int step = 0;
    long lastTime = 0;

    int originalSlot = -1;

    public RwJoiner() {
        super("RwJoiner", "Auto RW Joiner", ModuleCategory.MISC);
        settings(slot1, delay1, slot2, delay2, slot3, delay3, slot4, delay4, slot5, delay5, globalDelay);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        // ===== Проверка открытия меню =====
        if (mc.currentScreen instanceof HandledScreen<?>) {
            menuOpened = true;
        }

        if (!isProcessing) {
            int compassSlot = findCompass();

            if (compassSlot != -1) {
                isProcessing = true;
                step = 0;
                menuOpened = false;

                originalSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(compassSlot);

                // Открываем меню
                mc.player.networkHandler.sendChatCommand("menu");

                lastTime = System.currentTimeMillis();
            }
        } else {
            process();
        }
    }

    private void process() {
        long now = System.currentTimeMillis();

        int[] slots = {
                (int) slot1.getValue(),
                (int) slot2.getValue(),
                (int) slot3.getValue(),
                (int) slot4.getValue(),
                (int) slot5.getValue()
        };

        int[] delays = {
                (int) delay1.getValue(),
                (int) delay2.getValue(),
                (int) delay3.getValue(),
                (int) delay4.getValue(),
                (int) delay5.getValue()
        };

        switch (step) {

            // ⏳ Ждём открытия меню
            case 0 -> {
                if (menuOpened && now - lastTime >= 300) {
                    step++;
                    lastTime = now;
                }

                // если меню не открылось за 2 секунды — рестарт
                if (now - lastTime > 2000) {
                    reset();
                }
            }

            // 🎯 Клики по слотам
            case 1, 2, 3, 4, 5 -> {
                int index = step - 1;

                if (now - lastTime >= delays[index]) {
                    int slot = slots[index];

                    if (slot > 0 && mc.player.currentScreenHandler != null) {
                        int max = mc.player.currentScreenHandler.slots.size();

                        if (slot < max) {
                            InventoryUtils.click(slot, 0, SlotActionType.PICKUP);
                        }
                    }

                    lastTime = now;
                    step++;
                }
            }

            // ❌ Закрытие
            case 6 -> {
                if (now - lastTime >= (long) globalDelay.getValue()) {
                    mc.player.closeHandledScreen();

                    if (originalSlot != -1) {
                        mc.player.getInventory().setSelectedSlot(originalSlot);
                    }

                    lastTime = now;
                    step++;
                }
            }

            // 🔄 Reset
            case 7 -> {
                if (now - lastTime >= 150) {
                    reset();
                }
            }
        }
    }

    private int findCompass() {
        Item target = Items.COMPASS;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == target) {
                return i;
            }
        }
        return -1;
    }

    private void reset() {
        isProcessing = false;
        menuOpened = false;
        step = 0;
        originalSlot = -1;
    }

    @Override
    public void deactivate() {
        reset();
        super.deactivate();
    }
}
