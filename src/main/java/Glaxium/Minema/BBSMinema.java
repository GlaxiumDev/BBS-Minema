package Glaxium.Minema;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
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
    // Shared with UIVideoSettingsOverlayPanelMixin, which has no reference
    // to this class -- both read/write the same static MinemaConfig.INSTANCE.
    private final MinemaConfig config = MinemaConfig.INSTANCE;
    private boolean wasRecording = false;

    private KeyBinding toggleKey;

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
    }
}