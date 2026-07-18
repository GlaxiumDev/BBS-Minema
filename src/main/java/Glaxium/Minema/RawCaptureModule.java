package Glaxium.Minema;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Drives the "raw" (full screen, GUI included) capture -- see
 * RawCaptureRecorder for the actual pixel-pushing.
 *
 * <p>No custom/target resolution here -- this always records at whatever
 * the game window's actual current framebuffer size is, windowed or
 * fullscreen, at the moment F4 is pressed. The window is never resized.
 * BBS mod's configured "Frame Resolution" setting is not consulted at all
 * for F4 capture (UIFilmPanel's own export still uses it independently,
 * for its own preview/export resolution -- untouched by this).
 */
public class RawCaptureModule
{
    public static final RawCaptureModule INSTANCE = new RawCaptureModule();

    private final RawCaptureRecorder recorder = new RawCaptureRecorder();

    /**
     * Mirrors what {@code VideoRecorder#serverTicks} is for BBS mod's own
     * recorder -- a running count of how many fixed-timestep world ticks
     * this capture has asked for so far. Advanced from
     * MinemaRenderTickCounterMixin (once per frame, by however many whole
     * ticks that frame's fixed timestep covered). SyncModule reads this
     * instead of VideoRecorder#serverTicks while BBS-Minema's own F4
     * capture is the one actually running.
     */
    private volatile int serverTicks;

    public boolean isRecording()
    {
        return this.recorder.isRecording();
    }

    public int getServerTicks()
    {
        return this.serverTicks;
    }

    public void addServerTicks(int delta)
    {
        this.serverTicks += delta;
    }

    public void start()
    {
        if (this.recorder.isRecording())
        {
            return;
        }

        this.serverTicks = 0;

        Window window = MinecraftClient.getInstance().getWindow();

        // Whatever's actually being displayed right now, no resize, no
        // target resolution -- windowed or fullscreen alike.
        int width = window.getFramebufferWidth();
        int height = window.getFramebufferHeight();

        this.recorder.startRecording(width, height);
    }

    public void stop()
    {
        this.recorder.stopRecording();
    }

    /**
     * Call once per frame, after everything (world, GUI, screens, HUD) has
     * finished drawing for that frame -- i.e. at TAIL of
     * MinecraftClient#render, same spot SyncModule#endFrame() runs from.
     *
     * Gated on BBSRendering.canRender (same flag the depth pass and audio
     * use) so this only writes a frame on the same fixed-timestep cadence
     * as everything else -- without this check, a raw capture would write
     * one frame per real render call instead of one per target-FPS captured
     * frame, and the output video's actual framerate wouldn't match what
     * ffmpeg was told to encode it at.
     */
    public void captureFrame()
    {
        if (!this.recorder.isRecording() || !BBSRendering.canRender)
        {
            return;
        }

        this.recorder.recordFrame();
    }
}