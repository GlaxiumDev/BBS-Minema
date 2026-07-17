package Glacium.Minema;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.VideoRecorder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

/**
 * Piggybacks on BBS mod's own VideoRecorder rather than adding a UI of its
 * own: whenever BBS mod is recording, we're recording, one frame behind
 * nothing. If you want an independent on/off toggle later, this is the
 * place to add a keybind and drop the "isRecording()" mirroring.
 */
public class BBSMinema implements ClientModInitializer
{
    private final MinemaRecorder depthRecorder = new MinemaRecorder();
    private boolean wasRecording = false;

    @Override
    public void onInitializeClient()
    {
        WorldRenderEvents.LAST.register((context) ->
        {
            VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();
            boolean recordingNow = videoRecorder.isRecording() && BBSRendering.canRender;

            if (recordingNow && !this.wasRecording)
            {
                // BBS mod's video is already rolling by the time canRender flips
                // true on the same frame, so widths/heights are safe to read here.
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
        });
    }
}
