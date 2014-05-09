package cn.nju.seg.atg;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.model.ASTStringUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

import cn.nju.seg.atg.cfg.Cfg;
import cn.nju.seg.atg.cfg.CfgCondNode;
import cn.nju.seg.atg.cfg.CfgNode;
import cn.nju.seg.atg.cfg.CfgPath;
import cn.nju.seg.atg.cppmanager.CppManager;
import cn.nju.seg.atg.cppmanager.CppManager.ArgType;
import cn.nju.seg.atg.cppmanager.CppManager.CallResult;
import cn.nju.seg.atg.cppmanager.CppManager.CompileResult;
import cn.nju.seg.atg.cppmanager.CppManager.IstrumentResult;
import cn.nju.seg.atg.cppmanager.CppManager.LoadResult;
import cn.nju.seg.atg.model.Interval;
import cn.nju.seg.atg.plugin.AtgActivator;
import cn.nju.seg.atg.plugin.PreferenceConstants;


/**
 * 测试数据自动生成模块主类
 * @author ygsx
 * @time 2014/04/23 11:35
 * */
@SuppressWarnings("restriction")
public class Atg {

	/** 用于控制台输入的流 */
	private PrintStream console;
	/** 要生成测试数据的函数 */
	private IFunctionDeclaration function;
	/** 要生成测试数据的函数签名 */
	private String functionSignature;
	/** 函数名称 */
	private String functionName;
	/** 参数签名数组 */
	private String[] paraSigtArray;
	/** 参数类型数组 */
	private ArgType[] paraTypeArray;
	/** 参数个数 */
	private int paraCount;
	/** 当前要处理的函数对应的抽象语法树 */
	private IASTFunctionDefinition ast;
	/** 控制流程图类 */
	private Cfg cfg;
	/** 上一次操作的Cpp源文件名 */
	private String lastCppFileName;
	
	private CppManager cppManager;
	
	private LoadResult loadSoObject;
	
	private List<CfgPath> allPathList;
	
	/** 测试数据生成线程 */
	private Thread generateThread;

	/** 
	 * 每次向外扩展的最大步长，这必须是一个正数
	 * 对于每个不同的程序，这个值需要调整，调整算法未完成
	 *  */
	private static final double PARA_MAX_STEP = 5.0;
	/** 参数起始左边界 */
	private static final double PARA_BASE_LEFT_BOUNDARY = -PARA_MAX_STEP;
	/** 参数起始长度 */
	private static final double PARA_BASE_LENGTH = PARA_MAX_STEP * 2;
	
	
	private static final String ERR_NOT_FIND_FUNC = 
			"Error: Cannot find the function \"%s\".";
	private static final String ERR_STATE =
			"Error: State error.";
	private static final String ERR_INSTRUMENT =
			"Error: Instrumenting failed!(%s)";
	private static final String ERR_COMPILE =
			"Error: Compiling failed!(%s)";
	private static final String ERR_LOAD =
			"Error: Loading Failed!(%s)";
	private static final String ERR_RUN =
			"Error: Running Failed!(%s)";
	private static final String ERR_RUNNING =
			"Error: One Atg instance is running.";
	private static final String ERR_NOT_RUNNING =
			"Error: No Atg instance is running.";
	private static final String ERR_UPDATE_CFG_FAILED =
			"Error: Updating CFG failed.";
	
	public static interface IArgDataViewer {
		/**
		 * 显示所有CFG路径
		 * @param pathList 所有路径
		 * @param paraSigt 参数签名数组
		 * @param asynUpdate 是否异步刷新
		 * */
		public void showAllPathsData(List<CfgPath> pathList, String[] paraSigt, boolean ansyUpdate);
	}
	public static interface IMsgShower {
		public void showMsg(String msg);
	}
	
	IArgDataViewer dataViewer;
	
	/** 当前生成器状态 */
	private enum State {
		/** 未设置函数签名或函数签名获取失败 */
		Start(0),
		/** 已经获取了AST */
		Ast(1),
		/** 已经生成了CFG */
		Cfg(2),
		/** 已经预处理完毕 */
		Ready(3),
		/** 正在生成测试数据 */
		Generating(4),
		/** 完成测试数据的生成 */
		Finished(5),
		/** 已经释放了资源（动态库） */
		Released(6);
		
		int id;
		private State(int id){ this.id = id; }
	}
	
	/** 当前阶段 */
	private State fState = State.Start;
	
