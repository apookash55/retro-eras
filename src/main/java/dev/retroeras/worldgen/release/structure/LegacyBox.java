package dev.retroeras.worldgen.release.structure;

/** 1.6.4's StructureBoundingBox: an inclusive box of block coordinates. */
public final class LegacyBox {
	public int minX;
	public int minY;
	public int minZ;
	public int maxX;
	public int maxY;
	public int maxZ;

	public LegacyBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public LegacyBox(LegacyBox other) {
		this(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
	}

	/** A box spanning the whole column range, as 1.6.4 used for a chunk being populated. */
	public LegacyBox(int minX, int minZ, int maxX, int maxZ) {
		this(minX, 1, minZ, maxX, 512, maxZ);
	}

	public static LegacyBox empty() {
		return new LegacyBox(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
	}

	/** getComponentToAddBoundingBox: a box of the given size at an offset from (x, y, z), turned to face {@code direction}. */
	public static LegacyBox oriented(int x, int y, int z, int offsetX, int offsetY, int offsetZ, int width, int height, int depth, int direction) {
		return switch (direction) {
			case 1 -> new LegacyBox(x - depth + 1 + offsetZ, y + offsetY, z + offsetX, x + offsetZ, y + height - 1 + offsetY, z + width - 1 + offsetX);
			case 2 -> new LegacyBox(x + offsetX, y + offsetY, z - depth + 1 + offsetZ, x + width - 1 + offsetX, y + height - 1 + offsetY, z + offsetZ);
			case 3 -> new LegacyBox(x + offsetZ, y + offsetY, z + offsetX, x + depth - 1 + offsetZ, y + height - 1 + offsetY, z + width - 1 + offsetX);
			default -> new LegacyBox(x + offsetX, y + offsetY, z + offsetZ, x + width - 1 + offsetX, y + height - 1 + offsetY, z + depth - 1 + offsetZ);
		};
	}

	public boolean intersects(LegacyBox other) {
		return maxX >= other.minX && minX <= other.maxX && maxZ >= other.minZ && minZ <= other.maxZ && maxY >= other.minY && minY <= other.maxY;
	}

	public boolean intersectsXZ(int minX, int minZ, int maxX, int maxZ) {
		return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
	}

	public void expandTo(LegacyBox other) {
		minX = Math.min(minX, other.minX);
		minY = Math.min(minY, other.minY);
		minZ = Math.min(minZ, other.minZ);
		maxX = Math.max(maxX, other.maxX);
		maxY = Math.max(maxY, other.maxY);
		maxZ = Math.max(maxZ, other.maxZ);
	}

	public void offset(int x, int y, int z) {
		minX += x;
		minY += y;
		minZ += z;
		maxX += x;
		maxY += y;
		maxZ += z;
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
	}

	public int xSpan() {
		return maxX - minX + 1;
	}

	public int ySpan() {
		return maxY - minY + 1;
	}

	public int zSpan() {
		return maxZ - minZ + 1;
	}

	public int centerX() {
		return minX + (maxX - minX + 1) / 2;
	}

	public int centerY() {
		return minY + (maxY - minY + 1) / 2;
	}

	public int centerZ() {
		return minZ + (maxZ - minZ + 1) / 2;
	}

	@Override
	public String toString() {
		return "(" + minX + ", " + minY + ", " + minZ + "; " + maxX + ", " + maxY + ", " + maxZ + ")";
	}
}
