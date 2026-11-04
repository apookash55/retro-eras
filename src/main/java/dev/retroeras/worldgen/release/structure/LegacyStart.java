package dev.retroeras.worldgen.release.structure;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/** 1.6.4's StructureStart: the pieces of one structure and the box around them all. */
public abstract class LegacyStart {
	protected final List<LegacyPiece> pieces = new LinkedList<>();
	protected LegacyBox box;

	public LegacyBox box() {
		return box;
	}

	public List<LegacyPiece> pieces() {
		return pieces;
	}

	/**
	 * generateStructure: places every piece that reaches into {@code area}. A piece that refuses (by returning false) is
	 * dropped, as in 1.6.4. Chunks generate on several threads, so one structure is placed by one thread at a time.
	 */
	public synchronized void place(LegacyWorld world, Random random, LegacyBox area) {
		Iterator<LegacyPiece> iterator = pieces.iterator();
		while (iterator.hasNext()) {
			LegacyPiece piece = iterator.next();
			if (piece.box().intersects(area) && !piece.place(world, random, area)) {
				iterator.remove();
			}
		}
	}

	/** updateBoundingBox. */
	protected void updateBox() {
		box = LegacyBox.empty();
		for (LegacyPiece piece : pieces) {
			box.expandTo(piece.box());
		}
	}

	/** markAvailableHeight: sinks the structure to a random depth at least {@code offset} below sea level. */
	protected void sinkBelowSeaLevel(Random random, int offset) {
		int limit = 63 - offset;
		int top = box.ySpan() + 1;
		if (top < limit) {
			top += random.nextInt(limit - top);
		}
		int shift = top - box.maxY;
		moveBy(shift);
	}

	/** setRandomHeight. */
	protected void setRandomHeight(Random random, int min, int max) {
		int range = max - min + 1 - box.ySpan();
		int y = range > 1 ? min + random.nextInt(range) : min;
		moveBy(y - box.minY);
	}

	private void moveBy(int dy) {
		box.offset(0, dy, 0);
		for (LegacyPiece piece : pieces) {
			piece.box().offset(0, dy, 0);
		}
	}

	/** isSizeableStructure: whether the structure is worth generating at all. */
	public boolean isValid() {
		return true;
	}
}
