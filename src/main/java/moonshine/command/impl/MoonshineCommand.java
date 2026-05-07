package moonshine.command.impl;

import moonshine.command.Command;
import moonshine.command.CommandManager;
import moonshine.util.config.impl.prefix.PrefixConfig;
import moonshine.util.selfdestruct.SelfDestructManager;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MoonshineCommand extends Command {

    public MoonshineCommand() {
        super("moonshine", "Client activation command", "client");
    }

    @Override
    public void execute(String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            logDirect("SelfDestruct lock: " + (SelfDestructManager.isLocked() ? "active" : "inactive"));
            return;
        }

        if (args[0].equalsIgnoreCase("activate") || args[0].equalsIgnoreCase("unlock")) {
            if (args.length < 2) {
                logDirect("Usage: " + label + " activate <code>", Formatting.RED);
                return;
            }

            if (SelfDestructManager.activate(args[1])) {
                CommandManager.getInstance().setPrefix(PrefixConfig.getInstance().getPrefix());
                logDirect("Moonshine activated. Autoconfig was reloaded.", Formatting.GREEN);
            } else {
                logDirect("Wrong activation code.", Formatting.RED);
            }
            return;
        }

        logDirect("Usage: " + label + " status | activate <code>", Formatting.RED);
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("status", "activate", "unlock")
                    .filter(s -> s.startsWith(args[0].toLowerCase()));
        }
        return Stream.empty();
    }

    @Override
    public boolean hiddenFromHelp() {
        return true;
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Client activation command.",
                "Usage:",
                "> moonshine status",
                "> moonshine activate moonshine"
        );
    }
}
