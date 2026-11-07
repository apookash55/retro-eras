package dev.retroeras.client.texture;

import java.awt.image.BufferedImage;

/**
 * The compass and clock from before 1.6 (TextureCompassFX and TextureWatchFX), which drew their needle and dial by code each
 * tick. 26.3 picks one of a set of frames instead, so each frame is drawn by that same code at its angle.
 */
final class LegacyDials {
	private LegacyDials() {
	}

	/** 26.3's frame 16 points straight ahead, and later frames turn clockwise. */
	static BufferedImage compass(BufferedImage base, int frame, int frames) {
		double angle = Math.PI - (frame - frames / 2) * Math.PI * 2.0 / frames;
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		BufferedImage out = copy(base);
		for (int i = -4; i <= 4; i++) {
			out.setRGB((int) (8.5 + cos * i * 0.3), (int) (7.5 - sin * i * 0.3 * 0.5), 0xFF646464);
		}
		for (int i = -8; i <= 16; i++) {
			out.setRGB((int) (8.5 + sin * i * 0.3), (int) (7.5 + cos * i * 0.3 * 0.5), i >= 0 ? 0xFFFF1414 : 0xFF646464);
		}
		return out;
	}

	/** The magenta pixels of the item show the dial, turned for the time and shaded by how bright the magenta is. Frame 0 is noon. */
	static BufferedImage clock(BufferedImage base, BufferedImage dial, int frame, int frames) {
		double angle = -(double) frame / frames * Math.PI * 2.0;
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		BufferedImage out = copy(base);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int pixel = base.getRGB(x, y);
				int red = pixel >> 16 & 0xFF;
				int green = pixel >> 8 & 0xFF;
				int blue = pixel & 0xFF;
				if (red == blue && green == 0 && blue > 0) {
					double u = -(x / 15.0 - 0.5);
					double v = y / 15.0 - 0.5;
					int dialX = (int) ((u * cos + v * sin + 0.5) * 16.0) & 15;
					int dialY = (int) ((v * cos - u * sin + 0.5) * 16.0) & 15;
					int shown = dial.getRGB(dialX, dialY);
					out.setRGB(x, y, (shown & 0xFF000000) | ((shown >> 16 & 0xFF) * red / 255) << 16 | ((shown >> 8 & 0xFF) * red / 255) << 8 | (shown & 0xFF) * red / 255);
				}
			}
		}
		return out;
	}

	private static BufferedImage copy(BufferedImage image) {
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		out.getGraphics().drawImage(image, 0, 0, null);
		return out;
	}
}
