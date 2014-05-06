package cn.nju.seg.atg.model;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.internal.core.model.ASTStringUtil;


/**
 * 原子约束条件。对于条件表达式不是逻辑表达式而是简单表达式的，添加后缀“!= 0”处理
 * @author ygsx
 * @time 2014/04/23 15:01
 * */
@SuppressWarnings("restriction")
public class Constraint {
	
	public static enum CtType {
		/** 未确定态 */
		Suspensive("??"),
		/** &gt;符号 */
		Greater(">"),
		/** &lt;符号 */
		Less("<"),
		/** &gt;=符号 */
		GreaterEqual(">="),
		/** &lt;=符号 */
		LessEqual("<="),
		/** ==符号 */
		Equal("=="),
		/** !=符号 */
		NotEqual("!=");
		
		private String str;
		
		private CtType(String str){
			this.str = str;
		}
		
		public String toString(){
			return this.str;
		}
	}
	
	/**
	 * 每种类型都可以通过REVERSE_TYPE_ARRAY[CtType+7]来获取相反的类型
	 * */
	public static final CtType[] REVERSE_TYPE_ARRAY = new CtType[] {
		CtType.Suspensive, CtType.Greater, CtType.Less, 
		CtType.GreaterEqual, CtType.LessEqual, CtType.Equal, CtType.NotEqual,
		CtType.Suspensive, CtType.Less, CtType.Greater,
		CtType.LessEqual, CtType.GreaterEqual, CtType.NotEqual, CtType.Equal,
	};
	
	/** 
	 * 当前原子约束条件表达式。
	 *  */
	public IASTExpression expression;
	/** 约束条件的源类型 */
	public CtType srcType;
	
	/** 
	 * 约束条件的当前类型
	 * 此属性用于线性拟合
	 *  */
	public CtType curType;

	/** 条件表达式左边的操作数 */
	protected String leftOperand;
	/** 条件表达式右边的操作数 */
	protected String rightOperand;
	
	/** 当前约束条件表达式在文件中的偏移量 */
	protected int offset;
	/** 当前约束条件表达式在文件中的长度 */
	protected int length;
	
	/**
	 * 使用一个表达式创建一个约束对象
	 * */
	public Constraint(IASTExpression expression){
		this.expression = expression;
		this.handleExpression(expression);
		this.curType = this.srcType;
	}
	
	/** 设置约束类型<br />关于反转已经在约束节点中实现
	 * @param reverse 是否反转原类型
	 *  */
	@Deprecated
	public void setType(boolean reverse){
		if(reverse){
			//TODO 使用数组来反转并不是一个好的实现，因为这可能引起更大的时间开销
			this.curType = REVERSE_TYPE_ARRAY[this.srcType.ordinal() + 7];
		} else {
			this.curType = this.srcType;
		}
	}
	
	/** 处理表达式，如果表达式是一元的，那么添加“!= 0” */
	private void handleExpression(IASTExpression expression){
		if(expression == null) return;
		IASTFileLocation fileloc = expression.getFileLocation();
		if(fileloc != null){
			this.offset = fileloc.getNodeOffset();
			this.length = fileloc.getNodeLength();
		} else {
			this.offset = this.length = 0;
		}
		if(expression instanceof IASTBinaryExpression){
			IASTExpression op1 = ((IASTBinaryExpression) expression).getOperand1();
			IASTExpression op2 = ((IASTBinaryExpression) expression).getOperand2();
			//this.leftOperand = op1.toString();
			//this.rightOperand = op2.toString();
			this.leftOperand = ASTStringUtil.getExpressionString(op1);
			this.rightOperand = ASTStringUtil.getExpressionString(op2);
			int op = ((IASTBinaryExpression) expression).getOperator();
			if(op == IASTBinaryExpression.op_assign){
				
			} else if(op == IASTBinaryExpression.op_equals){
				this.srcType = CtType.Equal;
			} else if(op == IASTBinaryExpression.op_greaterEqual){
				this.srcType = CtType.GreaterEqual;
			} else if(op == IASTBinaryExpression.op_greaterThan){
				this.srcType = CtType.Greater;
			} else if(op == IASTBinaryExpression.op_lessEqual){
				this.srcType = CtType.LessEqual;
			} else if(op == IASTBinaryExpression.op_lessThan){
				this.srcType = CtType.Less;
			} else if(op == IASTBinaryExpression.op_notequals){
				this.srcType = CtType.NotEqual;
			}
		} else if(expression instanceof IASTUnaryExpression){
			this.leftOperand = ASTStringUtil.getExpressionString(expression);
			this.rightOperand = "0";
			this.srcType = CtType.NotEqual;
		}
		//IBasicType type = (IBasicType) expression.getExpressionType();
		//System.out.println(type);
	}

	/** 获取当前约束条件表达式在文件中的偏移量 */
	public int getOffset(){
		return this.offset;
	}
	/** 获取当前约束条件表达式在文件中的长度 */
	public int getLength(){
		return this.length;
	}
	
	/**
	 * 形如<b>E<sub>1</sub>&nbsp;<i>rel</i>&nbsp;E<sub>2</sub></b>的表达式。
	 * 其中<i>rel</i>是逻辑表达式：<i>&gt;, &gt;=, &lt;, &lt;=, ==, !=</i>
	 * */
	public String toString(){
		return this.leftOperand + this.srcType + this.rightOperand;
	}
	
	/** 获取左右操作数相减字符串 */
	public String toValueString(){
		return String.format("((%s)-(%s))", this.leftOperand, this.rightOperand);
	}
}










