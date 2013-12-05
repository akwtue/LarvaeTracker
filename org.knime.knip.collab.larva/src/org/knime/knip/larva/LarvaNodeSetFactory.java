package org.knime.knip.larva;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.config.ConfigRO;

/**
 * 
 * @author dietzc, hornm (University of Konstanz)
 */
public class LarvaNodeSetFactory implements NodeSetFactory {

	private Map<String, String> m_nodeFactories = new HashMap<String, String>();

	@Override
	public Collection<String> getNodeFactoryIds() {

		return m_nodeFactories.keySet();
	}

	@Override
	public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(
			String id) {
		try {
			return (Class<? extends NodeFactory<? extends NodeModel>>) Class
					.forName(id);
		} catch (ClassNotFoundException e) {
		}
		return null;
	}

	@Override
	public String getCategoryPath(String id) {
		return m_nodeFactories.get(id);
	}

	@Override
	public String getAfterID(String id) {
		return "/";
	}

	@Override
	public ConfigRO getAdditionalSettings(String id) {
		return null;
	}

}
