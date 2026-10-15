package verification;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Test-only instrumentation stops at the real Modern UI entry boundary, before GPU access. */
public final class BoundaryPlugin implements IMixinConfigPlugin {
    @Override public void postApply(String targetName, ClassNode node, String name, IMixinInfo info) {
        if (!name.equals("verification.mixin.BoundaryMixin")) return;
        if (Boolean.getBoolean("fix.test.disabled")) return;
        MethodNode boundary = node.methods.stream()
                .filter(m -> m.name.startsWith("redirect$") && m.name.endsWith("$onPrepareText"))
                .findFirst().orElseThrow(() -> new AssertionError("Actual Modern UI redirect missing"));
        boolean layout = false;
        for (AbstractInsnNode instruction : boundary.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("icyllis/modernui/mc/text/TextLayout")
                    && call.name.equals("prepareTextWithDensity")) layout = true;
        }
        if (!layout) throw new AssertionError("Original Modern UI layout call was not present");
        boundary.instructions.clear();
        boundary.tryCatchBlocks.clear();
        if (boundary.localVariables != null) boundary.localVariables.clear();
        int[] opcodes = {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.FLOAD, Opcodes.FLOAD,
                Opcodes.ILOAD, Opcodes.ILOAD, Opcodes.ILOAD, Opcodes.ILOAD};
        for (int i = 0; i < opcodes.length; i++) boundary.instructions.add(new VarInsnNode(opcodes[i], i + 1));
        boundary.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "verification/Boundary",
                "modernUi", boundary.desc, false));
        boundary.instructions.add(new InsnNode(Opcodes.ARETURN));
        boundary.maxStack = 8;
        boundary.maxLocals = 9;
    }
    @Override public void onLoad(String p) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String t, String m) {
        return !m.equals("icyllis.modernui.mc.text.mixin.MixinGuiTextRenderState")
                || !Boolean.getBoolean("fix.test.disabled");
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String t, ClassNode n, String m, IMixinInfo i) {}
}
