package org.knime.knip.larva.node.larvathinning;

import net.imglib2.type.logic.BitType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.larva.node.larvahead.LarvaHeadNodeModel;

/**
 * Factory for the node LarvaThinning.
 * 
 * @author wildnerm, University of Konstanz
 */
public class LarvaThinningNodeFactory<BITTYPE> extends
		NodeFactory<LarvaThinningNodeModel>{
//public class LarvaThinningNodeFactory<BITTYPE> extends
//		ValueToCellNodeFactory<ImgPlusValue<BitType>> {

	@Override
	public LarvaThinningNodeModel createNodeModel() {
		return new LarvaThinningNodeModel();
	}

//	@Override
//	protected ValueToCellNodeDialog<ImgPlusValue<BitType>> createNodeDialog() {
//		return new LarvaThinningNodeDialog<BITTYPE>();
//	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<LarvaThinningNodeModel> createNodeView(int viewIndex,
			LarvaThinningNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		// TODO Auto-generated method stub
		return new LarvaThinningNodeDialog<BITTYPE>();
	}

}