	/**
	 * 使用指定的控制台流，函数声明创建一个测试数据生成类
	 * @param console 用于输出信息的控制台流
	 * @param func 需要生成测试数据的函数声明
	 * @return 一个数据生成对象
	 * */
	public Atg( OutputStream console){
		if(console == null) {
			throw new IllegalArgumentException(
				"The param \"console\" cannot be null."
			);
		}
		this.console = new PrintStream(console);
		this.cppManager = CppManager.getInstance();
	}
	
	/** 更新Cfg */
	public boolean updateCfg(){
		if(fState == State.Generating ) return false;
		if(fState.id >= State.Ready.id 
				&& fState != State.Generating 
				&& fState != State.Released){
			this.posttreatment();
		}
		this.ast = getAst();
		if(this.ast == null){
			fState = State.Start;
			return false;
		}

		this.cfg = new Cfg(ast);
		if(!this.cfg.generateCfg()){
			fState = State.Ast;
			return false;
		}

		this.allPathList = this.cfg.getAllPaths();
		if(dataViewer != null) {
			dataViewer.showAllPathsData(allPathList, paraSigtArray, false);
		}
		fState = State.Cfg;
		return true;
		
	}
	
	/** 设置路径数据显示器 */
	public void setArgDataViewer(IArgDataViewer dataViewer){
		this.dataViewer = dataViewer;
	}
	
	/** 
	 * 1. 设置函数声明
	 * @param func 函数声明
	 * @return 是否设置成功
	 *  */
	public boolean setFunctionDeclaration(IFunctionDeclaration func){
		if(fState == State.Generating){
			console.println(ERR_RUNNING);
			return false;
		}
		if(fState.id >= State.Ready.id
				&& fState != State.Generating 
				&& fState != State.Released){
			this.posttreatment();
		}
		fState = State.Start;
		this.function = func;
		try {
			this.functionSignature = func.getSignature();
		} catch (CModelException e) {
			e.printStackTrace(console); 
			return false;
		}
		//获取对应的函数AST
		this.ast = getAst();
		if(this.ast == null){
			console.println(String.format(ERR_NOT_FIND_FUNC, 
					functionSignature));
			return false;
		}
		fState = State.Ast;
		return true;
	}
	/** 从函数声明获取对应的函数AST */
	private IASTFunctionDefinition getAst(){
		if(this.function == null) return null;
		ITranslationUnit unit = this.function.getTranslationUnit();
		IASTTranslationUnit funcunitast = null;
		try {
			funcunitast = unit.getAST(null, ITranslationUnit.AST_SKIP_ALL_HEADERS);
			//console.println(funcunitast);
		} catch (CoreException e) {
			e.printStackTrace();
		}/*
		IIndex index = null;
		try {
			index = CCorePlugin.getIndexManager().getIndex(
					this.function.getCProject(),
					IIndexManager.ADD_DEPENDENCIES | IIndexManager.ADD_DEPENDENT
				);
			index.acquireReadLock();
			astunit = unit.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
		} catch (CoreException ce) {
			ce.printStackTrace(console); return null;
		} catch (InterruptedException ie) {
			ie.printStackTrace(console); return null;
		}finally {
			if(index != null){
				index.releaseReadLock();
			}
		}//*/
		IASTDeclaration[] declarationArray = funcunitast.getDeclarations(false);
		return getDeclaration(functionSignature, declarationArray);
	}
	/**
	 * 根据声明数组获取指定的函数声明
	 * @param funcSignature 要查找的函数的签名
	 * @param declarationArray 被查找的声明数组
	 * */
	private IASTFunctionDefinition getDeclaration(
			String funcSignature, IASTDeclaration[] declarationArray){
		for(IASTDeclaration decln : declarationArray){
			//对于非函数定义的项直接跳过
			if(!(decln instanceof IASTFunctionDefinition)) continue;
			IASTFunctionDeclarator funcdecl = 
					((IASTFunctionDefinition)decln).getDeclarator();
			String funcname = funcdecl.getName().getRawSignature();
			StringBuilder funcSign = new StringBuilder(funcname);
			String[] paraStrArray = ASTStringUtil.
				getParameterSignatureArray(funcdecl);
			funcSign.append("(");
			for(int i=0;i<paraStrArray.length;i++)
			{
				funcSign.append(paraStrArray[i]);
				if(i < paraStrArray.length-1)
					funcSign.append(", ");
			}
			funcSign.append(")");
			
			if(funcSign.toString().equals(funcSignature)){
				paraSigtArray = paraStrArray;
				paraCount = paraStrArray.length;
				functionName = funcname;
				paraTypeArray = new ArgType[paraCount];
				for(int i = 0; i < paraCount; i ++){
					String type = paraSigtArray[i];
					//带有默认值的参数的类型对应字符串类似：“double=2.0”
					//所以不使用equals，而使用startsWith
					if(type.startsWith("double")){
						paraTypeArray[i] = ArgType.Double;
					} else if(type.startsWith("float")){
						paraTypeArray[i] = ArgType.Float;
					} else if(type.startsWith("int") || type.startsWith("long int")){
						paraTypeArray[i] = ArgType.Int;
					} else if(type.equals("long") || type.equals("long long")
							|| type.startsWith("long=")){
						paraTypeArray[i] = ArgType.Long;
					} else if(type.startsWith("short") || type.startsWith("short int")){
						paraTypeArray[i] = ArgType.Char;
					} else if(type.startsWith("char")){
						paraTypeArray[i] = ArgType.Byte;
					}
				}
				return (IASTFunctionDefinition) decln;
			}
		}
		return null;
	}
	

