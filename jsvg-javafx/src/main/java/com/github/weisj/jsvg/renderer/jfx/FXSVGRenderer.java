package com.github.weisj.jsvg.renderer.jfx;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.jfx.impl.FXOutput;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.ViewBox;
import javafx.scene.canvas.GraphicsContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class FXSVGRenderer {

    public static void render(@NotNull SVGDocument document, @NotNull GraphicsContext ctx) {
        render(document, ctx, null, null);
    }

    public static void render(@NotNull SVGDocument document, @NotNull GraphicsContext ctx, @Nullable ViewBox bounds, @Nullable AnimationState animationState) {
        Output output = createOutputForGraphicsContext(ctx);
        document.renderWithPlatform(NullPlatformSupport.INSTANCE, output, bounds, animationState);
        output.dispose();
    }

    public static @NotNull Output createOutputForGraphicsContext(@NotNull GraphicsContext ctx) {
        Output output = new FXOutput(ctx);
        setupDefaultJFXRenderingHints(output);
        return output;
    }

    // JFX defaults to the highest render quality
    public static void setupDefaultJFXRenderingHints(Output output){
        output.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        output.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        output.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        output.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
        output.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING, SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF);
        output.setRenderingHint(SVGRenderingHints.KEY_MASK_CLIP_RENDERING, SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_ACCURACY);
    }

}
