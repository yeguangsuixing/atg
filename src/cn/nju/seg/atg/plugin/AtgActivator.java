package cn.nju.seg.atg.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * @author ygsx
 * @time 2014/04/23 10:50
 */
public class AtgActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "cn.nju.seg.atg"; //$NON-NLS-1$

	// The shared instance
	private static AtgActivator plugin;
	
	/**
	 * 构造函数
	 */
	public AtgActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AtgActivator getDefault() {
		return plugin;
	}

}
