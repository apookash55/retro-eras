package dev.retroeras.client.entity;

import dev.retroeras.entity.CaveZombie;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws Cave game's zombies as plain humanoids in the default player skin, which the Cave game pack replaces with
 * rd-161348's own char.png, the texture its zombies used.
 */
public class CaveZombieRenderer extends HumanoidMobRenderer<CaveZombie, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

	public CaveZombieRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
