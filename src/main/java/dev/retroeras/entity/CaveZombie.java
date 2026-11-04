package dev.retroeras.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * A port of rd-161348's Zombie: a harmless figure in the player's own skin that wanders in slowly drifting circles and hops
 * now and then. Nothing could hurt it; it was only lost by falling off the edge of the world.
 */
public class CaveZombie extends PathfinderMob {
	/** Heading in radians, which drifts at rate {@link #rotA} just as the original's did. */
	private float rot = (float) (Math.random() * Math.PI * 2.0);
	private float rotA = (float) (Math.random() + 1.0) * 0.01F;

	public CaveZombie(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		// The original ticked 60 times a second; three steps per 26.3 tick keep its drift and hopping rate.
		for (int step = 0; step < 3; step++) {
			this.rot += this.rotA;
			this.rotA *= 0.99F;
			this.rotA += (float) ((Math.random() - Math.random()) * Math.random() * Math.random() * 0.08F);
			if (this.onGround() && Math.random() < 0.08) {
				this.getJumpControl().jump();
			}
		}
		this.getMoveControl().setWantedPosition(this.getX() + Math.sin(this.rot) * 2.0, this.getY(), this.getZ() + Math.cos(this.rot) * 2.0, 1.0);
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return !source.is(DamageTypes.FELL_OUT_OF_WORLD);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
