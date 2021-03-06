/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 *
 */
package org.knime.knip.larva.node.viewer;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.seg.labeleditor.DialogComponentStringTransformer;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.larva.node.viewer.LarvaViewerNodeModel.LabelTransformVariables;

/**
 * Dialog for the node Larva Viewer, based on the Dialog for the Segment Overlay
 * Node.
 * 
 * @author dietzc, hornm, schonenbergerf, wildnerm
 */
public class LarvaViewerNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * Constructor. Initializes all the dialog components.
	 */
	@SuppressWarnings("unchecked")
	public LarvaViewerNodeDialog() {
		super();
		addDialogComponent(new DialogComponentNumber(
				new SettingsModelDoubleBounded("min_run_speed_selection", 3.5,
						0, 99), "Minimum run speed (pixels per second)", 0.2));
		addDialogComponent(new DialogComponentNumber(
				new SettingsModelIntegerBounded("headcast_angle_start", 25, 1,
						179), "Threshold for head cast angle", 1));
		// addDialogComponent(new DialogComponentBoolean(new
		// SettingsModelBoolean(
		// "detect_headcasts_while_running_selection", false),
		// "Detect headcasts while running"));

		createNewTab("Column Selection");
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString("larva_time_column_selection", "Time"),
				"Column of time information", 1, DoubleValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString("larva_head_angle_column_selection",
						"theta (head angle)"), "Column of theta (head angle)",
				1, true, DoubleValue.class));

		createNewTab("Image Options");
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(LarvaViewerNodeModel.CFG_IMG_COL, ""),
				"Img column", 0, false, true, ImgPlusValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(LarvaViewerNodeModel.CFG_LABELING_COL,
						""), "Labeling column", 0, true, LabelingValue.class));

		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				LarvaViewerNodeModel.CFG_ADJUST_VIRTUALLY, true),
				"Virtually extend labeling or img?"));

		createNewTab("Label Transformation");
		final DialogComponentStringTransformer dialogComponentStringTransformer = new DialogComponentStringTransformer(
				new SettingsModelString(LarvaViewerNodeModel.CFG_EXPRESSION,
						"$" + LabelTransformVariables.Label + "$"), true, 0,
				"Label",
				EnumUtils.getStringListFromToString(LabelTransformVariables.values()));

		addDialogComponent(dialogComponentStringTransformer);
	}
}
