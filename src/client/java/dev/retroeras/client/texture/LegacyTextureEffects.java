package dev.retroeras.client.texture;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Old Minecraft drew water, lava, fire and the nether portal with code each tick rather than from a texture (the
 * TextureFX classes). These are ports of Beta 1.7.3's, run for a while and recorded as animation strips that a 26.3
 * resource pack can play back.
 */
final class LegacyTextureEffects {
	/** Ticks to run before recording, so the simulation has settled. */
	private static final int WARM_UP = 200;

	private LegacyTextureEffects() {
	}

	/** One frame of output: 16x16 RGBA pixels, row by row. */
	private interface Effect {
		void tick(Random random);

		int argb(int pixel);
	}

	/** Records {@code frames} ticks of the named effect as a vertical strip of 16x16 frames. */
	static BufferedImage strip(String name, int frames, long seed) {
		Random random = new Random(seed);
		Effect effect = switch (name) {
			case "water_still" -> new WaterStill();
			case "water_flow" -> new WaterFlow();
			case "lava_still" -> new Lava(false);
			case "lava_flow" -> new Lava(true);
			case "fire" -> new Fire();
			case "portal" -> new Portal();
			default -> throw new IllegalArgumentException("Unknown texture effect " + name);
		};
		for (int i = 0; i < WARM_UP; i++) {
			effect.tick(random);
		}
		BufferedImage out = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
		for (int frame = 0; frame < frames; frame++) {
			effect.tick(random);
			for (int pixel = 0; pixel < 256; pixel++) {
				out.setRGB(pixel % 16, frame * 16 + pixel / 16, effect.argb(pixel));
			}
		}
		return out;
	}

	private static int pack(int a, int r, int g, int b) {
		return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
	}

	private static float clamp(float value) {
		return value > 1.0F ? 1.0F : Math.max(value, 0.0F);
	}

	/** TextureWaterFX: ripples spreading sideways. */
	private static class WaterStill implements Effect {
		float[] current = new float[256];
		float[] next = new float[256];
		final float[] heat = new float[256];
		final float[] heatChange = new float[256];
		int ticks;

