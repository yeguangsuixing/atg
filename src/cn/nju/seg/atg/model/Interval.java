package cn.nju.seg.atg.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * 区间类，每个区间均是左闭右开
 * @author ygsx
 * @time 2014/04/23 11:05
 * */
public class Interval {
	/** 左边界值 */
	public double left;
	/** 右边界值 */
	public double right;
	
	protected double length;
	
	public static final double GOLDEN_RATIO_GREATER = (Math.sqrt(5)-1)/2.0;
	
	public static final double GOLDEN_RATIO_LESS = 1.0 - GOLDEN_RATIO_GREATER;
	
	public static final Interval MAX_INTERVAL = new Interval(-1000, 1000);
	/**
	 * 创建一个全集区间(-Double.MIN_VALUE, Double.MAX_VALUE]
	 * */
	public Interval() {
		this.left = -Double.MAX_VALUE;
		this.right = Double.MAX_VALUE;
		this.length = Double.MAX_VALUE;
	}
	/**
	 * 创建一个区间[left, right)
	 * @param left 左边界
	 * @param right 右边界
	 * */
	public Interval(double left, double right){
		this.left = left;
		this.right = right;
		this.updateLength();
	}
	
	/**
	 * 更新区间长度。当修改了区间的左右边界时请调用此方法以保持长度最新
	 * @return 更新后的区间长度
	 * */
	public double updateLength(){
		return this.length = this.right - this.left;
	}
	
	/**
	 * 
	 * <p>获取当前区间的黄金分割区间</p>
	 * <p>黄金分割区间是指以区间的黄金分割点隔开的区间</p>
	 * @param leftsplit 是否从左边分割（那么分割后的左边区间稍小）
	 * @param leftInterval 是否返回分割后左边的区间
	 * @return 当前区间的黄金分割区间
	 * @see #getGoldenSplit(boolean)
	 * */
	public Interval getGoldenSplit(boolean leftsplit, boolean leftInterval){
		double split = left + (right-left)*
				(leftsplit?GOLDEN_RATIO_LESS:GOLDEN_RATIO_GREATER);
		if(leftInterval){
			return new Interval(left, split);
		} else {
			return new Interval(split, right);
		}
	}
	
	/** 获取黄金分割点
	 * @param leftsplit 是否从左边分割（那么分割后的左边区间稍小）
	 *  */
	public double getGolden(boolean leftsplit){
		return left + (right-left)*
				(leftsplit ? GOLDEN_RATIO_LESS : GOLDEN_RATIO_GREATER);
	}

	/**
	 * 
	 * <p>获取当前区间的黄金分割区间</p>
	 * <p>黄金分割区间是指以区间的黄金分割点隔开的区间</p>
	 * @param leftsplit 是否从左边分割（那么分割后的左边区间稍小）
	 * @return 分割后的两段区间
	 * @see #getGoldenSplit(boolean, boolean)
	 * */
	public Interval[] getGoldenSplit(boolean leftsplit){
		double split = left + (right-left)*
				(leftsplit?GOLDEN_RATIO_LESS:GOLDEN_RATIO_GREATER);
		return new Interval[]{
				new Interval(left, split),
				new Interval(split, right)
		};
	}
	
	public double getRandom(){
		return left + Math.random() * length;
	}
	
	public Interval clone(){
		Interval t = new Interval();
		t.left = this.left;
		t.length = this.length;
		t.right = this.right;
		return t;
	}
	
	
	public String toString(){
		return String.format("[%f,%f)", left, right);
	}
	
	/**
	 * 标准化一个区间列表
	 * */
	public static List<Interval> standardize(List<Interval> intervals){
		if(intervals == null || intervals.isEmpty())
			return intervals;
		List<Interval> standardizablelist = new LinkedList<Interval>();
		Iterator<Interval> iter = intervals.iterator();
		
		Interval last = iter.next();
		double lastright = last.right;
		while(iter.hasNext()){
			Interval cur = iter.next().clone();
			if(cur.left > lastright){
				if(lastright == last.right){//直接添加原来的
					standardizablelist.add(last);
				} else {
					standardizablelist.add(new Interval(last.left, lastright));
				}
				last = cur;
				lastright = last.right;
			} else {
				lastright = cur.right;
			}
		}
		if(lastright == last.right){//直接添加原来的
			standardizablelist.add(last);
		} else {
			standardizablelist.add(new Interval(last.left, lastright));
		}
		return standardizablelist;
	}

	/**
	 * 求两个区间列表的交集
	 * @param list1 第一个区间列表
	 * @param list2 第二个区间列表
	 * @return 给定区间的交集
	 */
	@SuppressWarnings("unchecked")	
	public static List<Interval> getIntersection(List<Interval> list1, 
			List<Interval> list2) {
		List<Interval> intersectionList = new ArrayList<Interval>();
		if (list1 == null || list2 == null
				|| list1.isEmpty() || list2.isEmpty())
			return intersectionList;
		//迭代器对象数组
		Object[] iters = new Object[]{ list1.iterator(), list2.iterator() };
		Interval[] ts = new Interval[]{
			((Iterator<Interval>)iters[0]).next(),
			((Iterator<Interval>)iters[1]).next()
		};
		//记录当前比较的两个区间，以右边界为比较对象
		Interval tgreater = null, tless = null;
		while(true) {
			int greaterindex = 0, lessindex = 1;
			if(ts[0].right < ts[1].right){
				greaterindex = 1;
				lessindex = 0;
			}
			tgreater = ts[greaterindex];
			tless = ts[lessindex];
			if(((Iterator<Interval>)iters[lessindex]).hasNext()){
				ts[lessindex] = ((Iterator<Interval>)iters[lessindex]).next();
			} else {
				break;
			}
			if(tless.right <= tgreater.left) continue;
			double leftgreater = Math.max(tgreater.left, tless.left);
			intersectionList.add(new Interval(leftgreater, tless.right));
		}
		if(tgreater != null && tless != null) {
			double leftgreater = Math.max(tgreater.left, tless.left);
			intersectionList.add(new Interval(leftgreater, tless.right));
		}
		return intersectionList;
	}

