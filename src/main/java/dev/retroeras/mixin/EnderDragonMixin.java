package dev.retroeras.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.player.EraEnd;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.6.4's dragon (EntityDragon), ported, in an era's End: it has none of the modern phases, perches or breath. It flies
 * at random points over the island or straight at a player, breaking through everything but obsidian, end stone and
 * bedrock; its wings fling you and its head does 10 damage; a nearby crystal heals it and hurts it when it blows up. It
 * dies where it was, rising and spinning, and leaves the portal under it. All of this runs on the server: the client
 * keeps vanilla's code, which only follows the server and draws the dragon.
 *
 * <p>The old head was one 3 by 3 part. Modern dragons have a neck that size in the same place, so neck and head both count
 * as the head here.
 */
@Mixin(EnderDragon.class)
abstract class EnderDragonMixin extends Mob {
	@Shadow
	@Final
	public EnderDragonPart head;
	@Shadow
	@Final
	private EnderDragonPart neck;
	@Shadow
	@Final
	private EnderDragonPart body;
	@Shadow
	@Final
	private EnderDragonPart tail1;
	@Shadow
	@Final
	private EnderDragonPart tail2;
	@Shadow
	@Final
	private EnderDragonPart tail3;
	@Shadow
	@Final
	private EnderDragonPart wing1;
	@Shadow
	@Final
	private EnderDragonPart wing2;
	@Shadow
	@Final
	public DragonFlightHistory flightHistory;
	@Shadow
	public float oFlapTime;
	@Shadow
	public float flapTime;
	@Shadow
	public boolean inWall;
	@Shadow
	public int dragonDeathTime;
	@Shadow
	public float yRotA;
	@Shadow
	public @Nullable EndCrystal nearestCrystal;

	@Unique
	private double retroeras$targetX;
	@Unique
	private double retroeras$targetY = 100.0;
	@Unique
	private double retroeras$targetZ;
	@Unique
	private @Nullable Entity retroeras$target;
	/** 1.6.4's dragon had its health bar drawn by the client; a modern one gets it from the fight, which this End has none of. */
	@Unique
	private @Nullable ServerBossEvent retroeras$bossBar;

	@Shadow
	protected abstract void reallyHurt(ServerLevel level, DamageSource source, float damage);

	protected EnderDragonMixin(EntityType<? extends Mob> type, Level level) {
		super(type, level);
	}

	@Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
	private void retroeras$oldAiStep(CallbackInfo ci) {
		if (!(this.level() instanceof ServerLevel level) || !EraEnd.isEraEnd(level)) {
			return;
		}
		ci.cancel();
		this.oFlapTime = this.flapTime;
		if (retroeras$bossBar != null) {
			retroeras$bossBar.setProgress(this.getHealth() / this.getMaxHealth());
		}
		if (this.isDeadOrDying()) {
			return;
		}
		retroeras$checkCrystals(level);
		Vec3 motion = this.getDeltaMovement();
		float flapSpeed = 0.2F / ((float) motion.horizontalDistance() * 10.0F + 1.0F);
		flapSpeed *= (float) Math.pow(2.0, motion.y);
		this.flapTime += this.inWall ? flapSpeed * 0.5F : flapSpeed;
		this.setYRot(Mth.wrapDegrees(this.getYRot()));
		if (this.isNoAi()) {
			this.flapTime = 0.5F;
			return;
		}
		this.flightHistory.record(this.getY(), this.getYRot());
		retroeras$fly(level);
		this.yBodyRot = this.getYRot();
		retroeras$moveParts(level);
		this.inWall = retroeras$checkWalls(level, this.neck.getBoundingBox()) | retroeras$checkWalls(level, this.body.getBoundingBox());
	}

