package dev.retroeras.client.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;

import dev.retroeras.Era;

/**
 * The old mob models, swapped in when the era's resources load. The adult pig, sheep, chicken and wolf have barely changed
 * since, so those stay; 26.3 redrew the cow with a muzzle, and gave every baby its own model and texture. Old babies were
 * the adult with its body at half size and its head left big (ModelQuadruped.render), wearing the adult's texture.
 */
public final class OldMobModels {
	private static final float HALF_TURN = (float) (Math.PI / 2);

	private OldMobModels() {
	}

	/** {@code roots} with the old models that {@code era} had in place of the modern ones. */
	public static Map<ModelLayerLocation, LayerDefinition> forEra(Map<ModelLayerLocation, LayerDefinition> roots, Era era) {
		if (era == Era.CAVEGAME) {
			return roots;
		}
		Map<ModelLayerLocation, LayerDefinition> result = new HashMap<>(roots);
		MeshTransformer big = baby(8.0F, 4.0F);
		LayerDefinition cow = LayerDefinition.create(era == Era.OLD_RELEASE ? cow164() : cowBeta(), 64, 32);
		for (ModelLayerLocation layer : new ModelLayerLocation[] {ModelLayers.COW, ModelLayers.MOOSHROOM}) {
			result.put(layer, cow);
		}
		for (ModelLayerLocation layer : new ModelLayerLocation[] {ModelLayers.COW_BABY, ModelLayers.MOOSHROOM_BABY}) {
			result.put(layer, cow.apply(big));
		}
		LayerDefinition pig = LayerDefinition.create(pig(), 64, 32);
		result.put(ModelLayers.PIG, pig);
		result.put(ModelLayers.PIG_BABY, pig.apply(baby(4.0F, 4.0F)));
		result.put(ModelLayers.SHEEP_BABY, SheepModel.createBodyLayer().apply(big));
		result.put(ModelLayers.SHEEP_BABY_WOOL, SheepFurModel.createFurLayer().apply(big));
		result.put(ModelLayers.CHICKEN_BABY, AdultChickenModel.createBodyLayer().apply(baby(5.0F, 2.0F)));
		result.put(ModelLayers.WOLF_BABY, LayerDefinition.create(AdultWolfModel.createBodyLayer(CubeDeformation.NONE), 64, 32).apply(baby(5.0F, 2.0F)));
		if (era == Era.OLD_RELEASE) {
			MeshTransformer foal = MeshTransformer.scaling(0.5F);
			LayerDefinition horse = LayerDefinition.create(horse(false, false), 128, 128);
			LayerDefinition babyHorse = horse.apply(foal);
			for (ModelLayerLocation layer : new ModelLayerLocation[] {ModelLayers.HORSE, ModelLayers.SKELETON_HORSE, ModelLayers.ZOMBIE_HORSE}) {
				result.put(layer, horse);
			}
			for (ModelLayerLocation layer : new ModelLayerLocation[] {ModelLayers.HORSE_BABY, ModelLayers.SKELETON_HORSE_BABY, ModelLayers.ZOMBIE_HORSE_BABY}) {
				result.put(layer, babyHorse);
			}
			LayerDefinition donkey = LayerDefinition.create(horse(true, false), 128, 128);
			LayerDefinition babyDonkey = LayerDefinition.create(horse(true, true), 128, 128).apply(foal);
			result.put(ModelLayers.DONKEY, donkey.apply(MeshTransformer.scaling(0.87F)));
			result.put(ModelLayers.MULE, donkey.apply(MeshTransformer.scaling(0.92F)));
			result.put(ModelLayers.DONKEY_BABY, babyDonkey);
			result.put(ModelLayers.MULE_BABY, babyDonkey);
		}
		if (era != Era.CLASSIC && era != Era.PRE_ALPHA) {
			result.put(ModelLayers.OAK_BOAT, LayerDefinition.create(boat(), 64, 32));
		}
		return result;
	}

