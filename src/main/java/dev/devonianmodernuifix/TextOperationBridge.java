package dev.devonianmodernuifix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

public final class TextOperationBridge {
    private TextOperationBridge() {}

    /** Receives Devonian's already-adjusted coordinates and preserves the full wrapper chain. */
    public static Font.PreparedText callOriginal(
            Font font, FormattedCharSequence text, float x, float y, int color,
            boolean shadow, boolean includeEmpty, int backgroundColor,
            Operation<Font.PreparedText> original) {
        return original.call(font, text, x, y, color, shadow, includeEmpty, backgroundColor);
    }
}
