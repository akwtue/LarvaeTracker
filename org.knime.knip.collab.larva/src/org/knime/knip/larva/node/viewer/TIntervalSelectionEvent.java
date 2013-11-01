package org.knime.knip.larva.node.viewer;

import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * Event to notify changes of the t-interval selection to other components.
 * 
 * @author wildnerm, University of Konstanz
 *
 */
public class TIntervalSelectionEvent implements KNIPEvent {

	private long m_valueLeft;
	private long m_valueRight;

	public TIntervalSelectionEvent(long valueLeft, long valueRight) {
		m_valueLeft = valueLeft;
		m_valueRight = valueRight;
	}

	@Override
	public ExecutionPriority getExecutionOrder() {
		return ExecutionPriority.NORMAL;
	}

	@Override
	public <E extends KNIPEvent> boolean isRedundant(E thatEvent) {
		return this.equals(thatEvent);
	}

	public long getValueLeft() {
		return m_valueLeft;
	}

	public long getValueRight() {
		return m_valueRight;
	}
}
