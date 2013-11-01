package org.knime.knip.larva.node.viewer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.core.ui.event.EventService;

/**
 * JPanel holdig several JPanels like larva info box, larva configuration panel,
 * bar plot runs and bar plot headcasts.
 * 
 * @author wildnerm, University of Konstanz
 * 
 */
public class LarvaInfoPanel extends JPanel {

	private BarPlotRunsPanel m_barPlotRuns;
	private BarPlotHeadcastsPanel m_barPlotHeadcasts;
	private JPanel m_configPanel;
	private JPanel m_definitionPanel;
	private JPanel m_infoBox;
	private BufferedDataTable m_featuresTable;
	private int[] m_colPos;
	private ExecutionContext m_exec;
	private EventService m_eventservice;

	private JButton m_showHideSecondLinePlot;
	private JButton m_showHideConfig2;
	private JButton m_showHideConfig1;

	private boolean m_isSecondLinePlotVisible = false;
	private boolean m_isConfig2Visible = false;
	private boolean m_isConfig1Visible = true;

	private SpinnerNumberModel m_spinnerModelRunSpeed;
	private SpinnerNumberModel m_spinnerModelHeadCasts;
	private JSpinner m_spinnerRunSpeed;
	private JSpinner m_spinnerHeadcasts;
	private JLabel m_overallRunsNumber;
	private JLabel m_overallHeadCastsNumber;

	private long m_valueLeft = 1;
	private long m_valueRight = 2;

