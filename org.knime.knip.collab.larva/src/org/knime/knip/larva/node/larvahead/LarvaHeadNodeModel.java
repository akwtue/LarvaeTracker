package org.knime.knip.larva.node.larvahead;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node Model for node LarvaHead. It contains methods for sorting the table,
 * merging rows keeping equal time values, aligning the larva points to the
 * previous image and finding the head of the larva.
 * 
 * @author wildnerm, University of Konstanz
 */

public class LarvaHeadNodeModel extends NodeModel{

	/**
	 * Settings Models to communicate with the node dialog
	 */
	private SettingsModelString m_larvaEndOneXColumnSelection = createLarvaEndOneXColumnModel();
	private SettingsModelString m_larvaEndOneYColumnSelection = createLarvaEndOneYColumnModel();
	private SettingsModelString m_larvaEndTwoXColumnSelection = createLarvaEndTwoXColumnModel();
	private SettingsModelString m_larvaEndTwoYColumnSelection = createLarvaEndTwoYColumnModel();
	private SettingsModelString m_larvaCenterXColumnSelection = createLarvaCenterXColumnModel();
	private SettingsModelString m_larvaCenterYColumnSelection = createLarvaCenterYColumnModel();
	private SettingsModelString m_larvaCentroidXColumnSelection = createLarvaCentroidXColumnModel();
	private SettingsModelString m_larvaCentroidYColumnSelection = createLarvaCentroidYColumnModel();
	private SettingsModelString m_larvaTimeColumnSelection = createLarvaTimeColumnModel();
	private SettingsModelBoolean m_sortAccordingToTimeSelection = createSortAccordingToTimeSelectionModel();
	private SettingsModelBoolean m_mergeEqualTimeSelection = createMergeEqualTimeSelectionModel();

	protected LarvaHeadNodeModel() {
		super(1, 1);
	}

