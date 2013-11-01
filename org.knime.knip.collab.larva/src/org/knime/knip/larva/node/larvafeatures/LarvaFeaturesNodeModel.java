package org.knime.knip.larva.node.larvafeatures;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.knime.core.node.port.PortType;

/**
 * Node Model for node LarvaFeatures. It contains methods for computing features
 * describing movement, posture and orientation of a larva.
 * 
 * @author wildnerm, University of Konstanz
 */

public class LarvaFeaturesNodeModel extends NodeModel {

	/**
	 * Settings Models to communicate with the node dialog
	 */
	private SettingsModelString m_larvaHeadXColumnSelection = createLarvaHeadXColumnModel();
	private SettingsModelString m_larvaHeadYColumnSelection = createLarvaHeadYColumnModel();
	private SettingsModelString m_larvaTailXColumnSelection = createLarvaTailXColumnModel();
	private SettingsModelString m_larvaTailYColumnSelection = createLarvaTailYColumnModel();
	private SettingsModelString m_larvaCenterXColumnSelection = createLarvaCenterXColumnModel();
	private SettingsModelString m_larvaCenterYColumnSelection = createLarvaCenterYColumnModel();
	private SettingsModelString m_larvaCentroidXColumnSelection = createLarvaCentroidXColumnModel();
	private SettingsModelString m_larvaCentroidYColumnSelection = createLarvaCentroidYColumnModel();
	private SettingsModelString m_larvaTimeColumnSelection = createLarvaTimeColumnModel();
	private SettingsModelBoolean m_sortAccordingToTimeSelection = createSortAccordingToTimeSelectionModel();
	private SettingsModelBoolean m_computeDistanceToLeftSelection = createComputeDistanceToLeftSelectionModel();

	protected LarvaFeaturesNodeModel() {
		super(new PortType[] { new PortType(BufferedDataTable.class),
				new PortType(BufferedDataTable.class, true) },
				new PortType[] { new PortType(BufferedDataTable.class) });
	}

	/**
	 * Names of the new columns.
	 */
	private String[] m_newColNames = new String[] { "alphaAbs", "alpha (body angle)",
			"theta (head angle)", "reorientSpeed", "v_centroid", "v_head", "v_center", "distToContainer", "length"};

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		DataTableSpec inSpecOne = inSpecs[0];
		