	/**
	 * ModelBoat: a plain open box on the old 64 by 32 texture, with no paddles. 26.3's renderer turns its boats a quarter
	 * turn further than the old one did, so the box is turned back to face the way it used to.
	 */
	private static MeshDefinition boat() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition hull = root.addOrReplaceChild("hull", CubeListBuilder.create(), PartPose.rotation(0.0F, -HALF_TURN, 0.0F));
		hull.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 8).addBox(-12.0F, -8.0F, -3.0F, 24.0F, 16.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, HALF_TURN, 0.0F, 0.0F));
		CubeListBuilder side = CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F);
		hull.addOrReplaceChild("left", side, PartPose.offsetAndRotation(-11.0F, 4.0F, 0.0F, 0.0F, HALF_TURN * 3.0F, 0.0F));
		hull.addOrReplaceChild("right", side, PartPose.offsetAndRotation(11.0F, 4.0F, 0.0F, 0.0F, HALF_TURN, 0.0F));
		hull.addOrReplaceChild("back", side, PartPose.offsetAndRotation(0.0F, 4.0F, -9.0F, 0.0F, HALF_TURN * 2.0F, 0.0F));
		hull.addOrReplaceChild("front", side, PartPose.offset(0.0F, 4.0F, 9.0F));
		root.addOrReplaceChild("left_paddle", CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("right_paddle", CubeListBuilder.create(), PartPose.ZERO);
		return mesh;
	}

	/**
	 * ModelHorse in 1.6.4, on its 128 by 128 texture. Its legs had knees that bent as it walked; here each leg moves as one,
	 * because 26.3's horse animation only knows whole legs. Its saddle and armour were drawn on this model and are still 26.3's.
	 */
	private static MeshDefinition horse(boolean donkey, boolean foal) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition top = mesh.getRoot();
		CubeListBuilder hull = CubeListBuilder.create().texOffs(0, 34).addBox(-5.0F, -8.0F, -19.0F, 10.0F, 10.0F, 24.0F);
		// A foal donkey's model hangs everything from one body part, and the others' only the tail and chests.
		PartDefinition body = top.addOrReplaceChild("body", foal ? CubeListBuilder.create() : hull, foal ? PartPose.ZERO : PartPose.offset(0.0F, 11.0F, 9.0F));
		PartDefinition root = foal ? body : top;
		if (foal) {
			body.addOrReplaceChild("hull", hull, PartPose.offset(0.0F, 11.0F, 9.0F));
		}
		// 26.3 turns the tail by 30 degrees itself, so its pieces are turned that much less than they were.
		float tailTurn = (float) (Math.PI / 6);
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), foal ? PartPose.offset(0.0F, 3.0F, 14.0F) : PartPose.offset(0.0F, -8.0F, 5.0F));
		tail.addOrReplaceChild("tail_base", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.rotation(-1.134464F - tailTurn, 0.0F, 0.0F));
		tail.addOrReplaceChild("tail_middle", CubeListBuilder.create().texOffs(38, 7).addBox(-1.5F, -2.0F, 3.0F, 3.0F, 4.0F, 7.0F), PartPose.rotation(-1.134464F - tailTurn, 0.0F, 0.0F));
		tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(24, 3).addBox(-1.5F, -4.5F, 9.0F, 3.0F, 4.0F, 7.0F), PartPose.rotation(-1.40215F - tailTurn, 0.0F, 0.0F));
		leg(root, "left_hind_leg", 78, 29, 78, 43, 78, 51, -2.5F, -2.0F, -2.5F, -2.0F, -1.5F, -2.5F, -2.0F, 4.0F, 11.0F);
		leg(root, "right_hind_leg", 96, 29, 96, 43, 96, 51, -1.5F, -2.0F, -2.5F, -1.0F, -1.5F, -1.5F, -2.0F, -4.0F, 11.0F);
		leg(root, "left_front_leg", 44, 29, 44, 41, 44, 51, -1.9F, -1.0F, -2.1F, -1.9F, -1.6F, -2.4F, -2.1F, 4.0F, -8.0F);
		leg(root, "right_front_leg", 60, 29, 60, 41, 60, 51, -1.1F, -1.0F, -2.1F, -1.1F, -1.6F, -1.6F, -2.1F, -4.0F, -8.0F);
		PartDefinition head = root.addOrReplaceChild("head_parts", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, -10.0F));
		head.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -10.0F, -1.5F, 5.0F, 5.0F, 7.0F), PartPose.ZERO);
		head.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, -10.0F, -7.0F, 4.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 0.02F, 0.02F));
		head.addOrReplaceChild("lower_mouth", CubeListBuilder.create().texOffs(24, 27).addBox(-2.0F, -7.0F, -6.5F, 4.0F, 2.0F, 5.0F), PartPose.ZERO);
		head.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 12).addBox(-2.05F, -9.8F, -2.0F, 4.0F, 14.0F, 8.0F), PartPose.ZERO);
		head.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(58, 0).addBox(-1.0F, -11.5F, 5.0F, 2.0F, 16.0F, 4.0F), PartPose.ZERO);
		if (donkey) {
			head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -16.0F, 4.0F, 2.0F, 7.0F, 1.0F), PartPose.ZERO);
			head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -16.0F, 4.0F, 2.0F, 7.0F, 1.0F), PartPose.ZERO);
			body.addOrReplaceChild("left_chest", CubeListBuilder.create().texOffs(0, 47).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F), PartPose.offsetAndRotation(4.5F, foal ? 3.0F : -8.0F, foal ? 10.0F : 1.0F, 0.0F, HALF_TURN, 0.0F));
			body.addOrReplaceChild("right_chest", CubeListBuilder.create().texOffs(0, 34).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F), PartPose.offsetAndRotation(-7.5F, foal ? 3.0F : -8.0F, foal ? 10.0F : 1.0F, 0.0F, -HALF_TURN, 0.0F));
		} else {
			head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 0).addBox(0.45F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F), PartPose.ZERO);
			head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 0).addBox(-2.45F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F), PartPose.ZERO);
		}
		return mesh;
	}

	/** An old horse's leg: a thigh, a shin 7 down from it and a hoof, all one part. */
	private static void leg(PartDefinition root, String name, int thighU, int thighV, int shinU, int shinV, int hoofU, int hoofV,
			float thighX, float thighY, float thighZ, float shinX, float shinZ, float hoofX, float hoofZ, float x, float z) {
		float thighWidth = thighV == 29 && thighU >= 78 || thighU >= 96 ? 9.0F : 8.0F;
		float depth = thighU >= 78 ? 5.0F : 4.0F;
		root.addOrReplaceChild(name, CubeListBuilder.create()
				.texOffs(thighU, thighV).addBox(thighX, thighY, thighZ, thighU >= 78 ? 4.0F : 3.0F, thighWidth, depth)
				.texOffs(shinU, shinV).addBox(shinX, 7.0F, shinZ, 3.0F, 5.0F, 3.0F)
				.texOffs(hoofU, hoofV).addBox(hoofX, 12.1F, hoofZ, 4.0F, 3.0F, 4.0F),
				PartPose.offset(x, 9.0F, z));
	}

	private static MeshTransformer baby(float headY, float headZ) {
		return new BabyModelTransform(false, headY, headZ, Set.of("head"));
	}

	private static MeshDefinition cowBody(CubeListBuilder head, CubeListBuilder body) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", head, PartPose.offset(0.0F, 4.0F, -8.0F));
		root.addOrReplaceChild("body", body, PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, HALF_TURN, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-4.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(4.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-4.0F, 12.0F, -6.0F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(4.0F, 12.0F, -6.0F));
		return mesh;
	}

	/** ModelCow in Alpha and Beta: small horns set in from the sides of the head, and a thicker udder further back. */
	private static MeshDefinition cowBeta() {
		MeshDefinition mesh = cowBody(
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
						.texOffs(22, 0).addBox(-4.0F, -6.0F, -3.0F, 1.0F, 3.0F, 1.0F)
						.texOffs(22, 0).addBox(3.0F, -6.0F, -3.0F, 1.0F, 3.0F, 1.0F),
				CubeListBuilder.create().texOffs(18, 4).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F));
		mesh.getRoot().addOrReplaceChild("udder", CubeListBuilder.create().texOffs(52, 0).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 14.0F, 6.0F, HALF_TURN, 0.0F, 0.0F));
		return mesh;
	}

	/** ModelCow in 1.6.4: today's, without the muzzle, and with its horns a block further back. */
	private static MeshDefinition cow164() {
		return cowBody(
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
						.texOffs(22, 0).addBox(-5.0F, -5.0F, -4.0F, 1.0F, 3.0F, 1.0F)
						.texOffs(22, 0).addBox(4.0F, -5.0F, -4.0F, 1.0F, 3.0F, 1.0F),
				CubeListBuilder.create().texOffs(18, 4).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F)
						.texOffs(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F));
	}

	/** ModelPig, on the old 64 by 32 texture rather than 26.3's taller one. */
	private static MeshDefinition pig() {
		MeshDefinition mesh = QuadrupedModel.createBodyMesh(6, false, false, CubeDeformation.NONE);
		mesh.getRoot().addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F)
						.texOffs(16, 16).addBox(-2.0F, 0.0F, -9.0F, 4.0F, 3.0F, 1.0F),
				PartPose.offset(0.0F, 12.0F, -6.0F));
		return mesh;
	}
}
