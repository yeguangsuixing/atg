package cn.nju.seg.atg.model;

/**
 * 坐标点类
 * @author ygsx
 * @time 2014/04/23 11:02
 * */
public class Point implements Comparable<Point> {
	public double x;
	public double y;
	
	/**
	 * 创建一个坐标点，位置(0.0, 0.0)
	 * */
	public Point(){ }
	
	/**
	 * 创建一个坐标点，位置(x, y)
	 * */
	public Point(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public String toString(){
		return String.format("(%f,%f)", x, y);
	}

	@Override
	public int compareTo(Point o) {
		return ((Double)x).compareTo(o.x);
	}
}
