package Glaxium.Minema.ui;

import Glaxium.Minema.MinemaConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;

/**
 * Its own overlay panel, opened by clicking the "Minema" button that
 * UIVideoSettingsOverlayPanelMixin adds into BBS mod's own "Video export
 * settings" panel -- built the same way BBS mod builds that panel (a
 * UIScrollView of label + field rows dropped into UIOverlayPanel#content).
 */
public class UIMinemaSettingsOverlayPanel extends UIOverlayPanel
{
    private UIToggle captureDepth;
    private UITrackpad captureDepthDistance;

    public UIMinemaSettingsOverlayPanel()
    {
        super(IKey.raw("Minema settings"));

        MinemaConfig config = MinemaConfig.INSTANCE;

        this.captureDepth = new UIToggle(IKey.raw("Capture depth pass"), config.captureDepth, (toggle) ->
        {
            config.captureDepth = toggle.getValue();
            config.save();
        });
        this.captureDepth.tooltip(IKey.raw("Record a linearized depth pass alongside this recording"));

        this.captureDepthDistance = new UITrackpad((v) ->
        {
            config.captureDepthDistance = v.doubleValue();
            config.save();
        });
        this.captureDepthDistance.limit(1, 1024, true);
        this.captureDepthDistance.tooltip(IKey.raw("Far plane (in blocks) used to normalize the depth pass"));
        this.captureDepthDistance.setValue(config.captureDepthDistance);

        UIScrollView editor = UI.scrollView(5, 6,
                UI.label(IKey.raw("Depth pass")),
                this.captureDepth,
                UI.label(IKey.raw("Capture distance")).marginTop(6),
                this.captureDepthDistance
        );

        this.content.add(editor.full(this.content));
    }
}