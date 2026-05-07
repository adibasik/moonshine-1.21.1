package moonshine.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moonshine.events.api.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
