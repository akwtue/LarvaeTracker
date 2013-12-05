/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   29 Jan 2010 (hornm): created
 */
package org.knime.knip.larva.node.viewer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import net.imglib2.type.numeric.RealType;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.StringTransformer;

/**
 * Node model for the node Larva Viewer. It extends the model for the Segment
 * Overlay Node.
 * 
 * @author dietzc, hornm, schoenenbergerf, wildnerm
 */
public class LarvaViewerNodeModel<T extends RealType<T>, L extends Comparable<L>>
		extends NodeModel implements BufferedDataTableHolder, DataProvider {

	private class ClearableTableContentModel extends TableContentModel {
		/** */
		private static final long serialVersionUID = 1L;

		// make clearCache accessible to allow clearing before the
		// content model is thrown away
		// in order to free large image resources from the java swing
		// caching strategy
		public void clearCacheBeforeClosing() {
			clearCache();
		}

	}

	enum LabelTransformVariables {
		ImgName, ImgSource, Label, LabelingName, LabelingSource, RowID;
	};

	public static final String CFG_ADJUST_VIRTUALLY = "cfg_adjust_virtually";

	public static final String CFG_EXPRESSION = "cfg_expression";

	public static final String CFG_IMG_COL = "cfg_img_col";

	public static final String CFG_LABELING_COL = "cfg_seg_col";

	public static final String CFG_SRC_IMG_ID_COL = "cfg_src_img_id_col";

	public static final int COL_IDX_IMAGE = 0;

	public static final int COL_IDX_LABELING = 1;

	public static final int COL_IDX_SINGLE_LABELING = 0;

	/*
	 * Logging
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(LarvaViewerNodeModel.class);

	public static int PORT_IMG = 0;

	public static int PORT_FEATURES = 1;

	private final SettingsModelBoolean m_adjustVirtually = new SettingsModelBoolean(
			CFG_ADJUST_VIRTUALLY, true);

	private ClearableTableContentModel m_contentModel;

	private final SettingsModelString m_expression = new SettingsModelString(
			CFG_EXPRESSION, "$" + LabelTransformVariables.Label + "$");

	private final SettingsModelString m_imgCol = new SettingsModelString(
			CFG_IMG_COL, "");

	private BufferedDataTable m_imgTable;

	// private BufferedDataTable m_larvaFeaturesTableOld;

	private BufferedDataTable m_larvaFeaturesTable;

	/**
	 * Names of the new columns.
	 */
	private String[] newColNames = new String[] { "isRunning", "headDirection",
			"headCastNumber" };

	/*
	 * stores the positions of needed columns. order of column positions: Time,
	 * theta (head angle)
	 */
	private int[] m_colPos;

	private ExecutionContext m_exec;

	private int[] m_includedColumns;
	private int[] m_includedColumnsPlot1 = new int[] { 1, 2, 6 };
	private int[] m_includedColumnsPlot2 = new int[] { 5, 6 };
	private int[] m_includedColumnsPlot3 = new int[] { 1, 2 };
	private int[] m_includedColumnsPlot4 = new int[] { 5, 6 };

	private boolean m_isDataSetToModel;

	private final SettingsModelString m_labelingCol = new SettingsModelString(
			CFG_LABELING_COL, "");

	private SettingsModelString m_larvaTimeColumnSelection = createLarvaTimeColumnModel();
	private SettingsModelString m_larvaHeadAngleColumnSelection = createLarvaHeadAngleColumnModel();
	private SettingsModelDoubleBounded m_minRunSpeedSelection = createMinRunSpeedModel();
	private SettingsModelIntegerBounded m_headCastAngleStartSelection = createHeadCastAngleStartModel();
	private SettingsModelBoolean m_detectHeadcastsWhileRunningSelection = createDetectHeadcastsWhileRunningModel();

	protected LarvaViewerNodeModel() {
		super(new PortType[] { new PortType(BufferedDataTable.class),
				new PortType(BufferedDataTable.class, true) },
				new PortType[] { new PortType(BufferedDataTable.class) });
		m_contentModel = new ClearableTableContentModel();
		m_isDataSetToModel = false;
	}

	private SettingsModelString createLarvaTimeColumnModel() {
		return new SettingsModelString("larva_time_column_selection", "Time");
	}

	private SettingsModelString createLarvaHeadAngleColumnModel() {
		return new SettingsModelString("larva_head_angle_column_selection",
				"theta (head angle)");
	}

	private SettingsModelDoubleBounded createMinRunSpeedModel() {
		return new SettingsModelDoubleBounded("min_run_speed_selection", 3.5,
				0, 99);
	}

	private SettingsModelIntegerBounded createHeadCastAngleStartModel() {
		return new SettingsModelIntegerBounded("headcast_angle_start", 25, 1,
				179);
	}

	private SettingsModelBoolean createDetectHeadcastsWhileRunningModel() {
		return new SettingsModelBoolean(
				"detect_headcasts_while_running_selection", false);
	}

	private boolean areCompatible(final LabelingValue<L> labVal,
			final ImgPlusValue<T> imgPlusVal) {
		return Arrays
				.equals(labVal.getDimensions(), imgPlusVal.getDimensions());
	}

	/**
	 * Configures the data table spec.
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		NodeUtils.autoColumnSelection(inSpecs[0], m_labelingCol,
				LabelingValue.class, this.getClass());

		NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_imgCol,
				ImgPlusValue.class);

		DataTableSpec outspec = createOutSpec(inSpecs[1]);
		return new DataTableSpec[] { outspec };
	}

	/**
	 * Executes the node. New features are computed and stored in the data
	 * table.
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		m_exec = exec;

		assert (inData != null);
		assert (inData.length >= 1);

		m_imgTable = inData[PORT_IMG];
		assert (m_imgTable != null);

		m_isDataSetToModel = true;
		m_imgTable = inData[PORT_IMG];

		int numRows = inData[PORT_FEATURES].getRowCount();
		if (numRows < 2) {
			return new BufferedDataTable[] { inData[PORT_FEATURES] };
		}

		final int imgColIdx = inData[0].getDataTableSpec().findColumnIndex(
				m_imgCol.getStringValue());
		final int labelingColIdx = inData[0].getDataTableSpec()
				.findColumnIndex(m_labelingCol.getStringValue());

		DataTableSpec inSpecOne;
		if (imgColIdx != -1) {
			inSpecOne = new DataTableSpec(DataTableSpec.createColumnSpecs(
					new String[] { "Image", "Labeling" }, new DataType[] {
							ImgPlusCell.TYPE, LabelingCell.TYPE }));
		} else {
			inSpecOne = new DataTableSpec(DataTableSpec.createColumnSpecs(
					new String[] { "Labeling" },
					new DataType[] { LabelingCell.TYPE }));
		}
		final BufferedDataContainer con = exec.createDataContainer(inSpecOne);
		final RowIterator imgIt = m_imgTable.iterator();

		DataRow row;

		if (m_imgTable.getRowCount() == 0) {
			return new BufferedDataTable[0];
		}

		int rowCount = 0;
		while (imgIt.hasNext()) {
			row = imgIt.next();

			// load
			final DataCell labCell = row.getCell(labelingColIdx);
			final DataCell imgCell = imgColIdx != -1 ? row.getCell(imgColIdx)
					: null;

			// test for missing cells
			if (labCell.isMissing()
					|| ((imgColIdx != -1) && imgCell.isMissing())) {
				LOGGER.warn("Missing cell was ignored at row " + row.getKey());
			} else {
				// process
				if (imgColIdx != -1) {
					// check compatibility
					if (areCompatible((LabelingValue<L>) labCell,
							(ImgPlusCell<T>) imgCell)
							&& !m_adjustVirtually.getBooleanValue()) {
						setWarningMessage("The dimensions are not compatible in row "
								+ row.getKey());
					}

					con.addRowToTable(new DefaultRow(row.getKey(), imgCell,
							labCell));
				} else {
					con.addRowToTable(new DefaultRow(row.getKey(), labCell));
				}

			}

			exec.checkCanceled();
			exec.setProgress((double) rowCount++ / m_imgTable.getRowCount());

		}
		con.close();
		m_contentModel.setDataTable(con.getTable());

		// stores meta data about the table
		DataTableSpec inSpecTwo = inData[PORT_FEATURES].getDataTableSpec();

		/*
		 * stores the positions of needed columns. order of column positions:
		 * Time, theta (head angle)
		 */
		m_colPos = new int[] {
				inSpecTwo.findColumnIndex(m_larvaTimeColumnSelection
						.getStringValue()),
				inSpecTwo.findColumnIndex(m_larvaHeadAngleColumnSelection
						.getStringValue()), 2, 3, 5, 6 };

		// number of incoming columns
		int numColumnsIn = inSpecTwo.getNumColumns();
		// number of outgoing columns
		int numColumnsOut = numColumnsIn + newColNames.length;
		int ctr = 0;

		DataTableSpec outSpecTwo = createOutSpec(inData[PORT_FEATURES]
				.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outSpecTwo,
				true);

		double prevTime = 0;
		int prevHeadDirection = 0;
		int headCastNumber = 0;
		RowIterator larvaFeaturesIterator = inData[PORT_FEATURES].iterator();

		while (larvaFeaturesIterator.hasNext()) {
			DataRow currRow = larvaFeaturesIterator.next();

			DataCell[] cells = new DataCell[numColumnsOut];
			for (int i = 0; i < numColumnsIn; i++) {
				cells[i] = currRow.getCell(i); // default: transfer other
												// columns
			}
			double currTime = ((DoubleValue) currRow.getCell(m_colPos[0]))
					.getDoubleValue();
			if (ctr == 0) {
				prevTime = currTime;
			} else if (currTime == prevTime) {
				continue;
			}

			double runMinimumSpeed = m_minRunSpeedSelection.getDoubleValue();
			int isRunning = 0;
			if (((DoubleValue) currRow.getCell(m_colPos[5])).getDoubleValue() > runMinimumSpeed) {
				isRunning = 1;
			}
			cells[numColumnsIn + 0] = new IntCell(isRunning);

			double headCastAngleStart = m_headCastAngleStartSelection
					.getIntValue();
			double headCastAngleEndDifference = 5;
			int currHeadDirection = 0;
			double headAngle = ((DoubleValue) currRow.getCell(m_colPos[1]))
					.getDoubleValue();
			if (headAngle < -headCastAngleStart) {
				currHeadDirection = -2;
			} else if (headAngle > headCastAngleStart) {
				currHeadDirection = 2;
			} else if ((prevHeadDirection == -2 || prevHeadDirection == -1)) {
				if (headAngle < -headCastAngleStart
						+ headCastAngleEndDifference) {
					currHeadDirection = -1;
				} else {
					currHeadDirection = 0;
				}
			} else if ((prevHeadDirection == 2 || prevHeadDirection == 1)) {
				if (headAngle > headCastAngleStart - headCastAngleEndDifference) {
					currHeadDirection = 1;
				} else {
					currHeadDirection = 0;
				}
			} else {
				currHeadDirection = 0;
			}
			cells[numColumnsIn + 1] = new IntCell(currHeadDirection);

			boolean detectHeadCastsWhileRunning = m_detectHeadcastsWhileRunningSelection
					.getBooleanValue();
			if (detectHeadCastsWhileRunning) {
				if (prevHeadDirection != 0 && currHeadDirection == 0) {
					headCastNumber++;
				}
			} else {
				if (isRunning == 0 && prevHeadDirection != 0
						&& currHeadDirection == 0) {
					headCastNumber++;
				}
			}
			cells[numColumnsIn + 2] = new IntCell(headCastNumber);

			DataRow outRow = new DefaultRow(currRow.getKey(), cells);
			container.addRowToTable(outRow);

			prevTime = currTime;
			prevHeadDirection = currHeadDirection;
			exec.setProgress(ctr++ / numRows);
		}

		container.close();

		m_larvaFeaturesTable = container.getTable();

		return new BufferedDataTable[] { container.getTable() };
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
		int numColOut = numColIn + newColNames.length;

		DataColumnSpec[] colSpecs = new DataColumnSpec[numColOut];
		for (int i = 0; i < numColIn; i++) {
			colSpecs[i] = inSpec.getColumnSpec(i);
		}
		for (int j = 0; j < newColNames.length; j++) {
			colSpecs[numColIn + j] = new DataColumnSpecCreator(newColNames[j],
					DoubleCell.TYPE).createSpec();
		}
		// create data table spec using column specs
		return new DataTableSpec(colSpecs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedDataTable[] getInternalTables() {

		if (m_larvaFeaturesTable != null) {
			return new BufferedDataTable[] {
					(BufferedDataTable) m_contentModel.getDataTable(),
					m_larvaFeaturesTable };
		} else {
			return new BufferedDataTable[] { (BufferedDataTable) m_contentModel
					.getDataTable() };
		}
	}

	public TableContentModel getTableContentModel() {
		// temporary workaround since setDataTable blocks
		if (!m_isDataSetToModel) {
			m_contentModel.setDataTable(m_imgTable);
			m_isDataSetToModel = true;
		}
		return m_contentModel;
	}

	public StringTransformer getTransformer() {
		return new StringTransformer(m_expression.getStringValue(), "$");
	}

	public boolean isTransformationActive() {
		// is not active == "$Label$" || ""
		final String expression = m_expression.getStringValue().trim();
		return !((expression.equalsIgnoreCase("$"
				+ LabelTransformVariables.Label + "$")) || expression.isEmpty());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_imgCol.loadSettingsFrom(settings);
		m_labelingCol.loadSettingsFrom(settings);
		m_adjustVirtually.loadSettingsFrom(settings);
		m_larvaTimeColumnSelection.loadSettingsFrom(settings);
		m_larvaHeadAngleColumnSelection.loadSettingsFrom(settings);
		m_minRunSpeedSelection.loadSettingsFrom(settings);
		m_headCastAngleStartSelection.loadSettingsFrom(settings);
		m_detectHeadcastsWhileRunningSelection.loadSettingsFrom(settings);

		try {
			m_expression.loadSettingsFrom(settings);
		} catch (final Exception e) {
			//
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		m_imgTable = null;
		m_larvaFeaturesTable = null;
		m_includedColumns = null;
		m_contentModel.clearCacheBeforeClosing();
		m_contentModel = new ClearableTableContentModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_imgCol.saveSettingsTo(settings);
		m_labelingCol.saveSettingsTo(settings);
		m_adjustVirtually.saveSettingsTo(settings);
		m_expression.saveSettingsTo(settings);
		m_larvaTimeColumnSelection.saveSettingsTo(settings);
		m_larvaHeadAngleColumnSelection.saveSettingsTo(settings);
		m_minRunSpeedSelection.saveSettingsTo(settings);
		m_headCastAngleStartSelection.saveSettingsTo(settings);
		m_detectHeadcastsWhileRunningSelection.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		if ((tables.length != 1) && (tables.length != 2)) {
			throw new IllegalArgumentException();
		}

		// TODO: make workaround unnecessary
		// temporary workaround since setDataTable blocks
		// m_tableModel.setDataTable(tables[0]);
		m_imgTable = tables[0];

		if (tables.length > 1) {
			m_larvaFeaturesTable = tables[1];
			// providing data for lineplotter
			// setIncludedColumns();
			// m_includedColumns = new int[] { 1, 2, 3, 4, 5 };
			// DataTable linePlotterFeatureTable = new FilterColumnTable(
			// tables[1], getIncludedColumns());
			// m_inFeatures = new DefaultDataArray(linePlotterFeatureTable, 1,
			// tables[1].getRowCount());
		}

		// HiLiteHandler inProp = getInHiLiteHandler(INPORT);
		// m_contModel.setHiLiteHandler(inProp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_imgCol.validateSettings(settings);
		m_labelingCol.validateSettings(settings);
		m_adjustVirtually.validateSettings(settings);
		m_larvaTimeColumnSelection.validateSettings(settings);
		m_larvaHeadAngleColumnSelection.validateSettings(settings);
		m_minRunSpeedSelection.validateSettings(settings);
		m_headCastAngleStartSelection.validateSettings(settings);
		m_detectHeadcastsWhileRunningSelection.validateSettings(settings);

		try {
			m_expression.validateSettings(settings);
		} catch (final Exception e) {
			//
		}
	}

	public boolean virtuallyAdjustImgs() {
		return m_adjustVirtually.getBooleanValue();
	}

	/**
	 * Provides the data array for the line plots. The array depends on the ID
	 * of the line plot
	 * 
	 * @param numberOfPlot
	 *            the ID of the line plot
	 */
	@Override
	public DataArray getDataArray(int numberOfPlot) {
		if (getInternalTables().length > 1) {
			return new DefaultDataArray(new FilterColumnTable(
					m_larvaFeaturesTable, getIncludedColumns(numberOfPlot)), 1,
					m_larvaFeaturesTable.getRowCount());
		}
		return null;
	};

	/**
	 * Provides the included columns of each line plot.
	 * 
	 * @param numberOfPlot
	 *            ID of the line plot
	 * @return the included columns
	 */
	private int[] getIncludedColumns(int numberOfPlot) {
		switch (numberOfPlot) {
		case 1:
			return m_includedColumnsPlot1;
		case 2:
			return m_includedColumnsPlot2;
		case 3:
			return m_includedColumnsPlot3;
		case 4:
			return m_includedColumnsPlot4;
		default:
			return m_includedColumns;
		}
	}

	/**
	 * Gets the incoming data table of the node.
	 * 
	 * @return incoming data table
	 */
	public BufferedDataTable getLarvaFeaturesTable() {
		return m_larvaFeaturesTable;
	}

	/**
	 * Gets the positions of interesting columns.
	 * 
	 * @return the column positions
	 */
	public int[] getColIndxSet() {
		return m_colPos;
	}

	/**
	 * Gets the execution context of this model.
	 * 
	 * @return execution context of this model
	 */
	public ExecutionContext getExec() {
		return m_exec;
	}

	/**
	 * Gets the minimum run speed.
	 * 
	 * @return minimum run speed
	 */
	public double getMinRunSpeed() {
		return m_minRunSpeedSelection.getDoubleValue();
	}

	/**
	 * Gets the headcast angle threshold.
	 * 
	 * @return headcast angle threshold
	 */
	public int getHeadCastAngleThreshold() {
		return m_headCastAngleStartSelection.getIntValue();
	}
}
