package moonshine.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.InteractEntityEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFriendDamage extends ModuleStructure {

    public NoFriendDamage() {
        super("NoFriendDamage", "No Friend Damage", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onAttack(InteractEntityEvent e) {
        e.setCancelled(FriendUtils.isFriend(e.getEntity()));
    }
}

