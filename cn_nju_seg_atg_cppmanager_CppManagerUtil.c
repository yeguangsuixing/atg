
#include "stdlib.h"
#include "string.h"
#include "dlfcn.h"

#include "cn_nju_seg_atg_cppmanager_CppManagerUtil.h"



#define MAX_PATH_LENGTH		256
#define MAX_SO_FILE_SIZE	10
#define MAX_FUNC_NAME		100

#ifndef NULL 
#define NULL ((void*)0)
#endif


enum ArgType { 
	_Float = 0, 
	_Double = 1, 
	_Char = 2, 
	_Short = 3,
	_Int = 4, 
	_Long = 5,
	_FloatArray = 6,
	_DoubleArray = 7
};

//shared-object pointer
void* dpa[MAX_SO_FILE_SIZE];
void* dp_pathrec;


//the max number of node each path
#define MAX_NR_NODE			500
#define MAX_EXEC_INFO_SIZE	1024*100

int node[MAX_NR_NODE];
int nodeindex;
int innerNode[MAX_NR_NODE];
double innerNodeValue[MAX_NR_NODE];
int innerNodeIndex;
char execInfo[MAX_EXEC_INFO_SIZE];

int _cn_nju_seg_atg_cpppathrec_putNodeNumber2Path(int nodeNumber, double value){
	if(nodeNumber > 0){
		int c = 0;//c is for counting the "nodeNumber"
		//此处也是为了防止循环条件节点多次经过而被加入路径中，导致路径溢出
		for(int i = 0; i < nodeindex; i ++){
			if(node[i] == nodeNumber) {
				if(++c == 2) return -1;//每个节点最多经过2次
			}
		}
		node[nodeindex] = nodeNumber;
		return ++nodeindex;
	} else {
		//为了防止循环条件语句被多次运行，而导致结果不准确，这里限定每个约束节点只获取第一次时候的值
		for(int i = 0; i < innerNodeIndex; i ++){
			if(node[i] == nodeNumber) {
				return -1;
			}
		}
		innerNode[innerNodeIndex] = nodeNumber;
		innerNodeValue[innerNodeIndex] = value;
		return ++innerNodeIndex;
	}
}


void generateExecInfo(){
	int ic = 0;
	execInfo[ic++] = '1';//succeed
	execInfo[ic++] = '\n';//split char
	char number[15];//recore the reversal number of nodes, 1234=>"4321"
	int i = 0;
	while(i < nodeindex){
		int no = node[i++];
		int ii = 0;
		while(no > 0){
			number[ii++] = (char)((no%10) + '0');
			no /= 10;
		}
		while(ii){
			execInfo[ic++] = number[--ii];
		}
		execInfo[ic++] = ',';
	}
	execInfo[ic++] = '\0';
}


