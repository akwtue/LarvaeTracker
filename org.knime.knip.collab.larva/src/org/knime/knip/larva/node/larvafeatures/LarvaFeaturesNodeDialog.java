package org.knime.knip.larva.node.larvafeatures;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.base.node.ValueToCellNodeDialog;

/**
 * Dialog for the node LarvaFeatures where the columns of larva head, tail,
 * center, centroid and time information can be chosen.
 * 
 * @author wildnerm, University of Konstanz
 */

public class LarvaFeaturesNodeDialog<L extends Comparable<L>> extends
		ValueToCellNodeDialog<DoubleValue> {

	@SuppressWarnings("unchecked")
	@Override
	public void addDialogComponents() {
		addDialogComponent(
				"Options",
				"Columns of larva head",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaHeadXColumnModel(),
						"x-value of larva head: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva head",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaHeadYColumnModel(),
						"y-value of larva head: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva tail",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaTailXColumnModel(),
						"x-value of larva tail: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva tail",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaTailYColumnModel(),
						"y-value of larva tail: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva center",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaCenterXColumnModel(),
						"x-value of larva center: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva center",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaCenterYColumnModel(),
						"y-value of larva center: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva centroid",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaCentroidXColumnModel(),
						"x-value of larva centroid: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Columns of larva centroid",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaCentroidYColumnModel(),
						"y-value of larva centroid: ", 0, true,
						DoubleValue.class));
		addDialogComponent(
				"Options",
				"Column of larva time information",
				new DialogComponentColumnNameSelection(LarvaFeaturesNodeModel
						.createLarvaTimeColumnModel(),
						"t-value of larva points: ", 0, true, DoubleValue.class));
		addDialogComponent(
				"Options",
				"Column of larva time information",
				new DialogComponentBoolean(
						LarvaFeaturesNodeModel
								.createSortAccordingToTimeSelectionModel(),
						"sort table according to time (use if you cannot ensure incoming ascending time values)"));
		addDialogComponent(
				"Options",
				"Container position",
				new DialogComponentBoolean(
						LarvaFeaturesNodeModel
								.createComputeDistanceToLeftSelectionModel(),
						"Compute distance to left container/to right container otherwise (Needs second port)"));
	}
}
