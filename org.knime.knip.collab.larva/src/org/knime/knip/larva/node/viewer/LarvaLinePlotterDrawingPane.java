package org.knime.knip.larva.node.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.knime.base.node.viz.plotter.line.LinePlotterDrawingPane;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.core.data.property.ShapeFactory;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;

/**
 * Drawing pane of the Larva Line Plotter. It notices changes of the t-value and
 * the t-interval selection and paints them on the pane.
 * 
 * @author wildnerm, University of Konstanz
 * 
 */

public class LarvaLinePlotterDrawingPane extends LinePlotterDrawingPane {

	private EventService m_eventService;
	private int m_numberOfPlot;

	private long m_tSelectionLineXPos;
	private long m_tLeftBorder;
	private long m_tRightBorder;

	private int m_posOfHorizontalLine = 0;

	/**
	 * Constructor. Sets the event service of this component and stores the ID
	 * of the overlying line plot.
	 * 
	 * @param eventService
	 *            the event service to communicate with other components
	 * @param numberOfPlot
	 *            ID of the overlying line plot
	 */
	public LarvaLinePlotterDrawingPane(EventService eventService,
			int numberOfPlot) {
		m_eventService = eventService;
		m_eventService.subscribe(this);
		m_numberOfPlot = numberOfPlot;
		m_tSelectionLineXPos = 1;
		m_tLeftBorder = 1;
		m_tRightBorder = 1;
	}

	/**
	 * Sets the position of the horizontal line.
	 * 
	 * @param posOfHorizontalLine
	 *            position of the horizontal line
	 */
	public void setPosOfHorizontalLine(int posOfHorizontalLine) {
		m_posOfHorizontalLine = posOfHorizontalLine;
	}

	/**
	 * Paints (in addition to the feature lines) the horizontal line (x-axis),
	 * the vertical line (t-value) and the two gray areas which mark the borders
	 * of the t-interval.
	 */
	@Override
	public void paintContent(Graphics g) {
		DotInfo[] dotInfo = getDotInfoArray().getDots();
		int relativeT = 0;
		int relativeLeftBorder = 0;
		int relativeRightBorder = getWidth();

		if (m_numberOfPlot == 2) {
			g.setColor(Color.black);
			g.setPaintMode();
		}
		//
		if (dotInfo != null && dotInfo.length > 0) {
			relativeT = dotInfo[(int) (m_tSelectionLineXPos - 1)].getXCoord();
			relativeLeftBorder = dotInfo[(int) (m_tLeftBorder - 1)].getXCoord();
			relativeRightBorder = dotInfo[(int) (m_tRightBorder - 1)]
					.getXCoord();
		}
		g.setColor(Color.lightGray);
		g.fillRect(0, 0, relativeLeftBorder, getHeight());
		g.fillRect(relativeRightBorder, 0, getWidth() - relativeRightBorder,
				getHeight());

		ShapeFactory.Shape horizontalLine = ShapeFactory
				.getShape(ShapeFactory.HORIZONTAL_LINE);
		g.setColor(Color.black);
		horizontalLine.paintShape(g, (int) Math.floor(getWidth() / 2),
				m_posOfHorizontalLine, getWidth(), false, false);

		super.paintContent(g);

		ShapeFactory.Shape verticalLine = ShapeFactory
				.getShape(ShapeFactory.VERTICAL_LINE);
		g.setColor(Color.black);
		((Graphics2D) g).setStroke(new BasicStroke(1));
		verticalLine.paintShape(g, relativeT,
				(int) Math.floor(getHeight() / 2), getHeight(), false, false);
	}

	/**
	 * Listens to changes of the t-value, stores it and repaints the component.
	 * 
	 * @param tSelEvent the t-selection event
	 */
	@EventListener
	public void onTSelectionChanged(TSelectionEvent tSelEvent) {
		m_tSelectionLineXPos = tSelEvent.getTValue();
		repaint();
	}

	/**
	 * Listens to changes of the t-interval, stores its values and repaints the component.
	 * 
	 * @param tIntervalEvent the t-interval event
	 */
	@EventListener
	public void onTIntervalSelectionChanged(
			TIntervalSelectionEvent tIntervalEvent) {
		m_tLeftBorder = tIntervalEvent.getValueLeft();
		m_tRightBorder = tIntervalEvent.getValueRight();
		repaint();
	}
}