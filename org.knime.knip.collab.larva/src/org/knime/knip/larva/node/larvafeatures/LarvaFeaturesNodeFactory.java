package org.knime.knip.larva.node.larvafeatures;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for the node LarvaFeatures.
 * 
 * @author wildnerm, University of Konstanz
 */

public class LarvaFeaturesNodeFactory extends
		NodeFactory<LarvaFeaturesNodeModel> {

	@Override
	public LarvaFeaturesNodeModel createNodeModel() {
		return new LarvaFeaturesNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0; // 0 -> no node view
	}

	@Override
	public NodeView<LarvaFeaturesNodeModel> createNodeView(int viewIndex,
			LarvaFeaturesNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new LarvaFeaturesNodeDialog();
	}

}
