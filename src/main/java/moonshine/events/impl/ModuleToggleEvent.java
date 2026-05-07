package moonshine.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moonshine.events.api.events.Event;
import moonshine.modules.module.ModuleStructure;

@Getter
@AllArgsConstructor
public class ModuleToggleEvent implements Event {
    private final ModuleStructure module;
    private final boolean enabled;
}