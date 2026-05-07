package moonshine.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import moonshine.events.api.events.Event;

@Getter
@AllArgsConstructor
public class AttackEvent implements Event {
    private final Entity target;
}