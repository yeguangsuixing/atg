package cn.nju.seg.atg.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;

import cn.nju.seg.atg.cfg.CfgCondNode;
import cn.nju.seg.atg.cfg.CfgNode;
import cn.nju.seg.atg.cfg.CfgNode.Type;
import cn.nju.seg.atg.cfg.CfgPath;
import cn.nju.seg.atg.plugin.AtgView.IPathShower;

@SuppressWarnings("restriction")
public class CfgCEditor extends CEditor implements IPathShower {

	private static final String RES_ID_CFG_NODE =
			"cn.nju.seg.atg.TextEditor.AtgNode";

	/** CFG图的起始x偏移量 */
	/*default*/ static final int INTENTAITION = 15;
	
	/*default*/ static final int SINGLE_NODE_WIDTH = 18;
	
	private CfgRulerColumn fCfgRulerColumn = new CfgRulerColumn();

	/** 根节点 */
	private GraphNode fRoot;
	
	private List<GraphNode> allNodesList = new ArrayList<GraphNode>();
	
	/*default*/ static enum NodeType {
		/** 基准节点，用于分支节点(If/Loop)的子节点的基准 */
		Base,
		Normal, If, Loop, Break, Continue, Return
	}
	
	/*default*/ static class GraphNode {
		public NodeType type;
		/** 当前节点所在文档的行 */
		public int line;
		
		public boolean reachable;
		
		public int x, y;
		
		/** 宽度/高度 */
		public int width;
		
		public List<GraphNode> prevNodeList = new ArrayList<GraphNode>(2);
		
		public GraphNode next;
		
		/** 对于break节点和continue节点，我们使用then来指向对应的循环节点。
		 * 也就是说，如果普通节点的then不为null，说明是一个转到下一次循环的节点。 */
		public GraphNode then;

		//父节点
		public GraphNode parentNode;
		
		public CfgNode cfgMerge;
		
		public GraphNode merge;
		
		public CfgNode cfgNode;
		
		public GraphNode(CfgNode cfgnode, GraphNode parent, NodeType type, 
				int width, boolean reachable){
			this.cfgNode = cfgnode;
			this.line = cfgnode.getLine();
			this.type = type;
			this.parentNode = parent;
			this.width = width;
			this.reachable = reachable;
		}
		
		public String toString(){
			return this.cfgNode.toString();
		}
	}

	
	
