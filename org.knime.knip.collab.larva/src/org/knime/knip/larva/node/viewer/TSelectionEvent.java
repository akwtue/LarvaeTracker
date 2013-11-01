package org.knime.knip.larva.node.viewer;

import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * Event to notify changes of the t-value selection to other components.
 * 
 * @author wildnerm, University of Konstanz
 *
 */
public class TSelectionEvent implements KNIPEvent {

	private long m_tValue;
	
	public TSelectionEvent(long tValueNew) {
		m_tValue = tValueNew;
	}

	@Override
	public ExecutionPriority getExecutionOrder() {
		return ExecutionPriority.NORMAL;
	}

	@Override
	public <E extends KNIPEvent> boolean isRedundant(E thatEvent) {
		return this.equals(thatEvent);
	}
	
	public long getTValue() {
		return m_tValue;
	}

}