	/**
	 * 求两个区间的并集
	 * @param list1 第一个区间列表
	 * @param list2 第二个区间列表
	 * @return 给定区间的并集
	 */
	@SuppressWarnings("unchecked")
	public static List<Interval> getUnion(Interval maxInterval, List<Interval> list1, List<Interval> list2) {
		List<Interval> unionList = new ArrayList<Interval>();
		
		if (list1 == null || list1.isEmpty()) {
			if(list2 != null) {
				unionList.addAll(list2);
			}
			return unionList;
		}
		if (list2 == null || list2.isEmpty()) {
			unionList.addAll(list1);
			return unionList;
		}
		
		Object[] iters = new Object[]{ list1.iterator(), list2.iterator() };
		Interval[] ts = new Interval[]{
			((Iterator<Interval>)iters[0]).next().clone(),
			((Iterator<Interval>)iters[1]).next().clone()
		};
		//记录当前比较的两个区间，以右边界为比较对象
		Interval tgreater = null, tless;
		int greaterindex = 0, lessindex = 1;
		while(ts[lessindex] != null) {
			greaterindex = 0; lessindex = 1;
			if(ts[0].right < ts[1].right){
				greaterindex = 1;
				lessindex = 0;
			}
			tgreater = ts[greaterindex];
			tless = ts[lessindex];
			if(((Iterator<Interval>)iters[lessindex]).hasNext()){
				ts[lessindex] = ((Iterator<Interval>)iters[lessindex]).next().clone();
			} else {
				ts[lessindex] = null;
			}
			if(tless.right <= tgreater.left) {
				unionList.add(new Interval(tless.left, tless.right));
			} else if(ts[greaterindex].left > tless.left){
				ts[greaterindex].left = tless.left;
			}
		}
		unionList.add(new Interval(tgreater.left, tgreater.right));

		return unionList;
	}
	
	/**
	 * 求区间的补集
	 * @param 区间列表
	 * @return 给定区间的补集
	 */
	public static List<Interval> getComplementary(Interval maxInterval, List<Interval> list) {
		List<Interval> complementaryList = new ArrayList<Interval>();
		if (list == null || list.isEmpty()){
			complementaryList.add(maxInterval.clone());
			return complementaryList;
		}
		Iterator<Interval> iter = list.iterator();
		Interval interval = null;
		while(iter.hasNext()){ 
			if((interval = iter.next()).right <= maxInterval.left){
				continue;
			} else {
				break;
			}
		}
		if(iter.hasNext() && interval.left > maxInterval.right){
			return complementaryList;//空集
		}
		if(maxInterval.left < interval.left){
			Interval temp = new Interval(maxInterval.left, interval.left);
			complementaryList.add(temp);
		}
		Interval last = interval;
		//double lastright = interval.right;
		while(iter.hasNext()){ 
			interval = iter.next();
			if(interval.right >= maxInterval.right){
				break;
			}
			if(last.right < interval.left){//防止出现虽是连续区间但是却分段的情况，如：[3.0,4.0), [4.0, 5.0)
				Interval temp = new Interval(last.right, interval.left);
				complementaryList.add(temp);
			}
			last = interval;//lastright = interval.right;
		}
		if(interval.right < maxInterval.right){
			complementaryList.add(new Interval(interval.right, maxInterval.right));
		} else {
			double end = Math.min(interval.left, maxInterval.right);
			if(last.right < end){
				complementaryList.add(new Interval(last.right, end));
			}
		}
		
		return complementaryList;
	}
	
	/**
	 * 这里是当前类的一些测试用例
	 * @param args 【未使用】
	 * */
	public static void main(String[] args){
		Interval universalInterval = new Interval(-10, 10);
		List<Interval> list1 = new java.util.LinkedList<Interval>();
		List<Interval> list2 = new java.util.LinkedList<Interval>();
		list1.add(new Interval(0.0, 3.0));
		list1.add(new Interval(3.0, 6.0));
		list1.add(new Interval(6.0, 10.0));
		//list1.add(new Interval(11.0, 13.0));
		
		list2.add(new Interval(1.0, 5.0));
		list2.add(new Interval(6.5, 7.5));
		list2.add(new Interval(8.5, 10.5));

		//list1 = Interval.standardize(list1);
		
		List<Interval> il = Interval.getIntersection(list2, list1);
		List<Interval> ul = Interval.getUnion(universalInterval, list2, list1);
		List<Interval> cl = Interval.getComplementary(universalInterval, list2);
		System.out.println("UniversalSet="+universalInterval);
		System.out.println("SetList1="+list1);
		System.out.println("SetList2="+list2);
		System.out.println("Intersection(SetList1,SetList2)="+il);
		System.out.println("Union((SetList1,SetList2)=)="+ul);
		System.out.println("Complementary(UniversalSet, list2)="+cl);
	}
}







