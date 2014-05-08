package cn.nju.seg.atg.plugin;

//import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
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
@SuppressWarnings("restriction")
public class AtgAction implements IObjectActionDelegate {

	private static final String RES_ID_CFG_NODE =
			"cn.nju.seg.atg.plugin.cfgceditor";
	
	private static final String RES_ID_VIEW =
			"cn.nju.seg.atg.plugin.AtgView";
	
	/** 用于标记用户当前选择的内容 */
	private ISelection fSelection;
	/** cfg C编辑器 */
	private CfgCEditor fCfgCEditor;
	/** 标注模型 */
	private IAnnotationModel fAnnotationModel;
	/** 标记列表 */
	private List<Annotation> fAnnotationList = new ArrayList<Annotation>();
	
	private Atg atg;
	
	@Override
	public void run(IAction action) {
		//JOptionPane.showMessageDialog(null, "Hello, ATG!");
		if (!(fSelection instanceof IStructuredSelection)) return;
		Object funcdecln = ((IStructuredSelection)fSelection).getFirstElement();
		if(!(funcdecln instanceof IFunctionDeclaration)) return;


		fCfgCEditor = AtgActivator.getDefault().getCfgCEditor();
		//IDocumentProvider docprv = ((CEditor)ep).getDocumentProvider();
		ISourceViewer sourceView = fCfgCEditor.getViewer();
		fAnnotationModel = sourceView.getAnnotationModel();

		
		IWorkbenchPage page =  PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			page.showView(RES_ID_VIEW);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
		
		if(atg == null){
			atg = AtgActivator.getDefault().fAtg;
		}
		AtgView atgview = (AtgView)page.findView(RES_ID_VIEW);
		atg.setArgDataViewer(atgview);
		atgview.setPathShower(fCfgCEditor);
		
		//生成测试数据
		atg.setFunctionDeclaration((IFunctionDeclaration) funcdecln);
		atg.generateCfg(true);
		
		if(fCfgCEditor != null){
			fCfgCEditor.updateData(atg.getCfgEntry());
			fCfgCEditor.updateUi();
		}
		
		atg.pretreatment();
		atg.generateData();
		//atg.posttreatment();
		
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
	
	/** 
	 * 添加标注
	 * @param hoverString 当鼠标移动到标注上显示的信息
	 * @param offset 标注起始偏移量
	 * @param length 标注长度
	 *  */
	public void addAnnotation(String hoverString, int offset, int length){
		Annotation annotation = new Annotation( RES_ID_CFG_NODE, 
				true, hoverString);
		Position pos = new Position(offset, length);
		this.fAnnotationModel.addAnnotation(annotation, pos);
		this.fAnnotationList.add(annotation);
	}
	
	/** 清除所有标注 */
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
		/*do nothing*/
	}

}
