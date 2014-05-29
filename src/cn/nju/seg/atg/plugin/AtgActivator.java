package cn.nju.seg.atg.plugin;

import java.io.PrintStream;
import java.util.List;

import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.jface.util.Util;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import cn.nju.seg.atg.Atg;
import cn.nju.seg.atg.AtgReport;
import cn.nju.seg.atg.cfg.CfgPath;

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
	
	/**
	 * 操作系统类型
	 * @author ygsx
	 * @time 2014/05/14 11:02
	 * */
	public static enum OperatingSystem {
		Unknown,
		Linux, 
		Windows
	}
	/** 当前操作系统类型 */
	public static OperatingSystem OS = OperatingSystem.Unknown;
	

	static {
		if(Util.isLinux()){
			OS = OperatingSystem.Linux;
		} else if(Util.isWindows()){
			OS = OperatingSystem.Windows;
		}
	}

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
		ConsolePlugin.getDefault().getConsoleManager()
			.addConsoles(new IConsole[]{fConsole});
		MessageConsoleStream stream = 
				fConsole.newMessageStream();
		fAtg = new Atg(stream);
		fAtg.setAtgReportViewer(new ReportViewer(stream));
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

class ReportViewer implements Atg.IAtgReportViewer {

	private MessageConsoleStream stream;
	
	private static final String REPORT_TEMPLATE
			= "\n-----------ATG Report-----------\n"
			+ "File:%s\n"
			+ "Func:%s\n"
			+ "Start:\t%s\n"
			+ "Stop:\t%s\n"
			+ "Tick:%d(Algo:%d, Prog:%d)\n"
			+ "Cover:%f(%d/%d)\n"
			+ "Total-Dtct:%d\n"
			+ "Avg-Dtct:%f\n"
			+ "Path Detail:\n"
			+ "Id\tDetect\tCovered nodes\tTotal nodes\n%s";
	private static final String PATH_INFO_TEMPLATE
			= "%d\t%d\t%d\t%d\n";
	
	public ReportViewer(MessageConsoleStream stream){
		this.stream = stream;
	}
	
	@Override
	public void showReport(AtgReport report) {
		StringBuilder sb = new StringBuilder();
		List<CfgPath> pathlist = report.getPathList();
		for(CfgPath path : pathlist){
			sb.append(String.format(PATH_INFO_TEMPLATE, 
					path.getId(), path.getDetect(),
					path.getCoverredNodeCount(),
					path.getPath().size()));
		}
		String reportString = String.format(REPORT_TEMPLATE, 
				report.getProgramFile(),
				report.getFuncSignature(),
				report.getStartTime(),
				report.getStopTime(),
				report.getTotalTick(),
				report.getAlgorithmTick(),
				report.getProgramTick(),
				report.getAverageCoverRatio(),
				report.getCoveredPathCount(),
				report.getPathCount(),
				report.getTotalDetect(),
				report.getAverageDetect(),
				sb.toString()
				);
		stream.println(reportString);
	}
	
}