	/** 
	 * <p>2. 生成控制流程图</p>
	 * @param print 是否打印错误信息（如果有）
	 * @return 是否生成成功
	 *  */
	public boolean generateCfg(boolean print){
		if(fState != State.Ast) {
			if(print){
				console.println(ERR_STATE);
			}
			return false;
		}
		this.cfg = new Cfg(ast);
		if(!this.cfg.generateCfg()){
			return false;
		}
		this.allPathList = this.cfg.getAllPaths();
		if(dataViewer != null) {
			dataViewer.showAllPathsData(allPathList, paraSigtArray, false);
		}
		fState = State.Cfg;
		return true;
	}
	
	/**
	 * <p>3. 预处理，分步骤如下：</p>
	 * <li>(1). 对源程序插桩进行插桩</li>
	 * <li>(2). 编译插桩后的程序为动态库</li>
	 * <li>(3). 加载编译后的动态库 </li>
	 * */
	public boolean pretreatment(String cppfilename){
		lastCppFileName = cppfilename;
		if(fState != State.Cfg){
			console.println(ERR_STATE);
			return false;
		}
		//1. 插桩
		IstrumentResult ir = this.cppManager.instrument(
				function.getLocationURI(), cfg);
		if(!ir.succeed){
			console.println(String.format(ERR_INSTRUMENT, ir.msg));
			return false;
		}
		//2. 编译
		CompileResult cr = this.cppManager.compile(ir.outputFilenName);
		if(!cr.succeed){
			console.println(String.format(ERR_COMPILE, cr.msg));
			return false;
		}
		//3. 加载编译后的动态库
		loadSoObject = this.cppManager.load(cr.sofileName);
		if(!loadSoObject.succeed){
			console.println(String.format(ERR_LOAD, loadSoObject.msg));
			return false;
		}
		fState = State.Ready;
		return true;
	}

	
	/**
	 * <p>4. 创建新线程生成测试数据，分步骤如下：</p>
	 * <li>(1). 生成函数参数</li>
	 * <li>(2). 执行函数</li>
	 * <li>(3). 分析执行结果(线性拟合)</li>
	 * <li>(4). 如果没有覆盖路径转到(1)，否则转到(5)</li>
	 * <li>(5). 发送卸载命令</li>
	 * <li>(6). 输出结果</li>
	 * */
	public void generateData(){
		if(fState.id < State.Ready.id){
			console.println(ERR_STATE);
			return;
		}
	
		//*
		generateThread = new Thread(new Runnable(){
			@Override
			public void run() {
				//生成测试数据
				fState = State.Generating;
				geneTestData(allPathList);
				if(dataViewer != null) {
					dataViewer.showAllPathsData(allPathList, paraSigtArray, true);
				}
				fState = State.Finished;
			}
		});
		generateThread.start();//*/

		
	}

	/** 5. 后处理：释放资源 */
	private void posttreatment(){
		if(fState == State.Ready || fState == State.Finished){
			CppManager.getInstance().unload(loadSoObject);
		}
		fState = State.Released;
	}
	
	/**
	 * 一“键”运行
	 * @param shower 错误显示器
	 * @param cppfileName 源文件名
	 * */
	public void run(IMsgShower shower, String cppfileName){
		if(fState == State.Generating){
			if(shower != null){
				shower.showMsg(ERR_RUNNING);
			}
			return;
		}
		if(!updateCfg()){
			if(shower != null){
				shower.showMsg(ERR_UPDATE_CFG_FAILED);
			}
		}
		if(!pretreatment(lastCppFileName)){
			return;
		}
		generateData();
	}
	
	/** 暂停当前的生成数据操作/向C++运行服务器发送暂停命令 */
	@SuppressWarnings("deprecation")
	public void pause(IMsgShower shower){
		if(fState != State.Generating){
			if(shower != null){
				shower.showMsg(ERR_NOT_RUNNING);
			}
			return;
		}
		generateThread.suspend();//dangerous
	}

