package org.knime.knip.larva.node.larvathinning;

import net.imglib2.type.logic.BitType;

import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;

/**
 * Factory for the node Thinning.
 * 
 * @author wildnerm, University of Konstanz
 */
public class ThinningNodeFactory<BITTYPE> extends
		ValueToCellNodeFactory<ImgPlusValue<BitType>> {

	@Override
	public ThinningNodeModel createNodeModel() {
		return new ThinningNodeModel();
	}

	@Override
	protected ValueToCellNodeDialog<ImgPlusValue<BitType>> createNodeDialog() {
		return new ThinningNodeDialog<BITTYPE>();
	}

}