	@Unique
	private void retroeras$fly(ServerLevel level) {
		double dx = retroeras$targetX - this.getX();
		double dy = retroeras$targetY - this.getY();
		double dz = retroeras$targetZ - this.getZ();
		double distance = dx * dx + dy * dy + dz * dz;
		if (retroeras$target != null && retroeras$target.isRemoved()) {
			retroeras$target = null;
		}
		if (retroeras$target != null) {
			retroeras$targetX = retroeras$target.getX();
			retroeras$targetZ = retroeras$target.getZ();
			double toX = retroeras$targetX - this.getX();
			double toZ = retroeras$targetZ - this.getZ();
			double above = 0.4F + Math.sqrt(toX * toX + toZ * toZ) / 80.0 - 1.0;
			retroeras$targetY = retroeras$target.getBoundingBox().minY + Math.min(above, 10.0);
		} else {
			retroeras$targetX += this.random.nextGaussian() * 2.0;
			retroeras$targetZ += this.random.nextGaussian() * 2.0;
		}
		if (distance < 100.0 || distance > 22500.0 || this.horizontalCollision || this.verticalCollision) {
			retroeras$pickTarget(level);
		}
		float horizontal = Mth.sqrt((float) (dx * dx + dz * dz));
		if (horizontal > 0.0F) {
			dy /= horizontal;
		}
		dy = Mth.clamp(dy, -0.6F, 0.6F);
		this.setDeltaMovement(this.getDeltaMovement().add(0.0, dy * 0.1F, 0.0));
		this.setYRot(Mth.wrapDegrees(this.getYRot()));
		double wanted = 180.0 - Math.atan2(dx, dz) * 180.0 / (float) Math.PI;
		double turn = Mth.clamp(Mth.wrapDegrees(wanted - this.getYRot()), -50.0, 50.0);
		Vec3 aim = new Vec3(retroeras$targetX - this.getX(), retroeras$targetY - this.getY(), retroeras$targetZ - this.getZ()).normalize();
		Vec3 facing = new Vec3(Mth.sin(this.getYRot() * (float) Math.PI / 180.0F), this.getDeltaMovement().y, -Mth.cos(this.getYRot() * (float) Math.PI / 180.0F)).normalize();
		float along = Math.max((float) (facing.dot(aim) + 0.5) / 1.5F, 0.0F);
		this.yRotA *= 0.8F;
		double speed = this.getDeltaMovement().horizontalDistance();
		float turnScale = (float) speed + 1.0F;
		double speedCap = Math.min(speed + 1.0, 40.0);
		this.yRotA = (float) (this.yRotA + turn * (0.7F / speedCap / turnScale));
		this.setYRot(this.getYRot() + this.yRotA * 0.1F);
		float span = (float) (2.0 / (speedCap + 1.0));
		this.moveRelative(0.06F * (along * span + (1.0F - span)), new Vec3(0.0, 0.0, -1.0));
		this.move(MoverType.SELF, this.inWall ? this.getDeltaMovement().scale(0.8F) : this.getDeltaMovement());
		Vec3 moved = this.getDeltaMovement().normalize();
		float slide = 0.8F + 0.15F * ((float) (moved.dot(facing) + 1.0) / 2.0F);
		this.setDeltaMovement(this.getDeltaMovement().multiply(slide, 0.91F, slide));
	}

	/** Half the time a random player in the End; otherwise a point at least 10 blocks away over the island. */
	@Unique
	private void retroeras$pickTarget(ServerLevel level) {
		List<ServerPlayer> players = level.getPlayers(player -> !player.isSpectator());
		if (this.random.nextInt(2) == 0 && !players.isEmpty()) {
			retroeras$target = players.get(this.random.nextInt(players.size()));
			return;
		}
		double dx;
		double dy;
		double dz;
		do {
			retroeras$targetX = this.random.nextFloat() * 120.0F - 60.0F;
			retroeras$targetY = 70.0F + this.random.nextFloat() * 50.0F;
			retroeras$targetZ = this.random.nextFloat() * 120.0F - 60.0F;
			dx = this.getX() - retroeras$targetX;
			dy = this.getY() - retroeras$targetY;
			dz = this.getZ() - retroeras$targetZ;
		} while (dx * dx + dy * dy + dz * dz <= 100.0);
		retroeras$target = null;
	}

