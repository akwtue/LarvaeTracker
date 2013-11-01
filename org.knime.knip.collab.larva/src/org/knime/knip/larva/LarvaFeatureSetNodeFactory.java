package org.knime.knip.larva;

import net.imglib2.IterableInterval;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.ValuePair;

import org.knime.knip.base.nodes.features.IntervalFeatureSetNodeFactory;
import org.knime.knip.base.nodes.features.IntervalFeatureSetNodeModel.FeatureType;
import org.knime.knip.base.nodes.features.providers.FeatureSetProvider;
import org.knime.knip.base.nodes.features.providers.SegmentFeatureSetProvider;
import org.knime.knip.base.nodes.features.providers.ShapeDescriptorFeatureSetProvider;

/**
 * Factory for the larva segment feature set.
 * 
 * @author dietzc, hornm, wildnerm (University of Konstanz)
 */

public class LarvaFeatureSetNodeFactory<L extends Comparable<L>> extends
		IntervalFeatureSetNodeFactory<L, BitType> {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected FeatureSetProvider<ValuePair<IterableInterval<BitType>, CalibratedSpace>>[] getFeatureSetProviders() {
		return new FeatureSetProvider[] { new SegmentFeatureSetProvider(),
				new ShapeDescriptorFeatureSetProvider(),
				new LarvaFeatureSetProvider() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FeatureType getFeatureType() {
		return FeatureType.LABELING;
	}
}
