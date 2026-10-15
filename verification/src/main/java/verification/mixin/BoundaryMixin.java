package verification.mixin;

import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = GuiTextRenderState.class, priority = 100)
public abstract class BoundaryMixin {}
