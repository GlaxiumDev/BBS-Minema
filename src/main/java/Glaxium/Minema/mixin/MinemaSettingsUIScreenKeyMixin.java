package Glaxium.Minema.mixin;

import Glaxium.Minema.ui.MinemaSettingsOpener;
import mchorse.bbs_mod.ui.framework.UIScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BBS mod routes every key press on its own UIScreen through
 * {@code menu.handleKey(...)} instead of leaving it to vanilla's normal
 * KeyBinding polling -- which means BBSMinema's own vanilla
 * {@code minemaSettingsKey}/Shift+F4 KeyBinding check in onClientTick never
 * fires while BBS's dashboard has focus. This catches the same combo at
 * BBS's own routing point instead, so Shift+F4 opens Minema Settings
 * whether or not BBS UI currently has keyboard focus.
 *
 * HEAD, not cancellable-and-consuming by default -- Shift+F4 isn't a combo
 * BBS mod itself binds to anything by default, so there's nothing to steal
 * focus away from; this only opens the panel, it doesn't block the event
 * from continuing on to BBS's own handling afterward.
 */
@Mixin(UIScreen.class)
public class MinemaSettingsUIScreenKeyMixin
{
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void bbsMinema$shiftF4OpensSettings(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info)
    {
        if (keyCode == GLFW.GLFW_KEY_F4 && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0)
        {
            MinemaSettingsOpener.openDirect();

            info.setReturnValue(true);
            info.cancel();
        }
    }
}
