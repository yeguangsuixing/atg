package cn.nju.seg.atg.cfg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cn.nju.seg.atg.model.Interval;

/**
 * 条件节点(包括if节点，for/while/do-while循环节点)
 * @author ygsx
 * @time 2014/04/23 15:31
 * */
public class CfgCondNode extends CfgNode {

	
	/** 对于条件节点来说，符合条件时的下一个节点 */
	/*default*/ CfgNode then;
	/** 对于分支节点来说，下一个汇聚节点 */
	public /*default*/ CfgNode merge;
	/** 条件表达式 */
	/*default*/ IASTExpression expression;

	/** 原子约束单元根节点 */
	/*default*/ CfgConstraintUnit constraintUnitRoot;
	/** 所有原子约束单元列表 */
	protected List<CfgConstraintUnit> constraintUnitList = 
			new ArrayList<CfgConstraintUnit>();
	

	/** 是否处于满足状态 */
	/*default*/ boolean satisfied = true;
	
	public CfgCondNode(Type type, IASTExpression expression) {
		this(type, expression, null);
	}
	public CfgCondNode(Type type, IASTExpression expression, CfgNode prev) {
		super(type, prev, expression);
		this.expression = removeBrackets(expression);
	}
	
	/** 获取所有原子约束条件 */
	public List<CfgConstraintUnit> getAllInnerNodes(){
		return this.constraintUnitList;
	}
	/**
	 * 设置节点满足性
	 * @param satisfied 节点满足性
	 * */
	public void setSatisfied(boolean satisfied){
		this.satisfied = satisfied;
	}
	/** 
	 * 判断指定节点是否是当前节点的then节点
	 * @param node 要判断的节点
	 *  */
	public boolean isThenNode(CfgNode node){
		return this.then == node;
	}
	
	/** 获取符合条件的节点 */
	public CfgNode getThen(){
		return this.then;
	}
	
	/** 处理当前节点的条件表达式，生成逻辑二叉树 */
	public void handleExpression(){
		this.constraintUnitRoot = handleExpression(this.expression);
	}
	
	private CfgConstraintUnit handleExpression(IASTExpression curExp){
		//curExp = removeAllUnaryOp(curExp);
		boolean negative = false;
		while(curExp instanceof IASTUnaryExpression) {
			int op = ((IASTUnaryExpression) curExp).getOperator();
			if(op == IASTUnaryExpression.op_tilde
					|| op == IASTUnaryExpression.op_not){
				negative = !negative;
			}//其他情况不需要翻转符号
			curExp = ((IASTUnaryExpression) curExp).getOperand();
		}
		if(curExp instanceof IASTBinaryExpression){
			IASTBinaryExpression binExp = (IASTBinaryExpression)curExp;
			int op = binExp.getOperator();
			CfgConstraintUnit curnode = null;
			if(op == IASTBinaryExpression.op_logicalOr){
				curnode = handleExpression(binExp.getOperand1());
				curnode.nextOrUnit = handleExpression(binExp.getOperand2());
				return curnode;
			}
			if(op == IASTBinaryExpression.op_logicalAnd){
				curnode = handleExpression(binExp.getOperand1());
				curnode.childAndUnit = handleExpression(binExp.getOperand2());
				return curnode;
			}
			//其他情况当作一元表达式处理
		}
		//如果不是，或者说不存在【逻辑或】和【逻辑与】
		CfgConstraintUnit newnode = new CfgConstraintUnit(curExp, negative);
		constraintUnitList.add(newnode);
		return newnode;
	}
	
	
	/**
	 * 去除约束条件的外层括号
	 * @param exp 要处理的表达式
	 * @return 无外层括号的约束条件
	 */
	/*default*/ 
	static IASTExpression removeBrackets(IASTExpression exp){
		while(exp instanceof IASTUnaryExpression)
		{
			int optor = ((IASTUnaryExpression)exp).getOperator();
			if(optor != IASTUnaryExpression.op_bracketedPrimary)break;
			exp = ((IASTUnaryExpression) exp).getOperand();
		}
		return exp;
	}
	
	/** 清除所有的坐标信息 */
	public void clearAllNodesPoints(){
		if(this.constraintUnitRoot != null){
			clearPoints(this.constraintUnitRoot);
		}
	}
	private void clearPoints(CfgConstraintUnit node){
		node.clearPoints();
		if(node.nextOrUnit != null){
			clearPoints(node.nextOrUnit);
		}
		if(node.childAndUnit != null){
			clearPoints(node.childAndUnit);
		}
	}

	/**
	 * 根据内部节点的运行信息，获取有效的区间列表
	 * */
	public List<Interval> getEffectiveIntervalList(Interval maxInterval) {
		List<Interval> effIntervalList = new ArrayList<Interval>();
		effIntervalList.add(maxInterval.clone());
		List<Interval> temp = getEffectiveIntervalList(constraintUnitRoot, maxInterval);
		
		if(satisfied){
			return Interval.getUnion(maxInterval, temp, effIntervalList);
		} else {
			return Interval.getComplementary(maxInterval, temp);
		}
	}

	private List<Interval> getEffectiveIntervalList(CfgConstraintUnit consnode, 
			Interval maxInterval) {
		List<Interval> curlist = consnode.getEffectiveIntervalList(maxInterval);
		if(consnode.childAndUnit != null){
			List<Interval> temp = getEffectiveIntervalList(consnode.childAndUnit, 
					maxInterval);
			curlist = Interval.getIntersection(temp, curlist);
		}
		if(consnode.nextOrUnit != null){
			List<Interval> temp = getEffectiveIntervalList(consnode.nextOrUnit, 
					maxInterval);
			curlist =  Interval.getUnion(maxInterval, temp, curlist);
		}
		
		return curlist;
	}
}