	/**
	 * 递归设置节点信息
	 * @param cfgNode 当前要处理的cfg节点
	 * @param lastForNode 上一次for节点
	 * @param baseNode 基准节点
	 * @param parentNode 父节点
	 * @param stopNode 停止节点
	 * @param leaves 叶子列表
	 * @param reachable 是否可达
	 * @return 当前cfg节点对应的节点
	 * */
	private GraphNode handleCfgNode(CfgNode cfgNode, GraphNode lastForNode, 
			GraphNode baseNode, GraphNode parentNode, CfgNode stopNode, List<GraphNode> leaves,
			boolean reachable){
		//不处理循环迭代节点
		if(cfgNode == null) return null;
		if(cfgNode.getType() == Type.Iterate) return null;
		
		//int lineheight = fLineHeight.getLineHeight(cfgNode.getLine());
		if(cfgNode.getType() == Type.Normal){
			GraphNode newnode = new GraphNode(cfgNode, parentNode, NodeType.Normal,
					SINGLE_NODE_WIDTH, reachable);
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = newnode;
					newnode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			if(stopNode != null && cfgNode.getNext() == stopNode){
				newnode.next = null;
				if(leaves != null){
					leaves.add(newnode);
				}
				return newnode;
			}
			if(lastForNode!= null && cfgNode.getNext() == lastForNode.cfgNode){
				//此处为while循环设定
				newnode.next = null;
				newnode.then = lastForNode;
				if(leaves != null){
					leaves.add(newnode);
				}
				return newnode;
			}
			//下一个节点如果是循环迭代节点，那么直接终止
			if(cfgNode.getNext() != null && cfgNode.getNext().getType() == Type.Iterate){
				//也就是说，如果普通节点的then不为null，说明是一个转到下一次循环的节点
				newnode.then = lastForNode;
				return newnode;
			}
			//cfgNode.next一定不为空，因为末尾节点一定是tail
			List<GraphNode> innerleaves = new ArrayList<GraphNode>(2);
			GraphNode child = handleCfgNode(cfgNode.getNext(), lastForNode, 
					baseNode, newnode, stopNode, innerleaves, reachable);
			leaves.addAll(innerleaves);
			newnode.width = child.width;
			newnode.next = child;
			if(newnode.line == child.line){//处于同一行
				newnode.width = SINGLE_NODE_WIDTH + child.width;
			}
			return newnode;
		} else if(cfgNode.getType() == Type.CondIf){//TODO 空语句
			CfgCondNode condnode = (CfgCondNode)cfgNode;
			GraphNode ifnode = new GraphNode(cfgNode, parentNode, NodeType.If, 
					0, reachable);
			if(condnode.merge == null){
				condnode.merge = condnode.getNext();
			}
			ifnode.cfgMerge = condnode.merge;

			//合并叶子节点
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = ifnode;
					ifnode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			List<GraphNode> innerleaves = new ArrayList<GraphNode>(3);
			CfgNode newStopNode = condnode.merge!=null?condnode.merge:stopNode;
			GraphNode childthen = handleCfgNode(condnode.getThen(), lastForNode,
					ifnode, ifnode, newStopNode, innerleaves, reachable);
			if(leaves == null){
				leaves = new ArrayList<GraphNode>(3);
			}
			if(innerleaves.size() > 0){
				leaves.addAll(innerleaves);//内层叶子->当前层叶子
			}
			ifnode.then = childthen;
			GraphNode childelse = null;
			if(condnode.getNext().isElse()){//存在else分支
				innerleaves.clear();
				childelse = handleCfgNode(cfgNode.getNext(), lastForNode,
						ifnode, ifnode, newStopNode, innerleaves, reachable);
				leaves.addAll(innerleaves);//内层叶子->当前层叶子
				ifnode.next = childelse;
			} else {
				leaves.add(ifnode);
			}


			GraphNode child = null;
			if(condnode.merge != null &&
					(baseNode == null || baseNode.cfgMerge != condnode.merge)) {
				child = handleCfgNode(condnode.merge, lastForNode,
						baseNode, ifnode, stopNode, leaves,
						//如果上面的分支没有叶子节点，说明这是一个不可达节点
						leaves.size()>0?reachable:false);
				if(childelse != null)
					child.parentNode = childelse;
			}
			ifnode.merge = child;
			if(ifnode.next == null){
				ifnode.next = child;
			}
			int childwidth = 0, childline= 0;
			if(child != null){
				childwidth = child.width;
				childline = child.line;
			}
			if(childelse == null){//不存在else节点
				if(ifnode.line == childline){//处于同一行
					ifnode.width = SINGLE_NODE_WIDTH + childthen.width
							+ childwidth;//TODO 
				} else {
					ifnode.width = SINGLE_NODE_WIDTH
							+ childthen.width;
				}
			} else {//如果存在else分支
				if(ifnode.line == childelse.line){
					//else节点与if分支节点处于同一行，说明then节点也在这一行
					//向左右两边扩展宽度
					ifnode.width = childelse.width + SINGLE_NODE_WIDTH
							+ childthen.width;//TODO
				} else {//else节点与if分支节点处于不同行
					if(childthen.line == childelse.width){
						//TODO 
						ifnode.width = childthen.width + childelse.width;
					} else {
						ifnode.width = childthen.width + childelse.width;
					}
				}
			}
			return ifnode;
		} else if(cfgNode.getType() == Type.CondLoop){//TODO 空语句
			CfgCondNode condnode = (CfgCondNode)cfgNode;
			GraphNode fornode = new GraphNode(cfgNode, parentNode, NodeType.Loop, 
					0, reachable);
			//合并叶子节点
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = fornode;
					fornode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			//List<Node> innerleaves = new ArrayList<Node>(3);
			GraphNode childblock = handleCfgNode(condnode.getThen(), fornode,
					fornode, fornode, condnode.getNext(), null, reachable);
			GraphNode child = null;
			if(condnode.getNext() != stopNode){
				child = handleCfgNode(condnode.getNext(), lastForNode, 
						baseNode, fornode, stopNode, leaves, reachable);
			}
			fornode.then = childblock;
			fornode.next = child;
			int childblockwidth = childblock.width, childblockline= childblock.line;
			if(child == null){
				leaves.add(fornode);
			}
			if(childblockline == fornode.line){//TODO
				fornode.width = SINGLE_NODE_WIDTH + childblockwidth;
			} else {
				fornode.width = SINGLE_NODE_WIDTH + childblockwidth;
			}
			return fornode;
		} else if(cfgNode.getType() == Type.Break){
			GraphNode breaknode = new GraphNode(cfgNode, parentNode, NodeType.Break, 
					SINGLE_NODE_WIDTH, reachable);
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = returnnode;
					if(node.merge == null && node.type == NodeType.If){
						node.merge = breaknode;
					}
					breaknode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			breaknode.then = lastForNode;
			return breaknode;
		} else if(cfgNode.getType() == Type.Continue){
			GraphNode continuenode = new GraphNode(cfgNode, parentNode, NodeType.Continue, 
					SINGLE_NODE_WIDTH, reachable);
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = returnnode;
					if(node.merge == null && node.type == NodeType.If){
						node.merge = continuenode;
					}
					continuenode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			continuenode.then = lastForNode;
			return continuenode;
		} else if(cfgNode.getType() == Type.Return){
			GraphNode returnnode = new GraphNode(cfgNode, parentNode, NodeType.Return, 
					SINGLE_NODE_WIDTH, reachable);
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = returnnode;
					if(node.merge == null && node.type == NodeType.If){
						node.merge = returnnode;
					}
					returnnode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			returnnode.next = null;
			return returnnode;
		}
		
		return null;
	}//*/
	
	
	/**
	 * 重写{@link CEditor#createSourceViewer}，添加Cfg标尺
	 * */
	@Override
	protected ISourceViewer createSourceViewer(Composite parent, 
			IVerticalRuler ruler, int styles) {
		/* 添加Cfg标尺 */
		if(ruler instanceof CompositeRuler){
			CompositeRuler comruler = (CompositeRuler)ruler;
			comruler.addDecorator(0, fCfgRulerColumn);
		}
		return super.createSourceViewer(parent, ruler, styles);
	}
	
