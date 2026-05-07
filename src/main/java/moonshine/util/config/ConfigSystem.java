package moonshine.util.config;

import moonshine.util.config.impl.ConfigFileHandler;
import moonshine.util.config.impl.ConfigPath;
import moonshine.util.config.impl.ConfigSerializer;
import moonshine.util.config.impl.autosaver.ConfigAutoSaver;
import moonshine.util.config.impl.consolelogger.Logger;
import moonshine.util.selfdestruct.SelfDestructManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  © 2026 Copyright Moonshine Client 2.0
 *        All Rights Reserved ®
 */

public class ConfigSystem {

    private static ConfigSystem instance;

    private final ConfigSerializer serializer;
    private final ConfigFileHandler fileHandler;
    private final ConfigAutoSaver autoSaver;
    private final AtomicBoolean initialized;
    private final AtomicBoolean saving;

    public ConfigSystem() {
        instance = this;
        this.serializer = new ConfigSerializer();
        this.fileHandler = new ConfigFileHandler();
        this.autoSaver = new ConfigAutoSaver(this::save);
        this.initialized = new AtomicBoolean(false);
        this.saving = new AtomicBoolean(false);
    }

    public static ConfigSystem getInstance() {
        return instance;
    }

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            ConfigPath.init();
            if (SelfDestructManager.isLocked()) {
                registerShutdownHook();
                Logger.info("AutoConfiguration: disabled while SelfDestruct is active.");
                return;
            }

            fileHandler.createDirectories();
            load();
            autoSaver.start();
            registerShutdownHook();
            Logger.success("AutoConfiguration: System initialized!");
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("AutoConfiguration: Shutdown detected, saving...");
            shutdown();
        }, "Moonshine-ConfigShutdown"));
    }

    public void save() {
        if (!initialized.get()) {
            return;
        }
        if (SelfDestructManager.blocksSaving()) {
            Logger.info("AutoConfiguration: Save skipped while SelfDestruct is active.");
            return;
        }
        if (!saving.compareAndSet(false, true)) {
            return;
        }
        try {
            String data = serializer.serialize();
            boolean success = fileHandler.write(data);
            if (success) {
                Logger.success("AutoConfiguration: autoconfig.json saved successfully!");
            } else {
                Logger.error("AutoConfiguration: autoconfig.json save failed!");
            }
        } catch (Exception e) {
            Logger.error("AutoConfiguration: Save error! " + e.getMessage());
        } finally {
            saving.set(false);
        }
    }

    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::save);
    }

    public void load() {
        if (!fileHandler.exists()) {
            Logger.info("AutoConfiguration: No config found, creating new...");
            save();
            return;
        }
        try {
            String data = fileHandler.read();
            if (data != null && !data.isEmpty()) {
                serializer.deserialize(data);
                Logger.success("AutoConfiguration: autoconfig.json loaded successfully!");
            }
        } catch (Exception e) {
            Logger.error("AutoConfiguration: Load error! " + e.getMessage());
        }
    }

    public void shutdown() {
        if (!initialized.get()) {
            return;
        }
        autoSaver.shutdown();
        save();
        Logger.success("AutoConfiguration: Shutdown complete!");
    }

    public void resumeAutoSave() {
        if (initialized.get() && !SelfDestructManager.isLocked() && !autoSaver.isRunning()) {
            autoSaver.start();
        }
    }

    public void reload() {
        if (!SelfDestructManager.isLocked()) {
            fileHandler.createDirectories();
        }
        load();
        Logger.success("AutoConfiguration: Config reloaded!");
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isSaving() {
        return saving.get();
    }

    public ConfigAutoSaver getAutoSaver() {
        return autoSaver;
    }
}