	@Unique
	private void retroeras$moveParts(ServerLevel level) {
		DragonFlightHistory.Sample recent = this.flightHistory.get(5);
		DragonFlightHistory.Sample now = this.flightHistory.get(0);
		float tilt = (float) (recent.y() - this.flightHistory.get(10).y()) * 10.0F / 180.0F * (float) Math.PI;
		float tiltCos = Mth.cos(tilt);
		float tiltSin = -Mth.sin(tilt);
		float yaw = this.getYRot() * (float) Math.PI / 180.0F;
		float sin = Mth.sin(yaw);
		float cos = Mth.cos(yaw);
		retroeras$place(this.body, sin * 0.5F, 0.0, -cos * 0.5F);
		retroeras$place(this.wing1, cos * 4.5F, 2.0, sin * 4.5F);
		retroeras$place(this.wing2, -cos * 4.5F, 2.0, -sin * 4.5F);
		if (this.hurtTime == 0) {
			retroeras$fling(level.getEntities(this, this.wing1.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_SPECTATORS));
			retroeras$fling(level.getEntities(this, this.wing2.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_SPECTATORS));
			for (Entity entity : level.getEntities(this, this.neck.getBoundingBox().inflate(1.0), EntitySelector.NO_SPECTATORS)) {
				if (entity instanceof LivingEntity) {
					entity.hurtServer(level, this.damageSources().mobAttack(this), 10.0F);
				}
			}
		}
		float headSin = Mth.sin(yaw - this.yRotA * 0.01F);
		float headCos = Mth.cos(yaw - this.yRotA * 0.01F);
		double headY = now.y() - recent.y() + tiltSin * 5.5F;
		retroeras$place(this.head, headSin * 5.5F * tiltCos, headY, -headCos * 5.5F * tiltCos);
		retroeras$place(this.neck, headSin * 5.5F * tiltCos, headY, -headCos * 5.5F * tiltCos);
		EnderDragonPart[] tails = {this.tail1, this.tail2, this.tail3};
		for (int i = 0; i < tails.length; i++) {
			DragonFlightHistory.Sample sample = this.flightHistory.get(12 + i * 2);
			float tailYaw = yaw + (float) Mth.wrapDegrees(sample.yRot() - recent.yRot()) * (float) Math.PI / 180.0F;
			float along = (i + 1) * 2.0F;
			retroeras$place(tails[i], -(sin * 1.5F + Mth.sin(tailYaw) * along) * tiltCos, sample.y() - recent.y() - (along + 1.5F) * tiltSin + 1.5,
					(cos * 1.5F + Mth.cos(tailYaw) * along) * tiltCos);
		}
	}

	@Unique
	private void retroeras$place(EnderDragonPart part, double x, double y, double z) {
		part.setPos(this.getX() + x, this.getY() + y, this.getZ() + z);
	}