	/**
	 * Constructor. Sets the event service of this component and initializes all
	 * the panels.
	 * 
	 * @param eventservice
	 *            the event service to communicate with other components
	 * @param minRunSpeed
	 *            the definition for the minimum run speed
	 * @param headCastAngleThreshold
	 *            the threshold for headcast angle
	 */
	public LarvaInfoPanel(EventService eventservice, double minRunSpeed,
			int headCastAngleThreshold) {
		m_eventservice = eventservice;
		m_eventservice.subscribe(this);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setPreferredSize(new Dimension(750, 200));
		setMinimumSize(new Dimension(750, 200));
		setMaximumSize(new Dimension(1600, 200));

		m_barPlotRuns = new BarPlotRunsPanel(m_eventservice);
		m_barPlotRuns.setPreferredSize(new Dimension(300, 200));
		m_barPlotRuns.setMinimumSize(new Dimension(300, 200));
		m_barPlotRuns.setMaximumSize(new Dimension(300, 200));

		m_barPlotHeadcasts = new BarPlotHeadcastsPanel(m_eventservice);
		m_barPlotHeadcasts.setPreferredSize(new Dimension(300, 200));
		m_barPlotHeadcasts.setMinimumSize(new Dimension(300, 200));
		m_barPlotHeadcasts.setMaximumSize(new Dimension(300, 200));

		m_configPanel = new JPanel();
		m_configPanel.setLayout(new BoxLayout(m_configPanel, BoxLayout.Y_AXIS));
		m_configPanel.setPreferredSize(new Dimension(150, 150));
		m_configPanel.setMaximumSize(new Dimension(150, 150));
		m_configPanel.setMinimumSize(new Dimension(150, 150));

		JButton resizePlots = new JButton("resize line plots");
		resizePlots.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_eventservice.publish(new ResizePlotsEvent());
			}
		});

		m_showHideSecondLinePlot = new JButton("show upper plot");
		m_showHideSecondLinePlot.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!m_isSecondLinePlotVisible) {
					m_eventservice
							.publish(new ShowHideSecondLinePlotEvent(true));
					m_showHideSecondLinePlot.setText("hide upper plot");
					m_showHideConfig2.setVisible(true);
					m_isSecondLinePlotVisible = true;
				} else {
					m_eventservice.publish(new ShowHideSecondLinePlotEvent(
							false));
					m_showHideSecondLinePlot.setText("show upper plot");
					m_showHideConfig2.setVisible(false);
					m_isSecondLinePlotVisible = false;
				}
			}
		});

		m_showHideConfig2 = new JButton("show config top");
		m_showHideConfig2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!m_isConfig2Visible) {
					m_eventservice.publish(new ShowHideConfigEvent(2, true));
					m_showHideConfig2.setText("hide config top");
					m_isConfig2Visible = true;
				} else {
					m_eventservice.publish(new ShowHideConfigEvent(2, false));
					m_showHideConfig2.setText("show config top");
					m_isConfig2Visible = false;
				}
			}
		});
		m_showHideConfig2.setVisible(false);

		m_showHideConfig1 = new JButton("hide config bot");
		m_showHideConfig1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_isConfig1Visible) {
					m_eventservice.publish(new ShowHideConfigEvent(1, false));
					m_showHideConfig1.setText("show config bot");
					m_isConfig1Visible = false;
				} else {
					m_eventservice.publish(new ShowHideConfigEvent(1, true));
					m_showHideConfig1.setText("hide config bot");
					m_isConfig1Visible = true;
				}
			}
		});

		m_configPanel.add(resizePlots);
		JPanel space1 = new JPanel();
		space1.setPreferredSize(new Dimension(50, 30));
		space1.setMinimumSize(new Dimension(50, 30));
		space1.setMaximumSize(new Dimension(150, 30));
		m_configPanel.add(space1);
		m_configPanel.add(m_showHideSecondLinePlot);
		JPanel space2 = new JPanel();
		space2.setSize(150, 15);
		m_configPanel.add(space2);
		m_configPanel.add(m_showHideConfig2);
		m_configPanel.add(m_showHideConfig1);

		m_definitionPanel = new JPanel();
		m_definitionPanel.setPreferredSize(new Dimension(165, 150));
		m_definitionPanel.setMaximumSize(new Dimension(165, 150));
		m_definitionPanel.setMinimumSize(new Dimension(165, 150));
		JLabel spinnerLabelRunSpeed = new JLabel("Minimum run speed:    ");
		JLabel spinnerLabelHeadCasts = new JLabel("Threshold head angle:");
		m_spinnerModelRunSpeed = new SpinnerNumberModel(minRunSpeed, 0, 101,
				0.2);
		m_spinnerRunSpeed = new JSpinner(m_spinnerModelRunSpeed);
		m_spinnerModelHeadCasts = new SpinnerNumberModel(
				headCastAngleThreshold, 1, 179, 1);
		m_spinnerHeadcasts = new JSpinner(m_spinnerModelHeadCasts);

		m_spinnerRunSpeed.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				setBarPlotsTables();
			}
		});
		m_spinnerHeadcasts.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				setBarPlotsTables();
			}
		});

		m_definitionPanel.add(spinnerLabelRunSpeed);
		m_definitionPanel.add(m_spinnerRunSpeed);
		m_definitionPanel.add(spinnerLabelHeadCasts);
		m_definitionPanel.add(m_spinnerHeadcasts);

		JPanel space3 = new JPanel();
		space3.setPreferredSize(new Dimension(50, 20));
		space3.setMinimumSize(new Dimension(50, 20));
		space3.setMaximumSize(new Dimension(150, 20));
		m_definitionPanel.add(space3);
		JLabel overallRunsLabel = new JLabel("Overall Runs:                ");
		m_overallRunsNumber = new JLabel("");
		JLabel overallHeadCastsLabel = new JLabel("Overall Headcasts:       ");
		m_overallHeadCastsNumber = new JLabel("");
		m_definitionPanel.add(overallRunsLabel);
		m_definitionPanel.add(m_overallRunsNumber);
		m_definitionPanel.add(overallHeadCastsLabel);
		m_definitionPanel.add(m_overallHeadCastsNumber);

		m_infoBox = new JPanel();
		m_infoBox.setPreferredSize(new Dimension(300, 150));
		// infoBox.setMaximumSize(new Dimension(400, 150));
		m_infoBox.setMinimumSize(new Dimension(100, 150));

		add(m_definitionPanel);
		add(m_configPanel);
		add(m_barPlotRuns);
		add(m_barPlotHeadcasts);
		add(m_infoBox);
	}

	/**
	 * Sets the incoming table and its description.
	 * 
	 * @param featuresTable the data table
	 * @param colPos description of the table, positions of interesting columns
	 * @param executionContext the execution context of the node model
	 */
	public void setFeaturesTable(BufferedDataTable featuresTable, int[] colPos,
			ExecutionContext executionContext) {
		m_featuresTable = featuresTable;
		m_colPos = colPos;
		m_exec = executionContext;
		setBarPlotsTables();
	}

	/**
	 * Recalculates headcasts and runs if the definition has changed and passes
	 * the new data table to the bar plots.
	 */
	protected void setBarPlotsTables() {

		// stores meta data about the table
		DataTableSpec inSpec = m_featuresTable.getDataTableSpec();

		// number of incoming columns
		int numColumnsIn = inSpec.getNumColumns();
		// number of outgoing columns
		int numColumnsOut = 3;
		int ctr = 0;

		DataTableSpec outSpec = createOutSpec(inSpec);
		BufferedDataContainer container = m_exec.createDataContainer(outSpec,
				true);

		double prevTime = 0;
		int prevHeadDirection = 0;
		int headCastNumber = 0;
		RowIterator larvaFeaturesIterator = m_featuresTable.iterator();

		while (larvaFeaturesIterator.hasNext()) {
			DataRow currRow = larvaFeaturesIterator.next();

			DataCell[] cells = new DataCell[numColumnsOut];
			double currTime = ((DoubleValue) currRow.getCell(m_colPos[0]))
					.getDoubleValue();
			if (ctr == 0) {
				prevTime = currTime;
			} else if (currTime == prevTime) {
				continue;
			}

			double runMinimumSpeed = m_spinnerModelRunSpeed.getNumber()
					.doubleValue();
			int isRunning = 0;
			if (((DoubleValue) currRow.getCell(m_colPos[5])).getDoubleValue() > runMinimumSpeed) {
				isRunning = 1;
			}
			cells[0] = new IntCell(isRunning);

			int headCastAngleStart = m_spinnerModelHeadCasts.getNumber()
					.intValue();
			double headCastAngleEndDifference = 5;
			if (headCastAngleStart < headCastAngleEndDifference + 1) {
				headCastAngleEndDifference = 1;
			}
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
			cells[1] = new IntCell(currHeadDirection);

			// CODE CAN BE USED LATER
			// boolean detectHeadCastsWhileRunning =
			// m_detectHeadcastsWhileRunningSelection
			// .getBooleanValue();
			// if (detectHeadCastsWhileRunning) {
			// if (prevHeadDirection != 0 && currHeadDirection == 0) {
			// headCastNumber++;
			// }
			// } else {
			if (isRunning == 0 && prevHeadDirection != 0
					&& currHeadDirection == 0) {
				headCastNumber++;
			}
			// }
			cells[2] = new IntCell(headCastNumber);

			DataRow outRow = new DefaultRow(currRow.getKey(), cells);
			container.addRowToTable(outRow);

			prevTime = currTime;
			prevHeadDirection = currHeadDirection;
		}

		container.close();

		DataTable barPlotTable = container.getTable();

		m_barPlotRuns.changeData(barPlotTable);
		m_barPlotHeadcasts.changeData(barPlotTable);
		m_overallRunsNumber.setText(m_barPlotRuns.getOverallRuns());
		m_overallHeadCastsNumber.setText(m_barPlotHeadcasts
				.getOverallHeadCasts());
	}

	public void updateInfoLabels() {
		m_overallRunsNumber.setText(m_barPlotRuns.getOverallRuns());
		m_overallHeadCastsNumber.setText(m_barPlotHeadcasts
				.getOverallHeadCasts());

	}

	/**
	 * Creates a new data table spec because useless columns should be removed.
	 * 
	 * @param inSpec
	 *            incoming data table spec
	 * @return new data table spec containing three columns
	 */
	private DataTableSpec createOutSpec(DataTableSpec inSpec) {
		int numColIn = inSpec.getNumColumns();
		int numColOut = 3;

		DataColumnSpec[] colSpecs = new DataColumnSpec[numColOut];
		for (int i = 0; i < numColOut; i++) {
			colSpecs[i] = inSpec.getColumnSpec(numColIn - 3 + i);
		}
		return new DataTableSpec(colSpecs);
	}
}
