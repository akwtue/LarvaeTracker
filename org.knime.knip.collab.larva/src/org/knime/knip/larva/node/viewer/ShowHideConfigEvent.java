package org.knime.knip.larva.node.viewer;

import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * Event to notify changes of the visibility of the config panel to other components.
 * 
 * @author wildnerm, University of Konstanz
 *
 */
public class ShowHideConfigEvent implements KNIPEvent  {

	private int m_plotNumber;
	private boolean m_isSetVisible;
	
	public ShowHideConfigEvent(int plotNumber, boolean isSetVisible) {
		m_plotNumber = plotNumber;
		m_isSetVisible = isSetVisible;
	}

	@Override
	public ExecutionPriority getExecutionOrder() {
		return ExecutionPriority.NORMAL;
	}

	@Override
	public <E extends KNIPEvent> boolean isRedundant(E thatEvent) {
		return this.equals(thatEvent);
	}
	
	public int getPlotNumber() {
		return m_plotNumber;
	}
	
	public boolean getIsSetVisible() {
		return m_isSetVisible;
	}
}