	/** 
	 * 停止当前的生成数据操作
	 * 
	 *  */
	@SuppressWarnings("deprecation")
	public void stop(IMsgShower shower){
		if(fState != State.Generating){
			if(shower != null){
				shower.showMsg(ERR_NOT_RUNNING);
			}
			return;
		}
		generateThread.stop();//dangerous
		if(dataViewer != null) {
			dataViewer.showAllPathsData(allPathList, paraSigtArray, false);
		}
		fState = State.Finished;
	}
	

	/** 
	 * 获取所有CFG路径
	 * @return 当前CFG的所有路径
	 *  */
	public List<CfgPath> getAllPaths(){
		if(fState.id < State.Cfg.id) {
			console.println(ERR_STATE);
			return new ArrayList<CfgPath>(0);
		}
		return this.allPathList;
	}
	
	/** 获取CFG的入口 */
	public CfgNode getCfgEntry(){
		if(this.cfg == null) return null;
		return this.cfg.getEntry();
	}
	
	/** 生成测试数据 */
	private int geneTestData(List<CfgPath> pathList){
		assert(this.paraCount > 0);
		/*
		 * 覆盖所有路径{
		 * 		对于每一条路径，对每一个参数作测试数据生成
		 * }
		 * */
		IPreferenceStore store = AtgActivator.getDefault().getPreferenceStore();
		//对于每个参数，所需要遍历的轮数
		int circle = store.getInt(PreferenceConstants.NR_CIRCLE);
		int detect = store.getInt(PreferenceConstants.NR_DETECT);
		List<CfgNode> nodeList = this.cfg.getAllNodes();
		int coverredcount = 0;//记录覆盖路径数
		for(CfgPath targetpath : pathList){
			//如果已经覆盖过，直接跳过
			if(targetpath.isCoverred()){
				++coverredcount;
				continue;
			}
			//设置条件节点的满足性
			CfgCondNode condnode = null;
			for(CfgNode node : targetpath.getPath()){
				if(condnode != null){
					condnode.setSatisfied(condnode.isThenNode(node));
					condnode = null; continue;
				}
				if(!node.isCondType())  continue;
				condnode = (CfgCondNode) node;
			}
			//控制变量：每次选择一个变量（参数），保持其他参数不变
			for(int paraIndex = 0; paraIndex < this.paraCount; paraIndex ++){
				for(int i = 0; i < circle; i ++){
					//清除节点中的坐标信息
					for(CfgNode node : nodeList) {
						if(!node.isCondType()) continue;
						((CfgCondNode)node).clearAllNodesPoints();
					}
					geneTestDataByPathAndPara(pathList, targetpath, paraIndex, detect);
					if(targetpath.isCoverred()){
						++coverredcount;
						break;//如果已经覆盖，那么直接进入下一条路径
					}
				}
				if(targetpath.isCoverred()){
					break;//如果已经覆盖，那么直接进入下一条路径
				}
			}
		}
		return coverredcount;
	}

	
	
