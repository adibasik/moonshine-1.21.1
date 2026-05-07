package moonshine.util.selfdestruct;

import moonshine.Initialization;
import moonshine.modules.module.ModuleRepository;
import moonshine.modules.module.ModuleStructure;
import moonshine.util.config.ConfigSystem;
import moonshine.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SelfDestructManager {

    private static final Path ROOT_DIR = Paths.get("").toAbsolutePath().normalize();
    private static final Path LOCK_FILE = ROOT_DIR.resolve("config").resolve("moonshine.disabled").normalize();
    private static final Path LEGACY_LOCK_FILE = ROOT_DIR.resolve("Moonshine").resolve(".selfdestruct").normalize();
    private static final AtomicBoolean locked = new AtomicBoolean(false);

    private SelfDestructManager() {
    }

    public static void init() {
        if (Files.exists(LEGACY_LOCK_FILE) && !Files.exists(LOCK_FILE)) {
            writeLock();
        }

        locked.set(Files.exists(LOCK_FILE));
        if (locked.get()) {
            Logger.info("SelfDestruct: client is locked until manual activation.");
        }
    }

    public static boolean isLocked() {
        return locked.get();
    }

    public static boolean blocksSaving() {
        return locked.get();
    }

    public static boolean blocksModuleActivation(ModuleStructure module) {
        return locked.get() && !isSelfDestructModule(module);
    }

    public static void engage() {
        if (!locked.getAndSet(true)) {
            writeLock();
        }

        ConfigSystem configSystem = ConfigSystem.getInstance();
        if (configSystem != null) {
            configSystem.getAutoSaver().stop();
        }

        ModuleRepository repository = getRepository();
        if (repository != null) {
            for (ModuleStructure module : repository.allModules()) {
                if (module.isState()) {
                    module.setState(false);
                }
            }
        }

        if (ModuleStructure.mc != null && ModuleStructure.mc.currentScreen != null) {
            ModuleStructure.mc.setScreen(null);
        }

        writeLock();
        Logger.info("SelfDestruct: all modules disabled, autosave blocked.");
    }

    public static boolean activate(String code) {
        if (!"moonshine".equalsIgnoreCase(code)) {
            return false;
        }

        locked.set(false);
        deleteLock();

        Initialization initialization = Initialization.getInstance();
        if (initialization != null && initialization.getManager() != null) {
            initialization.getManager().loadRuntimeConfigs();
        }

        ModuleRepository repository = getRepository();
        if (repository != null) {
            for (ModuleStructure module : repository.hiddenModules()) {
                module.setState(true);
            }
        }

        ConfigSystem configSystem = ConfigSystem.getInstance();
        if (configSystem != null) {
            configSystem.reload();
            configSystem.resumeAutoSave();
        }

        Logger.success("SelfDestruct: client activated.");
        return true;
    }

    private static boolean isSelfDestructModule(ModuleStructure module) {
        return module != null && module.getClass().getName().equals("moonshine.modules.impl.misc.SelfDestruct");
    }

    private static ModuleRepository getRepository() {
        Initialization initialization = Initialization.getInstance();
        if (initialization == null || initialization.getManager() == null) {
            return null;
        }
        return initialization.getManager().getModuleRepository();
    }

    private static void writeLock() {
        try {
            Files.createDirectories(LOCK_FILE.getParent());
            Files.writeString(LOCK_FILE, "locked");
        } catch (IOException e) {
            Logger.error("SelfDestruct: failed to write lock file: " + e.getMessage());
        }
    }

    private static void deleteLock() {
        try {
            Files.deleteIfExists(LOCK_FILE);
        } catch (IOException e) {
            Logger.error("SelfDestruct: failed to delete lock file: " + e.getMessage());
        }
    }
}
