package Glaxium.Minema.mixin;

import Glaxium.Minema.ui.UIMinemaSettingsOverlayPanel;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.ui.UIVideoSettingsOverlayPanel;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Minema" icon button into the top-right icon strip of BBS mod's
 * own "Video export settings" panel -- the same strip that already holds
 * the close (X) button and the presets (film) icon. Clicking it opens
 * UIMinemaSettingsOverlayPanel as its own overlay, same
 * UIOverlay.addOverlay(...) mechanism BBS mod itself uses to open THIS
 * panel in the first place (see UIValueMap#register(ValueVideoSettings)).
 *
 * `icons` is a public field on UIOverlayPanel, so no accessor mixin is
 * needed here -- extending UIOverlayPanel below just gives this mixin
 * class compile-time access to it (and to getContext()).
 */
@Mixin(UIVideoSettingsOverlayPanel.class)
public abstract class UIVideoSettingsOverlayPanelMixin extends UIOverlayPanel
{
    // Only exists to satisfy the compiler -- Mixin merges this class onto
    // the real UIVideoSettingsOverlayPanel instance, this is never actually
    // called.
    private UIVideoSettingsOverlayPanelMixin()
    {
        super(null);
    }

    @Inject(method = "<init>(Lmchorse/bbs_mod/settings/values/ui/ValueVideoSettings;)V", at = @At("TAIL"))
    private void bbsMinema$addIcon(ValueVideoSettings value, CallbackInfo ci)
    {
        UIIcon minemaIcon = new UIIcon(Icons.FRUSTUM, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIMinemaSettingsOverlayPanel());
        });

        minemaIcon.tooltip(IKey.raw("Minema"));

        this.icons.add(minemaIcon);
    }
}
