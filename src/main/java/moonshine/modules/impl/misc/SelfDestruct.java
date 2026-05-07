package moonshine.modules.impl.misc;

import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.util.selfdestruct.SelfDestructManager;

public class SelfDestruct extends ModuleStructure {

    public SelfDestruct() {
        super("SelfDestruct", "Disable client until manual activation", ModuleCategory.MISC);
    }

    @Override
    public void activate() {
        SelfDestructManager.engage();
    }
}