	public void updateData(CfgNode cfgEntry){
		if(cfgEntry == null) return;
		//处理宽高
		this.allNodesList.clear();
		this.fRoot = handleCfgNode(cfgEntry, 
				null, //last for-node is null
				null, //not base node at first
				null, //parent is null
				null, //never stop
				new ArrayList<GraphNode>(), //empty list-no child
				true //reachable
		);
		this.fCfgRulerColumn.fPaintingPanel.setWidth(
				CfgCEditor.INTENTAITION*2 + this.fRoot.width);
		this.fCfgRulerColumn.setGraphRoot(fRoot);
	}
	

	/** 标注模型 */
	private IAnnotationModel fAnnotationModel;
	/** 标记列表 */
	private List<Annotation> fAnnotationList = new ArrayList<Annotation>();

	/**
	 * 显示路径
	 * */
	@Override
	public void showPath(CfgPath path){
		clearAnnotations();
		for(CfgNode node : path.getPath()){
			Set<Entry<Integer, Integer>> set = node.getSrcMapSet();
			for(Entry<Integer, Integer> entry : set){
				Annotation annotation = new Annotation( RES_ID_CFG_NODE, 
						true, node.toString());
				Position pos = new Position(entry.getKey(), entry.getValue());
				this.fAnnotationModel.addAnnotation(annotation, pos);
				this.fAnnotationList.add(annotation);
			}
		}
		
		
		this.fCfgRulerColumn.fPaintingPanel.setFocusPath(path);
		updateUi();
	}
	
	/** 清空所有标注 */
	public void clearAnnotations(){
		this.fAnnotationModel = getViewer().getAnnotationModel();
		for(Annotation an : fAnnotationList){
			this.fAnnotationModel.removeAnnotation(an);
		}
		this.fAnnotationList.clear();
	}

	
	/** 更新标尺 */
	public void updateUi(){
		this.fCfgRulerColumn.layout(true);
		this.fCfgRulerColumn.getControl().update();
		this.fCfgRulerColumn.redraw();
	}
	
	
	
	public void doSave(IProgressMonitor progressMonitor){
		super.doSave(progressMonitor);
		AtgActivator.getDefault().fAtg.updateCfg();
		updateData(AtgActivator.getDefault().fAtg.getCfgEntry());
		updateUi();
	}

	/** 获取当前编辑器所编辑的cpp文件绝对路径 */
	public String getCurrentCppFilePath(){
		IFile ifile = ((IFileEditorInput)(getEditorInput())).getFile();
		return ifile.getLocationURI().getPath();
	}
}




