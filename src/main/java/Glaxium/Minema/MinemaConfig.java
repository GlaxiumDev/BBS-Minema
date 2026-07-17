package Glaxium.Minema;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Deliberately NOT hooked into BBS mod's own settings/values system --
 * that's built around BBS mod's internal panels and isn't really meant for
 * third-party addons to register into. A flat properties file next to every
 * other mod's config is simpler and won't break if BBS mod's settings
 * internals change under us.
 *
 * Mirrors the two knobs Minema 1.12.2 actually exposed for this
 * (MinemaConfig#captureDepth, MinemaConfig#captureDepthDistance) instead of
 * the single hardcoded `far = 512F` the first version of this addon used.
 */
public class MinemaConfig
{
    private static final Path PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("bbs-minema.properties");

    /** Off by default -- same as Minema's captureDepth, opt-in not automatic. */
    public boolean captureDepth = false;

    /** Far plane in blocks used to normalize the depth pass. Minema's default was tied to render distance; 128 is a reasonable flat default for typical builds. */
    public double captureDepthDistance = 128.0;

    public void load()
    {
        if (!Files.exists(PATH))
        {
            this.save();

            return;
        }

        Properties props = new Properties();

        try (var in = Files.newInputStream(PATH))
        {
            props.load(in);

            this.captureDepth = Boolean.parseBoolean(
                props.getProperty("captureDepth", String.valueOf(this.captureDepth))
            );
            this.captureDepthDistance = Double.parseDouble(
                props.getProperty("captureDepthDistance", String.valueOf(this.captureDepthDistance))
            );
        }
        catch (IOException | NumberFormatException e)
        {
            e.printStackTrace();
        }
    }

    public void save()
    {
        Properties props = new Properties();

        props.setProperty("captureDepth", String.valueOf(this.captureDepth));
        props.setProperty("captureDepthDistance", String.valueOf(this.captureDepthDistance));

        try
        {
            Files.createDirectories(PATH.getParent());

            try (var out = Files.newOutputStream(PATH))
            {
                props.store(out, "BBS Minema -- depth pass recording settings");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void toggleCaptureDepth()
    {
        this.captureDepth = !this.captureDepth;
        this.save();
    }
}
