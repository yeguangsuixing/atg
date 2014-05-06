package cn.nju.seg.atg.plugin;

//import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;

import cn.nju.seg.atg.Atg;

/**
 * @author ygsx
 * @time 2014/04/23 10:52
 * */
@SuppressWarnings("restriction")
public class AtgAction implements IObjectActionDelegate {
	
	/** 用于标记用户当前选择的内容 */
	private ISelection fSelection;
	/** 是否已经添加控制台 */
	private boolean fAddedConsole = false;
	/** ATG控制台 */
	private MessageConsole fConsole = new MessageConsole("ATG Console",null);
	/** 当前编辑器 */
	private IEditorPart fCEditor;
	/** cfg C编辑器 */
	private CfgCEditor fCfgCEditor;
	/** 标注模型 */
	private IAnnotationModel fAnnotationModel;
	/** 标记列表 */
	private List<Annotation> fAnnotationList = new ArrayList<Annotation>();
	
	@Override
	public void run(IAction action) {
		//JOptionPane.showMessageDialog(null, "Hello, ATG!");
		if (!(fSelection instanceof IStructuredSelection)) return;

		IWorkbenchPage page =  PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage();
		fCEditor = page.getActiveEditor();
		//IDocumentProvider docprv = ((CEditor)ep).getDocumentProvider();
		ISourceViewer sourceView = ((CEditor)fCEditor).getViewer();
		fAnnotationModel = sourceView.getAnnotationModel();
		Object funcdecln = ((IStructuredSelection)fSelection).getFirstElement();
		if(!(funcdecln instanceof IFunctionDeclaration)) return;
		
		if(!fAddedConsole){//添加控制台
			fAddedConsole = true;
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(
					new IConsole[]{fConsole});
		}
		

		//生成测试数据
		Atg atg = new Atg(this, fConsole.newMessageStream(), 
				(IFunctionDeclaration)funcdecln);
		atg.pretreatment();
		
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
			page.openEditor(fCEditor.getEditorInput(), 
					"cn.nju.seg.atg.plugin.cfgceditor", true, 
					IWorkbenchPage.MATCH_ID | IWorkbenchPage.MATCH_INPUT);
			fCEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActiveEditor();
			if(fCEditor instanceof CfgCEditor){
				fCfgCEditor = (CfgCEditor)fCEditor;
				sourceView = fCfgCEditor.getViewer();
				fAnnotationModel = sourceView.getAnnotationModel();
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (CoreException e1) {
			e1.printStackTrace();
		}

		fCfgCEditor.setCfgPaintingPanel(atg.getCfgPanel());
		fCfgCEditor.fCfgRulerColumn.layout(true);
		
		atg.run();
		
		
	}
	
	/** 添加标注 */
	public void addAnnotation(String hoverString, int offset, int length){
		Annotation annotation = new Annotation(
				"cn.nju.seg.atg.TextEditor.AtgNode", true, hoverString);
		Position pos = new Position(offset, length);
		this.fAnnotationModel.addAnnotation(annotation, pos);
		this.fAnnotationList.add(annotation);
	}
	
	/** 清除标注 */
	public void clearAllAnnotations(){
		for(Annotation an : fAnnotationList){
			fAnnotationModel.removeAnnotation(an);
		}
		this.fAnnotationList.clear();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.fSelection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub
		return;
	}

}
