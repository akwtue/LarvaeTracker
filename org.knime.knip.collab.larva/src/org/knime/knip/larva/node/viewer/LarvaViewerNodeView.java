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
 * History
 *   29 Jan 2010 (hornm): created
 */
package org.knime.knip.larva.node.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.LabelingView;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.ImageMetadata;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.line.LinePlotterProperties;
import org.knime.base.node.viz.plotter.props.ColorLegendTab;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.view.segmentoverlay.LabelHiliteProvider;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.events.ImgAndLabelingChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelingWithMetadataChgEvent;
import org.knime.knip.core.ui.imgviewer.events.PlaneSelectionEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgLabelingViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.AWTImageProvider;
import org.knime.knip.core.ui.imgviewer.panels.providers.CombinedRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.ImageRU;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingRU;
import org.knime.knip.core.util.MiscViews;
import org.knime.knip.larva.node.viewer.LarvaViewerNodeModel.LabelTransformVariables;

/**
 * View of the node Larva Viewer, based on the View of the Segment Overlay Node.
 * 
 * @author dietzc, hornm, schonenbergerf, wildnerm
 */
public class LarvaViewerNodeView<T extends RealType<T>, L extends Comparable<L>, I extends IntegerType<I>>
		extends NodeView<LarvaViewerNodeModel<T, L>> implements
		ListSelectionListener {

	private class ExtNativeImgLabeling<LL extends Comparable<LL>, II extends IntegerType<II>>
			extends NativeImgLabeling<LL, II> {
		/**
		 * @param img
		 */
		public ExtNativeImgLabeling(final Img<II> img,
				final LabelingMapping<LL> mapping) {
			super(img);
			super.mapping = mapping;
		}
	}

	/* A node logger */
	static NodeLogger LOGGER = NodeLogger.getLogger(LarvaViewerNodeView.class);

	private LabelHiliteProvider<L, T> m_hiliteProvider;

	/* Image cell view pane */
	private ImgViewer m_imgView;

	private TIntervalSelectionPanel m_tIntervalSelectionPanel = new TIntervalSelectionPanel();

	/* Current row */
	private int m_row;

	/* The split pane for the view */
	private JSplitPane m_sp;

	/* Left split pane */
	private JSplitPane m_leftSP;

	private LarvaInfoPanel m_larvaInfoPanel;

	private JPanel m_larvaDoubleLinePlotPanel;
	private JTabbedPane m_larvaLinePlotTabs;

	private AbstractPlotter m_linePlot1;
	private AbstractPlotter m_linePlot2;
	private AbstractPlotter m_linePlot3;
	private AbstractPlotter m_linePlot4;
	private Boolean m_showPlot1Config;
	private Boolean m_showPlot2Config;

	/* Table for the images */
	private TableContentView m_tableContentView;

	/* The Table view */
	private TableView m_tableView;

	private EventService m_eventService;

	private long m_tValueOld = -1;

	private final ExecutorService UPDATE_EXECUTOR = Executors
			.newCachedThreadPool(new ThreadFactory() {
				private final AtomicInteger m_counter = new AtomicInteger();

				@Override
				public Thread newThread(final Runnable r) {
					final Thread t = new Thread(r,
							"Segment Overlay Viewer-Updater-"
									+ m_counter.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * Constructor. Initializes all the components of the Interactive Larva
	 * View.
	 * 
	 * @param model
	 *            the node model of this node
	 */
	public LarvaViewerNodeView(final LarvaViewerNodeModel<T, L> model) {
		super(model);

		m_showPlot1Config = true;
		m_showPlot2Config = true;

		m_sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		m_sp.setPreferredSize(new Dimension(1500, 900));

		m_row = -1;
		m_tableContentView = new TableContentView();
		m_tableContentView.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		m_tableContentView.getColumnModel().getSelectionModel()
				.addListSelectionListener(this);
		m_tableView = new TableView(m_tableContentView);

		m_imgView = new ImgViewer();
		m_eventService = m_imgView.getEventService();
		m_eventService.subscribe(this);
		m_imgView.addViewerComponent(new AWTImageProvider(20, new CombinedRU(
				new ImageRU<T>(true), new LabelingRU<L>())));
		m_imgView.addViewerComponent(new ImgLabelingViewInfoPanel<T, L>());
		m_imgView.addViewerComponent(new ImgCanvas<T, Img<T>>());
		m_imgView.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
		m_imgView.addViewerComponent(ViewerComponents.PLANE_SELECTION
				.createInstance());
		m_imgView.addViewerComponent(m_tIntervalSelectionPanel);

		if (getNodeModel().getInternalTables().length > LarvaViewerNodeModel.PORT_FEATURES) {
			m_hiliteProvider = new LabelHiliteProvider<L, T>();
			m_imgView.addViewerComponent(m_hiliteProvider);
		} else {
			m_hiliteProvider = null;
		}

		m_leftSP = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		m_leftSP.add(m_tableView);
		m_leftSP.add(m_imgView);
		m_leftSP.setDividerLocation(220);
		m_sp.add(m_leftSP);

		if (!(model instanceof DataProvider)) {
			throw new IllegalArgumentException(
					"Model must implement the DataProvider interface!");
		}

		m_larvaDoubleLinePlotPanel = new JPanel();
		m_larvaDoubleLinePlotPanel.setLayout(new BoxLayout(
				m_larvaDoubleLinePlotPanel, BoxLayout.Y_AXIS));
		// add Line Plotter to Viewer
		m_linePlot1 = new LarvaLinePlotter(m_eventService, 1);
		m_linePlot1.setDataProvider((DataProvider) model);
		m_linePlot1.setHiLiteHandler(model.getInHiLiteHandler(1));
		m_linePlot1.updatePaintModel();
		m_linePlot2 = new LarvaLinePlotter(m_eventService, 2);
		m_linePlot2.add(m_linePlot2.getComponent(1), 0);
		m_linePlot2.setDataProvider((DataProvider) model);
		m_linePlot2.setHiLiteHandler(model.getInHiLiteHandler(1));
		m_linePlot2.updatePaintModel();
		m_linePlot2.getComponent(0).setVisible(false);
		m_linePlot2.setVisible(false);
		m_larvaDoubleLinePlotPanel.add(m_linePlot2);
		m_larvaDoubleLinePlotPanel.add(m_linePlot1);

		m_linePlot3 = new LarvaLinePlotter(m_eventService, 3);
		m_linePlot3.setDataProvider((DataProvider) model);
		m_linePlot3.setHiLiteHandler(model.getInHiLiteHandler(1));
		m_linePlot3.updatePaintModel();
		m_linePlot4 = new LarvaLinePlotter(m_eventService, 4);
		m_linePlot4.setDataProvider((DataProvider) model);
		m_linePlot4.setHiLiteHandler(model.getInHiLiteHandler(1));
		m_linePlot4.updatePaintModel();

		// m_larvaLinePlotTabs = new JTabbedPane();
		// m_larvaLinePlotTabs.addTab("Two Line Plots",
		// m_larvaDoubleLinePlotPanel);
		// m_larvaLinePlotTabs.addTab("Line Plot: Angle", m_linePlot3);
		// m_larvaLinePlotTabs.addTab("Line Plot: Speed", m_linePlot4);

		m_larvaInfoPanel = new LarvaInfoPanel(m_eventService,
				model.getMinRunSpeed(), model.getHeadCastAngleThreshold());
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		// rightPanel.add(m_larvaLinePlotTabs);
		rightPanel.add(m_larvaDoubleLinePlotPanel);

		rightPanel.add(m_larvaInfoPanel);

		m_sp.add(rightPanel);

		setComponent(m_sp);

		m_sp.setDividerLocation(460);
		loadPortContent();
	}

	/**
	 * Listens to the resize plots event and fits the line plots to the current
	 * visible area.
	 * 
	 * @param e
	 *            the resize plots event
	 */
	@EventListener
	public void onResizePlotsClicked(ResizePlotsEvent e) {
		m_linePlot1.fitToScreen();
		if (m_linePlot2.isVisible()) {
			m_linePlot2.fitToScreen();
		}
		// m_linePlot3.fitToScreen();
		// m_linePlot4.fitToScreen();
	}

	/**
	 * Listens to changes of the visibility of the second line plot and passes
	 * it to the plot.
	 * 
	 * @param e
	 *            the show/hide second line plot event
	 */
	@EventListener
	public void onShowHideSecondLinePlotClicked(ShowHideSecondLinePlotEvent e) {
		if (e.getIsSetVisible()) {
			m_linePlot2.setVisible(true);
			m_linePlot1.fitToScreen();
			m_linePlot2.fitToScreen();
		} else {
			m_linePlot2.setVisible(false);
			m_linePlot1.fitToScreen();
		}
	}

	/**
	 * Listens to changes of the visibility of the config and passes it to the
	 * plot.
	 * 
	 * @param e
	 *            the show/hide config event
	 */
	@EventListener
	public void onShowHideConfigClicked(ShowHideConfigEvent e) {
		if (e.getPlotNumber() == 1) {
			if (e.getIsSetVisible()) {
				m_linePlot1.getComponent(1).setVisible(true);
			} else {
				m_linePlot1.getComponent(1).setVisible(false);
			}
		}
		if (e.getPlotNumber() == 2) {
			if (m_linePlot2.isVisible()) {
				if (e.getIsSetVisible()) {
					m_linePlot2.getComponent(0).setVisible(true);
				} else {
					m_linePlot2.getComponent(0).setVisible(false);
				}
			}
		}
	}

	/**
	 * Listens to changes of the plane selection (selection of the t-value) and
	 * publishes the t-value selection event
	 * 
	 * @param e
	 *            the plane selection event
	 */
	@EventListener
	public void onPlaneSelectionChanged(final PlaneSelectionEvent e) {
		if (e.numDimensions() > 2) {
			long t = e.getPlanePosAt(e.numDimensions() - 1);
			// add 1 to use the same value as displayed on the plane selection
			// panel
			t = t + 1;
			if (t != m_tValueOld) {
				m_eventService.publish(new TSelectionEvent(t));
				m_tValueOld = t;
			}
		}
	}

	/**
	 * Listens to changes of the table selection and forces all components to
	 * update their paint models and selections.
	 * 
	 * @param e
	 *            the image and labling changed event
	 */
	@EventListener
	public void onTableSelectionChanged(ImgAndLabelingChgEvent<?, ?> e) {
		m_linePlot1.updatePaintModel();
		m_linePlot2.updatePaintModel();
		long[] dims = new long[e.getRandomAccessibleInterval().numDimensions()];
		e.getRandomAccessibleInterval().dimensions(dims);
		long numRows = dims[dims.length - 1];
		m_tIntervalSelectionPanel.updatePaintModel(numRows);
		m_larvaInfoPanel.setFeaturesTable(getNodeModel()
				.getLarvaFeaturesTable(), getNodeModel().getColIndxSet(),
				getNodeModel().getExec());
		m_eventService.publish(new TIntervalSelectionEvent(1, numRows));

		ColorLegendTab colorLegend = ((LinePlotterProperties) m_linePlot2
				.getProperties()).getColorLegend();
		Map<String, Color> mapping = colorLegend.getColorMapping();
		Set<String> keySet = mapping.keySet();

		Map<String, Color> newMapping = new LinkedHashMap<String, Color>();
		// mapping.clear();
		int numEntries = keySet.size();
		float segment = 360f / (numEntries);
		int keyNumber = 0;
		for (String i : keySet) {
			// if new columns are added
			// String key = colSpec.getName();
			float h = ((keyNumber * segment + 40) / 360f);
			Color c = Color.getHSBColor(h, 1, 1);
			newMapping.put(i, c);
			keyNumber++;
		}
		colorLegend.update(newMapping);
	}

	private void loadPortContent() {

		m_tableContentView.setModel(getNodeModel().getTableContentModel());

		// Scale to thumbnail size
		m_tableView.validate();
		m_tableView.repaint();
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		m_tableContentView.setModel(getNodeModel().getTableContentModel());
		if (m_hiliteProvider != null) {
			final HiLiteHandler handler = getNodeModel().getInHiLiteHandler(
					getNodeModel().PORT_FEATURES);
			m_hiliteProvider.updateInHandler(handler);
		}
		m_linePlot1.updatePaintModel();
		m_linePlot2.updatePaintModel();
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		UPDATE_EXECUTOR.shutdownNow();
		if (m_hiliteProvider != null) {
			m_hiliteProvider.onClose();
		}

		m_tableView.removeAll();
		m_hiliteProvider = null;
		m_tableContentView.removeAll();
		m_imgView.getEventService().publish(new ViewClosedEvent());
		m_imgView.removeAll();
		m_imgView = null;
		m_tableContentView = null;
		m_tableView = null;
		m_sp = null;
		m_row = -1;
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// Scale to thumbnail size
		m_tableView.validate();
		m_tableView.repaint();
	}

	/**
	 * Updates the ViewPane with the selected image and labeling
	 * 
	 * 
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void valueChanged(final ListSelectionEvent e) {

		final int row = m_tableContentView.getSelectionModel()
				.getLeadSelectionIndex();

		if ((row == m_row) || e.getValueIsAdjusting()) {
			return;
		}

		m_row = row;

		try {
			RandomAccessibleInterval<T> img;
			ImgPlusMetadata metadata;
			Labeling<L> lab;
			DataCell currentLabelingCell;

			if (m_tableContentView.getModel().getColumnCount() == 2) {
				// Labeling and image
				final DataCell currentImgCell = m_tableContentView
						.getContentModel().getValueAt(row,
								LarvaViewerNodeModel.COL_IDX_IMAGE);

				currentLabelingCell = m_tableContentView.getContentModel()
						.getValueAt(row, LarvaViewerNodeModel.COL_IDX_LABELING);

				img = ((ImgPlusValue<T>) currentImgCell).getImgPlus();
				metadata = ((ImgPlusValue<T>) currentImgCell).getMetadata();

				lab = ((LabelingValue<L>) currentLabelingCell).getLabeling();

			} else {
				// only a Labeling (creates an empty image of
				// ByteType)
				currentLabelingCell = m_tableContentView.getContentModel()
						.getValueAt(row,
								LarvaViewerNodeModel.COL_IDX_SINGLE_LABELING);

				lab = ((LabelingValue<L>) currentLabelingCell).getLabeling();
				final long[] labMin = new long[lab.numDimensions()];
				final long[] labMax = new long[lab.numDimensions()];
				final long[] labDims = new long[lab.numDimensions()];

				lab.min(labMin);
				lab.max(labMax);
				lab.dimensions(labDims);

				final T max = (T) new ByteType();
				max.setReal(max.getMaxValue());
				img = MiscViews
						.constant(max, new FinalInterval(labMin, labMax));

				metadata = new DefaultImgMetadata(lab.numDimensions());
				// TODO review this code
				LabelingMetadata oldMetdata = ((LabelingValue<L>) currentLabelingCell)
						.getLabelingMetadata();
				MetadataUtil.copySource(oldMetdata, metadata);
				MetadataUtil.copyName(oldMetdata, metadata);
				MetadataUtil.copyTypedSpace(oldMetdata, metadata);

			}

			// Inputmap for transformation issues
			final Map<String, Object> transformationInputMap = new HashMap<String, Object>();
			transformationInputMap.put(LabelTransformVariables.LabelingName
					.toString(), ((LabelingValue<L>) currentLabelingCell)
					.getLabelingMetadata().getName());
			transformationInputMap.put(LabelTransformVariables.LabelingSource
					.toString(), ((LabelingValue<L>) currentLabelingCell)
					.getLabelingMetadata().getSource());
			transformationInputMap.put(
					LabelTransformVariables.ImgName.toString(),
					metadata.getName());
			transformationInputMap.put(
					LabelTransformVariables.ImgSource.toString(),
					metadata.getSource());
			transformationInputMap.put(
					LabelTransformVariables.RowID.toString(),
					m_tableContentView.getContentModel().getRowKey(row));

			if (lab instanceof NativeImgLabeling) {
				if (getNodeModel().isTransformationActive()) {
					final Img<I> nativeImgLabeling = ((NativeImgLabeling<L, I>) lab)
							.getStorageImg();

					final LabelingMapping<String> newMapping = new LabelingMapping<String>(
							nativeImgLabeling.firstElement().createVariable());
					final LabelingMapping<L> oldMapping = lab.firstElement()
							.getMapping();
					for (int i = 0; i < oldMapping.numLists(); i++) {
						final List<String> newList = new ArrayList<String>();
						for (final L label : oldMapping.listAtIndex(i)) {
							transformationInputMap.put(
									LabelTransformVariables.Label.toString(),
									label.toString());
							newList.add(getNodeModel().getTransformer()
									.transform(transformationInputMap));
						}

						newMapping.intern(newList);
					}

					// Unsafe cast but works
					lab = (Labeling<L>) new ExtNativeImgLabeling<String, I>(
							nativeImgLabeling, newMapping);
				}
			} else {
				LOGGER.warn("Labeling Transformer settings don't have any effect, as since now  this is only available for NativeImgLabelings.");
			}

			if (getNodeModel().virtuallyAdjustImgs()) {
				lab = new LabelingView<L>(MiscViews.synchronizeDimensionality(
						lab, ((LabelingValue<L>) currentLabelingCell)
								.getLabelingMetadata(), img, metadata),
						lab.<L> factory());
			}

			LabelingMetadata meta = ((LabelingValue<L>) currentLabelingCell)
					.getLabelingMetadata();
			m_imgView.getEventService().publish(
					new LabelingWithMetadataChgEvent<L>(lab,
							new DefaultLabelingMetadata(metadata, metadata,
									meta, new DefaultLabelingColorTable())));

			m_imgView.getEventService()
					.publish(
							new ImgAndLabelingChgEvent<T, L>(img, lab,
									((LabelingValue<L>) currentLabelingCell)
											.getLabelingMetadata(), metadata,
									metadata));
			m_imgView.getEventService().publish(new ImgRedrawEvent());
		} catch (final IndexOutOfBoundsException e2) {
			return;
		}

	}
}
