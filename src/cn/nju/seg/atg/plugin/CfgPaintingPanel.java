package cn.nju.seg.atg.plugin;

import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import cn.nju.seg.atg.cfg.CfgPath;
import cn.nju.seg.atg.plugin.CfgCEditor.GraphNode;
import cn.nju.seg.atg.plugin.CfgCEditor.NodeType;
import cn.nju.seg.atg.plugin.CfgRulerColumn.LineHeightDelegate;

/***
 * cfg标尺画笔类
 * @author ygsx
 * @time 2014/05/03 15:08
 * */
public class CfgPaintingPanel {

	

	private static final int LINE_MARGIN = 1;
	private static final int FOR_NEXT_LINE_MARGIN = 3;
	private static final int NODE_WIDTH = 10;
	private static final int LOOP_PADDING = 8;
	private static final int BREAK_POINT_RADIUS = 4;

	//private static final Color NORMAL_LINE_COLOR = new Color(null, 0, 0, 0);
	private static final Color BACKWARD_LINE_COLOR = new Color(null, 0, 0, 255);
	private static final Color FOR_FRAME_LINE_COLOR = new Color(null, 128, 0, 0);
	

	private static final Color NORMAL_NODE_COLOR = new Color(null, 0, 0, 0);
	private static final Color UNREACHABLE_NODE_COLOR = new Color(null, 255, 0, 0);
	private static final Color RETURN_NODE_COLOR = new Color(null, 50, 205, 50);
	
	private static final Color EXEC_COLOR = new Color(null, 138, 43, 226);
	
	
	/** 画板宽度 */
	private int fWidth;
	
	/** 焦点路径 */
	private CfgPath fFocusPath;
	
	//private LineHeightDelegate fLineHeight;
	
	public CfgPaintingPanel(){ }
	
	
	
	public void paint(GC gc, GraphNode root, ILineRange lineRange, 
			LineHeightDelegate lineHeightDelegate, int yStart) {

		if(root == null) return;
		//this.fLineHeight = lineHeight;
		int linestart = lineRange.getStartLine();
		int linestop = linestart + lineRange.getNumberOfLines();
		int lineHeight = lineHeightDelegate.getLineHeight(0);
		yStart -= lineHeight*linestart;

		//绘制控制流图
		root.x = CfgCEditor.INTENTAITION;
		drawCfg(gc, root, lineHeight, yStart, linestop);
	}
	
	private void drawCfg(GC gc, GraphNode curNode, int lineheight, 
				int yStart, int lineStop){
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
						CfgCEditor.SINGLE_NODE_WIDTH:curNode.next.width);
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
			curNode.then.x = curNode.x + CfgCEditor.SINGLE_NODE_WIDTH;
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
	
	/** 绘制普通线条 */
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
	
	/** 绘制回退线条 */
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

	public int getWidth() {
		return this.fWidth;
	}

}




