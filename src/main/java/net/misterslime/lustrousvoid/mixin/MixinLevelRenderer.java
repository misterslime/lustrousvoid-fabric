package net.misterslime.lustrousvoid.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

	@Shadow @Final private Minecraft minecraft;
	@Shadow private int ticks;
	@Nullable private VertexBuffer endStarBuffer;
	@Unique private boolean buildStars = true;
	@Unique private float starFade = 1.0f;

	private static final ResourceLocation END_STAR_LOCATION = new ResourceLocation("lustrousvoid", "textures/environment/end_star.png");

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void renderEndSky(PoseStack poseStack, Matrix4f matrix4f, float tickDelta, Runnable runnable, CallbackInfo ci) {
		final ServerLevel dimension = this.minecraft.getSingleplayerServer().getLevel(this.minecraft.level.dimension());

		runnable.run();
		if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
			BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
			FogRenderer.levelFogColor();

			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.enableTexture();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

			poseStack.pushPose();
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90.0f));
			poseStack.mulPose(Vector3f.XP.rotationDegrees((this.ticks + tickDelta) / 256f));
			Matrix4f matrix4f3 = poseStack.last().pose();

			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, END_STAR_LOCATION);

			if (dimension.getDragons().isEmpty()) {
				if (starFade < 1.0f) {
					starFade += 0.001f;
				} else if (starFade > 1.0f) {
					starFade = 1.0f;
				}
			} else {
				starFade = 0.0f;
			}

			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, starFade);

			if (this.buildStars || this.endStarBuffer == null) {
				if (this.endStarBuffer == null) this.endStarBuffer.close();
				this.endStarBuffer = new VertexBuffer();

				this.buildStars(matrix4f3, bufferBuilder);
				bufferBuilder.end();

				this.endStarBuffer.upload(bufferBuilder);
				this.buildStars = false;
			}

			this.endStarBuffer.drawWithShader(poseStack.last().pose(), matrix4f, GameRenderer.getPositionTexShader());

			runnable.run();
			poseStack.popPose();

			RenderSystem.disableBlend();
			RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
			RenderSystem.depthMask(true);

			ci.cancel();
		}
	}

	private void buildStars(Matrix4f matrix4f3, BufferBuilder buffer) {
		Random random = new Random(this.minecraft.getSingleplayerServer().overworld().getSeed());
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

		for(int i = 0; i < random.nextInt(5000) + 10000; ++i) {
			double d = random.nextFloat() * 2.0F - 1.0F;
			double e = random.nextFloat() * 2.0F - 1.0F;
			double f = random.nextFloat() * 2.0F - 1.0F;
			double g = randomStarSize(random);
			double h = d * d + e * e + f * f;
			if (h < 1.0D) {
				h = 1.0D / Math.sqrt(h);
				d *= h;
				e *= h;
				f *= h;
				double j = d * 100.0D;
				double k = e * 100.0D;
				double l = f * 100.0D;
				double m = Math.atan2(d, f);
				double n = Math.sin(m);
				double o = Math.cos(m);
				double p = Math.atan2(Math.sqrt(d * d + f * f), e);
				double q = Math.sin(p);
				double r = Math.cos(p);
				double s = random.nextDouble() * 3.141592653589793D * 2.0D;
				double t = Math.sin(s);
				double u = Math.cos(s);

				for(int v = 0; v < 4; ++v) {
					double x = (double)((v & 2) - 1) * g;
					double y = (double)((v + 1 & 2) - 1) * g;
					double aa = x * u - y * t;
					double ab = y * u + x * t;
					double ad = aa * q + 0.0D * r;
					double ae = 0.0D * q - aa * r;
					double af = ae * n - ab * o;
					double ah = ab * n + ae * o;

					Vec2 uv = new Vec2((v & 2) / 2f, (v + 1 & 2) / 2f);
					buffer.vertex(matrix4f3, (float) (j + af), (float) (k + ad), (float) (l + ah)).uv(uv.x, uv.y).endVertex();
				}
			}
		}
	}

	private float randomStarSize(Random random) {
		return (0.05F + (random.nextFloat() * random.nextFloat()));
	}
}
