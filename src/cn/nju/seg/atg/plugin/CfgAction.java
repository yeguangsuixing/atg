package cn.nju.seg.atg.plugin;

import javax.swing.JOptionPane;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @author ygsx
 * @time 2014/04/23 10:51
 * */
public class CfgAction implements IObjectActionDelegate {

	@Override
	public void run(IAction action) {
		// TODO Auto-generated method stub
		JOptionPane.showMessageDialog(null, "Hello, CFG!");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub

	}

}
