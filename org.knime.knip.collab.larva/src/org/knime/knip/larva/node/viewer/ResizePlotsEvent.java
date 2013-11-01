package org.knime.knip.larva.node.viewer;

import org.knime.knip.core.ui.event.KNIPEvent;
import org.knime.knip.core.ui.event.KNIPEvent.ExecutionPriority;

/**
 * Event to notify the adjustment of the plots to other components.
 * 
 * @author wildnerm, University of Konstanz
 *
 */
public class ResizePlotsEvent implements KNIPEvent  {

	@Override
	public ExecutionPriority getExecutionOrder() {
		return ExecutionPriority.NORMAL;
	}

	@Override
	public <E extends KNIPEvent> boolean isRedundant(E thatEvent) {
		return this.equals(thatEvent);
	}
}