		return new DataTableSpec[] { createOutSpec(inSpecOne) };
	}

	/**
	 * Execution of the node. Calculates the larva features.
	 */
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {

		if (inData[0] == null) {
			return inData;
		}
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
		colPos[0] = inDataSpec.findColumnIndex(m_larvaHeadXColumnSelection
				.getStringValue());
		colPos[1] = inDataSpec.findColumnIndex(m_larvaHeadYColumnSelection
				.getStringValue());
		colPos[2] = inDataSpec.findColumnIndex(m_larvaTailXColumnSelection
				.getStringValue());
		colPos[3] = inDataSpec.findColumnIndex(m_larvaTailYColumnSelection
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

		boolean isSecondTableEmpty = true;
		RowIterator secondTableIterator;
		Point2D containerPos = new Point2D.Double();
		if (inData[1] != null && (inData[1].getRowCount() == 1 || inData[1].getRowCount() == 2)) {
			isSecondTableEmpty = false;
//			String[] colNamesWithContainerDistance = new String[m_newColNames.length + 1];
//			for (int i = 0; i < m_newColNames.length; i++) {
//				colNamesWithContainerDistance[i] = m_newColNames[i];
//			}
//			colNamesWithContainerDistance[m_newColNames.length] = "distToContainer";
//			m_newColNames = colNamesWithContainerDistance;
			secondTableIterator = inData[1].iterator();
			DataRow currRow = secondTableIterator.next();
			if(!m_computeDistanceToLeftSelection.getBooleanValue() && inData[1].getRowCount() == 2) {
				currRow = secondTableIterator.next();
			}
			containerPos.setLocation(
					((DoubleCell) currRow.getCell(0)).getDoubleValue(),
					((DoubleCell) currRow.getCell(1)).getDoubleValue());
		}

		// number of incoming columns
		int numColIn = inDataSpec.getNumColumns();
		// number of outgoing columns
		int numColOut = numColIn + m_newColNames.length;
		// System.out.println("numColumns: " + numColumns);
		int ctr = 0;

		DataTableSpec outSpec = createOutSpec(inDataSpec);
		BufferedDataContainer container = exec.createDataContainer(outSpec,
				true);

		Point2D prevHead = new Point2D.Double();
		Point2D prevTail = new Point2D.Double();
		Point2D prevCenter = new Point2D.Double();
		Point2D prevCentroid = new Point2D.Double();
		double prevAlphaAbs = 0;
		double prevAlphaRel = 0;
		double prevTime = 0;

		DataRow currRow = null;

		while (featureVectorIterator.hasNext()) {
			currRow = featureVectorIterator.next();

			DataRow outRow = null;
			DataCell[] cells = new DataCell[numColOut];
			for (int i = 0; i < numColIn; i++) {
				cells[i] = currRow.getCell(i); // default: transfer other
												// columns
			}
			Point2D currHead = new Point2D.Double();
			Point2D currTail = new Point2D.Double();
			Point2D currCenter = new Point2D.Double();
			Point2D currCentroid = new Point2D.Double();
			currHead.setLocation(
					((DoubleValue) currRow.getCell(colPos[0])).getDoubleValue(),
					((DoubleValue) currRow.getCell(colPos[1])).getDoubleValue());
			currTail.setLocation(
					((DoubleValue) currRow.getCell(colPos[2])).getDoubleValue(),
					((DoubleValue) currRow.getCell(colPos[3])).getDoubleValue());
			currCenter
					.setLocation(((DoubleValue) currRow.getCell(colPos[4]))
							.getDoubleValue(), ((DoubleValue) currRow
							.getCell(colPos[5])).getDoubleValue());
			currCentroid
					.setLocation(((DoubleValue) currRow.getCell(colPos[6]))
							.getDoubleValue(), ((DoubleValue) currRow
							.getCell(colPos[7])).getDoubleValue());
			double currTime = ((DoubleValue) currRow.getCell(colPos[8]))
					.getDoubleValue();
			if (ctr == 0) {
				prevTime = currTime;
			}

			// calculation of absolute alpha
			double currAlphaAbs = computeAngleAbs(currTail, currCenter);
			cells[numColIn] = new DoubleCell(currAlphaAbs);

			// calculation of alpha (body angle)
			double alphaTemp = computeAngleDiff(prevAlphaAbs, currAlphaAbs);
			double alphaRel = prevAlphaRel + alphaTemp;
			cells[numColIn + 1] = new DoubleCell(alphaRel);

			// calculation of theta (head angle)
			// double angleBodyAxes = computeAngleAbs(currTail, currCenter);
			double angleHeadAxes = computeAngleAbs(currCenter, currHead);
			double theta = computeAngleDiff(currAlphaAbs, angleHeadAxes);
			cells[numColIn + 2] = new DoubleCell(theta);

			// calculation of reorientation speed
			double dTime = currTime - prevTime;
			double reoSpeed = 0;
			if (dTime != 0) {
				reoSpeed = Math.abs((alphaRel - prevAlphaRel)) / dTime;
			}
			cells[numColIn + 3] = new DoubleCell(reoSpeed);

			// calculation of v_centroid (locomotion speed)
			double vCentroid = 0;
			if (dTime != 0) {
				vCentroid = currCentroid.distance(prevCentroid) / dTime;
			}
			cells[numColIn + 4] = new DoubleCell(vCentroid);

			// calculation of v_head (head speed)
			double vHead = 0;
			if (dTime != 0) {
				vHead = currHead.distance(prevHead) / dTime;
			}
			cells[numColIn + 5] = new DoubleCell(vHead);

			// calculation of v_center (speed of the center of the larva)
			double vCenter = 0;
			if (dTime != 0) {
				vCenter = currCenter.distance(prevCenter);
			}
			cells[numColIn + 6] = new DoubleCell(vCenter);
			
			// calculation of the distance between larva and container
			double distToContainer = -1;
			if (!isSecondTableEmpty) {
				distToContainer = currCenter.distance(containerPos);
			}
			cells[numColIn + 7] = new DoubleCell(distToContainer);
			
			// calculation of the length of the larva; try to minimize the error
			double lengthLarva = currTail.distance(currCenter) + currCenter.distance(currHead) + 2;
			cells[numColIn + 8] = new DoubleCell(lengthLarva);

			outRow = new DefaultRow(currRow.getKey(), cells);
			container.addRowToTable(outRow);

			prevHead.setLocation(currHead);
			prevTail.setLocation(currTail);
			prevCenter.setLocation(currCenter);
			prevCentroid.setLocation(currCentroid);
			prevTime = currTime;
			if (currAlphaAbs != java.lang.Double.POSITIVE_INFINITY) {
				prevAlphaAbs = currAlphaAbs;
			}
			prevAlphaRel = alphaRel;
			exec.setProgress(ctr++ / numRows);
		}

		container.close();

		return new BufferedDataTable[] { container.getTable() };
	}

	/**
	 * Calculates the absolute angle of the larva orientation in comparison to
	 * the x-axis. The value is between -180 and 180.
	 * 
	 * @param fixedPoint
	 *            point of comparison
	 * @param movablePoint
	 *            point to which the angle should be calculated
	 * @return absolute angle between the points and the x-axis
	 */
	private double computeAngleAbs(Point2D fixedPoint, Point2D movablePoint) {
		double num = fixedPoint.getY() - movablePoint.getY();
		double denom = movablePoint.getX() - fixedPoint.getX();
		double angleAbs;
		if (denom == 0) {
			if (num < 0) {
				angleAbs = -90;
			} else if (num > 0) {
				angleAbs = 90;
			} else {
				angleAbs = java.lang.Double.POSITIVE_INFINITY;
			}
		} else {
			if (denom < 0) {
				if (num < 0) {
					angleAbs = -180 + Math.toDegrees((Math.atan(num / denom)));
				} else {
					angleAbs = 180 + Math.toDegrees((Math.atan(num / denom)));
				}
			} else {
				angleAbs = Math.toDegrees((Math.atan(num / denom)));
			}
		}
		return angleAbs;
	}

	/**
	 * Calculates the difference between two angles
	 * 
	 * @param fixedAngle
	 *            angle of comparison
	 * @param movableAngle
	 *            second angle
	 * @return difference between the two angles starting at the fixed angle
	 */
	private double computeAngleDiff(double fixedAngle, double movableAngle) {
		if (fixedAngle == java.lang.Double.POSITIVE_INFINITY
				|| movableAngle == java.lang.Double.POSITIVE_INFINITY) {
			return 0;
		} else {
			double tempFixed = (fixedAngle + 360) % 360;
			double tempMovable = (movableAngle + 360) % 360;
			double diff = tempMovable - tempFixed;
			if (diff > 180) {
				return -360 + diff;
			} else if (diff < -180) {
				return 360 + diff;
			} else {
				return diff;
			}
		}
	}

	/**
	 * Creates a new data table spec because new columns should be added
	 * 
	 * @param inSpec
	 *            incoming data table spec
	 * @return new data table spec containing the incoming plus the added
	 *         columns
	 */
	private DataTableSpec createOutSpec(DataTableSpec inSpec) {
		int numColIn = inSpec.getNumColumns();
		int numColOut = numColIn + m_newColNames.length;

		DataColumnSpec[] colSpecs = new DataColumnSpec[numColOut];
		for (int i = 0; i < numColIn; i++) {
			colSpecs[i] = inSpec.getColumnSpec(i);
		}
		for (int j = 0; j < m_newColNames.length; j++) {
			colSpecs[numColIn + j] = new DataColumnSpecCreator(m_newColNames[j],
					DoubleCell.TYPE).createSpec();
		}
		// create data table spec using column specs
		return new DataTableSpec(colSpecs);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_larvaHeadXColumnSelection.saveSettingsTo(settings);
		m_larvaHeadYColumnSelection.saveSettingsTo(settings);
		m_larvaTailXColumnSelection.saveSettingsTo(settings);
		m_larvaTailYColumnSelection.saveSettingsTo(settings);
		m_larvaCenterXColumnSelection.saveSettingsTo(settings);
		m_larvaCenterYColumnSelection.saveSettingsTo(settings);
		m_larvaCentroidXColumnSelection.saveSettingsTo(settings);
		m_larvaCentroidYColumnSelection.saveSettingsTo(settings);
		m_larvaTimeColumnSelection.saveSettingsTo(settings);
		m_sortAccordingToTimeSelection.saveSettingsTo(settings);
		m_computeDistanceToLeftSelection.saveSettingsTo(settings);
		// m_mergeEqualTimeSelection.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_larvaHeadXColumnSelection.validateSettings(settings);
		m_larvaHeadYColumnSelection.validateSettings(settings);
		m_larvaTailXColumnSelection.validateSettings(settings);
		m_larvaTailYColumnSelection.validateSettings(settings);
		m_larvaCenterXColumnSelection.validateSettings(settings);
		m_larvaCenterYColumnSelection.validateSettings(settings);
		m_larvaCentroidXColumnSelection.validateSettings(settings);
		m_larvaCentroidYColumnSelection.validateSettings(settings);
		m_larvaTimeColumnSelection.validateSettings(settings);
		m_sortAccordingToTimeSelection.validateSettings(settings);
		m_computeDistanceToLeftSelection.validateSettings(settings);
		// m_mergeEqualTimeSelection.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_larvaHeadXColumnSelection.loadSettingsFrom(settings);
		m_larvaHeadYColumnSelection.loadSettingsFrom(settings);
		m_larvaTailXColumnSelection.loadSettingsFrom(settings);
		m_larvaTailYColumnSelection.loadSettingsFrom(settings);
		m_larvaCenterXColumnSelection.loadSettingsFrom(settings);
		m_larvaCenterYColumnSelection.loadSettingsFrom(settings);
		m_larvaCentroidXColumnSelection.loadSettingsFrom(settings);
		m_larvaCentroidYColumnSelection.loadSettingsFrom(settings);
		m_larvaTimeColumnSelection.loadSettingsFrom(settings);
		m_sortAccordingToTimeSelection.loadSettingsFrom(settings);
		m_computeDistanceToLeftSelection.loadSettingsFrom(settings);
		// m_mergeEqualTimeSelection.loadSettingsFrom(settings);
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
	protected static SettingsModelString createLarvaHeadXColumnModel() {
		return new SettingsModelString("larva_head_x_column_selection",
				"Head X");
	}

	protected static SettingsModelString createLarvaHeadYColumnModel() {
		return new SettingsModelString("larva_head_y_column_selection",
				"Head Y");
	}

	protected static SettingsModelString createLarvaTailXColumnModel() {
		return new SettingsModelString("larva_tail_x_column_selection",
				"Tail X");
	}

	protected static SettingsModelString createLarvaTailYColumnModel() {
		return new SettingsModelString("larva_tail_y_column_selection",
				"Tail Y");
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
		return new SettingsModelString("larva_time_column_selection", "Time");
	}

	protected static SettingsModelBoolean createSortAccordingToTimeSelectionModel() {
		return new SettingsModelBoolean("sort_according_to_time_selection",
				true);
	}

	public static SettingsModelBoolean createComputeDistanceToLeftSelectionModel() {
		return new SettingsModelBoolean("compute_distance_to_left",
				true);
	}

}