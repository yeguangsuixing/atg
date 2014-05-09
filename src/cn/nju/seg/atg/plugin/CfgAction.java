package cn.nju.seg.atg.plugin;

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

public class CfgAction implements IObjectActionDelegate {

	/** 用于标记用户当前选择的内容 */
	private ISelection fSelection;
	
	@Override
	public void run(IAction action) {
		if(fSelection == null) return;
		if (!(fSelection instanceof IStructuredSelection)) return;
		Object funcdecln = ((IStructuredSelection)fSelection).getFirstElement();
		if(!(funcdecln instanceof IFunctionDeclaration)) return;
		
		CfgCEditor cfgCEditor = AtgActivator.getDefault().getCfgCEditor();
		Atg atg = AtgActivator.getDefault().fAtg;
		
		IWorkbenchPage page =  PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			page.showView(AtgView.ID);
		} catch (PartInitException e) {
			e.printStackTrace(new PrintStream(AtgActivator.getDefault()
					.fConsole.newMessageStream()));
		}
		AtgView atgview = (AtgView)page.findView(AtgView.ID);
		atg.setArgDataViewer(atgview);
		atg.setCfgViewer(cfgCEditor);
		atgview.setPathShower(cfgCEditor);
		
		atg.setFunctionDeclaration((IFunctionDeclaration) funcdecln);
		atg.generateCfg(true);
		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.fSelection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		/* do nothing */
	}

}
