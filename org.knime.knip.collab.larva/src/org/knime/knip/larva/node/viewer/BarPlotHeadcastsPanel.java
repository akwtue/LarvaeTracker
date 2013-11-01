package org.knime.knip.larva.node.viewer;

/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2004, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 *
 * ------------------
 * BarChartDemo8.java
 * ------------------
 * (C) Copyright 2004, by Object Refinery Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   -;
 *
 * $Id: BarChartDemo8.java,v 1.5 2004/04/26 19:11:53 taqua Exp $
 *
 * Changes
 * -------
 * 18-Feb-2004 : Version 1, based on BarChartDemo7.java (DG);
 *
 */

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.IntCell;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;

/**
 * JPanel displaying larva headcasts on a bar plot.
 * 
 * @author wildnerm, University of Konstanz
 * 
 */

public class BarPlotHeadcastsPanel extends JPanel {

	private DataTable m_table = null;
	private EventService m_eventService;
	private CategoryDataset m_dataset;
	private JFreeChart m_chart;
	private ChartPanel m_chartPanel;

	private long m_valueLeft = -1;
	private long m_valueRight = -1;

	private int m_overallHeadCasts = 0;

	/**
	 * Constructor. Sets the event service of this component.
	 * 
	 * @param eventService
	 *            the event service to communicate with other components
	 */
	public BarPlotHeadcastsPanel(EventService eventService) {
		m_eventService = eventService;
		m_eventService.subscribe(this);
	}

	/**
	 * Stores a new data table and starts to recalculate the plot.
	 * 
	 * @param barPlotTable
	 *            the new data table
	 */
	public void changeData(DataTable barPlotTable) {
		m_table = barPlotTable;
		calculatePlot();
	}

	/**
	 * Recalculates the plot and repaints it.
	 */
	public void calculatePlot() {
		if (m_valueLeft != -1 && m_valueRight != -1 && m_table != null) {
			m_dataset = createDataset(m_valueLeft, m_valueRight);
			m_chart = createChart(m_dataset);
			m_chartPanel = new ChartPanel(m_chart);
			m_chartPanel.setPreferredSize(new Dimension(300, 200));
			// setContentPane(chartPanel);
			this.removeAll();
			this.add(m_chartPanel);
			validate();
			repaint();
		}
	}

	/**
	 * Creates the data for the plot using the stored data table and the given
	 * interval.
	 * 
	 * @return dataset for a bar plot
	 */
	private CategoryDataset createDataset(long valueLeft, long valueRight) {
		// read runs and headcasts from table
		int ctr = 1;
		RowIterator iterator = m_table.iterator();
		DataRow currRow = null;
		while (ctr < valueLeft && iterator.hasNext()) {
			// current row
			currRow = iterator.next();
			ctr++;
		}
		int prevIsRunning = 0;
		int prevHeadCastCtr = 0;
		if (currRow != null) {
			prevIsRunning = ((IntCell) currRow.getCell(0)).getIntValue();
			prevHeadCastCtr = ((IntCell) currRow.getCell(2)).getIntValue();
		}
		// stores the quantity of each category
		int[] cat1Ctrs = new int[] { 0, 0, 0, 0, 0 };
		int currIsRunning = 0;
		int currHeadCastPerStop = 0;
		while (ctr < valueRight && iterator.hasNext()) {
			currRow = iterator.next();
			currIsRunning = ((IntCell) currRow.getCell(0)).getIntValue();
			int currHeadCastCtr = ((IntCell) currRow.getCell(2)).getIntValue();

			if (currIsRunning == 0 || prevIsRunning == 0) {
				if (prevHeadCastCtr < currHeadCastCtr) {
					currHeadCastPerStop++;
				}
			}
			if (currIsRunning == 1 && prevIsRunning == 0) {
				if (currHeadCastPerStop > 3) {
					cat1Ctrs[4]++;
				} else {
					cat1Ctrs[currHeadCastPerStop]++;
				}
				currHeadCastPerStop = 0;
			}

			prevIsRunning = currIsRunning;
			prevHeadCastCtr = currHeadCastCtr;
			ctr++;
		}
		if (currIsRunning == 0) {
			if (currHeadCastPerStop > 3) {
				cat1Ctrs[4]++;
			} else {
				cat1Ctrs[currHeadCastPerStop]++;
			}
		}

		final String series = "NumHeadCasts";

		// column keys
		final String category1 = "0";
		final String category2 = "1";
		final String category3 = "2";
		final String category4 = "3";
		final String category5 = ">3";

		// create the dataset
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		dataset.addValue(cat1Ctrs[0], series, category1);
		dataset.addValue(cat1Ctrs[1], series, category2);
		dataset.addValue(cat1Ctrs[2], series, category3);
		dataset.addValue(cat1Ctrs[3], series, category4);
		dataset.addValue(cat1Ctrs[4], series, category5);

		return dataset;
	}

	/**
	 * Creates the chart.
	 * 
	 * @param dataset
	 *            the dataset
	 * 
	 * @return the chart
	 */
	private JFreeChart createChart(final CategoryDataset dataset) {

		// create the chart
		final JFreeChart chart = ChartFactory.createBarChart(
				"Headcasts per Stop", // chart title
				"Number of headcasts", // domain axis label
				"Occurrence", // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				false, // include legend
				true, // tooltips?
				false // URLs?
				);

		// customize plot
		chart.setBackgroundPaint(Color.white);
		final CategoryPlot plot = chart.getCategoryPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setUpperMargin(0.15);
		final CategoryItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesItemLabelsVisible(0, Boolean.TRUE);

		return chart;

	}

	/**
	 * Listener for changes of the t interval selection. Sets the new borders
	 * and recalculates the plot.
	 * 
	 * @param the t interval selection event
	 */
	@EventListener
	public void onTIntervalSelectionChanged(TIntervalSelectionEvent e) {
		if (e.getValueLeft() != m_valueLeft
				|| e.getValueRight() != m_valueRight) {
			m_valueLeft = e.getValueLeft();
			m_valueRight = e.getValueRight();
			calculatePlot();
		}
	}

	/**
	 * Calculates all headcasts from the whole data.
	 * 
	 * @return number of headcasts as a string
	 */
	public String getOverallHeadCasts() {
		m_overallHeadCasts = 0;

		// read runs and headcasts from table
		RowIterator iterator = m_table.iterator();
		DataRow currRow = null;
		int prevIsRunning = 0;
		int prevHeadCastCtr = 0;
		int currIsRunning = 0;
		int currHeadCastPerStop = 0;
		while (iterator.hasNext()) {
			currRow = iterator.next();
			currIsRunning = ((IntCell) currRow.getCell(0)).getIntValue();
			int currHeadCastCtr = ((IntCell) currRow.getCell(2)).getIntValue();

			if (currIsRunning == 0 || prevIsRunning == 0) {
				if (prevHeadCastCtr < currHeadCastCtr) {
					currHeadCastPerStop++;
				}
			}
			if (currIsRunning == 1 && prevIsRunning == 0) {
				m_overallHeadCasts = m_overallHeadCasts + currHeadCastPerStop;
				currHeadCastPerStop = 0;
			}

			prevIsRunning = currIsRunning;
			prevHeadCastCtr = currHeadCastCtr;
		}
		if (currIsRunning == 0) {
			m_overallHeadCasts = m_overallHeadCasts + currHeadCastPerStop;
		}

		String overallHeadCasts = "" + m_overallHeadCasts;
		String space = "";
		for (int i = overallHeadCasts.length(); i < 4; i++) {
			space = space + " ";
		}
		return space + m_overallHeadCasts;
	}
}