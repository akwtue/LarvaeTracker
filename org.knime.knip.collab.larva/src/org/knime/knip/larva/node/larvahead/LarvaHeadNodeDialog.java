package org.knime.knip.larva.node.larvahead;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.base.node.ValueToCellNodeDialog;

/**
 * Dialog for the node LarvaHead where the columns of larva head, tail,
 * center, centroid and time information can be chosen.
 * 
 * @author wildnerm, University of Konstanz
 */

public class LarvaHeadNodeDialog<L extends Comparable<L>> extends
		ValueToCellNodeDialog<DoubleValue> {

	@SuppressWarnings("unchecked")
	@Override
	public void addDialogComponents() {
		addDialogComponent(
				"Options",
				"Columns of first larva end",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaEndOneXColumnModel(),
						"x-value of first larva end: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of first larva end",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaEndOneYColumnModel(),
						"y-value of first larva end: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of second larva end",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaEndTwoXColumnModel(),
						"x-value of second larva end: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of second larva end",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaEndTwoYColumnModel(),
						"y-value of second larva end: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva center",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaCenterXColumnModel(),
						"x-value of larva center: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva center",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaCenterYColumnModel(),
						"y-value of larva center: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva centroid",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaCentroidXColumnModel(),
						"x-value of larva centroid: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva centroid",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaCentroidYColumnModel(),
						"y-value of larva centroid: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Column of larva time information",
				new DialogComponentColumnNameSelection(LarvaHeadNodeModel
						.createLarvaTimeColumnModel(),
						"t-value of larva points: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Column of larva time information",
				new DialogComponentBoolean(
						LarvaHeadNodeModel
								.createSortAccordingToTimeSelectionModel(),
						"sort table according to time (use if you cannot ensure incoming ascending time values)"));
		addDialogComponent(
				"Options",
				"Column of larva time information",
				new DialogComponentBoolean(LarvaHeadNodeModel
						.createMergeEqualTimeSelectionModel(),
						"try to merge rows with equal t-value (implementation not finished yet)"));
	}
}
