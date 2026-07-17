package Glaxium.Minema;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.VideoRecorder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;

/**
 * Piggybacks on BBS mod's own VideoRecorder for start/stop timing, gated
 * behind an opt-in toggle (default OFF) matching how Minema 1.12.2 treated
 * depth capture as a separate setting. The toggle itself lives in two
 * places wired to the same MinemaConfig.INSTANCE: a row injected directly
 * into BBS mod's own "Video export settings" panel (see
 * UIVideoSettingsOverlayPanelMixin), and this class's own keybind as a
 * quick way to flip it without opening that panel.
 */
public class BBSMinema implements ClientModInitializer
{
    private final MinemaRecorder depthRecorder = new MinemaRecorder();
    private final GameAudioRecorder audioRecorder = new GameAudioRecorder();
    // Shared with UIVideoSettingsOverlayPanelMixin, which has no reference
    // to this class -- both read/write the same static MinemaConfig.INSTANCE.
    private final MinemaConfig config = MinemaConfig.INSTANCE;
    private boolean wasRecording = false;
    private boolean wasSyncing = false;
    private boolean wasRecordingAudio = false;
    private boolean wasRawCapturing = false;

    /** Set the moment we open the WAV file, used to find BBS mod's own output video for muxing afterwards -- see GameAudioRecorder#muxIntoVideo. */
    private long audioRecordingStartedAt;

    private KeyBinding toggleKey;
    private KeyBinding toggleSyncKey;
    private KeyBinding toggleAudioKey;
    private KeyBinding toggleAudioMuxKey;
    private KeyBinding toggleRawCaptureKey;

