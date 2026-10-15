package dev.devonianmodernuifix;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityPluginTest {
    private static final String TARGET="net.minecraft.client.renderer.state.gui.GuiTextRenderState";
    private static final String VANILLA="(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;";
    private static final String HANDLER="(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZZILcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)Lnet/minecraft/client/gui/Font$PreparedText;";
    private static ClassNode target(){var node=new ClassNode();node.name=TARGET.replace('.','/');return node;}
    private static MethodNode wrapper(ClassNode node,String origin,int calls){
        var method=new MethodNode(Opcodes.ACC_PRIVATE,"wrapOperation$abc$devonian$textFloatPosition",HANDLER,null,null);
        var annotation=new AnnotationNode("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;");
        annotation.values=new ArrayList<>(List.of("mixin",origin));method.visibleAnnotations=new ArrayList<>(List.of(annotation));
        for(int i=0;i<calls;i++)method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"net/minecraft/client/gui/Font","prepareText",VANILLA,false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));node.methods.add(method);return method;
    }
    private static MethodNode wrapper(ClassNode node){return wrapper(node,"com.github.synnerz.devonian.mixin.GuiTextRenderStateMixin",1);}
    @Test void restoresOnlyTheOriginalOperationAndRetainsSurroundingInstructions(){
        var node=target();var method=wrapper(node);var tail=method.instructions.getLast();
        new CompatibilityPlugin().postApply(TARGET,node,"fix",null);
        assertEquals(3,method.instructions.size());var operation=(VarInsnNode)method.instructions.getFirst();
        assertEquals(Opcodes.ALOAD,operation.getOpcode());assertEquals(9,operation.var);
        var bridge=(MethodInsnNode)operation.getNext();assertEquals(Opcodes.INVOKESTATIC,bridge.getOpcode());
        assertEquals("dev/devonianmodernuifix/TextOperationBridge",bridge.owner);assertEquals("callOriginal",bridge.name);
        assertEquals(HANDLER,bridge.desc);assertSame(tail,bridge.getNext());assertTrue(method.maxStack>=9);
    }
    @Test void ignoresUnrelatedGameTargets(){var node=target();new CompatibilityPlugin().postApply("other.Target",node,"fix",null);assertTrue(node.methods.isEmpty());}
    @Test void refusesMissingOrAmbiguousWrappers(){
        var node=target();assertThrows(IllegalStateException.class,()->new CompatibilityPlugin().postApply(TARGET,node,"fix",null));
        wrapper(node);wrapper(node);assertThrows(IllegalStateException.class,()->new CompatibilityPlugin().postApply(TARGET,node,"fix",null));
    }
    @Test void refusesHandlersFromAnotherMod(){var node=target();wrapper(node,"other.mixin",1);assertThrows(IllegalStateException.class,()->new CompatibilityPlugin().postApply(TARGET,node,"fix",null));}
    @Test void refusesStaticHandlersAndUnknownCallShapes(){
        var node=target();var method=wrapper(node);method.access|=Opcodes.ACC_STATIC;
        assertThrows(IllegalStateException.class,()->new CompatibilityPlugin().postApply(TARGET,node,"fix",null));
        for(int count:new int[]{0,2}){var changed=target();wrapper(changed,"com.github.synnerz.devonian.mixin.GuiTextRenderStateMixin",count);assertThrows(IllegalStateException.class,()->new CompatibilityPlugin().postApply(TARGET,changed,"fix",null));}
    }
    @Test void packagedMetadataAcceptsAllVerifiedDevonianVersionsWithoutBundlingThirdPartyClasses()throws Exception{
        try(var jar=new java.util.zip.ZipFile(System.getProperty("fix.binaryJar"))){
            var json=new String(jar.getInputStream(jar.getEntry("fabric.mod.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            var metadata=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            assertEquals("1.0.1+26.1.2",metadata.get("version").getAsString());
            assertEquals(List.of("1.32.9","1.33.9","1.34.9"),metadata.getAsJsonObject("depends").getAsJsonArray("devonian").asList().stream().map(e->e.getAsString()).toList());
            assertEquals("3.13.0.5",metadata.getAsJsonObject("depends").get("modernui").getAsString());
            assertTrue(jar.stream().noneMatch(e->e.getName().startsWith("com/github/synnerz/")||e.getName().startsWith("icyllis/")||e.getName().startsWith("verification/")));
            assertNotNull(jar.getEntry("LICENSE"));
        }
    }
}
