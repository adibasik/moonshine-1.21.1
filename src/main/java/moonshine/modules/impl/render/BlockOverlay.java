package moonshine.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import moonshine.events.api.EventHandler;
import moonshine.events.impl.WorldRenderEvent;
import moonshine.modules.module.ModuleStructure;
import moonshine.modules.module.category.ModuleCategory;
import moonshine.util.Instance;
import moonshine.util.render.Render3D;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockOverlay extends ModuleStructure {
    public static BlockOverlay getInstance() {
        return Instance.get(BlockOverlay.class);
    }

    public BlockOverlay() {
        super("BlockOverlay", "Block Overlay", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(HitResult.Type.BLOCK)) {
            BlockPos pos = result.getBlockPos();
            Render3D.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), new Color(109, 252, 255,230).getRGB(), 1.5f, true, true);
        }
    }
}
