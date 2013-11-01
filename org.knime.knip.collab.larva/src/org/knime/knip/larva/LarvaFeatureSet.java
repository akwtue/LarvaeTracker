package org.knime.knip.larva;

import java.util.ArrayList;
import java.util.BitSet;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.type.logic.BitType;

import org.knime.knip.core.features.FeatureSet;
import org.knime.knip.core.features.FeatureTargetListener;

/**
 * Calculation of larva segment features like positions of larva ends and
 * center.
 * 
 * @author Manuel Wildner, University of Konstanz
 * 
 */

public class LarvaFeatureSet implements FeatureSet {

	/**
	 * Stores the feature names.
	 */
	public static String[] FEATURE_NAMES = new String[] { "End1 X", "End1 Y",
			"End2 X", "End2 Y", "Center X", "Center Y" };

	private final BitSet m_enabled = new BitSet();

	/**
	 * Stores the interval to analyze.
	 */
	private IterableInterval<BitType> m_interval;
	private long[] minPos;

	/**
	 * Positions of larva end one.
	 */
	private int[] m_larvaEnd1Pos = new int[2];
	/**
	 * Positions of larva end two.
	 */
	private int[] m_larvaEnd2Pos = new int[2];
	/**
	 * Positions of larva center.
	 */
	private int[] m_larvaCenterPos = new int[2];

	/**
	 * Calculates the positions of the larva points.
	 * 
	 * @param interval
	 *            the interval to analyze
	 */
	@FeatureTargetListener
	public void iiUpdated(IterableInterval<BitType> interval) {

		m_interval = interval;

		minPos = new long[m_interval.numDimensions()];
		m_interval.min(minPos);

		int activeDims = 0;
		for (int d = 0; d < m_interval.numDimensions(); d++) {
			if (m_interval.dimension(d) > 1) {
				activeDims++;
			}
		}

		if (activeDims > 2) {
			System.out
					.println("Larva features can only be calculated on 2-dimensional Bit Masks. Dims: "
							+ activeDims);
		} else if (m_enabled.cardinality() > 0) {
			int[][] skeleton = getSkeleton(m_interval);
			if (m_enabled.get(0, 3).cardinality() > 0) {
				int[][] larvaEnds = getLarvaEnds(skeleton);
				m_larvaEnd1Pos = larvaEnds[0];
				m_larvaEnd2Pos = larvaEnds[1];
			}
			if (m_enabled.get(4)) {
				m_larvaCenterPos = skeleton[(int) Math
						.floor(skeleton.length / 2)];
			}
		}
	}

	@Override
	public double value(int id) {
		switch (id) {
		case 0:
			return m_larvaEnd1Pos[0] + 1;
		case 1:
			return m_larvaEnd1Pos[1] + 1;
		case 2:
			return m_larvaEnd2Pos[0] + 1;
		case 3:
			return m_larvaEnd2Pos[1] + 1;
		case 4:
			return m_larvaCenterPos[0] + 1;
		case 5:
			return m_larvaCenterPos[1] + 1;

		default:
			return Double.NaN;
		}
	}

	@Override
	public void enable(int id) {
		m_enabled.set(id);
	}

	@Override
	public String name(int id) {
		return FEATURE_NAMES[id];
	}

	@Override
	public int numFeatures() {
		return FEATURE_NAMES.length;
	}

	@Override
	public String featureSetId() {
		return "Larva Features Set";
	}

