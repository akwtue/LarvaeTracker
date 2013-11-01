package org.knime.knip.larva.node.viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;

/**
 * Panel which offers the possibility to set borders of the t-interval using
 * spinners and buttons.
 * 
 * @author wildnerm, University of Konstanz
 * 
 */
public class TIntervalSelectionPanel extends ViewerComponent {

	private EventService m_eventService;

	private SpinnerNumberModel m_spinnerModelLeft;
	private SpinnerNumberModel m_spinnerModelRight;
	private JSpinner m_spinnerLeft;
	private JSpinner m_spinnerRight;

	private long m_numRows;
	private long m_CurrentTValue;

	private JButton m_buttonSetLeft;
	private JButton m_buttonSetRight;

	/**
	 * Constructor. Sets the size of the panel.
	 */
	public TIntervalSelectionPanel() {
		super("t interval selection", false);
		this.setMaximumSize(new Dimension(180, this.getMaximumSize().height));
		this.setPreferredSize(new Dimension(180, this.getPreferredSize().height));
		this.setMinimumSize(new Dimension(180, this.getMinimumSize().height));
	}

	/**
	 * Updates the paint model of the panel and redraws all the components. The
	 * maximum value of the spinners depends on the amount of rows in the
	 * incoming data table.
	 * 
	 * @param numRows
	 *            number of rows of the incoming data table
	 */
	public void updatePaintModel(long numRows) {
		this.removeAll();
		m_numRows = numRows;

		JLabel spinnerLabelLeft = new JLabel("t value of left border:   ");
		JLabel spinnerLabelRight = new JLabel("t value of right border: ");
		m_spinnerModelLeft = new SpinnerNumberModel(1, 1, m_numRows - 1, 1);
		m_spinnerLeft = new JSpinner(m_spinnerModelLeft);
		m_spinnerModelRight = new SpinnerNumberModel(m_numRows, 2, m_numRows, 1);
		m_spinnerRight = new JSpinner(m_spinnerModelRight);

		m_spinnerLeft.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				onSpinnerLeftSelectionChanged(m_spinnerModelLeft.getNumber()
						.intValue());
			}
		});
		m_spinnerRight.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				onSpinnerRightSelectionChanged(m_spinnerModelRight.getNumber()
						.intValue());
			}
		});

		add(spinnerLabelLeft);
		add(m_spinnerLeft);
		add(spinnerLabelRight);
		add(m_spinnerRight);

		m_buttonSetLeft = new JButton("Set current t as left border");
		m_buttonSetRight = new JButton("Set current t as right border");

		m_buttonSetLeft.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_CurrentTValue != m_spinnerModelLeft.getNumber()
						.intValue() && m_CurrentTValue < m_numRows) {
					m_spinnerLeft.setValue((Object) m_CurrentTValue);
				}
			}
		});
		m_buttonSetRight.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_CurrentTValue != m_spinnerModelRight.getNumber()
						.intValue() && m_CurrentTValue > 1) {
					m_spinnerRight.setValue((Object) m_CurrentTValue);
				}
			}
		});

		add(m_buttonSetLeft);
		add(m_buttonSetRight);

		this.validate();
		this.repaint();
	}

	/**
	 * Sets the new value of the spinner and communicates with the other spinner to publish a t-interval selection event.
	 * 
	 * @param valueLeft left border of the interval
	 */
	protected void onSpinnerLeftSelectionChanged(int valueLeft) {
		int valueRight = m_spinnerModelRight.getNumber().intValue();
		if (valueLeft < valueRight) {
			m_eventService.publish(new TIntervalSelectionEvent(valueLeft,
					valueRight));
		} else {
			m_spinnerModelRight.setValue(valueLeft + 1);
		}
	}

	/**
	 * Sets the new value of the spinner and communicates with the other spinner to publish a t-interval selection event.
	 * 
	 * @param valueRight right border of the interval
	 */
	protected void onSpinnerRightSelectionChanged(int valueRight) {
		int valueLeft = m_spinnerModelLeft.getNumber().intValue();
		if (valueRight > valueLeft) {
			m_eventService.publish(new TIntervalSelectionEvent(valueLeft,
					valueRight));
		} else {
			m_spinnerModelLeft.setValue(valueRight - 1);
		}
	}

	/**
	 * Listens to changes of the t-value and stores it.
	 * 
	 * @param tSelEvent the t-selection event
	 */
	@EventListener
	public void onTSelectionChanged(TSelectionEvent tSelEvent) {
		m_CurrentTValue = tSelEvent.getTValue();
	}

	/**
	 * Sets the event service of this component
	 * 
	 * @param eventService the event service to communicate with other components
	 */
	@Override
	public void setEventService(EventService eventService) {
		m_eventService = eventService;
		eventService.subscribe(this);
	}

	@Override
	public Position getPosition() {
		return Position.SOUTH;
	}

	@Override
	public void saveComponentConfiguration(ObjectOutput out) throws IOException {
		// Do nothing
	}

	@Override
	public void loadComponentConfiguration(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// Do nothing
	}

}
