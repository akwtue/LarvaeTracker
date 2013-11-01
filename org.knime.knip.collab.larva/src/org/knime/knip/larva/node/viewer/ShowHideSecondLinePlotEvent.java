package org.knime.knip.larva.node.viewer;

import org.knime.knip.core.ui.event.KNIPEvent;
import org.knime.knip.core.ui.event.KNIPEvent.ExecutionPriority;

/**
 * Event to notify changes of the visibility of the second line plot to other components.
 * 
 * @author wildnerm, University of Konstanz
 *
 */
public class ShowHideSecondLinePlotEvent implements KNIPEvent  {

	boolean m_isSetVisible;
	
	public ShowHideSecondLinePlotEvent(boolean isSetVisible) {
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
	public boolean getIsSetVisible() {
		return m_isSetVisible;
	}
}
