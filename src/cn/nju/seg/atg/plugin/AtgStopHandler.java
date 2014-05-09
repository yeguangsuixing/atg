package cn.nju.seg.atg.plugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import cn.nju.seg.atg.Atg;
import cn.nju.seg.atg.Atg.IMsgShower;


public class AtgStopHandler extends AbstractHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final IWorkbenchWindow window = HandlerUtil.
				getActiveWorkbenchWindowChecked(event);
		Atg atg = AtgActivator.getDefault().fAtg;
		atg.stop(new IMsgShower(){
			@Override
			public void showMsg(String msg) {
				MessageDialog.openError(window.getShell(),"Atg", msg);
			}
		});
		
		return null;
	}
}