		@Override
		public void tick(Random random) {
			ticks++;
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					float sum = 0.0F;
					for (int dx = x - 1; dx <= x + 1; dx++) {
						sum += current[(dx & 15) + (y & 15) * 16];
					}
					next[x + y * 16] = sum / 3.3F + heat[x + y * 16] * 0.8F;
				}
			}
			cool(random, 0.1F, 0.05);
			swap();
		}

		void cool(Random random, float drop, double chance) {
			for (int i = 0; i < 256; i++) {
				heat[i] += heatChange[i] * 0.05F;
				if (heat[i] < 0.0F) {
					heat[i] = 0.0F;
				}
				heatChange[i] -= drop;
				if (random.nextDouble() < chance) {
					heatChange[i] = 0.5F;
				}
			}
		}

		void swap() {
			float[] swap = next;
			next = current;
			current = swap;
		}

		float value(int pixel) {
			return current[pixel];
		}

		@Override
		public int argb(int pixel) {
			float f = clamp(value(pixel));
			float squared = f * f;
			return pack((int) (146.0F + squared * 50.0F), (int) (32.0F + squared * 32.0F), (int) (50.0F + squared * 64.0F), 255);
		}
	}

	/** TextureWaterFlowFX: the same ripples, smeared downward and scrolling. */
	private static final class WaterFlow extends WaterStill {
		@Override
		public void tick(Random random) {
			ticks++;
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					float sum = 0.0F;
					for (int dy = y - 2; dy <= y; dy++) {
						sum += current[(x & 15) + (dy & 15) * 16];
					}
					next[x + y * 16] = sum / 3.2F + heat[x + y * 16] * 0.8F;
				}
			}
			cool(random, 0.3F, 0.2);
			swap();
		}

		@Override
		float value(int pixel) {
			return current[pixel - ticks * 16 & 255];
		}
	}

	/** TextureLavaFX and TextureLavaFlowFX: slow churning blobs; the flowing kind scrolls a row every three ticks. */
	private static final class Lava implements Effect {
		private final boolean flowing;
		private float[] current = new float[256];
		private float[] next = new float[256];
		private final float[] heat = new float[256];
		private final float[] heatChange = new float[256];
		private int ticks;

		Lava(boolean flowing) {
			this.flowing = flowing;
		}

		@Override
		public void tick(Random random) {
			ticks++;
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					float sum = 0.0F;
					int offsetX = (int) (sin(y * (float) Math.PI * 2.0F / 16.0F) * 1.2F);
					int offsetY = (int) (sin(x * (float) Math.PI * 2.0F / 16.0F) * 1.2F);
					for (int dx = x - 1; dx <= x + 1; dx++) {
						for (int dy = y - 1; dy <= y + 1; dy++) {
							sum += current[(dx + offsetX & 15) + (dy + offsetY & 15) * 16];
						}
					}
					next[x + y * 16] = sum / 10.0F + (heat[(x & 15) + (y & 15) * 16] + heat[(x + 1 & 15) + (y & 15) * 16]
							+ heat[(x + 1 & 15) + (y + 1 & 15) * 16] + heat[(x & 15) + (y + 1 & 15) * 16]) / 4.0F * 0.8F;
					heat[x + y * 16] += heatChange[x + y * 16] * 0.01F;
					if (heat[x + y * 16] < 0.0F) {
						heat[x + y * 16] = 0.0F;
					}
					heatChange[x + y * 16] -= 0.06F;
					if (random.nextDouble() < 0.005) {
						heatChange[x + y * 16] = 1.5F;
					}
				}
			}
			float[] swap = next;
			next = current;
			current = swap;
		}

		@Override
		public int argb(int pixel) {
			float f = clamp(current[flowing ? pixel - ticks / 3 * 16 & 255 : pixel] * 2.0F);
			return pack(255, (int) (f * 100.0F + 155.0F), (int) (f * f * 255.0F), (int) (f * f * f * f * 128.0F));
		}
	}

	/** TextureFlamesFX: flames rising from a randomly flickering bottom row. */
	private static final class Fire implements Effect {
		private float[] current = new float[320];
		private float[] next = new float[320];

		@Override
		public void tick(Random random) {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 20; y++) {
					int count = 18;
					float sum = current[x + (y + 1) % 20 * 16] * count;
					for (int dx = x - 1; dx <= x + 1; dx++) {
						for (int dy = y; dy <= y + 1; dy++) {
							if (dx >= 0 && dy >= 0 && dx < 16 && dy < 20) {
								sum += current[dx + dy * 16];
							}
							count++;
						}
					}
					next[x + y * 16] = sum / (count * 1.06F);
					if (y >= 19) {
						next[x + y * 16] = (float) (random.nextDouble() * random.nextDouble() * random.nextDouble() * 4.0
								+ random.nextDouble() * 0.1F + 0.2F);
					}
				}
			}
			float[] swap = next;
			next = current;
			current = swap;
		}

		@Override
		public int argb(int pixel) {
			float f = clamp(current[pixel] * 1.8F);
			int alpha = f < 0.5F ? 0 : 255;
			return pack(alpha, (int) (f * 155.0F + 100.0F), (int) (f * f * 255.0F), (int) (Math.pow(f, 10) * 255.0F));
		}
	}

	/** TexturePortalFX: two swirls, precomputed for 32 frames from a fixed seed. */
	private static final class Portal implements Effect {
		private final int[][] frames = new int[32][256];
		private int ticks;

		Portal() {
			Random random = new Random(100L);
			for (int frame = 0; frame < 32; frame++) {
				for (int x = 0; x < 16; x++) {
					for (int y = 0; y < 16; y++) {
						float value = 0.0F;
						for (int swirl = 0; swirl < 2; swirl++) {
							float dx = (x - swirl * 8) / 16.0F * 2.0F;
							float dy = (y - swirl * 8) / 16.0F * 2.0F;
							if (dx < -1.0F) {
								dx += 2.0F;
							}
							if (dx >= 1.0F) {
								dx -= 2.0F;
							}
							if (dy < -1.0F) {
								dy += 2.0F;
							}
							if (dy >= 1.0F) {
								dy -= 2.0F;
							}
							float distance = dx * dx + dy * dy;
							float angle = (float) Math.atan2(dy, dx) + (frame / 32.0F * (float) Math.PI * 2.0F - distance * 10.0F + swirl * 2) * (swirl * 2 - 1);
							angle = (sin(angle) + 1.0F) / 2.0F;
							angle /= distance + 1.0F;
							value += angle * 0.5F;
						}
						value += random.nextFloat() * 0.1F;
						int blue = (int) (value * 100.0F + 155.0F);
						int red = (int) (value * value * 200.0F + 55.0F);
						int green = (int) (value * value * value * value * 255.0F);
						int alpha = (int) (value * 100.0F + 155.0F);
						frames[frame][y * 16 + x] = pack(alpha, red, green, blue);
					}
				}
			}
		}

		@Override
		public void tick(Random random) {
			ticks++;
		}

		@Override
		public int argb(int pixel) {
			return frames[ticks & 31][pixel];
		}
	}

	/** MathHelper.sin's lookup table. */
	private static float sin(float value) {
		return SIN[(int) (value * 10430.378F) & 65535];
	}

	private static final float[] SIN = new float[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
	}
}
