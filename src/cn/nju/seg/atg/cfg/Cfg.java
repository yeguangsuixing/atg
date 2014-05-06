package cn.nju.seg.atg.cfg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import cn.nju.seg.atg.cfg.CfgNode.Type;
import cn.nju.seg.atg.model.Point;

/**
 * 控制流图类
 * @author ygsx
 * @time 2014/04/23 11：38
 * */
public class Cfg {
	
	
	/** 当前处理的AST */
	private IASTFunctionDefinition ast;

	/** 附件头节点 */
	private CfgNode attachHeader, attachTail;
	/** 所有节点列表 */
	private List<CfgNode> nodeList = new ArrayList<CfgNode>(30);
	/** 所有的条件节点列表 */
	private List<CfgCondNode> condNodeList = new ArrayList<CfgCondNode>(10);
	/** 所有路径列表，每次调用{@link #getAllPaths()}时重新生成 */
	private List<CfgPath> allPathList;
	
	/** 参数声明的起始偏移，用于在第一个参数之前插入
	 * 我们的路径记录函数
	 *  */
	private int paraDeclOffset = -1;
	
	
	public Cfg(IASTFunctionDefinition ast){
		this.ast = ast;
		this.attachHeader = new CfgNode(Type.Head);
		this.attachTail = new CfgNode(Type.Tail);
	}
	
	/**
	 * 获取所有的节点（节点按照在文件中出现的先后顺序排列）
	 * */
	public List<CfgNode> getAllNodes(){
		return this.nodeList;
	}
	
	/** 获取所有的条件节点 */
	public List<CfgCondNode> getAllCondNodes(){
		return this.condNodeList;
	}
	
	public CfgConstraintNode getConstraintNodeById(int id){
		for(CfgCondNode condnode : condNodeList){
			for(CfgConstraintNode cn : condnode.getAllInnerNodes()){
				if(cn.getId() == id) return cn;
			}
		}
		return null;
	}
	
	/** 根据节点列表获取对应的路径
	 * @param nodeList 包含头尾节点的列表
	 *  */
	public CfgPath getCfgPath(List<CfgNode> nodeList){
		for(CfgPath path : this.allPathList){
			if(path.equals(nodeList)) return path;
		}
		return null;
	}
	
	/** 获取函数的起始偏移量 */
	public int getFuncOffset(){
		return this.ast.getFileLocation().getNodeOffset();
	}
	
	/**
	 * 获取控制流图入口
	 * */
	public CfgNode getEntry(){
		return this.attachHeader;
	}
	
	/**
	 * 生成控制流图，根据输入的AST
	 * */
	public void run(){
		this.nodeList.clear();
		this.condNodeList.clear();
		this.attachHeader.resetRelative();
		this.attachTail.resetRelative();
		this.nodeList.add(attachHeader);
		this.nodeList.add(attachTail);
		IASTStatement body = ast.getBody();
		assert(body instanceof IASTCompoundStatement);
		
		IASTFunctionDeclarator  funcdec = ast.getDeclarator();
		//TODO 通过下标获取参数节点不是一个好方法
		IASTParameterDeclaration para = (IASTParameterDeclaration)
				(funcdec.getChildren()[1]);
		this.paraDeclOffset = para.getFileLocation().getNodeOffset();
		//一般来说，2层比较普遍，那么就会有至多4个叶子节点
		List<CfgNode> leavesNodeList = new ArrayList<CfgNode>(4);
		this.attachHeader.next = handleStatement((IASTCompoundStatement)body,
				this.attachHeader, leavesNodeList);
		for(CfgNode cfgnode : leavesNodeList){
			cfgnode.next = this.attachTail;
			this.attachTail.prev = cfgnode;
		}
		for(CfgCondNode node : this.condNodeList){
			node.handleExpression();
		}
	}
	
