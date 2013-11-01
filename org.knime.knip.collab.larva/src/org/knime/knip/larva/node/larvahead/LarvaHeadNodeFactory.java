package org.knime.knip.larva.node.larvahead;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for the node LarvaHead.
 * 
 * @author wildnerm, University of Konstanz
 */

public class LarvaHeadNodeFactory extends NodeFactory<LarvaHeadNodeModel>{

	@Override
	public LarvaHeadNodeModel createNodeModel() {
		return new LarvaHeadNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0; // 0 -> no node view
	}

	@Override
	public NodeView<LarvaHeadNodeModel> createNodeView(int viewIndex,
			LarvaHeadNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new LarvaHeadNodeDialog();
	}

}
