package moonshine.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.KeyEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.modules.module.setting.implement.BindSetting;
import moonshine.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClickFriend extends ModuleStructure {

    BindSetting friendBind = new BindSetting("Добавить друга", "Добавить/удалить друга");

    public ClickFriend() {
        super("ClickFriend", "Click Friend", ModuleCategory.MISC);
        settings(friendBind);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(friendBind.getKey()) && mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof PlayerEntity player) {
            if (FriendUtils.isFriend(player)) FriendUtils.removeFriend(player);
            else FriendUtils.addFriend(player);
        }
    }
}