	/** The wings do no damage, but throw you away from the body. */
	@Unique
	private void retroeras$fling(List<Entity> entities) {
		AABB box = this.body.getBoundingBox();
		double centreX = (box.minX + box.maxX) / 2.0;
		double centreZ = (box.minZ + box.maxZ) / 2.0;
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity) {
				double dx = entity.getX() - centreX;
				double dz = entity.getZ() - centreZ;
				double distance = Math.max(dx * dx + dz * dz, 0.1);
				entity.push(dx / distance * 4.0, 0.2F, dz / distance * 4.0);
				entity.needsSync = true;
			}
		}
	}

	/** The nearest crystal in 32 blocks heals it a point every half second; when that crystal is destroyed, the head takes 10. */
	@Unique
	private void retroeras$checkCrystals(ServerLevel level) {
		if (this.nearestCrystal != null) {
			if (this.nearestCrystal.isRemoved()) {
				((EnderDragon) (Object) this).hurt(level, this.head, this.damageSources().explosion((Entity) null, (Entity) null), 10.0F);
				this.nearestCrystal = null;
			} else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
				this.setHealth(this.getHealth() + 1.0F);
			}
		}
		if (this.random.nextInt(10) == 0) {
			EndCrystal nearest = null;
			double distance = Double.MAX_VALUE;
			for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0))) {
				double to = crystal.distanceToSqr(this);
				if (to < distance) {
					distance = to;
					nearest = crystal;
				}
			}
			this.nearestCrystal = nearest;
		}
	}

	/** Everything in the way is destroyed unless it is obsidian, end stone or bedrock. */
	@Unique
	private boolean retroeras$checkWalls(ServerLevel level, AABB box) {
		boolean hitWall = false;
		boolean destroyed = false;
		for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.END_STONE) && !state.is(Blocks.BEDROCK) && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
					destroyed = level.removeBlock(pos, false) || destroyed;
				} else {
					hitWall = true;
				}
			}
		}
		if (destroyed) {
			level.sendParticles(ParticleTypes.EXPLOSION, box.minX + box.getXsize() * this.random.nextFloat(), box.minY + box.getYsize() * this.random.nextFloat(),
					box.minZ + box.getZsize() * this.random.nextFloat(), 1, 0.0, 0.0, 0.0, 0.0);
		}
		return hitWall;
	}

	/** Only the head takes full damage. Any hit makes it break off and fly on, but only players and explosions hurt it. */
	@Inject(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			at = @At("HEAD"), cancellable = true)
	private void retroeras$oldHurt(ServerLevel level, EnderDragonPart part, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (!EraEnd.isEraEnd(level)) {
			return;
		}
		float amount = part == this.head || part == this.neck ? damage : damage / 4.0F + 1.0F;
		float yaw = this.getYRot() * (float) Math.PI / 180.0F;
		retroeras$targetX = this.getX() + Mth.sin(yaw) * 5.0F + (this.random.nextFloat() - 0.5F) * 2.0F;
		retroeras$targetY = this.getY() + this.random.nextFloat() * 3.0F + 1.0;
		retroeras$targetZ = this.getZ() - Mth.cos(yaw) * 5.0F + (this.random.nextFloat() - 0.5F) * 2.0F;
		retroeras$target = null;
		if (source.getEntity() instanceof Player || source.is(DamageTypeTags.IS_EXPLOSION)) {
			this.reallyHurt(level, source, amount);
		}
		cir.setReturnValue(true);
	}

	/** The body itself cannot be hurt, only its parts. */
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void retroeras$onlyParts(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (EraEnd.isEraEnd(level)) {
			cir.setReturnValue(false);
		}
	}

	/** It really dies, with no dying phase to fly back to the portal in. */
	@Inject(method = "handleKillingBlow", at = @At("HEAD"), cancellable = true)
	private void retroeras$oldKillingBlow(CallbackInfo ci) {
		if (EraEnd.isEraEnd(this.level())) {
			this.dead = true;
			ci.cancel();
		}
	}

	/** It rises and spins where it was for ten seconds, shedding experience, and the portal appears under it. */
	@Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
	private void retroeras$oldDeath(CallbackInfo ci) {
		if (!(this.level() instanceof ServerLevel level) || !EraEnd.isEraEnd(level)) {
			return;
		}
		ci.cancel();
		this.dragonDeathTime++;
		if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0) {
			ExperienceOrb.award(level, this.position(), 1000);
		}
		if (this.dragonDeathTime == 1 && !this.isSilent()) {
			level.globalLevelEvent(1028, this.blockPosition(), 0);
		}
		this.move(MoverType.SELF, new Vec3(0.0, 0.1F, 0.0));
		this.setYRot(this.getYRot() + 20.0F);
		this.yBodyRot = this.getYRot();
		if (this.dragonDeathTime >= 200) {
			ExperienceOrb.award(level, this.position(), 2000);
			EraEnd.buildExitPortal(level, Mth.floor(this.getX()), Mth.floor(this.getZ()));
			this.remove(Entity.RemovalReason.KILLED);
			this.gameEvent(GameEvent.ENTITY_DIE);
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (EraEnd.isEraEnd(this.level())) {
			if (retroeras$bossBar == null) {
				retroeras$bossBar = new ServerBossEvent(this.getUUID(), this.getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
				retroeras$bossBar.setProgress(this.getHealth() / this.getMaxHealth());
			}
			retroeras$bossBar.addPlayer(player);
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		if (retroeras$bossBar != null) {
			retroeras$bossBar.removePlayer(player);
		}
	}
}