	/**
	 * Sorts the points of a thinned larva beginning at one end.
	 * 
	 * @param interval
	 *            interval containing the points of the thinned larva
	 * @return sorted skeleton points
	 */
	private int[][] getSkeleton(IterableInterval<BitType> interval) {

		// stores all white points (larva)
		ArrayList<int[]> whitePoints = new ArrayList<int[]>();

		Cursor<BitType> c = interval.localizingCursor();
		BitType val = c.get();
		int[] pos = new int[c.numDimensions()];
		while (c.hasNext()) {
			c.fwd();
			if (val.get() == false) {
				c.localize(pos);
				int[] currentPos = new int[2];
				currentPos[0] = pos[0];
				currentPos[1] = pos[1];
				whitePoints.add(currentPos);
			} else {
				System.out.println("background value");
			}
		}
		if (whitePoints.isEmpty()) {
			int[][] emptySkeleton = new int[2][2];
			emptySkeleton[0][0] = 0;
			emptySkeleton[0][1] = 0;
			emptySkeleton[1][0] = 0;
			emptySkeleton[1][1] = 0;
			return emptySkeleton;
		}
		if (whitePoints.size() == 1) {
			int[][] oneElementSkeleton = new int[2][2];
			int[] point = whitePoints.get(0);
			oneElementSkeleton[0][0] = point[0];
			oneElementSkeleton[0][1] = point[1];
			oneElementSkeleton[1][0] = point[0];
			oneElementSkeleton[1][1] = point[1];
			return oneElementSkeleton;
		}

		int[][] skeleton = new int[whitePoints.size()][2];

		// find larvaEnd
		for (int i = 0; i < whitePoints.size(); i++) {
			if (isLarvaEnd(whitePoints, i)) {
				skeleton[0] = whitePoints.get(i);
				// System.out.println("larvaEnd found: " +
				// skeleton[0][0] + ", "
				// + skeleton[0][1]);
				whitePoints.remove(i);
				break;
			}
		}

		// sort all white points, start at larvaEnd
		for (int k = 0; k < skeleton.length - 1; k++) {
			if (!whitePoints.isEmpty()) {
				int neighbourIndex = getNeighbourIndex(whitePoints, skeleton[k]);
				if (neighbourIndex < 0) {
					break;
				} else {
					skeleton[k + 1] = whitePoints.get(neighbourIndex);
					whitePoints.remove(neighbourIndex);
				}
			}
		}
		return skeleton;
	}

	/**
	 * Checks if a point is a larva end.
	 * 
	 * @param whitePoints
	 *            points of the larva
	 * @param index
	 *            position of the point to check
	 * @return true if point is a larva end, false if not
	 */
	private boolean isLarvaEnd(ArrayList<int[]> whitePoints, int index) {
		int countNeighbours = 0;
		int[] c = whitePoints.get(index);
		for (int i = 0; i < whitePoints.size(); i++) {
			int[] t = whitePoints.get(i);
			if (t != c
					&& (t[0] == c[0] + 1 || t[0] == c[0] - 1 || t[0] == c[0])
					&& (t[1] == c[1] + 1 || t[1] == c[1] - 1 || t[1] == c[1])) {
				countNeighbours++;
			}
		}
		if (countNeighbours == 1) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the position of the neighbored point.
	 * 
	 * @param whitePoints
	 *            points of the larva
	 * @param c
	 *            point to check for a neighbor
	 * @return position of the neighbor, -1 if no neighbor is in range
	 */
	private int getNeighbourIndex(ArrayList<int[]> whitePoints, int[] c) {
		for (int i = 0; i < whitePoints.size(); i++) {
			int[] t = whitePoints.get(i);
			if (t != c
					&& (t[0] == c[0] + 1 || t[0] == c[0] - 1 || t[0] == c[0])
					&& (t[1] == c[1] + 1 || t[1] == c[1] - 1 || t[1] == c[1])) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Get the position of both ends of a larva skeleton.
	 * 
	 * @param skeleton sorted points of a larva
	 * @return the position of both ends of a larva
	 */
	private int[][] getLarvaEnds(int[][] skeleton) {
		// front end and back end of the larva [end ID][0/1 for x/y
		// coordinate]
		int[][] larvaEnds = new int[2][2];
		larvaEnds[0] = skeleton[0];
		larvaEnds[1] = skeleton[skeleton.length - 1];
		return larvaEnds;
	}

}
