package cn.nju.seg.atg.cppmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import cn.nju.seg.atg.cfg.Cfg;
import cn.nju.seg.atg.cfg.CfgCondNode;
import cn.nju.seg.atg.cfg.CfgConstraintUnit;
import cn.nju.seg.atg.cfg.CfgNode;
import cn.nju.seg.atg.cfg.CfgNode.Type;
import cn.nju.seg.atg.cfg.CfgPath;
import cn.nju.seg.atg.plugin.AtgActivator;
import cn.nju.seg.atg.plugin.AtgActivator.OperatingSystem;
import cn.nju.seg.atg.plugin.PreferenceConstants;


/**
 * C++管理器
 * @author ygsx
 * @time 2014/04/23 14:07
 * */
public class CppManager {

	public static class OpResult {
		/** 该操作是否成功 */
		public boolean succeed = false;
		/** 该操作失败的信息 */
		public String msg;
	}
	
	/**
	 * 参数类型
	 * @author ygsx
	 * @time 2014/04/26 14:00
	 * */
	public static class CompileResult extends OpResult {
		/** 
		 * 动态库(Shared-Object/Dynamic Linking Library)/文件名，在windows下是dll文件，在Linux下是so文件
		 *  */
		public String sofileName;
		/** 编译起始时间 */
		public Date startTime;
		/** 编译完成/停止时间 */
		public Date stopTime;
		/** g++编译返回的信息 */
		public String msg;//覆盖父类成员
	}
	/** 调用函数执行结果类
	 * @author ygsx
	 * @time 2014/04/26 14:09
	 *  */
	public static class CallResult extends OpResult{
		/** 执行路径 */
		public CfgPath path;
		/** 内部节点运行值 */
		public Map<CfgConstraintUnit, Double> innerNodeMap;
	}
	
	public static class LoadResult extends OpResult {
		/** 当前加载结果索引 */
		/*default*/ int index;
	}
	public static class IstrumentResult extends OpResult{
		/** 插桩输出文件名 */
		public String outputFilenName;
	}

