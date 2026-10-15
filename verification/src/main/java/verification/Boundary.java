package verification;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;

public final class Boundary {
    public static int modernCalls, vanillaCalls;
    public static Object[] arguments;
    public static final Font.PreparedText RESULT = new Font.PreparedText() {
        @Override public void visit(Font.GlyphVisitor visitor) {}
        @Override public ScreenRectangle bounds() { return new ScreenRectangle(10, 20, 30, 40); }
    };
    public static Font.PreparedText modernUi(Font font, FormattedCharSequence text,
            float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
        modernCalls++;
        arguments = new Object[]{font, text, x, y, color, shadow, includeEmpty, background};
        return RESULT;
    }
    public static final class SpyFont extends Font {
        public SpyFont() { super(null); }
        @Override public PreparedText prepareText(FormattedCharSequence text,
                float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
            vanillaCalls++;
            arguments = new Object[]{this, text, x, y, color, shadow, includeEmpty, background};
            return RESULT;
        }
    }
}
