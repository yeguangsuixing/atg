package cn.nju.seg.atg.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import cn.nju.seg.atg.Atg.IArgDataViewer;
import cn.nju.seg.atg.cfg.CfgPath;


/**
 * ATG测试数据显示
 * @author ygsx
 * @time 2014/05/07 21:26
 * */
public class AtgView extends ViewPart implements IArgDataViewer{

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "cn.nju.seg.atg.plugin.AtgView";


	private TreeViewer viewer;

	private TreeParent invisibleRoot;

	 
	class TreeObject implements IAdaptable {
		protected String name;
		protected TreeParent parent;
		
		public TreeObject(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setParent(TreeParent parent) {
			this.parent = parent;
		}
		public TreeParent getParent() {
			return parent;
		}
		public String toString() {
			return getName();
		}
		public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
			return null;
		}
	}
	
	class TreeParent extends TreeObject {
		private ArrayList<TreeObject> children;
		public TreeParent(String name) {
			super(name);
			children = new ArrayList<TreeObject>();
		}
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}
		public TreeObject [] getChildren() {
			return (TreeObject [])children.toArray(new TreeObject[children.size()]);
		}
		public boolean hasChildren() {
			return children.size()>0;
		}
		public void removeAllChildren(){
			children.clear();
		}
	}

	/**
	 * CFG路径树节点类
	 * @author ygsx
	 * @time 2014/05/07 21:44
	 * */
	class CfgPathTreeNode extends TreeParent {
		private CfgPath path;
		public CfgPathTreeNode(CfgPath path) {
			super(null);
			if(path == null){
				this.name = "";
			} else {				
				this.name = String.format("[%d/%d]%s", 
						path.getCoverredNodeCount(),
						path.length(), path.getPathString());
			}
			this.path = path;
		}
		public CfgPathTreeNode(String tip){
			super(tip);
		}
	} 
	/** 参数数据节点 */
	class AtgParaDataTreeNode extends TreeObject {

		public AtgParaDataTreeNode(String[] paraSigt, Object[] paras) {
			super(null);
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < paras.length; i ++){
				String parasigt = paraSigt[i];
				Object para = paras[i];
				sb.append(parasigt);
				sb.append("=");
				sb.append(para);
				sb.append(", ");
			}
			this.name = sb.toString();
		}
		
	}
	
	class ViewContentProvider implements IStructuredContentProvider, 
										   ITreeContentProvider {

		public ViewContentProvider(){
			invisibleRoot = new TreeParent("");
		}
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot==null) {
					initialize();
				}
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject)child).getParent();
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent)parent).getChildren();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent)parent).hasChildren();
			return false;
		}

		private void initialize() {
			TreeParent root = new CfgPathTreeNode("No Path");
			invisibleRoot.addChild(root);
		}
	}
	
	class ViewLabelProvider extends LabelProvider {

		Image pathImage, dataImage;
		public ViewLabelProvider(){
			super();
			Bundle bundle = Platform.getBundle(AtgActivator.PLUGIN_ID);
			URL url = bundle.getResource("icons/path.png");
			pathImage = ImageDescriptor.createFromURL(url).createImage();
			dataImage = PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
		
		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
			if (obj instanceof CfgPathTreeNode){
				return pathImage;
			} else {
				return dataImage;
			}
		}
	}
	
	class NameSorter extends ViewerSorter { }
	
	/***
	 * 路径显示器
	 * @author ygsx
	 * @time 2014/05/08 09:41
	 */
	public static interface IPathShower {
		public void showPath(CfgPath path);
	}
	
	private IPathShower fPathShower;
	
	/**
	 * The constructor.
	 */
	public AtgView() { }

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		//drillDownAdapter = new DrillDownAdapter(viewer);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj instanceof CfgPathTreeNode && fPathShower!= null){
					fPathShower.showPath(((CfgPathTreeNode)obj).path);
				}
			}
		});
	}
	
	@Override
	public void showAllPathsData(List<CfgPath> pathList, String[] paraNameArray, boolean asynUpdate){
		invisibleRoot.removeAllChildren();
		for(CfgPath path : pathList){
			CfgPathTreeNode pathnode = new CfgPathTreeNode(path);
			for(int i = 0; i < path.getParasListSize(); i ++){
				Object[] paras = path.getParas(i);
				AtgParaDataTreeNode node = new AtgParaDataTreeNode(paraNameArray, paras);
				pathnode.addChild(node);
			}
			invisibleRoot.addChild(pathnode);
		}
		if(asynUpdate){
			viewer.getTree().getDisplay().asyncExec(new Runnable(){
				@Override
				public void run() {
					viewer.refresh();
				}
			});
		} else {
			viewer.refresh();
		}
	}

	
	public void setPathShower(IPathShower pathShower){
		this.fPathShower = pathShower;
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

}
