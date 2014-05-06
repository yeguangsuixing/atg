package cn.nju.seg.atg.cppmanager;


/**
 * C++管理器助手，负责Java本地化工作，与C++交互
 * @author ygsx
 * @time 2014/04/26 14:54
 * */
public class CppManagerUtil {

	
	/** 初始化 */
	public native String init();
	
	/** 加载一个动态链接库 */
	public native String load(String sofilename);
	
	/** 卸载一个动态链接库 */
	public native boolean unload(int index);
	
	/** 卸载所有的动态库 */
	public native boolean unloadAll();
	/**
	 * 调用一个函数
	 * @param index 动态库下标
	 * @param funcname 函数名
	 * @param argc 参数个数
	 * @param argt 逆序参数类型列表
	 * @param args 逆序参数列表
	 * @param byteArgs byte类型参数数组
	 * @param charArgs char类型参数数组
	 * @param intArgs int类型参数数组
	 * @param longArgs long类型参数数组
	 * @param floatArgs float类型参数数组
	 * @param doubleArgs double类型参数数组
	 * @return <div>执行结果</div>
	 * <div>第一个字节是0/1，1-成功，0-失败；</div>
	 * <div>第二个字节是一个换行符(\n)；</div>
	 * <div>第三个字节开始，如果失败则是错误信息，否则是执行路径信息。
	 * 执行路径信息是一系列的节点编号，每个节点编号之间用制表符(\t)隔开。
	 * </div>
	 * */
	public native String call(int index, String funcname,
			int argc, int[] argt, byte[] byteArgs, char[]charArgs,
			int[] intArgs, long[] longArgs, 
			float[]floatArgs, double[] doubleArgs);
	
	/**
	 * 获取内部节点的运行情况
	 * */
	public native int[] getInnerNodePath();
	/** 获取内部节点的运行时刻的值 */
	public native double[] getInnerNodeValue();
}










