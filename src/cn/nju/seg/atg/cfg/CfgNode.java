package cn.nju.seg.atg.cfg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;


/**
 * 
 * <p>Control Flow Graphic Node class</p>
 * <p>控制流图中的节点</p>
 * <p>每个节点包括</p>
 * @author ygsx
 * @time 2014/04/23 10:51
 * */
public class CfgNode {
	
	/**
	 * <p>节点类型枚举</p>
	 * @author ygsx
	 * @time 2014/04/23 10:54
	 * */
	public static enum Type {
		/** 附加头节点 */
		Head(0x00),
		/** 附件尾节点 */
		Tail(0x01),
		/** 普通节点 */
		Normal(0x02),
		/** if条件节点 */
		CondIf(0x04),
		/** 循环条件节点，包括for/while/do-while循环节点 */
		CondLoop(0x08),
		/** 迭代节点，用于for循环迭代语句 */
		Iterate(0x10),
		/** continue节点，此节点的【出边】是回退的 */
		Continue(0x20),
		/** break节点 */
		Break(0x40),
		/** return节点 */
		Return(0x80);
		
		int id;
		private Type(int id){ this.id = id;}
		public int getId(){ return this.id; }
	}

	private static int ID = 1000;
	
	/**
	 * 节点id
	 * */
	protected int id;
	
	/** 节点类型 */
	protected Type type;
	
	protected List<IASTNode> nodeList = new LinkedList<IASTNode>();
	
	/** 
	 * <p>该节点所对应的源文件中的内容的偏移和长度</p>
	 * <p>之所以使用一个映射，是因为节点对应的内容可能分布在多个位置</p>
	 * <p>使用TreeMap和HashMap都可以</p>
	 *  */
	protected Map<Integer, Integer> srcMap = new TreeMap<Integer, Integer>();
	
	/** 上一个节点 */
	/*default*/ List<CfgNode> prevList = new ArrayList<CfgNode>(1);
	
	/** 下一个节点 */
	/*default*/ CfgNode next;
	
	/** 第一次偏移量，用于插桩和标注 */
	protected int firstOffset;
	/** 结束的偏移量，用于标注 */
	protected int endOffset;
	
	/** 当前节点是否只包含一个语句块 */
	/*default*/ boolean isSingle = false;
	
	/** 是否是for循环初始化的节点 */
	/*default*/ boolean forInit = false;
	/** 是否是条件语句的else从句 */
	/*default*/ boolean isElse = false;
	
	/** 
	 * <p>当前节点所剩余的经过次数</p>
	 * <p>对于非循环节点，该数在初始化时为1，循环节点为2</p>
	 * <p></p>
	 *  */
	protected byte coveredLeft;
	
	/** 当前节点在文档中的最开始的行 */
	protected int line;
	
	/** 
	 * 使用内部编号以及输入类型创建一个节点
	 * @param type 节点类型
	 **/
	public CfgNode(Type type){
		this(++ID, type);
	}
	
	public CfgNode(int id, Type type){
		this(id, type, null);
	}

	public CfgNode(Type type, CfgNode prev){
		this(++ID, type, prev);
	}
	public CfgNode(int id, Type type, CfgNode prev){
		this(id, type, prev, null);
	}
	public CfgNode(Type type, CfgNode prev, IASTNode astnode){
		this(++ID, type, prev, astnode);
	}
	public CfgNode(int id, Type type, CfgNode prev, IASTNode astnode){
		this.id = id;
		this.type = type;
		this.prevList.add(prev);
		this.firstOffset = Integer.MAX_VALUE;
		this.endOffset = Integer.MIN_VALUE;
		this.resetCoveredLeft();
		this.addASTNode(astnode);
	}
	
	/** 获取节点所在文档行数 */
	public int getLine(){
		return this.line;
	}
	
	/** 获取当前节点的最先偏移量 */
	public int getFirstOffset(){
		return this.firstOffset;
	}
	/** 获取当前节点的结束偏移量 */
	public int getEndOffset(){
		return this.endOffset;
	}
	
	/** 获取当前节点是否不包含任何语句 */
	public boolean isEmpty(){
		return this.srcMap.isEmpty();
	}

	/**
	 * 判断当前节点是否是条件(if 或loop)节点
	 * */
	public boolean isCondType(){
		return this.type == Type.CondIf || this.type == Type.CondLoop;
	}
	/** 获取当前节点的类型 */
	public Type getType(){
		return this.type;
	}
	
	/** 获取下一个节点，如果if存在else从句，那么else从句节点将会是此if节点的next节点 */
	public CfgNode getNext(){
		return this.next;
	}
	
	/** 判断当前节点是否是else分支节点 */
	public boolean isElse(){
		return this.isElse;
	}
	
	/** 添加一个AST节点到当前CfgNode
	 * @param astnode 被添加的节点
	 *  */
	public void addASTNode(IASTNode astnode){
		if(astnode == null) return;
		this.nodeList.add(astnode);
		IASTFileLocation fileloc = astnode.getFileLocation();
		if(fileloc != null){
			int off = fileloc.getNodeOffset();
			int len = fileloc.getNodeLength();
			int end = off + len;
			if(off < this.firstOffset){
				this.firstOffset = off;
				this.line = fileloc.getStartingLineNumber();
			}
			if(end > this.endOffset){
				this.endOffset = end;
			}
			this.srcMap.put(off, fileloc.getNodeLength());
		}
	}
	
	/** 获取节点的id */
	public int getId(){ return this.id; }
	/** 获取语句列表 */
	public List<IASTNode> getNodeList(){ 
		return this.nodeList; 
	}
	/** 获取当前节点是否是for循环的初始化节点 */
	public boolean isForInit(){
		return this.forInit;
	}
	/** 获取当前节点是否是单独节点（单独节点是指当前语句块中只有一条语句） */
	public boolean isSingle(){
		return this.isSingle;
	}
	
	/** 获取节点字符串"id(type)" */
	public String toString(){
		return String.format("%d(%s)", id, type);
	}
	
	
	/** 
	 * 获取节点信息字符串
	 * @return 节点信息字符串：id(type): [off1,len1],[off2,len2]...
	 *  */
	public String getNodeInfoString(){
		if(srcMap.size() == 0) return this.toString();
		StringBuilder sb = new StringBuilder(this.toString());
		sb.append(": ");
		Set<Integer> keyset = srcMap.keySet();
		for(Integer key : keyset){
			int len = srcMap.get(key);
			sb.append("[");
			sb.append(key);
			sb.append(",");
			sb.append(key+len);
			sb.append("],");
		}
		return sb.toString();
	}
	/** 
	 * 获取coveredLeft
	 * @param 当前节点剩余经过次数
	 *  */
	public byte getCoveredLeft(){
		return this.coveredLeft;
	}
	/** 
	 * 自减coveredLeft并返回
	 * @param 当前节点自减后的剩余经过次数
	 *  */
	public byte decCoveredLeft(){
		return --this.coveredLeft;
	}
	/** 
	 * 自增coveredLeft并返回
	 * @param 当前节点自增后的剩余经过次数
	 *  */
	public byte incCoveredLeft(){
		return ++this.coveredLeft;
	}
	/** 重置coveredLeft */
	public void resetCoveredLeft(){
		if(this.type == Type.CondLoop){
			this.coveredLeft = 2;
		} else {
			this.coveredLeft = 1;
		}
	}
	
	/** 清除前后关系 */
	public void resetRelative(){
		this.prevList.clear();
		this.next = null;
	}

}








