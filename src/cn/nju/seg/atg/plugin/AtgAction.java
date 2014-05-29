package cn.nju.seg.atg.plugin;

//import javax.swing.JOptionPane;

import java.io.PrintStream;

import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import cn.nju.seg.atg.Atg;

/**
 * @author ygsx
 * @time 2014/04/23 10:52
 * */
public class AtgAction implements IObjectActionDelegate {

	/** 用于标记用户当前选择的内容 */
	private ISelection fSelection;
	
	@Override
	public void run(IAction action) {
		//JOptionPane.showMessageDialog(null, "Hello, ATG!");
		if (!(fSelection instanceof IStructuredSelection)) return;
		Object funcdecln = ((IStructuredSelection)fSelection).getFirstElement();
		if(!(funcdecln instanceof IFunctionDeclaration)) return;


		CfgCEditor cfgCEditor = AtgActivator.getDefault().getCfgCEditor();
		//IDocumentProvider docprv = ((CEditor)ep).getDocumentProvider();
		
		IWorkbenchPage page =  PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			page.showView(AtgView.ID);
		} catch (PartInitException e) {
			e.printStackTrace(new PrintStream(AtgActivator.getDefault()
					.fConsole.newMessageStream()));
		}
		
		Atg atg = AtgActivator.getDefault().fAtg;
		AtgView atgview = (AtgView)page.findView(AtgView.ID);
		atg.setArgDataViewer(atgview);
		atg.setCfgViewer(cfgCEditor);
		atgview.setPathShower(cfgCEditor);
		
		//
		boolean rsl = atg.setFunctionDeclaration((IFunctionDeclaration) funcdecln);
		if(!rsl) return;
		rsl = atg.generateCfg(true);
		if(!rsl) return;
		atg.pretreatment(cfgCEditor.getCurrentCppFileDir(), cfgCEditor.getCurrentCppFilePath());
		atg.generateData();
		//atg.posttreatment();
		//atg.report();
		
		
		/*
		//生成cfcpp文件
		IFile ifile = ((IFileEditorInput)(fCEditor.getEditorInput())).getFile();
		IProject project = ifile.getProject();
		IFile cfcppifile = project.getFile(new Path(ifile.getName()+".cfcpp"));
		try {
			if(!cfcppifile.exists()){
				cfcppifile.create(ifile.getContents(), true, null);
			} else {
				cfcppifile.setContents(ifile.getContents(), IFile.FORCE, null);
			}
			//IDE.setDefaultEditor(cfcppifile, "cn.nju.seg.atg.plugin.cfgceditor");
			//IDE.openEditor(page, cfcppifile, "cn.nju.seg.atg.plugin.cfgceditor");
			
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (CoreException e1) {
			e1.printStackTrace();
		}//*/

		//fCfgCEditor.setCfgPaintingPanel(atg.getCfgPanel());
		//fCfgCEditor.fCfgRulerColumn.layout(true);
		
	}

	
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.fSelection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) { 
		/*do nothing*/
	}

}
