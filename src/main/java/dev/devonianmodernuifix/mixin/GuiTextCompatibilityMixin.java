package dev.devonianmodernuifix.mixin;

import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Registers the target for the narrowly scoped post-apply compatibility repair. */
@Mixin(value = GuiTextRenderState.class, priority = 900)
public abstract class GuiTextCompatibilityMixin {
}
