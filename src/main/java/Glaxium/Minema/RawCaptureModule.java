package Glaxium.Minema;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Drives the "raw" (full screen, GUI included) capture -- see
 * RawCaptureRecorder for the actual pixel-pushing.
 *
 * <p>The literal displayed frame is only ever at BBS mod's configured Frame
 * Resolution if the game window itself is that size -- there's no way to
 * render the *entire* frame (world + every GUI screen + HUD + other mods'
 * overlays) into an arbitrary offscreen size without re-implementing
 * MinecraftClient#render from scratch. So instead, this temporarily resizes
 * the real game window to match BBS mod's configured width/height the
 * moment raw capture starts, and restores your original window size the
 * moment it stops. Same trade-off Minema 1.12.2 made (its
 * DisplaySizeModifier did the same thing) -- the window will visibly resize
 * on your screen while this is active.
 */
public class RawCaptureModule
{
    public static final RawCaptureModule INSTANCE = new RawCaptureModule();

    private final RawCaptureRecorder recorder = new RawCaptureRecorder();

    private int originalWidth;
    private int originalHeight;
    private boolean resized;

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

        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();

        this.originalWidth = window.getWidth();
        this.originalHeight = window.getHeight();

        int targetWidth = BBSRendering.getVideoWidth();
        int targetHeight = BBSRendering.getVideoHeight();

        if (window.getWidth() != targetWidth || window.getHeight() != targetHeight)
        {
            window.setWindowedSize(targetWidth, targetHeight);
            this.resized = true;

            // setWindowedSize() only resizes the actual GLFW window -- it
            // doesn't itself recalculate Minecraft's GUI scale or re-lay-out
            // whatever screen might already be open. That normally happens
            // via a GLFW framebuffer-size callback processed later in the
            // frame loop, which meant the *first* screen opened right after
            // starting capture (inventory, BBS's own UI, anything) could get
            // built against the pre-resize dimensions -- rendering
            // correctly only once you closed and reopened it, by which
            // point the callback had caught up. Forcing this here makes the
            // resize take full effect immediately instead of racing it.
            client.onResolutionChanged();
        }
        else
        {
            this.resized = false;
        }

        // Read back the real framebuffer pixel size rather than trusting
        // targetWidth/targetHeight directly -- on HiDPI/fractional-scaling
        // displays the window manager can hand back a framebuffer that
        // doesn't exactly match the requested windowed size, and the PBO
        // readback below has to match the actual FBO 0 dimensions or
        // glReadPixels will read garbage/out-of-bounds.
        int actualWidth = window.getFramebufferWidth();
        int actualHeight = window.getFramebufferHeight();

        this.recorder.startRecording(actualWidth, actualHeight);
    }

    public void stop()
    {
        this.recorder.stopRecording();

        if (this.resized)
        {
            MinecraftClient client = MinecraftClient.getInstance();

            client.getWindow().setWindowedSize(this.originalWidth, this.originalHeight);
            client.onResolutionChanged();

            this.resized = false;
        }
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