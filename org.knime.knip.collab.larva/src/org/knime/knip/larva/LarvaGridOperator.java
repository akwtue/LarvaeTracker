package org.knime.knip.larva;

import java.io.IOException;

import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.knip.base.data.aggregation.ImgAggregrationOperation;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * Pixel-wise addition of images
 * 
 * @author Christian Dietz, University of Konstanz
 * 
 */
public class LarvaGridOperator<T extends RealType<T>> extends
		ImgAggregrationOperation {

	private static final double grid_y_factor = 0.09;

	private static final double grid_x_factor = 0.09;

	/* label */
	private final static String LABEL = "Grid Image (Unsigned Short Result)";

	/* the tempory result holding the pixel-wise mean */
	private ImgPlus<UnsignedShortType> m_resImg = null;

	/* the tempory result holding the pixel-wise mean */
	private short[][] m_initGrid = null;

	private int m_gridYWidth;

	private int m_gridXWidth;

	public LarvaGridOperator() {
		super(LABEL, LABEL, LABEL);
	}

	/**
         */
	public LarvaGridOperator(GlobalSettings globalSettings) {
		super(LABEL, LABEL, globalSettings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean computeInternal(DataCell cell) {
		if (cell.isMissing()) {
			return true;
		}
		ImgPlus<T> img = ((ImgPlusValue<T>) cell).getImgPlus();
		if (m_resImg == null) {
			m_resImg = new ImgPlus<UnsignedShortType>(
					new ArrayImgFactory<UnsignedShortType>().create(new long[] {
							img.dimension(0), img.dimension(1) },
							new UnsignedShortType()), img);

			m_gridXWidth = (int) (m_resImg.dimension(0) * grid_x_factor);
			m_gridYWidth = (int) (m_resImg.dimension(1) * grid_y_factor);

			m_initGrid = new short[(int) (1 / grid_x_factor)][(int) (1 / grid_y_factor)];

		}

		try {

			Cursor<T> inCursor = img.cursor();

			while (inCursor.hasNext()) {
				inCursor.fwd();
				if (inCursor.get().getRealDouble() > 0) {
					m_initGrid[(int) Math.min(inCursor.getIntPosition(0)
							/ m_gridXWidth, m_initGrid.length - 1)][(int) Math
							.min(inCursor.getIntPosition(1) / m_gridYWidth,
									m_initGrid[0].length - 1)]++;
				}
			}

		} catch (IllegalArgumentException e) {
			setSkipMessage("Images are not compatible (dimensions, iteration order, etc.): "
					+ e.getMessage());
			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataType getDataType(DataType origType) {
		return ImgPlusCell.TYPE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataCell getResultInternal() {
		try {
			Cursor<UnsignedShortType> cursor = m_resImg.cursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.get()
						.set(m_initGrid[(int) Math.min(cursor.getIntPosition(0)
								/ m_gridXWidth, m_initGrid.length - 1)][(int) Math
								.min(cursor.getIntPosition(1) / m_gridYWidth,
										m_initGrid[0].length - 1)]);
			}

			return getImgPlusCellFactory().createCell(m_resImg);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void resetInternal() {
		m_resImg = null;
	}

	@Override
	public String getDescription() {
		return "Calculates the sum image as short type image.";
	}

	@Override
	public AggregationOperator createInstance(GlobalSettings globalSettings,
			OperatorColumnSettings opColSettings) {
		return new LarvaGridOperator<T>(globalSettings);
	}
}