	/** 从给定的字符串获取执行路径 */
	public CfgPath getPathFromString(String pathString, String splitStr){
		List<CfgNode> nodelist = new ArrayList<CfgNode>();
		nodelist.add(this.attachHeader);
		String[] pathStrArray = pathString.split(splitStr);
		Stack<CfgNode> stack = new Stack<CfgNode>();
		CfgNode stackTop = null;//记录栈顶元素，不用调用stack.pop()获取
		boolean discard = false;
		for(String nodestr : pathStrArray){
			if(nodestr == null || nodestr.length() == 0) continue;
			int no = Integer.parseInt(nodestr);
			CfgNode curnode = null;
			for(CfgNode node : this.nodeList){
				if(node.getId() != no) continue;
				curnode = node; break;
			}
			if(discard){//进入舍弃阶段，舍弃第二次进入循环的节点
				if(curnode != stackTop.next){
					continue;
				} else {
					discard = false;
				}
			}
			//如果是循环条件节点，那么查看此节点是否已经在栈顶
			//如果不是在栈顶，说明是内循环，继续压栈，否则出栈
			if(curnode.type == Type.CondLoop){
				if(stackTop == null || stackTop != curnode){//内嵌循环
					stack.add(stackTop = curnode);
				} else {//说明是第二次到达循环节点
					stackTop = stack.pop();
					discard = true;
				}
			}
			nodelist.add(curnode);
		}
		nodelist.add(this.attachTail);
		
		return this.getCfgPath(nodelist);
	}
	
	public void updateConstraintNode(Double para, Map<CfgConstraintNode, Double> nodeMap){
		Set<Entry<CfgConstraintNode, Double>> nodes = nodeMap.entrySet();
		for(Entry<CfgConstraintNode, Double> entry : nodes){
			entry.getKey().getPointSet().add(new Point(para, entry.getValue()));
		}
	}
	
	/** 获取参数声明的起始偏移量 */
	public int getParaDeclOffset(){
		return this.paraDeclOffset;
	}
	
	/**
	 * 获取当前cfg的所有路径，在调用此方法前请先调用<code>run()</code>
	 * @return 返回所有路径的列表
	 * */
	public List<CfgPath> getAllPaths(){
		for(CfgNode cfgnode : nodeList){
			cfgnode.resetCoveredLeft();
		}
		allPathList = new LinkedList<CfgPath>();
		CfgPath currentpath = new CfgPath();
		handleGetAllPaths(allPathList, currentpath, this.attachHeader);
		return allPathList;
	}
	
	/**
	 * 打印所有节点
	 * */
	public void printAllNodes(){
		for(CfgNode cfgnode : this.nodeList){
			System.out.println(cfgnode.getNodeInfoString());
		}
	}
	
	private void handleGetAllPaths(List<CfgPath> pathList, 
			CfgPath currentpath, CfgNode currentnode){
		//在调用前已经确认currentnode不为空，所以不用再次判断
		//if(currentnode == null) return;
		currentpath.push(currentnode);
		boolean tail = true;
		if(currentnode instanceof CfgCondNode){
			CfgCondNode condnode = (CfgCondNode)currentnode;
			if(condnode.then != null && condnode.then.getCoveredLeft() > 0){
				tail = false;
				condnode.then.decCoveredLeft();
				handleGetAllPaths(pathList, currentpath, condnode.then);
				condnode.then.incCoveredLeft();
			}
		}
		if(currentnode.next != null && currentnode.next.getCoveredLeft() > 0){
			tail = false;
			currentnode.next.decCoveredLeft();
			handleGetAllPaths(pathList, currentpath, currentnode.next);
			currentnode.next.incCoveredLeft();
		}
		if(tail){
			if(currentnode == this.attachTail){
				pathList.add(currentpath.clone());
			} else {
				System.out.println("currentnode != this.attachTail");
			}
		}
		currentpath.pop();
	}
	
