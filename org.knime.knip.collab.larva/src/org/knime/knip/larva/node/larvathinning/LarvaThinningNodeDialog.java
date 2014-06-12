package org.knime.knip.larva.node.larvathinning;

import net.imglib2.type.logic.BitType;

import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;

/**
 * Dialog for the node LarvaThinning where you can select which columns contain the images.
 * 
 * @author wildernm, dietzc (University of Konstanz)
 */
public class LarvaThinningNodeDialog<BITTYPE> extends
	ValueToCellNodeDialog<ImgPlusValue<BitType>> {

    @Override
    public void addDialogComponents() {

	addDialogComponent(new DialogComponentDimSelection(
			LarvaThinningNodeModel.createDimSelectionModel(),
		"Dimension selection"));

    }

}
