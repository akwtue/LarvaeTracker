package org.knime.knip.larva.node.viewer;

import org.knime.base.node.viz.plotter.line.LinePlotter;
import org.knime.base.node.viz.plotter.line.LinePlotterProperties;
import org.knime.core.data.def.IntCell;
import org.knime.knip.core.ui.event.EventService;

/**
 * Plots not only numeric values in a plot but also a horizontal line (x-axis),
 * a vertical movable line and two gray areas which border an interval.
 * 
 * @author wildnerm, University of Konstanz
 * 
 */

public class LarvaLinePlotter extends LinePlotter {

	private EventService m_eventService;

	private int m_numberOfPlot;

	/**
	 * Constructor. Sets the event service of the line plot and stores its ID.
	 * 
	 * @param eventService the event service to communicate with other components
	 * @param numberOfPlot ID of this line plot
	 */
	public LarvaLinePlotter(EventService eventService, int numberOfPlot) {
		super(new LarvaLinePlotterDrawingPane(eventService, numberOfPlot),
				new LinePlotterProperties());
		m_eventService = eventService;
		m_eventService.subscribe(this);
		m_numberOfPlot = numberOfPlot;
		setDataArrayIdx(m_numberOfPlot);
	}

	/**
	 * Sets the position of the horizontal line (x-axis).
	 */
	@Override
	public void updatePaintModel() {
		super.updatePaintModel();
		try {
			((LarvaLinePlotterDrawingPane) getDrawingPane())
					.setPosOfHorizontalLine(getMappedYValue(new IntCell(0)));
		} catch (Exception e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}

	/**
	 * Sets the new position of the horizontal line (x-axis) if the size of the plot has changed.
	 */
	@Override
	public void updateSize() {
		try {
			((LarvaLinePlotterDrawingPane) getDrawingPane())
					.setPosOfHorizontalLine(getMappedYValue(new IntCell(0)));
		} catch (Exception e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
		}
		super.updateSize();
	}
}
