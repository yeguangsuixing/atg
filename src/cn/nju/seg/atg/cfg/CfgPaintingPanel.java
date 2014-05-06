package cn.nju.seg.atg.cfg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import cn.nju.seg.atg.cfg.CfgNode.Type;
import cn.nju.seg.atg.plugin.CfgRulerColumn.ICfgPaintingPanel;
import cn.nju.seg.atg.plugin.CfgRulerColumn.LineHeightDelegate;

/***
 * cfg标尺画笔类
 * @author ygsx
 * @time 2014/05/03 15:08
 * */
public class CfgPaintingPanel implements ICfgPaintingPanel {


	private static enum NodeType {
		/** 基准节点，用于分支节点(If/Loop)的子节点的基准 */
		Base,
		Normal, If, Loop, Break, Continue, Return
	}
	
	private static class GraphNode {
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
	
	
	private Cfg fCfg;
	
	/** 附加头节点 */
	private GraphNode fHeader;
	/** 画板宽度 */
	private int fWidth;
	
	private boolean handle = false;
	
	/** 焦点路径 */
	private CfgPath fFocusPath;
	
	//private LineHeightDelegate fLineHeight;
	
	public CfgPaintingPanel(Cfg cfg){
		this.fCfg = cfg;
	}
	
	
	private static final int SINGLE_NODE_WIDTH = 18;
	private static final int NODE_PADDING_WIDTH = 18;
	//private static final int FOR_DIRECT_WIDTH = 18;
	
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
		if(cfgNode.type == Type.Iterate) return null;
		
