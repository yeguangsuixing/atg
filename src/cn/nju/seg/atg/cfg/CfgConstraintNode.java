package cn.nju.seg.atg.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cn.nju.seg.atg.model.Constraint;
import cn.nju.seg.atg.model.Constraint.CtType;
import cn.nju.seg.atg.model.Interval;
import cn.nju.seg.atg.model.Point;

public class CfgConstraintNode implements Comparable<CfgConstraintNode> {

	private static int ID = 0;
	/** 当前节点的唯一标识 */
	private int id;
	/** 当前节点的约束 */
	public Constraint constraint;
	/** 下一个用||连接的约束条件 */
	public CfgConstraintNode nextOrNode;
	/** 第一个用&&连接的约束条件 */
	public CfgConstraintNode childAndNode;
	/** 当前约束条件对应的坐标点 */
	private Set<Point> points = new TreeSet<Point>();
	
	/** 是否处于满足状态 */
	private boolean negative = false;
	
	private CfgConstraintNode(int id, IASTExpression exp, boolean negative){
		this.id = id;
		if(exp == null) return;
		this.constraint = new Constraint(exp);
		this.negative = negative;
	}
	
	/** 对于约束节点，我们使用负数id来标识 */
	public CfgConstraintNode(IASTExpression exp, boolean negative){
		this(--ID, exp, negative);
	}
	
	public int getId(){
		return this.id;
	}

	/**
	 * 根据当前的坐标点通过线性拟合的方法获取一些有效的区间列表
	 * @return 不为null但可能size为0的列表
	 * */
	public List<Interval> getEffectiveIntervalList(Interval maxInterval) {
		List<Interval> effIntervalList = new ArrayList<Interval>();
		Point plast = null;
		for (Point p : this.points) {
			if(plast == null){
				plast = p; continue;
			}
			Interval interval = Clf.getEffectiveInterval(
					constraint.curType, plast, p);
			if (interval != null) {
				effIntervalList.add(interval);
			}
			plast = p;
		}
		if(negative){
			return Interval.getComplementary(maxInterval, effIntervalList);
		} else {
			return effIntervalList;
		}
	}
	
	/** 获取点集，用于线性拟合 */
	public Set<Point> getPointSet(){
		return this.points;
	}
	
	/** 清除所有的坐标信息 */
	public void clearPoints(){
		this.points.clear();
	}
	
	/**
	 * 线性拟合静态类，提供线性拟合的静态方法
	 * @author ygsx
	 * @time 2014/04/30 21:30
	 * */
	private static class Clf {

		/**
		 * 计算有效取值区间
		 * @return 一个区间
		 */
		public static Interval getEffectiveInterval(CtType ctType, Point p1, Point p2) {
			if (ctType == CtType.Greater || ctType == CtType.GreaterEqual) {
				return getGreaterInterval(p1, p2);
			} else if (ctType == CtType.Less || ctType == CtType.LessEqual) {
				return getLessInteval(p1, p2);
			} else if (ctType == CtType.Equal || ctType == CtType.NotEqual) {
				return new Interval(p1.x, p2.x);
			} else {
				return null;
			}
		}

		private static Interval getLessInteval(Point p1, Point p2) {

			Interval interval = null;
			double x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
			
			if (Math.abs(y1 - y2) <= Double.MIN_VALUE) {
				if (y1 >= 0 || y2 >= 0) {
					interval = null;
				} else if (y1 <= 0 || y2 <= 0) {
					interval = new Interval(x1, x2);
				}
			} else {
				if (y1 <= 0) {
					if (y2 <= 0) {
						interval = new Interval(x1, x2);
					} else {
						interval = new Interval(x1, (x1*y2-x2*y1)/(y2-y1));
					}
				} else {
					if (y2 >= 0) {
						interval = null;
					} else {
						interval = new Interval((x1*y2-x2*y1)/(y2-y1), x2);
					}
				}
			}
			return interval;
		}

		private static Interval getGreaterInterval(Point p1, Point p2) {
			Interval interval = null;
			if (Math.abs(p1.y - p2.y) <= Double.MIN_VALUE) {//斜率为0
				if (p1.y >= 0 || p2.y >= 0) {
					interval = new Interval(p1.x, p2.x);
				} else if (p1.y <= 0 || p2.y <= 0) {
					interval = null;
				}
			} else {//斜率不为0
				if (p1.y >= 0) {
					if (p2.y >= 0) {
						interval = new Interval(p1.x, p2.x);
					} else { 
						interval = new Interval(p1.x, (p1.x*p2.y - p2.x*p1.y)/(p2.y - p1.y));
					}
				} else {
					if (p2.y <= 0) {
						interval = null;
					} else {
						interval = new Interval((p1.x*p2.y - p2.x*p1.y)/(p2.y - p1.y), p2.x);
					}
				}
			}
			return interval;
		}
	}

	@Override
	public int compareTo(CfgConstraintNode o) {
		int t = o.constraint.getOffset();
		int m = this.constraint.getOffset();
		return t - m;//越靠前越大
	}
	
	
	public String toString(){
		return id + ":" + constraint.toString();
	}
}