	/**
	 * 递归处理语句方法
	 * @param ast 需要处理的ast
	 * @param current 当前节点
	 * @param returnNode 返回节点
	 * @param contnNode continue节点
	 * @param leavesNodeList 叶子节点列表
	 * */
	private CfgNode handleStatement(IASTStatement ast, CfgNode current, 
				List<CfgNode> leavesNodeList) {
		IASTNode[] statementNodeList = null;
		CfgNode firstNode = null;
		if(ast instanceof IASTCompoundStatement){
			statementNodeList = ast.getChildren();
			if(statementNodeList == null || statementNodeList.length == 0){
				return firstNode;//TODO 空语句？
			}
		} else {
			statementNodeList = new IASTNode[]{ast};
		}
		List<CfgNode> breakleaves = new ArrayList<CfgNode>();
		for(IASTNode astnode : statementNodeList){
			if(astnode == null) {
				continue;
			}
			if( astnode instanceof IASTIfStatement ) {
				IASTIfStatement ifstatement = (IASTIfStatement)astnode;
				//新建一个条件节点
				CfgCondNode condnode = (CfgCondNode) mergeLeaves2CfgNode(
						Type.CondIf, 
						current, ifstatement.getConditionExpression(), 
						leavesNodeList, breakleaves);
				current = condnode;
				//如果当前层还没有设置起始节点，那么设置之
				if(firstNode == null){ firstNode = current; }
				//处理then分支
				List<CfgNode> nestedleavesnodelist = new ArrayList<CfgNode>(3);
				CfgNode thennode = handleStatement(ifstatement.getThenClause(), 
						condnode, nestedleavesnodelist);
				leavesNodeList.addAll(nestedleavesnodelist);//内层叶子->当前层叶子
				//处理else分支
				IASTStatement elseclause = ifstatement.getElseClause();
				condnode.next = null;//必须设置！
				if(elseclause != null){
					nestedleavesnodelist.clear();
					CfgNode elsenode = handleStatement(elseclause, condnode, 
						nestedleavesnodelist);
					leavesNodeList.addAll(nestedleavesnodelist);//内层叶子->当前层叶子
					//nestedleavesnodelist.clear();//列表不再使用，所以不用clear
					elsenode.isElse = true;
					condnode.next = elsenode;
				}
				condnode.then = thennode;
			} else if(astnode instanceof IASTForStatement){
				IASTForStatement forstatement = (IASTForStatement)astnode;
				IASTStatement initstatement = forstatement.getInitializerStatement();
				//===========首先，将初始化的语句归结为上一个节点的内容===========
				if(current.type != Type.Normal){
					current = mergeLeaves2CfgNode(Type.Normal, current, 
							initstatement, leavesNodeList, breakleaves);
					current.forInit = true;
					//如果当前层还没有设置起始节点，那么设置之
					if(firstNode == null){ firstNode = current; }
				} else {
					current.addASTNode(initstatement);
				}
				//====================其次，处理for循环=====================
				//新建一个条件节点
				IASTExpression forCondExp = forstatement.getConditionExpression();
				IASTExpression forIterExp = forstatement.getIterationExpression();
				CfgCondNode condnode = new CfgCondNode(Type.CondLoop,
						forCondExp, current);
				CfgNode iterNode = new CfgNode(Type.Iterate, null,forIterExp);
				this.condNodeList.add(condnode);
				this.nodeList.add(condnode);
				this.nodeList.add(iterNode);
				current.next = condnode;
				iterNode.next = condnode;
				current = condnode;
				//如果当前层还没有设置起始节点，那么设置之
				if(firstNode == null){ firstNode = current; }
				//递归处理
				List<CfgNode> nestedleavesnodelist = new ArrayList<CfgNode>(3);
				CfgNode forbodynode = handleStatement(forstatement.getBody(), 
						condnode, nestedleavesnodelist);
				for(CfgNode cfgnode : nestedleavesnodelist){
					if(cfgnode.type == Type.Break){
						breakleaves.add(cfgnode);
						//对于break节点来说，next临时指向其对应的循环
						cfgnode.next = condnode;
					} else {//congtinue节点和其他节点做相同处理
						cfgnode.next = iterNode;
					}
				}
				condnode.then = forbodynode;
				condnode.next = null;
			} else if(astnode instanceof IASTWhileStatement){
				IASTWhileStatement whilestatement = (IASTWhileStatement)astnode;
				current = mergeLeaves2CfgNode(Type.CondLoop, current, 
						whilestatement.getCondition(), leavesNodeList, breakleaves);
				//递归处理
				List<CfgNode> nestedleavesnodelist = new ArrayList<CfgNode>(3);
				CfgNode whilebodynode = handleStatement(whilestatement.getBody(), 
						current, nestedleavesnodelist);
				for(CfgNode cfgnode : nestedleavesnodelist){
					if(cfgnode.type == Type.Break){
						breakleaves.add(cfgnode);
						//对于break节点来说，next临时指向其对应的循环
						cfgnode.next = current;
					} else {//congtinue节点和其他节点做相同处理
						cfgnode.next = current;
					}
				}
				((CfgCondNode)current).then = whilebodynode;
				current.next = null;
				//如果当前层还没有设置起始节点，那么设置之
				if(firstNode == null){ firstNode = current; }
			} else if(astnode instanceof IASTReturnStatement){
				current = mergeLeaves2CfgNode(Type.Return, 
						current, astnode,leavesNodeList, breakleaves);
				//如果当前层还没有设置起始节点，那么设置之
				if(firstNode == null){ firstNode = current; }
				//如果在return语句后面还有句子的话，那么无论怎么线性拟合都不可能完全覆盖
				//因此这里如果遇到结束语句，那么直接忽略后面的内容。
				//虽然在return语句后面还有语句的情况不太可能出现
				break;
			} else if(astnode instanceof IASTContinueStatement){
				//CfgNode contnnode = new CfgNode(Type.Continue, current, astnode);
				//this.nodeList.add(contnnode);
				//current.next = contnnode;
				//current = contnnode;
				current = mergeLeaves2CfgNode(Type.Continue, 
						current, astnode,leavesNodeList, breakleaves);
				//如果当前层还没有设置起始节点，那么设置之
				if(firstNode == null){ firstNode = current; }
			} else if(astnode instanceof IASTBreakStatement){
				//CfgNode breaknode = new CfgNode(Type.Break, current, astnode);
				//this.nodeList.add(breaknode);
				//current.next = breaknode;
				//current = breaknode;
				current = mergeLeaves2CfgNode(Type.Break, 
						current, astnode,leavesNodeList, breakleaves);
				//如果当前层还没有设置起始节点，那么设置之
				if(firstNode == null){ firstNode = current; }
			} else {
				if(current.type != Type.Normal){
					current = mergeLeaves2CfgNode(Type.Normal, 
							current, astnode,leavesNodeList, breakleaves);
					current.isSingle = statementNodeList.length == 1;
					//如果当前层还没有设置起始节点，那么设置之
					if(firstNode == null){ firstNode = current; }
				} else {
					current.addASTNode(astnode);
				}
			}
		}//end for
		//如果当前层最后存在循环，并且循环体内有break节点，那么会被放置在breakleaves中
		leavesNodeList.addAll(breakleaves);
		//如果当前节点不是一个条件节点，说明叶子节点不包含任何内容，
		//取当前节点作为当前语句块的叶子节点
		if(current.type != Type.CondIf && current.type != Type.CondLoop
				|| current.next == null){
			leavesNodeList.add(current);
		}
		return firstNode;
	}//end function
	
