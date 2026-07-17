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
    private UIToggle recordGameAudio;
    private UIToggle generateWavFile;
    private UIToggle captureDepth;
    private UITrackpad captureDepthDistance;
    private UIToggle syncEngine;

    public UIMinemaSettingsOverlayPanel()
    {
        super(IKey.raw("Minema settings"));

        MinemaConfig config = MinemaConfig.INSTANCE;

        this.recordGameAudio = new UIToggle(IKey.raw("Record in-game audio"), config.recordGameAudio, (toggle) ->
        {
            config.recordGameAudio = toggle.getValue();
            config.save();
        });
        this.recordGameAudio.tooltip(IKey.raw("Adds the game's real audio to the video. Mutes your speakers while recording."));

        this.generateWavFile = new UIToggle(IKey.raw("Generate .wav Audio file"), config.generateWavFile, (toggle) ->
        {
            config.generateWavFile = toggle.getValue();
            config.save();
        });
        this.generateWavFile.tooltip(IKey.raw("Also saves the audio as a separate .wav file."));

        this.captureDepth = new UIToggle(IKey.raw("Capture depth pass"), config.captureDepth, (toggle) ->
        {
            config.captureDepth = toggle.getValue();
            config.save();
        });
        this.captureDepth.tooltip(IKey.raw("Records a depth pass video alongside this recording."));

        this.captureDepthDistance = new UITrackpad((v) ->
        {
            config.captureDepthDistance = v.doubleValue();
            config.save();
        });
        this.captureDepthDistance.limit(1, 1024, true);
        this.captureDepthDistance.tooltip(IKey.raw("Max distance (blocks) the depth pass covers."));
        this.captureDepthDistance.setValue(config.captureDepthDistance);

        this.syncEngine = new UIToggle(IKey.raw("Sync engine to capture"), config.syncEngine, (toggle) ->
        {
            config.syncEngine = toggle.getValue();
            config.save();
        });
        this.syncEngine.tooltip(IKey.raw("Keeps fast-moving things (like TNT) from desyncing with the video. Singleplayer only."));

        UIScrollView editor = UI.scrollView(5, 6,
                UI.label(IKey.raw("In-game audio")),
                this.recordGameAudio,
                this.generateWavFile,
                UI.label(IKey.raw("Depth pass")).marginTop(6),
                this.captureDepth,
                UI.label(IKey.raw("Capture distance")).marginTop(6),
                this.captureDepthDistance,
                UI.label(IKey.raw("Tick synchronization")).marginTop(6),
                this.syncEngine
        );

        this.content.add(editor.full(this.content));
    }
}