	/** 根据指定的目标路径和参数生成测试数据 */
	private void geneTestDataByPathAndPara(List<CfgPath> pathList, 
			CfgPath targetpath, int paraIndex, int detectCount){
		//测试参数数组
		Object[] paraArray = null;

		List<Double> paraValuePool = new ArrayList<Double>();
		Interval maxInterval = new Interval(Double.MAX_VALUE, -Double.MAX_VALUE);
		List<CfgNode> commonAncesters = null;
		for(int i = 0; i < 2; i ++){
			//paraArray：第一次的时候为null，第二次时不为null
			paraArray = geneParaArray(paraIndex, paraArray);
			//运行程序
			CallResult rsl = callFunction(paraArray, targetpath, 
					paraIndex, paraValuePool);
			//rsl 为空，要么是运行出错，要么是覆盖了目标路径
			if(rsl == null) return;
			commonAncesters = targetpath.getCommonAncesters(rsl.path);
			targetpath.addParas(commonAncesters.size(), paraArray);
			//以下根据这两次的随机生成参数设置maxInterval
			double para = (Double) paraArray[paraIndex];
			if(para < maxInterval.left){
				maxInterval.left = para;
			}
			if(para > maxInterval.right){
				maxInterval.right = para;
			}
		}
		maxInterval.updateLength();
		

		int totalgenecount = detectCount;
		while(totalgenecount-- > 0){
			
			/******************************************************************
			 * 
			 * 从自变参数衍生池中衍生新的自变参数
			 * 
			 * ****************************************************************/
			//对于当前覆盖路径上的条件节点，获取它们线性拟合后的区间列表的交集
			List<Double> newparaList = new ArrayList<Double>();
			List<Interval> effectIntervalList = new ArrayList<Interval>();
			effectIntervalList.add(maxInterval);
			for (CfgNode ancesternode : commonAncesters) {
				if (ancesternode.isCondType()) {
					List<Interval> templist = ((CfgCondNode)ancesternode)
							.getEffectiveIntervalList(maxInterval);
					effectIntervalList = Interval.getIntersection(
							effectIntervalList, templist);
				}
			}
			
			//根据得到的交集，也就是相交后的区间列表，分别获取一个参数
			int numOfNewParams = 0;
			
			if(effectIntervalList != null){
				for (Interval effinterval : effectIntervalList) {
					double newParameter = effinterval.getRandom();//TODO effinterval.getGolden(true);//
					newparaList.add(newParameter);
					numOfNewParams++;
				}
			} // else TODO 为空如何处理
			
			// 如果衍生出的新参数不多于2个，则以当前衍生池边界值向外扩展出新参数
			if (numOfNewParams <= 2) {
				double autoIncreased = maxInterval.left - 
						Math.random()*PARA_MAX_STEP;
				//标记为向外扩展时生成的
				//Builder.autoIncreasedParameterList.add(autoIncreased);
				newparaList.add(autoIncreased);
				autoIncreased = maxInterval.right + 
						Math.random()*PARA_MAX_STEP;
				//标记为向外扩展时生成的
				//Builder.autoIncreasedParameterList.add(autoIncreased);
				newparaList.add(autoIncreased);
			}
			
			//遍历上面衍生出来的参数
			for(Double para : newparaList){
				Object[] newparaArray = new Object[this.paraCount];
				for(int ii = 0; ii < this.paraCount; ii ++){
					if(ii == paraIndex){
						newparaArray[ii] = para;
					} else {
						newparaArray[ii] = paraArray[ii];
					}
				}
				CallResult rsl = callFunction(newparaArray, targetpath, 
						paraIndex, paraValuePool);
				//rsl 为空，要么是运行出错，要么是覆盖了目标路径
				if(rsl == null) return;
				//else continue;
				commonAncesters = targetpath.getCommonAncesters(rsl.path);
				targetpath.addParas(commonAncesters.size(), newparaArray);
				if(maxInterval.left > para){
					maxInterval.left = para;
				}
				if(maxInterval.right < para){
					maxInterval.right = para;
				}
				maxInterval.updateLength();
			}
		}
	}
	
	
	/** 生成参数数据，如果对应的数据为null，则随机生成一个值
	 * 
	 * @param paraIndex 当前一定发生改变的参数下标
	 * @param paraArray 原有参数向量
	 *  */
	private Object[] geneParaArray(int paraIndex, Object[] paraArray){
		Interval paraGeneInterval = new Interval(PARA_BASE_LEFT_BOUNDARY,
				PARA_BASE_LEFT_BOUNDARY + PARA_BASE_LENGTH);
		Object[] paras = new Object[this.paraCount];
		for(int i = 0; i < this.paraCount; i ++){
			//TODO 这里我们只处理float类型和double类型
			//if(this.paraSigtArray[i].equals("double")){
			if(this.paraSigtArray[i].startsWith("float")
					|| this.paraSigtArray[i].startsWith("double")){
				//对于带有默认值的参数来说，对应字符串类似"double=3"
				if(i == paraIndex ||paraArray == null 
						|| paraArray[i] == null ){
					paras[i] = paraGeneInterval.getRandom();
				} else {
					paras[i] = paraArray[i];
				}
			}
		}
		return paras;
	}
	
	private CallResult callFunction(Object[] paraArray, CfgPath targetpath, 
			int paraIndex, List<Double> paraValuePool){
		CallResult clr = cppManager.callFunction(loadSoObject, 
				this.functionName, this.paraTypeArray, paraArray);
		if(!clr.succeed || clr.path == null){//运行出错
			console.println(String.format(ERR_RUN, clr.msg));
			return null;
		}
		clr.path.addParas(clr.path.length(), paraArray);
		//生成的路径恰好是目标路径
		if(clr.path == targetpath) return null;
		//添加到参数池中，在重新生成参数时需要此池
		paraValuePool.add((Double) paraArray[paraIndex]);
		//添加约束节点运行时的值到约束节点中
		this.cfg.updateConstraintNode((Double) paraArray[paraIndex], 
				clr.innerNodeMap);
		return clr;
	}
	

}















