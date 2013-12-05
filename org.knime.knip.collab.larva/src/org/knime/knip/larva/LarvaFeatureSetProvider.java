package org.knime.knip.larva;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.nodes.features.providers.FeatureSetProvider;
import org.knime.knip.core.features.FeatureFactory;

/**
 * Provider for the larva segment feature set.
 * 
 * @author dietzc, hornm, wildnerm (University of Konstanz)
 */

public class LarvaFeatureSetProvider
		implements
		FeatureSetProvider<ValuePair<IterableInterval<BitType>, CalibratedSpace>> {

	static final String[] FEATURES = LarvaFeatureSet.FEATURE_NAMES;

	private static SettingsModelStringArray createFeatModel() {
		return new SettingsModelStringArray("larva_feature_set",
				new String[] {});
	}

	/*
	 * The feature factory
	 */
	private FeatureFactory m_featFac;

	private SettingsModelStringArray m_genFeat;

	@Override
	public void initAndAddDialogComponents(
			List<DialogComponent> dialogComponents) {

		dialogComponents.add(new DialogComponentStringListSelection(
				createFeatModel(), "Larva Features", Arrays.asList(FEATURES),
				false, 5));
	}

	@Override
	public void initAndAddColumnSpecs(List<DataColumnSpec> columnSpecs) {

		m_featFac = new FeatureFactory(false, new LarvaFeatureSet());
		String[] selectedFeatures = m_genFeat.getStringArrayValue();

		String[] allFeat = LarvaFeatureSet.FEATURE_NAMES;
		BitSet selection = new BitSet(allFeat.length);
		int j = 0;
		for (int i = 0; i < allFeat.length; i++) {
			if (j < selectedFeatures.length
					&& selectedFeatures[j].equals(allFeat[i])) {
				selection.set(i);
				j++;
			}
		}
		m_featFac.initFeatureFactory(selection);

		// create outspec according to the selected features
		String[] featNames = m_featFac.getFeatureNames();
		for (int i = 0; i < featNames.length; i++) {
			columnSpecs.add(new DataColumnSpecCreator(featNames[i],
					DoubleCell.TYPE).createSpec());
		}
	}

	@Override
	public void calcAndAddFeatures(
			ValuePair<IterableInterval<BitType>, CalibratedSpace> roi,
			List<DataCell> resCells) {

		m_featFac.updateFeatureTarget(roi.a);

		// add larva features
		for (int featID = 0; featID < m_featFac.getNumFeatures(); featID++) {
			resCells.add(new DoubleCell(m_featFac.getFeatureValue(featID)));
		}
	}

	@Override
	public String getFeatureSetName() {
		return "Larva features";
	}

	@Override
	public void initAndAddSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_genFeat = createFeatModel());
	}

	@Override
	public String getFeatureSetId() {
		return "Larva Features";
	}
}
