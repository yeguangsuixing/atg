package cn.nju.seg.atg;

import java.util.Date;
import java.util.List;

import cn.nju.seg.atg.cfg.CfgPath;

/**
 * 测试数据生成报告
 * @author ygsx
 * @time 2014/05/23 10:34
 * */
public class AtgReport {

	/** 开始时间 */
	private Date startTime;
	
	/** 停止/结束时间 */
	private Date stopTime;
	
	/** 总时间 */
	private long totalTick;
	
	/** 程序占用时间 */
	private long progTick;
	
	/** 算法占用时间 */
	private long algoTick;
	
	
	/** 路径列表 */
	private List<CfgPath> pathList;
	
	/** 源文件名 */
	private String progFile;
	
	/** 函数签名 */
	private String funcSigt;

	/** 路径总数 */
	private int pathCount;
	
	/** 被覆盖的路径 */
	private int coveredPathCount;
	
	/** 总探测次数 */
	private int totalDetect;
	
	/** 平均探测次数 */
	private float averageDetect;
	
	/** 路径平均覆盖率 */
	private float averageCoverRate;
	
	public AtgReport(String progFile, String funcSigt, Date startTime, List<CfgPath> pathlist){
		this.startTime = startTime;
		this.stopTime = null;
		this.totalTick = this.progTick = this.algoTick = 0;
		this.pathList = pathlist;
		this.pathCount = pathlist.size();
		this.progFile = progFile;
		this.funcSigt = funcSigt;
		this.coveredPathCount = 0;
		this.totalDetect = 0;
	}
	/**设置结束时间为当前时间*/
	public void setStopTime(){
		this.stopTime = new Date();
	}
	
	void addProgTick(long tick){
		this.progTick += tick;
	}
	/**
	 * 生成评估报告
	 * */
	public void calculate(){
		totalTick = stopTime.getTime() - startTime.getTime();
		algoTick = totalTick - progTick;
		coveredPathCount = 0;
		totalDetect = 0;
		for(CfgPath path : pathList){
			totalDetect += path.getDetect();
			if(path.isCoverred()){
				coveredPathCount++;
			}
		}
		if(pathCount > 0){
			averageDetect = (float)totalDetect / pathCount;
			averageCoverRate = (float)coveredPathCount/pathCount;
		} else {
			averageDetect = 0.0f;
			averageCoverRate = 0.0f;
		}
	}
	
	public Date getStartTime(){ return this.startTime; }
	
	public Date getStopTime(){ return this.stopTime; }
	
	public long getTotalTick(){ return this.totalTick; }

	public long getProgramTick(){ return this.progTick; }
	
	public long getAlgorithmTick(){ return this.algoTick; }
	
	public List<CfgPath> getPathList(){ return this.pathList; }
	
	public String getProgramFile(){ return this.progFile; }

	
	public String getFuncSignature(){ return this.funcSigt; }
	
	public int getPathCount(){ return this.pathCount; }
	
	public int getCoveredPathCount(){ return this.coveredPathCount; }
	
	public int getTotalDetect(){ return this.totalDetect; }
	
	public float getAverageDetect(){ return this.averageDetect; }
	
	public float getAverageCoverRatio(){ return this.averageCoverRate; }
}










