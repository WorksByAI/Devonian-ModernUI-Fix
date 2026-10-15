package dev.devonianmodernuifix;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Uses strings for Minecraft types so plugin loading cannot load game classes before Mixin. */
public final class CompatibilityPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("DevonianModernUIFix");
    private static final String TARGET = "net.minecraft.client.renderer.state.gui.GuiTextRenderState";
    private static final String DEVONIAN_MIXIN = "com.github.synnerz.devonian.mixin.GuiTextRenderStateMixin";
    private static final String FONT = "net/minecraft/client/gui/Font";
    private static final String TEXT = "Lnet/minecraft/util/FormattedCharSequence;";
    private static final String RESULT = "Lnet/minecraft/client/gui/Font$PreparedText;";
    private static final String OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;";
    private static final String VANILLA_DESC = "(" + TEXT + "FFIZZI)" + RESULT;
    private static final String HANDLER_DESC = "(L" + FONT + ";" + TEXT + "FFIZZI" + OPERATION + ")" + RESULT;
    private static final String BRIDGE = "dev/devonianmodernuifix/TextOperationBridge";

    @Override
    public void postApply(String targetName, ClassNode target, String mixinName, IMixinInfo mixinInfo) {
        if (!TARGET.equals(targetName)) return;

        List<MethodNode> handlers = target.methods.stream()
                .filter(method -> HANDLER_DESC.equals(method.desc))
                .filter(method -> method.name.endsWith("$devonian$textFloatPosition"))
                .filter(CompatibilityPlugin::comesFromDevonian)
                .toList();
        if (handlers.size() != 1) {
            throw unsupported("expected one Devonian GUI text wrapper, found " + handlers.size());
        }
        MethodNode handler = handlers.getFirst();
        if ((handler.access & Opcodes.ACC_STATIC) != 0) {
            throw unsupported("unexpected static wrapper");
        }
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode instruction : handler.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && FONT.equals(call.owner) && "prepareText".equals(call.name)
                    && VANILLA_DESC.equals(call.desc)) {
                calls.add(call);
            }
        }
        if (calls.size() != 1) {
            throw unsupported("expected one direct Font.prepareText call, found " + calls.size());
        }

        // The matched descriptor fixes original's local-variable slot at 9.
        // Leave all coordinate selection and argument-producing instructions untouched.
        MethodInsnNode call = calls.getFirst();
        handler.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 9));
        handler.instructions.set(call, new MethodInsnNode(
                Opcodes.INVOKESTATIC, BRIDGE, "callOriginal", HANDLER_DESC, false));
        handler.maxStack = Math.max(handler.maxStack, 9);
        LOGGER.info("Restored Devonian GUI text Operation chain; Modern UI text rendering can now run.");
    }

    private static boolean comesFromDevonian(MethodNode method) {
        return hasDevonianOrigin(method.visibleAnnotations) || hasDevonianOrigin(method.invisibleAnnotations);
    }

    private static boolean hasDevonianOrigin(List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        for (AnnotationNode annotation : annotations) {
            if (!"Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;".equals(annotation.desc)
                    || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if ("mixin".equals(annotation.values.get(i))
                        && DEVONIAN_MIXIN.equals(annotation.values.get(i + 1))) return true;
            }
        }
        return false;
    }

    private static IllegalStateException unsupported(String detail) {
        return new IllegalStateException("Devonian Modern UI Text Fix: " + detail
                + ". This patch supports Devonian 1.32.9, 1.33.9 and 1.34.9 with Modern UI 3.13.0.5 on Minecraft 26.1.2."
                + " Remove this compatibility patch if another mod has changed that wrapper.");
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetName, String mixinName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetName, ClassNode target, String mixinName, IMixinInfo mixinInfo) {}
}
