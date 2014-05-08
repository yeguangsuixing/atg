package cn.nju.seg.atg.plugin;

import java.io.PrintStream;

import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import cn.nju.seg.atg.Atg;

/**
 * The activator class controls the plug-in life cycle
 * @author ygsx
 * @time 2014/04/23 10:50
 */
@SuppressWarnings("restriction")
public class AtgActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "cn.nju.seg.atg"; //$NON-NLS-1$

	private static final String RES_ID_CFG_CEDITOR =
			"cn.nju.seg.atg.plugin.cfgceditor";
	// The shared instance
	private static AtgActivator plugin;

	/** ATG控制台 */
	/*default*/ MessageConsole fConsole = new MessageConsole("ATG Console",null);
	
	/*default*/ Atg fAtg = null;

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
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(
				new IConsole[]{fConsole});
		fAtg = new Atg(fConsole.newMessageStream());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		ConsolePlugin.getDefault().getConsoleManager()
				.removeConsoles(new IConsole[]{fConsole});
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
	
	public CfgCEditor getCfgCEditor(){
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage();
		IEditorPart ceditor = page.getActiveEditor();
		CfgCEditor cfgCEditor = null;
		if(ceditor instanceof CfgCEditor){
			cfgCEditor = (CfgCEditor)ceditor;
		} else if(ceditor instanceof CEditor){
			try {
				page.openEditor(ceditor.getEditorInput(), 
						RES_ID_CFG_CEDITOR, true, 
						IWorkbenchPage.MATCH_ID | IWorkbenchPage.MATCH_INPUT);
			} catch (PartInitException e) {
				e.printStackTrace(new PrintStream(fConsole.newMessageStream()));
			}
			ceditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActiveEditor();
			if(ceditor instanceof CfgCEditor){
				cfgCEditor = (CfgCEditor)ceditor;
			}
		}
		return cfgCEditor;
	}

}