/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT jstring JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_init
  (JNIEnv *env, jobject){
  	int i;
  	for(i = 0; i < MAX_SO_FILE_SIZE; i ++){
  		dpa[i] = NULL;
  	}
  	return (env)->NewStringUTF("1\nRight.");
}

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    load
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jstring JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_load
  (JNIEnv * env, jobject, jstring jcppfilename){
  	int i;
  	void*dp;
  	char filename[MAX_PATH_LENGTH];
  	strncpy( filename, env->GetStringUTFChars(jcppfilename, JNI_FALSE), MAX_PATH_LENGTH );
  	dp = dlopen(filename, RTLD_LAZY);
  	char* error = dlerror();
  	if(error){
  		char terror[100];
  		terror[0] = '0';
  		terror[1] = '\n';
  		strncpy(terror+2, error, 100-2);
  		return (env)->NewStringUTF(terror);
  	}
  	for(i = 0; i < MAX_SO_FILE_SIZE; i ++){
  		if(dpa[i]) continue;
		dpa[i] = dp;
		return (env)->NewStringUTF("1\nRight.");
  	}
  	return (env)->NewStringUTF("0\nError.");
 }

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    unload
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_unload
  (JNIEnv *, jobject, jint jindex){
  	int i = (int)jindex;
  	if(dpa[i] == NULL) return JNI_FALSE;
  	dlclose(dpa[i]);
  	dpa[i] = NULL;
  	if(dlerror()) return JNI_FALSE;
  	return JNI_TRUE;
}

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    unloadAll
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_unloadAll
  (JNIEnv *, jobject){
  	int i = 0;
  	for(; i < MAX_SO_FILE_SIZE; i ++){
  		if(dpa[i]){
  			dlclose(dpa[i]);
  			dpa[i] = NULL;
  		}
  	}
  	return JNI_TRUE;
}

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    call
 * Signature: (ILjava/lang/String;I[I[Ljava/lang/String;)Ljava/lang/String;
 */

JNIEXPORT jstring JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_call
  (JNIEnv *env, jobject, 
  	jint jindex, //dpa index
  	jstring jfuncname, //function name
  	jint jargc, //arg count
  	jintArray jargt,//arg type
  	jbyteArray jbargs, jcharArray jcargs, 
  	jintArray jiargs, jlongArray jlargs, 
  	jfloatArray jfargs, jdoubleArray jdargs,
  	jintArray jarrayEntries){
  	
  	int i;
  	int index = (int)jindex;
  	char funcname[MAX_FUNC_NAME];
	void* func;
  	int argc = (int)jargc;
  	int* argt;
  	strncpy( funcname, env->GetStringUTFChars(jfuncname, JNI_FALSE), MAX_FUNC_NAME );
  	
	func = dlsym(dpa[index], funcname);
	if(func == NULL){
		return (env)->NewStringUTF("0\nfunc is null!");
	}
	argt = (int*)malloc(sizeof(int)*argc);
	jint *nargt = env->GetIntArrayElements( jargt, JNI_FALSE );
	for(i = 0; i < argc; i ++) { argt[i] = (int)nargt[i]; }
	//calculate the total memory size of the arguments
	int floatc = 0, doublec = 0, charc = 0, shortc = 0, intc = 0, longc = 0;
	int floatarrayc = 0, doublearrayc = 0;
	for(i = 0; i < argc; i ++){ 
		if(_Float == argt[i]){
			floatc ++;
		} else if(_Double == argt[i]){
			doublec ++;
		} else if(_FloatArray == argt[i]){
			floatarrayc ++;
		} else if(_DoubleArray == argt[i]){
			doublearrayc ++;
		}
	}
	void* args = malloc( sizeof(float) * floatc + sizeof(double) * doublec
				+ sizeof(char)*charc + sizeof(short) * shortc
				+ sizeof(long int)*(intc+1) + sizeof(long long)*longc 
				+ sizeof(float*)*floatarrayc + sizeof(double*)*doublearrayc);
	//float array args, double array args
	float** faargs = NULL;
	double** daargs = NULL;
	if(floatarrayc > 0){
		faargs = (float**)malloc(sizeof(float*)*floatarrayc);
	}
	if(doublearrayc > 0){
		daargs = (double**)malloc(sizeof(double*)*doublearrayc);
	}
	//set the stack-likely memory space
	
	char* p = (char*)args;
	jfloat* fargs = NULL;
	jdouble* dargs = NULL;
	jint* arrayEntries = NULL;
	if(jfargs != NULL){
		fargs = env->GetFloatArrayElements(jfargs, JNI_FALSE);
	}
	if(jdargs != NULL){
		dargs = env->GetDoubleArrayElements(jdargs, JNI_FALSE);
	}
	if(jarrayEntries != NULL){
		arrayEntries = env->GetIntArrayElements(jarrayEntries, JNI_FALSE);
	}
	floatc = doublec = charc = shortc = intc = longc = 0;
	floatarrayc = doublearrayc = 0;
	int arrayEntriesIndex = 0;
	for(i = 0; i < argc; i ++){
		if(argt[i] == _Float){
			jfloat value = fargs[floatc++];
			*(float*)p = (jfloat)value;
			p += sizeof(float);
		} else if(argt[i] == _Double){
			jdouble value = dargs[doublec++];
			*(double*)p = (jdouble)value;
			p += sizeof(double);
		} else if(argt[i] == _FloatArray){
			int arrlen = arrayEntries[arrayEntriesIndex++];
			faargs[floatarrayc] = (float*)malloc(sizeof(float)*arrlen);
			float* temp = (float*)(faargs[floatarrayc]);
			for(int k = 0; k < arrlen; k ++){
				temp[k] = fargs[floatc++];
			}
			floatarrayc++;
			*(float**)p = temp;
			p += sizeof(float*);
		} else if(argt[i] == _DoubleArray){
			int arrlen = arrayEntries[arrayEntriesIndex++];
			daargs[doublearrayc] = (double*)malloc(sizeof(double)*arrlen);
			double* temp = (double*)(daargs[doublearrayc]);
			for(int k = 0; k < arrlen; k ++){
				temp[k] = dargs[doublec++];
			}
			doublearrayc++;
			*(double**)p = temp;
			p += sizeof(double*);
		}
	}

	//the last entry in args is for the addr of 
	//_cn_nju_seg_atg_cpppathrec_putNodeNumber2Path
	*(long int*)p = (long int)(_cn_nju_seg_atg_cpppathrec_putNodeNumber2Path);

  	innerNodeIndex = nodeindex = 0;

	//call the function
	/*
	input:
		a = arg type array
		b = arg base addr
		c = arg count
		d = function name
	output:
		void
	*/
		//*
	asm volatile(
		"pushl	%%ebp;"
		"pushl	%%edi;"
		"movl	%%esp, %%ebp;"
	"LABEL_START:"
		"cmpl	$0, %%ecx;"
		"je		LABEL_END;"
		"cmpl	$0, (%%eax);"
		"je		LABEL_FLOAT;"
		"cmpl	$1, (%%eax);"
		"je		LABEL_DOUBLE;"
		"cmpl	$6, (%%eax);"
		"jge	LABEL_ARRAY;"
	"LABEL_FLOAT:"
		"subl	$4, %%esp;"
		"flds	(%%ebx);"
		"fstps	(%%esp);"
		"addl	$4, %%ebx;"
		"jmp	LABEL_PUSH_END;"
	"LABEL_DOUBLE:"
		"subl	$8, %%esp;"
		"fldl	(%%ebx);"
		"fstpl	(%%esp);"
		"addl	$8, %%ebx;"
		"jmp	LABEL_PUSH_END;"
	"LABEL_ARRAY:"
		"subl	$4, %%esp;"
		"movl	(%%ebx), %%edi;"
		"movl	%%edi, (%%esp);"
		"addl	$4, %%ebx;"
		"jmp	LABEL_PUSH_END;"
	"LABEL_PUSH_END:"
		"decl	%%ecx;"
		"addl	$4, %%eax;"
		"jmp	LABEL_START;"
	"LABEL_END:"
		"pushl	(%%ebx);"
		"call	*%%edx;"
		"movl	%%ebp, %%esp;"
		"popl	%%edi;"
		"popl	%%ebp;"
		:
		:"a"(argt),"b"(args),"c"(argc),"d"(func)
	);
//*/
	free(argt);
	free(args);
	while(floatarrayc-- > 0){
		free(faargs[floatarrayc]);
	}
	while(doublearrayc-- > 0){
		free(daargs[doublearrayc]);
	}
	if(faargs != NULL) free(faargs);
	if(daargs != NULL) free(daargs);
	generateExecInfo();
	return (env)->NewStringUTF(execInfo);
}



/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    getInnerNodePath
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_getInnerNodePath
  (JNIEnv * env, jobject){
  		jintArray innernodes = env->NewIntArray(innerNodeIndex);
  		env->SetIntArrayRegion(innernodes, 0, innerNodeIndex, innerNode);
  		return innernodes;
  }

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    getInnerNodeValue
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_getInnerNodeValue
  (JNIEnv * env, jobject){
  		jdoubleArray innernodevalues = env->NewDoubleArray(innerNodeIndex);
  		env->SetDoubleArrayRegion(innernodevalues, 0, innerNodeIndex, innerNodeValue);
  		return innernodevalues;
  }