package verification;

import com.github.synnerz.devonian.GuiTextRenderStateAccessor;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Probe implements PreLaunchEntrypoint {
    private int checks;
    private final boolean baseline = Boolean.getBoolean("fix.test.baseline");
    private final boolean disabled = Boolean.getBoolean("fix.test.disabled");

    @Override public void onPreLaunch() {
        try {
            verify();
            String result = "PASS: " + checks + " assertions. Mode: "
                    + (baseline ? "baseline (original bypass reproduced)" : disabled ? "patched with text engine disabled" : "patched Modern UI chain")
                    + ". Actual Minecraft 26.1.2 and original released mixin bytecode.\n"
                    + "Modern UI GPU/layout endpoint replaced by test instrumentation. No visual rendering or game window tested.\n";
            Files.writeString(Path.of("verification-result.txt"), result);
            System.out.println(result);
            System.exit(0);
        } catch (Throwable problem) {
            problem.printStackTrace();
            System.exit(1);
        }
    }

    private void verify() {
        float[][] coords = {{Float.NaN, Float.NaN}, {12.5f, -3.25f}, {9.75f, Float.NaN}, {Float.NaN, 41.5f}};
        for (int i = 0; i < coords.length; i++) {
            Boundary.modernCalls = Boundary.vanillaCalls = 0;
            var font = new Boundary.SpyFont();
            boolean obfuscated = i != 0;
            Style style = Style.EMPTY.withObfuscated(obfuscated).withBold(true);
            FormattedCharSequence text = FormattedCharSequence.forward("Tooltip", style);
            int color = 0xFF12ABCD, background = 0x90123456;
            boolean shadow = i % 2 == 0, empty = i % 2 != 0;
            var state = new GuiTextRenderState(font, text, new Matrix3x2f(), 20, 30,
                    color, background, shadow, empty, null);
            var accessor = (GuiTextRenderStateAccessor)(Object)state;
            accessor.devonian$setXf(coords[i][0]);
            accessor.devonian$setYf(coords[i][1]);
            check(state.ensurePrepared() == Boundary.RESULT, "result preserved");
            check(Boundary.modernCalls == (baseline || disabled ? 0 : 1), "Modern UI chain count");
            check(Boundary.vanillaCalls == (baseline || disabled ? 1 : 0), "no unintended vanilla bypass");
            Object[] args = Boundary.arguments;
            check(args[0] == font && args[1] == text, "font and text identities retained");
            check(args[2].equals(Float.isNaN(coords[i][0]) ? 20f : coords[i][0]), "X including fractional override");
            check(args[3].equals(Float.isNaN(coords[i][1]) ? 30f : coords[i][1]), "Y including fractional override");
            check(args[4].equals(color) && args[7].equals(background), "colors retained");
            check(args[5].equals(shadow) && args[6].equals(empty), "flags retained");
            int[] characters = {0};
            ((FormattedCharSequence)args[1]).accept((index, deliveredStyle, codepoint) -> {
                if (deliveredStyle.isObfuscated() != obfuscated || !deliveredStyle.isBold()) {
                    throw new AssertionError("Text styles lost");
                }
                characters[0]++;
                return true;
            });
            check(characters[0] == 7, "all styled characters preserved");
            check(state.ensurePrepared() == Boundary.RESULT, "cache preserved");
            check(Boundary.modernCalls + Boundary.vanillaCalls == 1, "prepared once per state");
            check(state.bounds().width() == 30 && state.bounds().height() == 40, "layout bounds preserved");
        }
    }

    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