		//int lineheight = fLineHeight.getLineHeight(cfgNode.getLine());
		if(cfgNode.type == Type.Normal){
			GraphNode newnode = new GraphNode(cfgNode, parentNode, NodeType.Normal,
					SINGLE_NODE_WIDTH, reachable);
			if(leaves != null && leaves.size() > 0){
				for(GraphNode node : leaves) {
					//node.next = newnode;
					newnode.prevNodeList.add(node);
				}
				leaves.clear();
			}
			if(stopNode != null && cfgNode.next == stopNode){
				newnode.next = null;
				if(leaves != null){
					leaves.add(newnode);
				}
				return newnode;
			}
			if(lastForNode!= null && cfgNode.next == lastForNode.cfgNode){
				//此处为while循环设定
				newnode.next = null;
				newnode.then = lastForNode;
				if(leaves != null){
					leaves.add(newnode);
				}
				return newnode;
			}
			//下一个节点如果是循环迭代节点，那么直接终止
			if(cfgNode.next != null && cfgNode.next.type == Type.Iterate){
				//也就是说，如果普通节点的then不为null，说明是一个转到下一次循环的节点
				newnode.then = lastForNode;
				return newnode;
			}
			//cfgNode.next一定不为空，因为末尾节点一定是tail
			List<GraphNode> innerleaves = new ArrayList<GraphNode>(2);
			GraphNode child = handleCfgNode(cfgNode.next, lastForNode, 
					baseNode, newnode, stopNode, innerleaves, reachable);
			leaves.addAll(innerleaves);
			newnode.width = child.width;
			newnode.next = child;
			if(newnode.line == child.line){//处于同一行
				newnode.width = SINGLE_NODE_WIDTH
						+ NODE_PADDING_WIDTH + child.width;
			}
			return newnode;
		} else if(cfgNode.type == Type.CondIf){//TODO 空语句
			CfgCondNode condnode = (CfgCondNode)cfgNode;
			GraphNode ifnode = new GraphNode(cfgNode, parentNode, NodeType.If, 
					0, reachable);
			if(condnode.merge == null){
				condnode.merge = condnode.next;
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
			GraphNode childthen = handleCfgNode(condnode.then, lastForNode,
					ifnode, ifnode, newStopNode, innerleaves, reachable);
			if(leaves == null){
				leaves = new ArrayList<GraphNode>(3);
			}
			if(innerleaves.size() > 0){
				leaves.addAll(innerleaves);//内层叶子->当前层叶子
			}
			ifnode.then = childthen;
			GraphNode childelse = null;
			if(condnode.next.isElse){//存在else分支
				innerleaves.clear();
				childelse = handleCfgNode(cfgNode.next, lastForNode,
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
					ifnode.width = SINGLE_NODE_WIDTH + NODE_PADDING_WIDTH
							+ childthen.width + childwidth;//TODO 
				} else {
					ifnode.width = SINGLE_NODE_WIDTH
							+ childthen.width;
				}
			} else {//如果存在else分支
				if(ifnode.line == childelse.line){
					//else节点与if分支节点处于同一行，说明then节点也在这一行
					//向左右两边扩展宽度
					ifnode.width = childelse.width + NODE_PADDING_WIDTH
							+ SINGLE_NODE_WIDTH +  NODE_PADDING_WIDTH
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
		} else if(cfgNode.type == Type.CondLoop){//TODO 空语句
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
			GraphNode childblock = handleCfgNode(condnode.then, fornode,
					fornode, fornode, condnode.next, null, reachable);
			GraphNode child = handleCfgNode(condnode.next, lastForNode, 
					baseNode, fornode, stopNode, leaves, reachable);
			fornode.then = childblock;
			fornode.next = child;
			int childblockwidth = 0, childblockline= 0;
			if(child != null){
				childblockwidth = childblock.width;
				childblockline = childblock.line;
			}
			if(childblockline == fornode.line){//TODO
				fornode.width = SINGLE_NODE_WIDTH + childblockwidth;
			} else {
				fornode.width = SINGLE_NODE_WIDTH + childblockwidth;
			}
			return fornode;
		} else if(cfgNode.type == Type.Break){
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
		} else if(cfgNode.type == Type.Continue){
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
		} else if(cfgNode.type == Type.Return){
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
	}
	
	private static final int INTENTAITION = 15;
	
	@Override
	public void paint(GC gc, ILineRange lineRange, 
			LineHeightDelegate lineHeightDelegate, int yStart) {

		//this.fLineHeight = lineHeight;
		int linestart = lineRange.getStartLine();
		int linestop = linestart + lineRange.getNumberOfLines();
		int lineHeight = lineHeightDelegate.getLineHeight(0);
		yStart -= lineHeight*linestart;
		
		if(!this.handle){
			CfgNode entry = this.fCfg.getEntry();
			if(entry.next == null){
				return;
			}
			if(entry.type == Type.Head){
				entry = entry.next;//跳过附加头节点
			}
			//处理宽高
			this.fHeader = handleCfgNode(entry, 
					null, //last for-node is null
					null, //not base node at first
					null, //parent is null
					null, //never stop
					new ArrayList<GraphNode>(), //empty list-no child
					true //reachable
			);

			this.fWidth = this.fHeader.width;
			
			this.handle = true;
		}
		//绘制控制流图
		this.fHeader.x = INTENTAITION;
		this.drawCfg(gc, this.fHeader, lineHeight, yStart, linestop);
	}
	
	private void drawCfg(GC gc, GraphNode curNode, int lineheight, int yStart, int lineStop){
		if(curNode == null) return;
		//curNode.x is set by last calling the func
		curNode.y = lineheight*curNode.line - lineheight/2 + yStart;
		for(GraphNode node : curNode.prevNodeList){
			drawLine(gc, curNode, node);
		}
		if(curNode.parentNode != null) {
			drawLine(gc, curNode, curNode.parentNode);
		}
		if(curNode.type == NodeType.Normal){
			if(curNode.next != null) {
				curNode.next.x = curNode.x;
				drawCfg(gc, curNode.next, lineheight, yStart, lineStop);
			}
			if(curNode.then != null){//回退到循环分支处
				drawBack(gc, curNode, curNode.then);
			}
		} else if(curNode.type == NodeType.If){
			if(curNode.then != null) {
				curNode.then.x = curNode.x + (curNode.next == null?
						SINGLE_NODE_WIDTH:curNode.next.width);
				drawCfg(gc, curNode.then, lineheight, yStart, lineStop);
			}
			if(curNode.next != null && curNode.next != curNode.merge) {
				curNode.next.x = curNode.x;//TODO 不考虑同一行
				drawCfg(gc, curNode.next, lineheight, yStart, lineStop);
			}
			if(curNode.merge != null){
				curNode.merge.x = curNode.x;
				drawCfg(gc, curNode.merge, lineheight, yStart, lineStop);
			}
		} else if(curNode.type == NodeType.Loop){
			curNode.then.x = curNode.x + SINGLE_NODE_WIDTH;
			drawCfg(gc, curNode.then, lineheight, yStart, lineStop);
			if(curNode.next != null) {
				curNode.next.x = curNode.x;//TODO 不考虑同一行
				drawCfg(gc, curNode.next, lineheight, yStart, lineStop);
			}
		} else if(curNode.type == NodeType.Continue){
			drawLine(gc, curNode, curNode.parentNode);
			drawBack(gc, curNode, curNode.then);
		} else if(curNode.type == NodeType.Break){
			drawLine(gc, curNode, curNode.parentNode);
			drawBack(gc, curNode, curNode.then);
		} else if(curNode.type == NodeType.Return){
			//TODO 绘制向下箭头
			//return;
		}
		drawNode(gc, curNode);
		
	}
	
	private static final int LINE_MARGIN = 1;
	private static final int FOR_NEXT_LINE_MARGIN = 3;
	private static final int NODE_WIDTH = 10;
	private static final int LOOP_PADDING = 8;
	private static final int BREAK_POINT_RADIUS = 4;

	//private static final Color NORMAL_LINE_COLOR = new Color(null, 0, 0, 0);
	private static final Color BACKWARD_LINE_COLOR = new Color(null, 0, 0, 255);
	private static final Color FOR_FRAME_LINE_COLOR = new Color(null, 128, 0, 0);
	
	/** 绘制线条 */
	private void drawLine(GC gc, GraphNode curNode, GraphNode lastNode){
		if(lastNode == null || !curNode.reachable) return;
		
		boolean drawback = false;
		if(lastNode.type == NodeType.Loop && curNode == lastNode.next){
			Color oldfgc = gc.getForeground();
			if(this.isFocusNode(curNode) && this.isFocusNode(lastNode)){
				gc.setForeground(EXEC_COLOR);
			} else {
				gc.setForeground(FOR_FRAME_LINE_COLOR);
			}
			gc.drawLine(curNode.x, curNode.y - FOR_NEXT_LINE_MARGIN,
					curNode.x - LOOP_PADDING, curNode.y - FOR_NEXT_LINE_MARGIN);
			gc.drawLine(curNode.x - LOOP_PADDING, curNode.y - FOR_NEXT_LINE_MARGIN, 
					lastNode.x - LOOP_PADDING, lastNode.y);
			gc.drawLine(lastNode.x - LOOP_PADDING, lastNode.y, 
					lastNode.x, lastNode.y);
			gc.setForeground(oldfgc);
			drawback = true;
		}
		
		Color oldfgc = gc.getForeground();
		if(this.isFocusNode(curNode) && this.isFocusNode(lastNode)){
			gc.setForeground(EXEC_COLOR);
		}
		if(lastNode.type == NodeType.If 
				|| lastNode.type == NodeType.Loop && lastNode.then == curNode ){
			//先水平，后向下
			gc.drawLine(curNode.x, lastNode.y + LINE_MARGIN, 
					lastNode.x, lastNode.y + LINE_MARGIN);
			gc.drawLine(curNode.x, curNode.y, 
					curNode.x, lastNode.y + LINE_MARGIN);
		} else if(curNode.x != lastNode.x){//不再同一条垂直线上
			//先向下，后水平
			gc.drawLine(lastNode.x, curNode.y - LINE_MARGIN, 
					lastNode.x, lastNode.y);
			gc.drawLine(curNode.x, curNode.y - LINE_MARGIN, 
					lastNode.x, curNode.y - LINE_MARGIN);//*/
		} else if(!drawback){
			gc.drawLine(curNode.x, curNode.y, lastNode.x, lastNode.y);
		}
		if(this.isFocusNode(curNode) && this.isFocusNode(lastNode)){
			gc.setForeground(oldfgc);
		}
	}
	
	
	
	private void drawBack(GC gc, GraphNode curNode, GraphNode backNode){
		if(!curNode.reachable) return;
		Color oldfgc = gc.getForeground();
		if(curNode.type == NodeType.Normal && curNode.then == backNode
				|| curNode.type == NodeType.Continue){
			//回退边---先向左，后向上
			if(this.isFocusNode(curNode) && this.isFocusNode(backNode)){
				gc.setForeground(EXEC_COLOR);
			} else {
				gc.setForeground(BACKWARD_LINE_COLOR);
			}
			gc.drawLine(curNode.x, curNode.y, backNode.x, curNode.y);
			gc.drawLine(backNode.x, curNode.y, backNode.x, backNode.y);
			gc.setForeground(oldfgc);
		} else if(curNode.type == NodeType.Break){
			//直接向左，并在最右端打点
			if(this.isFocusNode(curNode) && this.isFocusNode(backNode)){
				gc.setForeground(EXEC_COLOR);
			}
			gc.drawLine(curNode.x, curNode.y, backNode.x - LOOP_PADDING, curNode.y);
			gc.drawOval(backNode.x - LOOP_PADDING - BREAK_POINT_RADIUS/2, 
					curNode.y - BREAK_POINT_RADIUS/2, 
					BREAK_POINT_RADIUS, BREAK_POINT_RADIUS);
			if(this.isFocusNode(curNode)){
				gc.setForeground(oldfgc);
			}
		}
	}
	
	
	private static final Color NORMAL_NODE_COLOR = new Color(null, 0, 0, 0);
	private static final Color UNREACHABLE_NODE_COLOR = new Color(null, 255, 0, 0);
	private static final Color RETURN_NODE_COLOR = new Color(null, 50, 205, 50);
	
	private static final Color EXEC_COLOR = new Color(null, 138, 43, 226);
	
	/** 绘制节点 */
	private void drawNode(GC gc, GraphNode curNode){
		Color oldbgc = gc.getBackground();
		//Color bgc = new Color(gc.getDevice(), 0, 0,0);
		if(curNode.reachable){
			if(this.isFocusNode(curNode)){
				gc.setBackground(EXEC_COLOR);
			} else if(curNode.type == NodeType.Return){
				gc.setBackground(RETURN_NODE_COLOR);
			} else {
				gc.setBackground(NORMAL_NODE_COLOR);
			}
		} else {
			gc.setBackground(UNREACHABLE_NODE_COLOR);
		}
		gc.fillRectangle(curNode.x - NODE_WIDTH/2, curNode.y-NODE_WIDTH/2, 
				NODE_WIDTH, NODE_WIDTH);
		gc.setBackground(oldbgc);
	}

	private boolean isFocusNode(GraphNode node){
		if(this.fFocusPath == null || this.fFocusPath.getPath() == null
				||node == null || node.cfgNode == null){
			return false;
		}
		return this.fFocusPath.getPath().contains(node.cfgNode);
	}
	
	/**
	 * 获取当前焦点路径
	 * @return 当前焦点路径
	 */
	public CfgPath getfFocusPath() {
		return fFocusPath;
	}

	/**
	 * 设置焦点路径
	 * @param fFocusPath 要设置的焦点路径
	 */
	public void setFocusPath(CfgPath fFocusPath) {
		this.fFocusPath = fFocusPath;
	}

	@Override
	public int getWidth() {
		return this.fWidth;
	}

}




