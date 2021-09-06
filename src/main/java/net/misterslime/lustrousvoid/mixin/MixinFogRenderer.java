package net.misterslime.lustrousvoid.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {

    @ModifyVariable(method = "setupColor", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"), ordinal = 2, require = 1, allow = 1)
    private static Vec3 onSampleColor(Vec3 val) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final ServerLevel dimension = minecraft.getSingleplayerServer().getLevel(level.dimension());

        if (level.effects().skyType() == DimensionSpecialEffects.SkyType.END && dimension.getDragons().isEmpty()) {
            return new Vec3(0, 0, 0);
        }

        return val;
    }
}