	/** 
	 * 合并叶子节点到一个新的节点，不合并continue节点和break节点
	 * @param type 新节点类型
	 * @param current 上一节点
	 * @param astnode 新节点对应的ast节点
	 * @param leavesNodeList 叶子节点列表
	 * @param brkLeavesNodeList 当前层循环体内部break叶子列表
	 * */
	private CfgNode mergeLeaves2CfgNode(Type type, CfgNode current, IASTNode astnode,
			List<CfgNode> leavesNodeList, List<CfgNode> brkLeavesNodeList){
		CfgNode newnode = null;
		if(type == Type.CondIf || type == Type.CondLoop){
			//保证astnode是一个IASTExpression
			newnode = new CfgCondNode(type, (IASTExpression) astnode, current);
			this.condNodeList.add((CfgCondNode)newnode);
		} else {
			newnode = new CfgNode(type, current, astnode);
		}
		this.nodeList.add(newnode);
		if(current.type == Type.CondIf){
			if(current.next == null){//不存在else从句
				current.next = newnode;
			} else {
			//这一句会导致在遍历current的分支时merge也被赋值成分支节点，因此需要下面的for循环合并节点时重新设置
				((CfgCondNode)current).merge = newnode;
			}
		} else {
			current.next = newnode;
		}
		for(CfgNode cfgnode : brkLeavesNodeList){
			cfgnode.next = newnode;
		}
		brkLeavesNodeList.clear();
		if(current.type == Type.CondLoop) {
			//如果是上一个节点是循环节点，那么说明当前循环已经结束，将break节点弹出
			for(CfgNode cfgnode : leavesNodeList){
				cfgnode.next = newnode;
				if(cfgnode.type == Type.CondIf){
					((CfgCondNode)cfgnode).merge = newnode;
				}
			}
			leavesNodeList.clear();
		} else {//上一个节点不是循环节点，说明还在循环内部，或者是不再任何循环中
			//rmnodelist保存要被移除的叶子节点
			//这里假设continue节点和break节点大概占20%的叶子节点比率
			List<CfgNode> rmnodelist = new ArrayList<CfgNode>(
					(int) (0.8*leavesNodeList.size()));
			for(CfgNode cfgnode : leavesNodeList){
				//上一个节点不是循环节点，说明还在循环内部，或者是不再任何循环中
				//保留break节点和continue节点
				if(cfgnode.type != Type.Break && cfgnode.type != Type.Continue){
					rmnodelist.add(cfgnode);
					cfgnode.next = newnode;
					if(cfgnode.type == Type.CondIf){
						((CfgCondNode)cfgnode).merge = newnode;
					}
				}
			}
			//是否可以减小函数调用开销?
			if(rmnodelist.size() > 0){
				leavesNodeList.removeAll(rmnodelist);
			}
		}
		return newnode;
	}
}

















