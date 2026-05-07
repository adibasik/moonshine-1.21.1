package moonshine.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import moonshine.screens.menu.MainMenuScreen;
import moonshine.util.selfdestruct.SelfDestructManager;

@Mixin(AccessibilityOnboardingScreen.class)
public class AccessibilityOnboardingScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (SelfDestructManager.isLocked()) {
            return;
        }
        ci.cancel();
        MinecraftClient.getInstance().setScreen(new MainMenuScreen());
    }
}
