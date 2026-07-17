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
    private UIToggle syncEngine;

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

        this.syncEngine = new UIToggle(IKey.raw("Sync engine to capture"), config.syncEngine, (toggle) ->
        {
            config.syncEngine = toggle.getValue();
            config.save();
        });
        this.syncEngine.tooltip(IKey.raw(
                "Pause the world simulation each tick until the frame it produced has "
                        + "actually been captured, instead of letting it run freely alongside "
                        + "capture. Prevents things like fast-moving TNT/entities from drifting "
                        + "out of sync with the recorded video. Singleplayer only; adds a small "
                        + "amount of real-world recording time."
        ));

        UIScrollView editor = UI.scrollView(5, 6,
                UI.label(IKey.raw("Depth pass")),
                this.captureDepth,
                UI.label(IKey.raw("Capture distance")).marginTop(6),
                this.captureDepthDistance,
                UI.label(IKey.raw("Tick synchronization")).marginTop(6),
                this.syncEngine
        );

        this.content.add(editor.full(this.content));
    }
}