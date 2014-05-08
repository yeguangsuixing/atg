package cn.nju.seg.atg.cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制流图路径类
 * @author ygsx
 * @time 2014/04/23 11:30
 * */
public class CfgPath {

	/** 路径中的节点信息 */
	private List<CfgNode> path;

	private static int ID = 0;
	
	private int id;
	
	public static int MAX_NR_PARAS = 100;
	
	/** 是否覆盖 */
	/*default*/ boolean isCoverred = false;
	
	/*default*/ int coverredNodeCount = 0;
	
	/** 
	 * <p>最优覆盖时的输入数据列表</p>
	 *  */
	private List<Object[]> parasList;
	
	
	public CfgPath(){
		this.id = ++ID;
		this.path = new ArrayList<CfgNode>(20);
		this.parasList = new ArrayList<Object[]>();
	}
	
	public int getId(){
		return this.id;
	}
	
	public boolean isCoverred(){
		return this.isCoverred;
	}
	
	public int getCoverredNodeCount(){
		return this.coverredNodeCount;
	}
	public void addParas(int count, Object[] paras){
		if(this.coverredNodeCount < count){
			this.parasList.clear();
			this.coverredNodeCount = count;
			if(count == this.path.size()){
				this.isCoverred = true;
			}
			if(paras != null && this.parasList.size() < MAX_NR_PARAS){
				this.parasList.add(paras);			
			}
		} else if(this.coverredNodeCount == count){
			if(paras != null && this.parasList.size() < MAX_NR_PARAS){
				this.parasList.add(paras);			
			}
		}
	}
	
	public int getParasListSize(){
		return this.parasList.size();
	}
	
	public Object[] getParas(int index){
		return this.parasList.get(index);
	}
	
	public List<CfgNode> getPath() {
		return path;
	}
	/** 获取路径长度（节点个数） */
	public int length(){ return path.size(); }
	
	public void push(CfgNode cfgnode){
		path.add(cfgnode);
	}
	public void pop(){
		int size = path.size();
		if(size > 0){
			path.remove(size-1);
		}
	}
	
	/** 获取路径字符串，每个节点之间使用“->”相连 */
	public String getPathString(){
		StringBuilder sb = new StringBuilder();
		int size = path.size();
		if(size == 0) return sb.toString();
		sb.append(path.get(0));
		for(int i = 1; i < size; i ++){
			sb.append("->");
			sb.append(path.get(i));
		}
		return sb.toString();
	}
	
	public CfgPath clone(){
		CfgPath newcfgpath = new CfgPath();
		newcfgpath.path.addAll(path);
		newcfgpath.isCoverred = this.isCoverred;
		newcfgpath.coverredNodeCount = this.coverredNodeCount;
		return newcfgpath;
	}
	
	/**
	 * 判断指定的输入是否与当前路径节点列表一致
	 * @param nodeList 要比较的节点列表
	 * @return 节点列表是否完全按序覆盖了当前路径上的点
	 * */
	public boolean equals(List<CfgNode> nodeList){
		if(nodeList == null) return false;
		int i = 0;
		for(CfgNode node : nodeList){
			//路径节点列表使用arraylist，所以此处get开销很小
			if(i >= this.path.size()
				|| node != this.path.get(i++)) return false;
		}
		return true;
	}
	
	/**
	 * 获取相同的祖先节点列表
	 * @param path 要处理的节点列表
	 * @return 指定输入的节点列表与当前节点列表具有的共同祖先列表
	 * */
	public List<CfgNode> getCommonAncesters(CfgPath path){
		List<CfgNode> ancesterlist = new ArrayList<CfgNode>();
		int i = 0;
		for(CfgNode node : path.path){
			if(i >= this.path.size()) return ancesterlist;
			if(node == this.path.get(i++)) {
				ancesterlist.add(node);
			} else {//不相同则直接返回
				return ancesterlist;
			}
		}
		return ancesterlist;
	}
}