	/**
	 * DataTableSpec
	 */
	private DataTableSpec m_outSpec;

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		return null;

	}

	/**
	 * Execution of the node. Reads the incoming table and returns it after
	 * finding the head of the larva. Sorting the table by time and merging rows
	 * containing equal time values are optional.
	 * 
	 * @param inData
	 *            incoming data table
	 * @return the processed data table
	 */
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {

		int numRows = inData[0].getRowCount();
		if (numRows < 2) {
			return inData;
		}

		// stores meta data about the table
		DataTableSpec inDataSpec = inData[0].getDataTableSpec();
		/*
		 * stores the positions of needed columns. order of column positions:
		 * EndOneX, EndOneY, EndTwoX, EndTwoY, CenterX, CenterY, Time
		 */
		int[] colPos = new int[9];
		colPos[0] = inDataSpec.findColumnIndex(m_larvaEndOneXColumnSelection
				.getStringValue());
		colPos[1] = inDataSpec.findColumnIndex(m_larvaEndOneYColumnSelection
				.getStringValue());
		colPos[2] = inDataSpec.findColumnIndex(m_larvaEndTwoXColumnSelection
				.getStringValue());
		colPos[3] = inDataSpec.findColumnIndex(m_larvaEndTwoYColumnSelection
				.getStringValue());
		colPos[4] = inDataSpec.findColumnIndex(m_larvaCenterXColumnSelection
				.getStringValue());
		colPos[5] = inDataSpec.findColumnIndex(m_larvaCenterYColumnSelection
				.getStringValue());
		colPos[6] = inDataSpec.findColumnIndex(m_larvaCentroidXColumnSelection
				.getStringValue());
		colPos[7] = inDataSpec.findColumnIndex(m_larvaCentroidYColumnSelection
				.getStringValue());
		colPos[8] = inDataSpec.findColumnIndex(m_larvaTimeColumnSelection
				.getStringValue());

		// checks if a needed column is missing
		for (int i = 0; i < colPos.length; i++) {
			if (colPos[i] == -1) {
				throw new InvalidSettingsException(
						"At least one column is missing! Check selection!");
			}
			for (int j = i + 1; j < colPos.length; j++) {
				if (colPos[i] == colPos[j]) {
					throw new InvalidSettingsException(
							"Multiple selection of a column is not allowed! Choose another one!");
				}
			}
		}

		RowIterator featureVectorIterator;
		// sort table by time if selected in the dialog
		if (m_sortAccordingToTimeSelection.getBooleanValue()) {
			List<String> sortColumns = new ArrayList<String>();
			sortColumns.add(m_larvaTimeColumnSelection.getStringValue());
			boolean m_sortInMemory = false;
			BufferedDataTableSorter tableSorter = new BufferedDataTableSorter(
					inData[0], sortColumns, new boolean[] { true }, true);
			tableSorter.setSortInMemory(m_sortInMemory);
			BufferedDataTable sortedTable = tableSorter.sort(exec);
			featureVectorIterator = sortedTable.iterator();
		} else {
			featureVectorIterator = inData[0].iterator();
		}

		// number of columns
		int numColumns = inDataSpec.getNumColumns();
		int ctr = 0;

		BufferedDataContainer container = exec.createDataContainer(inDataSpec,
				true);

		DataRow prevRow = null;
		DataRow currRow = featureVectorIterator.next();
		DataRow nextRow = null;

		// 2-dimensional "table" for distinguishing head from tail by analyzing
		// the movement
		ArrayList<double[]> distTable = new ArrayList<double[]>();
		// interval of the rows which should be used for analyzing the movement
		int startRow = 0 - 1;
		int endRow = Math.min(numRows, (50 + startRow + 2));

		while (currRow != null) {
			if (featureVectorIterator.hasNext()) {
				nextRow = featureVectorIterator.next();
				// try to merge rows if selected in the dialog
				if (m_mergeEqualTimeSelection.getBooleanValue()) {
					if (((DoubleValue) currRow.getCell(colPos[8]))
							.getDoubleValue() == ((DoubleValue) nextRow
							.getCell(colPos[8])).getDoubleValue()) {
						DataRow mergedRow = mergeRows(currRow, nextRow, colPos);
						currRow = mergedRow;
						numRows--;
						continue;
					}
				}
			} else {
				nextRow = null;
			}

			/*
			 * The two ends of the larva are stored in two columns. Here the
			 * Ends are aligned to the previous row.
			 */
			boolean swapEnds = false;
			if (prevRow != null) {
				swapEnds = isSwapNeeded(prevRow, currRow, colPos);
			}
			DataRow outRow;
			if (swapEnds) {
				DataCell[] cells = new DataCell[numColumns];
				// if row is inside the interval, store it for analyzing the
				// movement
				if (startRow < ctr && ctr < endRow) {
					double[] rowToStore = new double[6];
					for (int i = 0; i < cells.length; i++) {
						if (i == colPos[0]) {
							cells[i] = currRow.getCell(colPos[2]);
							rowToStore[0] = ((DoubleValue) cells[i])
									.getDoubleValue();
						} else if (i == colPos[1]) {
							cells[i] = currRow.getCell(colPos[3]);
							rowToStore[1] = ((DoubleValue) cells[i])
									.getDoubleValue();
						} else if (i == colPos[2]) {
							cells[i] = currRow.getCell(colPos[0]);
							rowToStore[2] = ((DoubleValue) cells[i])
									.getDoubleValue();
						} else if (i == colPos[3]) {
							cells[i] = currRow.getCell(colPos[1]);
							rowToStore[3] = ((DoubleValue) cells[i])
									.getDoubleValue();
						} else
							cells[i] = currRow.getCell(i); // ##### default:
															// transfer other
															// columns. is this
															// needed? #####
					}
					rowToStore[4] = ((DoubleValue) currRow.getCell(colPos[4]))
							.getDoubleValue();
					rowToStore[5] = ((DoubleValue) currRow.getCell(colPos[5]))
							.getDoubleValue();
					distTable.add(rowToStore);
				} else {
					for (int i = 0; i < cells.length; i++) {
						if (i == colPos[0]) {
							cells[i] = currRow.getCell(colPos[2]);
						} else if (i == colPos[1]) {
							cells[i] = currRow.getCell(colPos[3]);
						} else if (i == colPos[2]) {
							cells[i] = currRow.getCell(colPos[0]);
						} else if (i == colPos[3]) {
							cells[i] = currRow.getCell(colPos[1]);
						} else
							cells[i] = currRow.getCell(i); // ##### default:
															// transfer other
															// columns. is this
															// needed? #####
					}
				}
				outRow = new DefaultRow(currRow.getKey(), cells);

			} else {
				outRow = currRow;

				// if row is inside the interval, store it for analyzing the
				// movement
				if (startRow < ctr && ctr < endRow) {
					double[] rowToStore = new double[6];
					for (int i = 0; i < 6; i++) {
						rowToStore[i] = ((DoubleValue) currRow
								.getCell(colPos[i])).getDoubleValue();
					}
					distTable.add(rowToStore);
				}
			}

			container.addRowToTable(outRow);
			prevRow = outRow;
			currRow = nextRow;
			exec.setProgress(ctr++ / numRows);
		}
		System.out.println("dist table size: " + distTable.size());

		container.close();

		// stores which end is the larva head
		boolean isHeadEqualToEndOne = isHeadEqualToEndOne(distTable);

		// creation of new column specs renaming the columns of larva ends
		DataColumnSpec[] colSpecs = new DataColumnSpec[numColumns];
		for (int i = 0; i < numColumns; i++) {
			if (i == colPos[0]) {
				String newColName = isHeadEqualToEndOne ? "Head X" : "Tail X";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[1]) {
				String newColName = isHeadEqualToEndOne ? "Head Y" : "Tail Y";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[2]) {
				String newColName = isHeadEqualToEndOne ? "Tail X" : "Head X";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[3]) {
				String newColName = isHeadEqualToEndOne ? "Tail Y" : "Head Y";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[4]) {
				String newColName = "Center X";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[5]) {
				String newColName = "Center Y";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[6]) {
				String newColName = "Centroid X";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[7]) {
				String newColName = "Centroid Y";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else if (i == colPos[8]) {
				String newColName = "Time";
				colSpecs[i] = new DataColumnSpecCreator(newColName, inDataSpec
						.getColumnSpec(i).getType()).createSpec();
			} else {
				colSpecs[i] = inDataSpec.getColumnSpec(i);
			}
		}
		// create new data table spec using column specs
		m_outSpec = new DataTableSpec(colSpecs);
		// change table spec only
		BufferedDataTable outTable = exec.createSpecReplacerTable(
				container.getTable(), m_outSpec);

		return new BufferedDataTable[] { outTable };
	}

	/**
	 * Merges two rows in a naive way by calculating the mean of double values.
	 * 
	 * @param currRow
	 *            first row to merge
	 * @param nextRow
	 *            second row to merge
	 * @param colPos
	 *            positions of the columns
	 * @return the current row containing new double values
	 */
	private DataRow mergeRows(DataRow currRow, DataRow nextRow, int[] colPos) {
		DataCell[] cells = new DataCell[currRow.getNumCells()];
		for (int i = 0; i < currRow.getNumCells(); i++) {
			if (currRow.getCell(i).getType() == DoubleCell.TYPE) {
				cells[i] = new DoubleCell(
						(((DoubleValue) currRow.getCell(i)).getDoubleValue() + ((DoubleValue) nextRow
								.getCell(i)).getDoubleValue()) / 2);
			} else {
				cells[i] = currRow.getCell(i);
			}
		}
		DataRow mergedRow = new DefaultRow(currRow.getKey(), cells);
		return mergedRow;
	}

	/**
	 * Checks which point of the current row belongs to which point in the
	 * previous row.
	 * 
	 * @param prevRow
	 *            previous row
	 * @param currRow
	 *            current row
	 * @param colPos
	 *            positions of the columns
	 * @return false if the related point is inside the same column, true if not
	 */
	private boolean isSwapNeeded(DataRow prevRow, DataRow currRow, int[] colPos) {

		Point2D prevEndOne = new Point2D.Double();
		prevEndOne.setLocation(
				((DoubleValue) prevRow.getCell(colPos[0])).getDoubleValue(),
				((DoubleValue) prevRow.getCell(colPos[1])).getDoubleValue());

		Point2D prevEndTwo = new Point2D.Double();
		prevEndTwo.setLocation(
				((DoubleValue) prevRow.getCell(colPos[2])).getDoubleValue(),
				((DoubleValue) prevRow.getCell(colPos[3])).getDoubleValue());

		Point2D currEndOne = new Point2D.Double();
		currEndOne.setLocation(
				((DoubleValue) currRow.getCell(colPos[0])).getDoubleValue(),
				((DoubleValue) currRow.getCell(colPos[1])).getDoubleValue());

		Point2D currEndTwo = new Point2D.Double();
		currEndTwo.setLocation(
				((DoubleValue) currRow.getCell(colPos[2])).getDoubleValue(),
				((DoubleValue) currRow.getCell(colPos[3])).getDoubleValue());

		/*
		 * Following lines are needed if points should be shifted by the larva
		 * midpoint instead of the larva center
		 */
		// Point2D midPointPrev = new Point2D.Double();
		// midPointPrev.setLocation(
		// Math.abs((prevEndTwo.getX() + prevEndOne.getX())) / 2,
		// Math.abs((prevEndTwo.getY() + prevEndOne.getY())) / 2);
		// Point2D midPointCurr = new Point2D.Double();
		// midPointCurr.setLocation(
		// Math.abs((currEndTwo.getX() + currEndOne.getX())) / 2,
		// Math.abs((currEndTwo.getY() + currEndOne.getY())) / 2);
		//
		// prevEndOne = shiftPoint(prevEndOne, midPointPrev);
		// prevEndTwo = shiftPoint(prevEndTwo, midPointPrev);
		// currEndOne = shiftPoint(currEndOne, midPointCurr);
		// currEndTwo = shiftPoint(currEndTwo, midPointCurr);

		Point2D prevCenter = new Point2D.Double();
		Point2D currCenter = new Point2D.Double();
		prevCenter.setLocation(
				((DoubleValue) prevRow.getCell(colPos[4])).getDoubleValue(),
				((DoubleValue) prevRow.getCell(colPos[5])).getDoubleValue());
		currCenter.setLocation(
				((DoubleValue) currRow.getCell(colPos[4])).getDoubleValue(),
				((DoubleValue) currRow.getCell(colPos[5])).getDoubleValue());

		prevEndOne = shiftPoint(prevEndOne, prevCenter);
		prevEndTwo = shiftPoint(prevEndTwo, prevCenter);
		currEndOne = shiftPoint(currEndOne, currCenter);
		currEndTwo = shiftPoint(currEndTwo, currCenter);

		// distances
		double dist11 = prevEndOne.distance(currEndOne);
		double dist12 = prevEndOne.distance(currEndTwo);
		double dist21 = prevEndTwo.distance(currEndOne);
		double dist22 = prevEndTwo.distance(currEndTwo);

		/*
		 * find the minimum distance to the previous point for both points. if
		 * minimum isn't different, trust the point holding the absolute
		 * minimum.
		 */
		if (dist11 == Math.min(dist11, dist12)
				&& dist22 == Math.min(dist21, dist22)) {
			return false;
		} else if (dist12 == Math.min(dist11, dist12)
				&& dist21 == Math.min(dist21, dist22)) {
			return true;
		} else if (dist11 == Math.min(Math.min(dist11, dist12),
				Math.min(dist21, dist22))
				|| dist22 == Math.min(Math.min(dist11, dist12),
						Math.min(dist21, dist22))) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Shift a 2D point by subtracting another 2D point.
	 * 
	 * @param pointToShift point which should be shifted
	 * @param shiftValues point which should be subtracted
	 * @return the shifted point
	 */
	private Point2D shiftPoint(Point2D pointToShift, Point2D shiftValues) {
		pointToShift.setLocation(pointToShift.getX() - shiftValues.getX(),
				pointToShift.getY() - shiftValues.getY());
		return pointToShift;
	}

	/**
	 * Analyzes if end one of the larva is the head.
	 * 
	 * @param distTable table containing all rows which should be used for analyzing the movement
	 * @return true if end one is the head of the larva, false if not
	 */
	private boolean isHeadEqualToEndOne(ArrayList<double[]> distTable) {
		if (distTable.size() > 1) {
			Iterator<double[]> iter = distTable.iterator();
			double[] prevRow = iter.next();
			double[] currRow;

			Point2D prevPrevCenter = new Point2D.Double();
			Point2D prevEndOne = new Point2D.Double();
			Point2D prevEndTwo = new Point2D.Double();
			Point2D prevCenter = new Point2D.Double();
			Point2D currEndOne = new Point2D.Double();
			Point2D currEndTwo = new Point2D.Double();
			Point2D currCenter = new Point2D.Double();

			prevEndOne.setLocation(prevRow[0], prevRow[1]);
			prevEndTwo.setLocation(prevRow[2], prevRow[3]);
			prevCenter.setLocation(prevRow[4], prevRow[5]);
			prevPrevCenter.setLocation(prevCenter);

			int endOneIsTailCtr = 0;
			int endTwoIsTailCtr = 0;
			int loopCtr = 0;
			while (iter.hasNext()) {
				currRow = iter.next();
				currEndOne.setLocation(currRow[0], currRow[1]);
				currEndTwo.setLocation(currRow[2], currRow[3]);
				currCenter.setLocation(currRow[4], currRow[5]);

				double distCenters = prevCenter.distance(currCenter);
				Point2D pointOfComparison;
				// if larva moved slowly use an earlier center
				if (distCenters < 3) {
					pointOfComparison = prevPrevCenter;
				} else {
					pointOfComparison = prevCenter;
				}

				double distPrev1 = prevEndOne.distance(pointOfComparison);
				double distPrev2 = prevEndTwo.distance(pointOfComparison);
				double distCurr1 = currEndOne.distance(pointOfComparison);
				double distCurr2 = currEndTwo.distance(pointOfComparison);

				// counts how often a point is classified as tail
				if (distPrev1 - distCurr1 >= 0) {
					endOneIsTailCtr++;
				}
				if (distPrev2 - distCurr2 >= 0) {
					endTwoIsTailCtr++;
				}

				prevEndOne.setLocation(currEndOne);
				prevEndTwo.setLocation(currEndTwo);
				prevPrevCenter.setLocation(prevCenter);
				prevCenter.setLocation(currCenter);
				loopCtr++;
			}
			// checks which point is classified as tail more often
			if (endOneIsTailCtr > endTwoIsTailCtr) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_larvaEndOneXColumnSelection.saveSettingsTo(settings);
		m_larvaEndOneYColumnSelection.saveSettingsTo(settings);
		m_larvaEndTwoXColumnSelection.saveSettingsTo(settings);
		m_larvaEndTwoYColumnSelection.saveSettingsTo(settings);
		m_larvaCenterXColumnSelection.saveSettingsTo(settings);
		m_larvaCenterYColumnSelection.saveSettingsTo(settings);
		m_larvaCentroidXColumnSelection.saveSettingsTo(settings);
		m_larvaCentroidYColumnSelection.saveSettingsTo(settings);
		m_larvaTimeColumnSelection.saveSettingsTo(settings);
		m_sortAccordingToTimeSelection.saveSettingsTo(settings);
		m_mergeEqualTimeSelection.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_larvaEndOneXColumnSelection.validateSettings(settings);
		m_larvaEndOneYColumnSelection.validateSettings(settings);
		m_larvaEndTwoXColumnSelection.validateSettings(settings);
		m_larvaEndTwoYColumnSelection.validateSettings(settings);
		m_larvaCenterXColumnSelection.validateSettings(settings);
		m_larvaCenterYColumnSelection.validateSettings(settings);
		m_larvaCentroidXColumnSelection.validateSettings(settings);
		m_larvaCentroidYColumnSelection.validateSettings(settings);
		m_larvaTimeColumnSelection.validateSettings(settings);
		m_sortAccordingToTimeSelection.validateSettings(settings);
		m_mergeEqualTimeSelection.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_larvaEndOneXColumnSelection.loadSettingsFrom(settings);
		m_larvaEndOneYColumnSelection.loadSettingsFrom(settings);
		m_larvaEndTwoXColumnSelection.loadSettingsFrom(settings);
		m_larvaEndTwoYColumnSelection.loadSettingsFrom(settings);
		m_larvaCenterXColumnSelection.loadSettingsFrom(settings);
		m_larvaCenterYColumnSelection.loadSettingsFrom(settings);
		m_larvaCentroidXColumnSelection.loadSettingsFrom(settings);
		m_larvaCentroidYColumnSelection.loadSettingsFrom(settings);
		m_larvaTimeColumnSelection.loadSettingsFrom(settings);
		m_sortAccordingToTimeSelection.loadSettingsFrom(settings);
		m_mergeEqualTimeSelection.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// Nothing to do here, yet
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

	}

	/**
	 * Creation of the different Settings Models to communicate with the node
	 * dialog
	 */

	protected static SettingsModelString createLarvaEndOneXColumnModel() {
		return new SettingsModelString("larva_end_one_x_column_selection",
				"End1 X");
	}

	protected static SettingsModelString createLarvaEndOneYColumnModel() {
		return new SettingsModelString("larva_end_one_y_column_selection",
				"End1 Y");
	}

	protected static SettingsModelString createLarvaEndTwoXColumnModel() {
		return new SettingsModelString("larva_end_two_x_column_selection",
				"End2 X");
	}

	protected static SettingsModelString createLarvaEndTwoYColumnModel() {
		return new SettingsModelString("larva_end_two_y_column_selection",
				"End2 Y");
	}

	protected static SettingsModelString createLarvaCenterXColumnModel() {
		return new SettingsModelString("larva_center_x_column_selection",
				"Center X");
	}

	protected static SettingsModelString createLarvaCenterYColumnModel() {
		return new SettingsModelString("larva_center_y_column_selection",
				"Center Y");
	}

	protected static SettingsModelString createLarvaCentroidXColumnModel() {
		return new SettingsModelString("larva_centroid_x_column_selection",
				"Centroid X");
	}

	protected static SettingsModelString createLarvaCentroidYColumnModel() {
		return new SettingsModelString("larva_centroid_y_column_selection",
				"Centroid Y");
	}

	protected static SettingsModelString createLarvaTimeColumnModel() {
		return new SettingsModelString("larva_time_column_selection",
				"Centroid Time");
	}

	protected static SettingsModelBoolean createSortAccordingToTimeSelectionModel() {
		return new SettingsModelBoolean("sort_according_to_time_selection",
				true);
	}

	protected static SettingsModelBoolean createMergeEqualTimeSelectionModel() {
		return new SettingsModelBoolean("merge_equal_time_selection", true);
	}
}