	/**
	 * 参数类型
	 * @author ygsx
	 * @time 2014/04/26 14:06
	 * */
	public static enum ArgType {
		Float,
		Double,
		Int8,
		Int16,
		Int32,
		Int64,
		/** 一维单精度数组 */
		FloatArray,
		/** 一维双精度数组 */
		DoubleArray,
		/** 如果变量类型经过宏定义/或者是复合数据类型，那么暂时不考虑 */
		Unknown
	}

	
	static {
		try{
			String sodllfilename = null;
			if(AtgActivator.OS == OperatingSystem.Linux){
				sodllfilename = "lib/CppManagerUtil.so";
			} else if(AtgActivator.OS == OperatingSystem.Windows){
				sodllfilename = "lib/CppManagerUtil.dll";
			}
			Bundle bundle = Platform.getBundle(AtgActivator.PLUGIN_ID);
			URL sodllUrl = bundle.getResource(sodllfilename);
			String sodll = FileLocator.toFileURL(sodllUrl).getPath();
			System.load(sodll);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private static final String STR_FUNC_EXTERN_C = "extern \"C\" ";
	private static final String STR_FUNC_EXTERN_C_WIN32 = 
			"extern \"C\" __declspec(dllexport) ";
	//此处的返回值只是为了for循环语句中的变量定义通过编译，并不使用其返回值
	private static final String STR_FUNC_DECL = 
			"int (*_cn_nju_seg_atg_cpppathrec_putNodeNumber2Path)(int, double),";
	private static final String STR_FUNC_CALL = 
			"_cn_nju_seg_atg_cpppathrec_putNodeNumber2Path(%d, %s)";
	
	private static final String FOR_INIT_VAR_DEF = 
			"_cn_nju_seg_atg_for_var%d = "
			+ "_cn_nju_seg_atg_cpppathrec_putNodeNumber2Path(%d, %s)";
	private static int FOR_INIT_VAR_NO = 0;
	
	private static final String STR_ERR_CMD = "Command error.(cmd=%s)";
	
	private static final String STR_ERR_CPP_URI_OVERFLOW = "Cpp Uri is too long(>255)! ";
	
	private static final String STR_C_DECL = " __cdecl ";
	
	private static final String STR_PREFIX_ENV_LINUX = "export ";
	private static final String STR_PREFIX_ENV_WINDOWS = "set ";
	
	public static final String DEFAULT_CMD_COMPILE_LINUX = 
			"gcc -shared -fpic -m32 -o $OUT_SO_FILE_NAME $CPP_FILE_NAME ";
	
	public static final String DEFAULT_CMD_COMPILE_WINDOWS =
			"set Path=C:\\Program Files\\Microsoft Visual Studio 10.0\\Common7\\IDE;\r\n"
			+ "set INCLUDE=C:\\Program Files\\Microsoft Visual Studio 10.0\\VC\\include;C:\\Program Files\\Microsoft SDKs\\Windows\\v7.0A\\Include;\r\n"
			+ "set LIB=C:\\Program Files\\Microsoft Visual Studio 10.0\\VC\\lib;C:\\Program Files\\Microsoft SDKs\\Windows\\v7.0A\\Lib;\r\n"
			+ "set TMP=$WORK_DIR\r\n"
			+ "\"C:\\Program Files\\Microsoft Visual Studio 10.0\\VC\\bin\\cl.exe\" \"$CPP_FILE_NAME\" /c /LD /Fo\"$OUT_OBJ_FILE_NAME\"\r\n"
			+ "\"C:\\Program Files\\Microsoft Visual Studio 10.0\\VC\\bin\\link.exe\" \"$OUT_OBJ_FILE_NAME\" /DLL /out:\"$OUT_DLL_FILE_NAME\" ";
	
	
	private static final DateFormat DATE_FMT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	
	/** 最大加载数量 */
	public static final int MAX_LOAD_SIZE = 10;
	
	/** 唯一的管理器实例 */
	private static CppManager MANAGER = new CppManager();
	
	private CppManagerUtil util = null;
	
	
	private Cfg cfg;
	/**
	 * <p>所有已加载但未卸载的共享对象文件名列表</p>
	 * */
	private List<LoadResult> loadList = new ArrayList<LoadResult>(MAX_LOAD_SIZE);
	
			
	/** 不允许直接生成实例
	 * @see #getInstance()
	 *  */
	private CppManager(){
		for(int i = 0; i < CppManager.MAX_LOAD_SIZE; i ++){
			this.loadList.add(null);
		}
		util = new CppManagerUtil();
		util.init();
		//System.out.println("util init:"+b);
	}
	
	/** 获取管理器实例 */
	public static CppManager getInstance(){
		return MANAGER;
	}
	
	/**
	 * 编译一个C++文件
	 * @param workdirstr 当前工作目录
	 * @param cppfilename 要编译的c++文件名(已插桩)
	 * @param printStream 输出流
	 * @return 是否编译成功
	 * */
	public CompileResult compile(String workdirstr, String cppfilename, PrintStream printStream){
		CompileResult cr = new CompileResult();
		//generate output files' names
		int dotindex = cppfilename.lastIndexOf(".");
		String filename = cppfilename;
		if(dotindex > 0) {
			filename = cppfilename.substring(0, dotindex);
		}
		String sofileName = filename + ".so";
		String dllfileName = filename + ".dll";
		String objfileName = filename + ".obj";
		String expfileName = filename + ".exp";
		String libfileName = filename + ".lib";
		if(AtgActivator.OS == OperatingSystem.Windows){
			cr.sofileName = dllfileName;
		} else if(AtgActivator.OS == OperatingSystem.Linux){
			cr.sofileName = sofileName;
		}
		cr.startTime = new Date();
		//
		String compileCmd = AtgActivator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.CMD_COMPILE);
		compileCmd = compileCmd.replace("$OUT_SO_FILE_NAME", sofileName);
		compileCmd = compileCmd.replace("$OUT_DLL_FILE_NAME", dllfileName);
		compileCmd = compileCmd.replace("$OUT_OBJ_FILE_NAME", objfileName);
		compileCmd = compileCmd.replace("$OUT_EXP_FILE_NAME", expfileName);
		compileCmd = compileCmd.replace("$OUT_LIB_FILE_NAME", libfileName);
		compileCmd = compileCmd.replace("$CPP_FILE_NAME", cppfilename);
		compileCmd = compileCmd.replace("$WORK_DIR", workdirstr);
		compileCmd = compileCmd.replace("\r", "");
		String[] compileCmdArray = compileCmd.split("\n");
		List<String> envplist = new ArrayList<String>();
		String envprefix = null;
		if(AtgActivator.OS == OperatingSystem.Linux){
			envprefix = CppManager.STR_PREFIX_ENV_LINUX;
		} else if(AtgActivator.OS == OperatingSystem.Windows){
			envprefix = CppManager.STR_PREFIX_ENV_WINDOWS;
		}

		cr.succeed = true;
		String[] envlistarray = null;
		Process process = null;
		boolean change = false;//has env changed
		File workdir = new File(workdirstr);
		for(String cmd : compileCmdArray){
			if(cmd.startsWith(envprefix)){
				cmd = cmd.substring(envprefix.length());
				String[] envmap = cmd.split("=");
				if(envmap.length >= 2){
					envplist.add(cmd);
					change = true;
					continue;
				} else {
					cr.msg = String.format(CppManager.STR_ERR_CMD, cmd);
					break;
				}
			}
			if(change){
				change = false;
				envlistarray = new String[envplist.size()];//if changed, size is always > 0
				envplist.toArray(envlistarray);
			}
			printStream.println(cmd);
			try {
				process = Runtime.getRuntime().exec(cmd, envlistarray, workdir);
			} catch (IOException e) {
				e.printStackTrace();
				cr.msg = e.getMessage();
				break;
			}
			BufferedReader ireader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			BufferedReader ereader = new BufferedReader(
					new InputStreamReader(process.getErrorStream()));
			String temp = null;
			try {
				while((temp = ireader.readLine()) != null){
					printStream.println(temp);
					cr.msg += temp;
				}
				while((temp = ereader.readLine()) != null){
					printStream.println(temp);
					cr.msg += temp;
					//cr.succeed = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		cr.stopTime = new Date();
		return cr;
	}
	
	/**
	 * 根据给定的控制流对象对一个cpp文件进行插桩
	 * @param cppfilename 要插桩的cpp文件绝对路径
	 * @param cfg cpp文件对应的控制流对象
	 * @param 操作结果，包含是否插桩成功等信息
	 * */
	public IstrumentResult instrument(URI cppUri, final Cfg cfg){
		this.cfg = cfg;
		IstrumentResult ir = new IstrumentResult();
		if(cppUri == null || cfg == null) {
			ir.msg = "Illegal Argument(s)! ";
			return ir;
		}
		File cppfile = new File(cppUri);
		String outfilename = cppfile.getParent()
				+ File.separator
				+ CppManager.generateInstrFileName(cppfile.getName());
		ir.outputFilenName = outfilename;
		if(outfilename.length() > 255){
			ir.msg = STR_ERR_CPP_URI_OVERFLOW;
			return ir;
		}
		File outputfile = new File(outfilename);
		int length = (int) cppfile.length();
		char[] filebuf = new char[length];
		FileReader filereader = null;
		FileWriter writer = null;
		try {//首先将文件读入内存缓冲区filebuf
			filereader = new FileReader(cppfile);
			filereader.read(filebuf);
			filereader.close();
			writer = new FileWriter(outputfile);
		} catch (IOException e) {
			e.printStackTrace();
			ir.msg = e.getMessage();
			return ir;
		}
		List<CfgNode> nodelist = cfg.getAllNodes();
		int index = 0;
		int funcdecloff = cfg.getFuncOffset();
		int funcnameoff = cfg.getFuncNameOffset();
		int paradecloff = cfg.getParaDeclOffset();//
		try {
			for(; index < funcdecloff; index ++){
				writer.append(filebuf[index]);
			}
			//先写入 extern "C"
			if(AtgActivator.OS == OperatingSystem.Linux){
				writer.append(CppManager.STR_FUNC_EXTERN_C);
			} else if(AtgActivator.OS == OperatingSystem.Windows){
				writer.append(CppManager.STR_FUNC_EXTERN_C_WIN32);
			}
			if(AtgActivator.OS == OperatingSystem.Windows){
				for(; index < funcnameoff; index ++){
					writer.append(filebuf[index]);
				}
				writer.append(CppManager.STR_C_DECL);
			}
			for(; index < paradecloff; index ++){
				writer.append(filebuf[index]);
			}
			//再写入函数指针参数
			writer.append(CppManager.STR_FUNC_DECL);
			for(CfgNode node : nodelist){
				if(node.isEmpty()) continue;
				//将节点之前的内容原样输出
				int nodeoff = node.getFirstOffset();
				for(; index < nodeoff; index ++){
					writer.append(filebuf[index]);
				}
				//插桩
				if(node.isForInit()) {//如果是for循环初始化节点，那么先在for之前插桩
					//int foroff = node.getForOffset();
					int end = node.getEndOffset();
					for(; index < end-1; index ++){//-1是为了不输出分号
						writer.append(filebuf[index]);
					}
					writer.append(",");
					if(node.isDeclaration()){
						writer.append(String.format(FOR_INIT_VAR_DEF, 
								FOR_INIT_VAR_NO++, node.getId(), "0"));
					} else {
						writer.append(String.format(
								CppManager.STR_FUNC_CALL, node.getId(), "0"
						));
					}
					continue;
				}
				if(node.isCondType()){
					writer.append(String.format(
							CppManager.STR_FUNC_CALL, node.getId(), "0"
					));
					writer.append(",");
					CfgCondNode condnode = (CfgCondNode)node;
					List<CfgConstraintUnit> consnodelist = condnode.getAllInnerNodes();
					for(CfgConstraintUnit consnode : consnodelist){
						int off = consnode.getOffset();
						if(off < 0) continue;
						int len = consnode.getLength();
						//将原子约束单元之前的内容原样输出
						for(; index < off; index ++){
							writer.append(filebuf[index]);
						}
						writer.append("(");
						writer.append(String.format(
								CppManager.STR_FUNC_CALL, consnode.getId(), 
								consnode.getValueString()
						));
						writer.append(",");
						for(; index < off+len; index ++){
							writer.append(filebuf[index]);
						}
						writer.append(")");
					}
				} else {
					if(node.getType() == Type.Normal && node.isSingle()
							|| node.getType() == Type.Return
							|| node.getType() == Type.Break
							|| node.getType() == Type.Continue){//需要添加大括号的情况
						writer.append('{');
						writer.append(String.format(
								CppManager.STR_FUNC_CALL, node.getId(), "0"));
						writer.append(";");
						//TODO 如果语句包含分号，那么以下的【跳过分号】操作将会出现问题
						for(; filebuf[index] != ';'; index ++){
							writer.append(filebuf[index]);
						}
						writer.append(filebuf[index++]);////跳过分号
						writer.append('}');
					} else {//不需要添加大括号
						writer.append(String.format(
								CppManager.STR_FUNC_CALL, node.getId(), "0"
						));
						if(node.getType() == Type.Iterate){
							writer.append(',');
						} else {//TODO 还有什么类型？
							writer.append(';');
						}
					}
				}
			}
			for(;index < length; index++){
				writer.append(filebuf[index]);
			}
			writer.close();
			ir.succeed = true;
			ir.msg = "Succeed! ";
			return ir;
		} catch (IOException e) {
			e.printStackTrace();
			ir.msg = e.getMessage();
		}
		return ir;
	}
	
	/** 生成插桩后文件名 */
	private static String generateInstrFileName(String originFileName){
		String dstname;
		int index = originFileName.indexOf(".");
		String suffix = "_instr_"+DATE_FMT.format(new Date());
		if(index < 0){//没有扩展名
			dstname =  originFileName + suffix;
		} else {
			dstname = 
				originFileName.substring(0, index)
				+ suffix
				+ originFileName.substring(index);
		}
		return dstname;
	}
	
	/** 加载一个共享对象
	 * @param sofilename 要加载的共享对象文件名
	 * @return 是否加载成功
	 *  */
	public LoadResult load(String sofilename){
		LoadResult lr = new LoadResult();
		//TODO 未设置lr.index，使用默认值0
		lr.msg = util.load(sofilename);
		lr.succeed = lr.msg != null && lr.msg.length() >= 2 && lr.msg.charAt(0)=='1';
		if(lr.succeed){
			loadList.set(lr.index, lr);
		}
		return lr;
	}
	
	/** 卸载一个共享对象
	 * @param sofilename 要卸载的共享对象文件名
	 * @return 是否卸载成功
	 *  */
	public boolean unload(LoadResult lr){
		boolean unloadresult = util.unload(lr.index);
		if(unloadresult){
			this.loadList.set(lr.index, null);
		}
		return unloadresult;
	}
	
	/**
	 * 调用一次指定的函数
	 * @param funcname 要调用的函数名
	 * @param argc 函数参数个数
	 * @param args 字符串格式的参数类型
	 * @param argt 参数类型
	 * @return 执行信息
	 * */
	public CallResult callFunction(LoadResult lr, String funcname,
			ArgType[] argt, Object[] args){
		CallResult cr = new CallResult();
		int int8c = 0, int16c = 0, int32c = 0, int64c = 0, floatc = 0, doublec = 0;
		int arraycount = 0;
		//计算每种类型的参数个数，保存到bytec, charc, intc, longc, floatc, doublec
		for(int i = 0; i < argt.length; i ++){
			ArgType t = argt[i];
			if(t == ArgType.Float){
				floatc++;
			} else if(t == ArgType.Double){
				doublec++;
			} else if(t == ArgType.Int8){
				int8c++;
			} else if(t == ArgType.Int16){
				int16c++;
			} else if(t == ArgType.Int32){
				int32c++;
			} else if(t == ArgType.Int64){
				int64c++;
			} else if(t == ArgType.FloatArray){
				floatc += ((Float[])args[i]).length;
				++ arraycount;
			} else if(t == ArgType.DoubleArray){
				doublec += ((Double[])args[i]).length;
				++ arraycount;
			}
		}
		//以j开头的变量用于传递到C++中
		int[] jargt = new int[argt.length];
		//反向设置参数类型
		for(int i = 0; i < argt.length; i ++){
			jargt[i] = argt[argt.length-1-i].ordinal();
		}
		byte[] jbargs = null;
		char[] jcargs = null;
		int[] jiargs = null;
		long[] jlargs = null;
		float[] jfargs = null;
		double[] jdargs = null;
		int[] jarrayEntries = null;
		if(int8c > 0){
			jbargs = new byte[int8c];
		}
		if(int16c > 0){
			jcargs = new char[int16c];
		}
		if(int32c > 0){
			jiargs = new int[int32c];
		}
		if(int64c > 0){
			jlargs = new long[int64c];
		}
		if(floatc > 0){
			jfargs = new float[floatc];
		}
		if(doublec > 0){
			jdargs = new double[doublec];
		}
		if(arraycount > 0){
			jarrayEntries = new int[arraycount];
		}
		//bytec = 0; charc = 0; intc = 0; longc = 0; floatc = 0; doublec = 0;
		for(int i = 0; i < argt.length; i ++){
			if(argt[i] == ArgType.Float){
				jfargs[--floatc] = (Float) args[i];
			} else if(argt[i] == ArgType.Double){
				jdargs[--doublec] = (Double) args[i];
			} else if(argt[i] == ArgType.Int8){
				jbargs[--int8c] = (Byte) args[i];
			} else if(argt[i] == ArgType.Int16){
				jcargs[--int16c] = (Character) args[i];
			} else if(argt[i] == ArgType.Int32){
				jiargs[--int32c] = (Integer) args[i];
			} else if(argt[i] == ArgType.Int64){
				jlargs[--int64c] = (Long) args[i];
			} else if(argt[i] == ArgType.FloatArray){
				Float[] fa = ((Float[]) args[i]);
				for(int k = fa.length - 1; k >= 0; k --){
					jfargs[--floatc] = fa[k];
				}
				jarrayEntries[--arraycount] = fa.length;
			} else if(argt[i] == ArgType.DoubleArray){
				Double[] da = ((Double[]) args[i]);
				for(int k = da.length - 1; k >= 0; k --){
					jdargs[--doublec] = da[k];
				}
				jarrayEntries[--arraycount] = da.length;
			}
		}
		String rsl = util.call(lr.index, funcname, argt.length, jargt, 
				jbargs, jcargs, jiargs, jlargs, jfargs, jdargs, jarrayEntries);
		//System.out.println("result:"+rsl);
		if(rsl == null || rsl.length() < 3){
			cr.msg = null;
			cr.succeed = false;
			return cr;
		}
		cr.succeed = rsl.charAt(0) == '1';
		cr.msg = rsl.substring(2);
		if(cr.succeed){
			cr.path = this.cfg.getPathFromString(cr.msg, ",");
			cr.innerNodeMap = getConstraintUnitExecInfo();
		}
		return cr;
	}
	
	/**
	 * 获取约束单元运行信息
	 * */
	private Map<CfgConstraintUnit,Double> getConstraintUnitExecInfo(){
		Map<CfgConstraintUnit,Double> execmap = new TreeMap<CfgConstraintUnit,Double>();
		int[] nodes = util.getInnerNodePath();
		double[] values = util.getInnerNodeValue();
		for(int i = 0; i < nodes.length; i ++){
			CfgConstraintUnit cn = this.cfg.getConstraintNodeById(nodes[i]);
			execmap.put(cn, values[i]);
		}
		return execmap;
	}
	
	/**
	 * 释放资源，卸载所有未卸载的链接库
	 * */
	protected void finalize(){
		util.unloadAll();
	}
}













