package cn.nju.seg.atg;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.model.ASTStringUtil;
import org.eclipse.core.runtime.CoreException;

import cn.nju.seg.atg.cfg.Cfg;
import cn.nju.seg.atg.cfg.CfgCondNode;
import cn.nju.seg.atg.cfg.CfgNode;
import cn.nju.seg.atg.cfg.CfgPaintingPanel;
import cn.nju.seg.atg.cfg.CfgNode.Type;
import cn.nju.seg.atg.cfg.CfgPath;
import cn.nju.seg.atg.cppmanager.CppManager;
import cn.nju.seg.atg.cppmanager.CppManager.ArgType;
import cn.nju.seg.atg.cppmanager.CppManager.CallResult;
import cn.nju.seg.atg.cppmanager.CppManager.CompileResult;
import cn.nju.seg.atg.cppmanager.CppManager.IstrumentResult;
import cn.nju.seg.atg.cppmanager.CppManager.LoadResult;
import cn.nju.seg.atg.model.Interval;
import cn.nju.seg.atg.plugin.AtgAction;
import cn.nju.seg.atg.plugin.CfgRulerColumn.ICfgPaintingPanel;


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
	
	private CppManager cppManager;
	
	private LoadResult loadSoObject;
	
	/** 用于界面操作的对象 */
	private AtgAction atgUi;
	/** cfg绘制面板 */
	private ICfgPaintingPanel cfgPanel;
	
	/**
	 * 使用指定的控制台流，函数声明创建一个测试数据生成类
	 * @param console 用于输出信息的控制台流
	 * @param func 需要生成测试数据的函数声明
	 * @return 一个数据生成对象
	 * */
	public Atg(AtgAction atgAction, OutputStream console, IFunctionDeclaration func){
		if(console == null) {
			throw new IllegalArgumentException(
				"The param \"console\" cannot be null."
			);
		}
		this.console = new PrintStream(console);
		this.function = func;
		this.atgUi = atgAction;
		this.cppManager = CppManager.getInstance();
		try {
			this.functionSignature = func.getSignature();
		} catch (CModelException e) {
			e.printStackTrace();
		}
	}
	
	/** 打印str字符串到控制台 */
	public void print(String str){
		try { console.write(str.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Cfg getCfg(){
		return this.cfg;
	}
	
	/**
	 * <p>预处理，分步骤如下：</p>
	 * <li>1. 生成抽象语法树</li>
	 * <li>2. 生成控制流程图</li>
	 * <li>3. 插桩</li>
	 * <li>4. 启动/连接C++运行服务器</li>
	 * <li>5. 发送编译+加载命令 </li>
	 * */
	public void pretreatment(){
		atgUi.clearAllAnnotations();
		/** 1. 生成抽象语法树 */
		generateAst();
		/** 2. 生成控制流程图 */
		this.cfg = new Cfg(ast);
		this.cfg.run();
		//获取所有路径
		//List<CfgPath> pathList = this.cfg.getAllPaths();
		//Atg.printPath(pathList);
		//this.cfg.printAllNodes();
		//插桩
		IstrumentResult ir = this.cppManager.instrument(
				function.getLocationURI(), cfg);
		if(!ir.succeed){
			System.out.println("Instrument Failed!(msg:"+ir.msg+")");
			return;
		}
		//编译
		CompileResult cr = this.cppManager.compile(ir.outputFilenName);
		if(!cr.succeed){
			System.out.println("Compiling Failed!(msg:"+cr.msg+")");
			return;
		}
		//加载编译后的动态库
		loadSoObject = this.cppManager.load(cr.sofileName);
		if(!loadSoObject.succeed){
			System.out.println("Loading Failed!(msg:"+loadSoObject.msg+")");
			return;
		}
		
		cfgPanel = new CfgPaintingPanel(cfg);
		
		
	}

	/**
	 * <p>开始生成测试数据，分步骤如下：</p>
	 * <li>6. 生成函数参数</li>
	 * <li>7. 发送执行命令</li>
	 * <li>8. 分析执行结果(线性拟合)</li>
	 * <li>9. 发送卸载命令</li>
	 * <li>10.输出结果</li>
	 * */
	public void run(){
		
		List<CfgPath> pathList = this.cfg.getAllPaths();

		this.atgUi.clearAllAnnotations();
		if(pathList.size() > 0){
			CfgPath path = pathList.get(0);
			cfgPanel.setFocusPath(path);
			for(CfgNode node : path.getPath()){
				Set<Entry<Integer, Integer>> set = node.srcMap.entrySet();
				for(Entry<Integer, Integer> entry : set){
					this.atgUi.addAnnotation(null, entry.getKey(), entry.getValue());
				}
			}
		}
		
		//生成测试数据
		int coverredCount = geneTestDate(pathList);
		
		console.println("路径总数："+pathList.size());
		console.println("覆盖路径数："+coverredCount);
		for(CfgPath path : pathList){
			console.println(String.format(
					"path %d: 覆盖-%s, 最长覆盖-%d, %s", path.getId(), 
					path.isCoverred, path.coverredNodeCount, path.getPathString()));
			List<Object[]> parasl = path.getParasList();
			for(Object[] t : parasl){
				for(Object o : t){
					console.print(o);
					console.print(",");
				}
				console.println();
			}
		}
		
		CppManager.getInstance().unload(loadSoObject);
		
	}
	
	public ICfgPaintingPanel getCfgPanel(){
		return this.cfgPanel;
	}
	
	/** 暂停当前的生成数据操作/向C++运行服务器发送暂停命令 */
	public void pause(){
		
	}

	/** 停止当前的生成数据操作/向C++运行服务器发送停止+卸载命令 */
	public void stop(){
		
	}
	
	/** 生成测试数据 */
	private int geneTestDate(List<CfgPath> pathList){
		assert(this.paraCount > 0);
		/*
		 * 覆盖所有路径{
		 * 		对于每一条路径，对每一个参数作测试数据生成
		 * }
		 * */
		List<CfgNode> nodeList = this.cfg.getAllNodes();
		int coverredcount = 0;//记录覆盖路径数
		for(CfgPath targetpath : pathList){
			int paraIndex = 0;
			//如果已经覆盖过，直接跳过
			if(targetpath.isCoverred){
				++coverredcount;
				continue;
			}
			//设置条件节点的满足性
			CfgCondNode condnode = null;
			for(CfgNode node : targetpath.getPath()){
				if(condnode != null){
					condnode.satisfied = condnode.then == node;
					condnode = null; continue;
				}
				if(node.type != Type.CondIf)  continue;
				condnode = (CfgCondNode) node;
			}
			//控制变量：每次选择一个变量（参数），保持其他参数不变
			for(; paraIndex < this.paraCount; paraIndex ++){
				//清除节点中的坐标信息
				for(CfgNode node : nodeList) {
					if(node.type != Type.CondIf && node.type != Type.CondLoop) 
						continue;
					((CfgCondNode)node).clearAllNodesPoints();
				}
				geneTestDateByPathAndPara(pathList, targetpath, paraIndex);
				if(targetpath.isCoverred){
					++coverredcount;
					break;//如果已经覆盖，那么直接进入下一条路径
				}
			}
		}
		return coverredcount;
	}

	
	/** 
	 * 每次向外扩展的最大步长，这必须是一个正数
	 * 对于每个不同的程序，这个值需要调整，调整算法未完成
	 *  */
	private static final double PARA_MAX_STEP = 5.0;
	/** 参数起始左边界 */
	private static final double PARA_BASE_LEFT_BOUNDARY = -PARA_MAX_STEP;
	/** 参数起始长度 */
	private static final double PARA_BASE_LENGTH = PARA_MAX_STEP * 2;
	
	/** 根据指定的目标路径和参数生成测试数据 */
	private void geneTestDateByPathAndPara(List<CfgPath> pathList, 
			CfgPath targetpath, int paraIndex){
		//测试参数数组
		Object[] paraArray = null;

		int i = 0;
		List<Double> paraValuePool = new ArrayList<Double>();
		Interval maxInterval = new Interval(Double.MAX_VALUE, Double.MIN_VALUE);
		List<CfgNode> commonAncesters = null;
		for(; i < 2; i ++){
			//paraArray：第一次的时候为null，第二次时不为null
			paraArray = geneParaArray(paraIndex, paraArray);
			//运行程序
			CallResult rsl = callFunction(paraArray, targetpath, 
					paraIndex, paraValuePool);
			//rsl 为空，要么是运行出错，要么是覆盖了目标路径
			if(rsl == null) return;
			commonAncesters = targetpath.getCommonAncesters(rsl.path);
			handleAncester(commonAncesters, targetpath, paraArray);
			//以下根据这两次的随机生成参数设置maxInterval
			double para = (Double) paraArray[paraIndex];/*
			if(i == 0){
				maxInterval.left = para;
			} else if(i == 1){//第二次
				if(maxInterval.left > para){
					maxInterval.right = maxInterval.left;
					maxInterval.left = para;
				} else {
					maxInterval.right = para;
				}
			}//*/
			if(para < maxInterval.left){
				maxInterval.left = para;
			}
			if(para > maxInterval.right){
				maxInterval.right = para;
			}
		}
		maxInterval.updateLength();
		int totalgenecount = 100;
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
				if (ancesternode.type == Type.CondIf 
						|| ancesternode.type == Type.CondLoop) {
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
				handleAncester(commonAncesters, targetpath, newparaArray);
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
	
	private Object[] geneParaArray(int paraIndex, Object[] paraArray){
		Interval paraGeneInterval = new Interval(PARA_BASE_LEFT_BOUNDARY,
				PARA_BASE_LEFT_BOUNDARY + PARA_BASE_LENGTH);
		Object[] paras = new Object[this.paraCount];
		for(int i = 0; i < this.paraCount; i ++){
			//TODO 这里我们只处理double类型
			//if(this.paraSigtArray[i].equals("double")){
			if(this.paraSigtArray[i].startsWith("double")){//对于带有默认值的参数来说，对应字符串类似"double=3"
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
	
	private CallResult callFunction(Object[] paraArray, CfgPath targetpath, int paraIndex,
			List<Double> paraValuePool){
		CallResult clr = cppManager.callFunction(loadSoObject, 
				this.functionName, this.paraTypeArray, paraArray);
		if(!clr.succeed || clr.path == null){//运行出错
			System.out.println("Running Failed!(msg:"+clr.msg+")");
			return null;
		}
		//设置此条路径被覆盖
		if(!clr.path.isCoverred){
			clr.path.isCoverred = true;
			clr.path.coverredNodeCount = clr.path.length();
			clr.path.getParasList().clear();
		}
		/*
		int sz = clr.path.getParasList().size();
		if(sz > 0){
			Object[] t = clr.path.getParasList().get(sz-1);
			boolean eq = true;
			for(int i = 0; i < t.length; i ++){
				if(((Double)t[i]).doubleValue() != ((Double)paraArray[i]).doubleValue()) {
					eq = false; break;
				}
			}
			if(eq)
				System.out.println(eq);
		}//*/
		clr.path.getParasList().add(paraArray);
		//生成的路径恰好是目标路径
		if(clr.path == targetpath) return null;
		//添加到参数池中，在重新生成参数时需要此池
		paraValuePool.add((Double) paraArray[paraIndex]);
		//添加约束节点运行时的值到约束节点中
		this.cfg.updateConstraintNode((Double) paraArray[paraIndex], clr.innerNodeMap);
		return clr;
	}
	
	private void handleAncester(List<CfgNode> commonAncesters, 
			CfgPath targetpath, Object[] paraArray){
		int ancestercount = commonAncesters.size();
		if( targetpath.coverredNodeCount < ancestercount ){
			targetpath.coverredNodeCount = ancestercount;
			targetpath.getParasList().clear();
			targetpath.getParasList().add(paraArray);
		} else if( targetpath.coverredNodeCount == ancestercount){
			/*
			int sz = targetpath.getParasList().size();
			if(sz > 0){
				Object[] t = targetpath.getParasList().get(sz-1);
				boolean eq = true;
				for(int i = 0; i < t.length; i ++){
					if(((Double)t[i]).doubleValue() != ((Double)paraArray[i]).doubleValue()) {
						eq = false; break;
					}
				}
				if(eq)
					System.out.println(eq);
			}//*/
			targetpath.getParasList().add(paraArray);
		}
	}
	
	/** 从函数声明获取函数AST */
	private void generateAst(){
		ITranslationUnit unit = this.function.getTranslationUnit();
		IIndex index = null;
		IASTTranslationUnit astunit = null;
		try {
			index = CCorePlugin.getIndexManager().getIndex(
					this.function.getCProject(),
					IIndexManager.ADD_DEPENDENCIES | IIndexManager.ADD_DEPENDENT
				);
			index.acquireReadLock();
			astunit = unit.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
		} catch (CoreException ce) {
			ce.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}finally {
			if(index != null){
				index.releaseReadLock();
			}
		}
		IASTDeclaration[] declarationArray = astunit.getDeclarations(false);
		this.ast = getDeclaration(functionSignature,declarationArray);
		assert(this.ast != null);
	}
	

	/**
	 * 根据声明数组获取指定的函数声明，该方法仅在{@link #generateAst()}中调用
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
					} else if(type.equals("long") || type.startsWith("long=")){
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
	
	//打印所有路径
	@SuppressWarnings("unused")
	private static void printPath(List<CfgPath> pathlist){
		for(CfgPath path : pathlist){
			System.out.println(path.getId() + ":" + path.getPathString());
		}
	}
}