    @Override
    public void onInitializeClient()
    {
        this.config.load();

        this.toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bbs_minema.toggle_depth",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default, set it in Controls
                "key.categories.bbs_minema"
        ));

        this.toggleSyncKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bbs_minema.toggle_sync",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default, set it in Controls
                "key.categories.bbs_minema"
        ));

        this.toggleAudioKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bbs_minema.toggle_audio",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default, set it in Controls
                "key.categories.bbs_minema"
        ));

        this.toggleAudioMuxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bbs_minema.toggle_audio_mux",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default, set it in Controls
                "key.categories.bbs_minema"
        ));

        this.toggleRawCaptureKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bbs_minema.toggle_raw_capture",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default -- bind this to F4 yourself in Controls to match Minema 1.12.2's default
                "key.categories.bbs_minema"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        WorldRenderEvents.LAST.register(this::onWorldRenderLast);
    }

    private void onClientTick(MinecraftClient client)
    {
        if (this.toggleKey.wasPressed())
        {
            this.config.toggleCaptureDepth();

            if (client.player != null)
            {
                String state = this.config.captureDepth ? "ON" : "OFF";

                client.player.sendMessage(Text.literal("BBS Minema depth pass: " + state), true);
            }
        }

        if (this.toggleSyncKey.wasPressed())
        {
            this.config.toggleSyncEngine();

            if (client.player != null)
            {
                String state = this.config.syncEngine ? "ON" : "OFF";

                client.player.sendMessage(Text.literal("BBS Minema tick sync: " + state), true);
            }
        }

        if (this.toggleAudioKey.wasPressed())
        {
            this.config.toggleRecordGameAudio();

            if (client.player != null)
            {
                String state = this.config.recordGameAudio ? "ON" : "OFF";

                client.player.sendMessage(Text.literal("BBS Minema game audio: " + state), true);
            }
        }

        if (this.toggleAudioMuxKey.wasPressed())
        {
            this.config.toggleGenerateWavFile();

            if (client.player != null)
            {
                String state = this.config.generateWavFile ? "ON" : "OFF";

                client.player.sendMessage(Text.literal("BBS Minema generate .wav file: " + state), true);
            }
        }

        if (this.toggleRawCaptureKey.wasPressed())
        {
            this.config.toggleRawCaptureMode();

            if (client.player != null)
            {
                String state = this.config.rawCaptureMode ? "ON" : "OFF";

                client.player.sendMessage(Text.literal("BBS Minema raw (full screen) capture: " + state), true);
            }
        }

        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();

        // Either toggle alone should still capture -- "Generate .wav" by
        // itself needs audio captured just as much as "Record in-game
        // audio" does, it just skips the mux step below. Independent of
        // BBSRendering.canRender (unlike the depth pass) -- this only needs
        // VideoRecorder's own isRecording() flag, so it reacts identically
        // whether recording was started via F4 or the film editor, and
        // starts as early as possible to give the loopback device time to
        // come up before the first captured frame.
        boolean wantsAudio = this.config.recordGameAudio || this.config.generateWavFile;
        boolean recordingAudioNow = wantsAudio && videoRecorder.isRecording();

        if (recordingAudioNow && !this.wasRecordingAudio)
        {
            this.audioRecordingStartedAt = System.currentTimeMillis();

            this.audioRecorder.startRecording(
                    BBSRendering.getVideoFolder().toPath(),
                    StringUtils.createTimestampFilename(),
                    (int) Math.max(1, BBSRendering.getVideoFrameRate())
            );
        }
        else if (!recordingAudioNow && this.wasRecordingAudio)
        {
            File wav = this.audioRecorder.stopRecording();

            if (wav != null && this.config.recordGameAudio)
            {
                long startedAt = this.audioRecordingStartedAt;
                boolean keepWav = this.config.generateWavFile;

                // ffmpeg mux can take a while on longer recordings -- run it
                // off the client tick thread so it doesn't freeze the game.
                // muxIntoVideo/findColorVideoOutput/File I/O below don't
                // touch anything client-thread-only.
                Thread muxThread = new Thread(() -> this.audioRecorder.muxIntoVideo(
                        wav,
                        BBSRendering.getVideoFolder().toPath(),
                        startedAt,
                        keepWav
                ), "bbs-minema-audio-mux");

                muxThread.setDaemon(true);
                muxThread.start();
            }

            // "Generate .wav" was the only thing on (recordGameAudio is
            // off) -- the WAV GameAudioRecorder already wrote is the
            // finished output, nothing further to do. If NEITHER toggle
            // was on, wav is null and this whole branch is a no-op.
        }

        this.wasRecordingAudio = recordingAudioNow;
        boolean syncingNow = this.config.syncEngine
                && videoRecorder.isRecording()
                && client.isIntegratedServerRunning();

        if (syncingNow && !this.wasSyncing)
        {
            // Rising edge only -- re-arms SyncModule's bookkeeping against
            // whatever VideoRecorder#serverTicks currently is, so turning
            // sync on mid-recording doesn't try to catch up on ticks that
            // already ran unsynced.
            SyncModule.reset();
        }

        SyncModule.enabled = syncingNow;
        this.wasSyncing = syncingNow;
    }

    private void onWorldRenderLast(WorldRenderContext context)
    {
        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();
        boolean recordingNow = videoRecorder.isRecording()
                && BBSRendering.canRender
                && this.config.captureDepth;

        if (recordingNow && !this.wasRecording)
        {
            // BBS mod's video is already rolling by the time canRender flips
            // true on the same frame, so widths/heights are safe to read here.
            this.depthRecorder.setPlanes(0.05F, (float) this.config.captureDepthDistance);
            this.depthRecorder.startRecording(
                    BBSRendering.getVideoWidth(),
                    BBSRendering.getVideoHeight()
            );
        }
        else if (!recordingNow && this.wasRecording)
        {
            this.depthRecorder.stopRecording();
        }

        if (recordingNow)
        {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            int depthTextureId = framebuffer.getDepthAttachment();

            this.depthRecorder.recordFrame(depthTextureId);
        }

        this.wasRecording = recordingNow;

        // Same canRender gate as the depth pass -- one audio frame per
        // color frame BBS mod actually captures, not per render call.
        // Either audio toggle being on should keep frames flowing in --
        // see the wantsAudio comment in onClientTick for why.
        boolean capturingAudioNow = videoRecorder.isRecording()
                && BBSRendering.canRender
                && (this.config.recordGameAudio || this.config.generateWavFile);

        if (capturingAudioNow)
        {
            this.audioRecorder.captureFrame();
        }

        // Raw (full screen, GUI included) capture -- own start/stop edge,
        // independent of the depth pass toggle. Actual per-frame capture
        // happens later in the frame from MinecraftClientRawCaptureMixin
        // (TAIL of MinecraftClient#render), after GUI/screens have drawn --
        // this only handles start/stop, same canRender-gated rising/falling
        // edge pattern as everything else above.
        boolean rawCapturingNow = videoRecorder.isRecording()
                && BBSRendering.canRender
                && this.config.rawCaptureMode;

        if (rawCapturingNow && !this.wasRawCapturing)
        {
            RawCaptureModule.INSTANCE.start();
        }
        else if (!rawCapturingNow && this.wasRawCapturing)
        {
            RawCaptureModule.INSTANCE.stop();
        }

        this.wasRawCapturing = rawCapturingNow;
    }
}