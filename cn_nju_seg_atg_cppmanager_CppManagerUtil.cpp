

#include "stdlib.h"
#include "string.h"

#ifdef __WIN32__
#include <windows.h>
#else
#include "dlfcn.h"
#endif

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
	_Int8 = 2, 
	_Int16 = 3,
	_Int32 = 4, 
	_Int64 = 5,
	_FloatArray = 6,
	_DoubleArray = 7,
	_Unknown = 8
};

//shared-object pointer
#ifdef __WIN32__
HINSTANCE dpa[MAX_SO_FILE_SIZE];
#else
void* dpa[MAX_SO_FILE_SIZE];
#endif


//the max number of node each path
#define MAX_NR_NODE			500
#define MAX_EXEC_INFO_SIZE	1024*100

int node[MAX_NR_NODE];
int nodeindex = 0;
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
  	char filename[MAX_PATH_LENGTH];
  	char* error = NULL;
  	strncpy( filename, env->GetStringUTFChars(jcppfilename, JNI_FALSE), MAX_PATH_LENGTH );
#ifdef __WIN32__
	HINSTANCE dp = LoadLibrary(filename);
	if(dp == NULL) error="Error at Win32.LoadLibrary.";
#else
  	void* dp = dlopen(filename, RTLD_LAZY);
  	error = dlerror();
#endif
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
#ifdef __WIN32__
	FreeLibrary(dpa[i]);
#else
  	dlclose(dpa[i]);
#endif
  	dpa[i] = NULL;
  	if(
#ifdef __WIN32__
		GetLastError()
#else
		dlerror()
#endif
	) return JNI_FALSE;
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
#ifdef __WIN32__
			FreeLibrary(dpa[i]);
#else
  			dlclose(dpa[i]);
#endif
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
  	int argc = (int)jargc;
  	int* argt;
  	strncpy( funcname, env->GetStringUTFChars(jfuncname, JNI_FALSE), MAX_FUNC_NAME );

#ifdef __WIN32__
	void* func = GetProcAddress(dpa[index], funcname);
#else
	void* func = dlsym(dpa[index], funcname);
#endif
	if(func == NULL){
		return (env)->NewStringUTF("0\nfunc is null!");
	}
	argt = (int*)malloc(sizeof(int)*argc);
	jint *nargt = env->GetIntArrayElements( jargt, JNI_FALSE );
	for(i = 0; i < argc; i ++) { argt[i] = (int)nargt[i]; }
	//calculate the total memory size of the arguments
	int floatc = 0, doublec = 0, int8c = 0, int16c = 0, int32c = 0, int64c = 0, unknownc = 0;
	int floatarrayc = 0, doublearrayc = 0;
	for(i = 0; i < argc; i ++){ 
		if(_Float == argt[i]){
			floatc ++;
		} else if(_Double == argt[i]){
			doublec ++;
		} else if(_Int8 == argt[i]){
			int8c ++;
		} else if(_Int16 == argt[i]){
			int16c ++;
		} else if(_Int32 == argt[i]){
			int32c ++;
		} else if(_Int64 == argt[i]){
			int64c ++;
		} else if(_FloatArray == argt[i]){
			floatarrayc ++;
		} else if(_DoubleArray == argt[i]){
			doublearrayc ++;
		} else if(_Unknown == argt[i]){
			unknownc ++;
		}
	}
	void* args = malloc( sizeof(float) * floatc + sizeof(double) * doublec
				+ sizeof(long int)*int8c + sizeof(long int) * int16c
				+ sizeof(long int)*(int32c) + sizeof(long long int)*int64c 
				+ sizeof(float*)*floatarrayc + sizeof(double*)*doublearrayc
				+ sizeof(long int)*(unknownc)
				+ sizeof(long int)//for the function pointer
				);
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
	jbyte* int8args = NULL;
	jchar* int16args = NULL;
	jint*  int32args = NULL;
	jlong* int64args = NULL;
	jfloat* fargs = NULL;
	jdouble* dargs = NULL;
	jint* arrayEntries = NULL;
	if(jbargs != NULL){
		int8args = env->GetByteArrayElements(jbargs, JNI_FALSE);
	}
	if(jcargs != NULL){
		int16args = env->GetCharArrayElements(jcargs, JNI_FALSE);
	}
	if(jiargs != NULL){
		int32args = env->GetIntArrayElements(jiargs, JNI_FALSE);
	}
	if(jlargs != NULL){
		int64args = env->GetLongArrayElements(jlargs, JNI_FALSE);
	}
	if(jfargs != NULL){
		fargs = env->GetFloatArrayElements(jfargs, JNI_FALSE);
	}
	if(jdargs != NULL){
		dargs = env->GetDoubleArrayElements(jdargs, JNI_FALSE);
	}
	if(jarrayEntries != NULL){
		arrayEntries = env->GetIntArrayElements(jarrayEntries, JNI_FALSE);
	}
	floatc = doublec = int8c = int16c = int32c = int64c = 0;
	floatarrayc = doublearrayc = 0;
	int arrayEntriesIndex = 0;
	for(i = 0; i < argc; i ++){
		if(argt[i] == _Float){
			*(float*)p = (jfloat)fargs[floatc++];
			p += sizeof(float);
		} else if(argt[i] == _Double){
			*(double*)p = (jdouble)dargs[doublec++];
			p += sizeof(double);
		} else if(argt[i] == _Int8){
			*(long int*)p = int8args[int8c++];
			p += sizeof(long int);
		} else if(argt[i] == _Int16){
			*(long int*)p = int16args[int16c++];
			p += sizeof(long int);
		} else if(argt[i] == _Int32){
			*(long int*)p = int32args[int32c++];
			p += sizeof(long int);
		} else if(argt[i] == _Int64){
			*(long long int*)p = int64args[int64c++];
			p += sizeof(long long int);
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
		} else if(argt[i] == _Unknown){
			*(void**)p = NULL;
			p += sizeof(void*);
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
#ifdef __WIN32__
	__asm {
		pushad;
		xor		eax, eax;
		xor		ebx, ebx;
		xor		ecx, ecx;
		xor		edx, edx;
		mov		eax, argt;
		mov		ebx, args;
		mov		ecx, argc;
		mov		edx, func;
		push	ebp;
		mov		ebp, esp;
	LABEL_START:
		cmp		ecx, 0;
		je		LABEL_END;
		cmp		dword ptr [eax], 0;
		je		LABEL_FLOAT;
		cmp		dword ptr [eax], 1;
		je		LABEL_DOUBLE;
		cmp		dword ptr [eax], 4;
		jle		LABEL_INT8_INT16_INT32_ARRAY;
		cmp		dword ptr [eax], 5;
		je		LABEL_INT64;
		cmp		dword ptr [eax], 6;
		jge		LABEL_INT8_INT16_INT32_ARRAY;
	LABEL_FLOAT:
		sub		esp, 4;
		fld		dword ptr [ebx];
		fstp	dword ptr [esp];
		add		ebx, 4;
		jmp		LABEL_PUSH_END;
	LABEL_DOUBLE:
		sub		esp, 8;
		fld		qword ptr [ebx];
		fstp	qword ptr [esp];
		add		ebx, 8;
		jmp		LABEL_PUSH_END;
	LABEL_INT64:
		sub		esp, 8;
		mov		edi, dword ptr [ebx];
		mov		dword ptr [esp], edi;
		mov		edi, dword ptr [ebx+4];
		mov		dword ptr [esp+4], edi;
		add		ebx, 8;
		jmp		LABEL_PUSH_END;
	LABEL_INT8_INT16_INT32_ARRAY:
		sub		esp, 4;
		mov		edi, dword ptr [ebx];
		mov		dword ptr [esp], edi;
		add		ebx, 4;
		jmp		LABEL_PUSH_END;
	LABEL_PUSH_END:
		dec		ecx;
		add		eax, 4;
		jmp		LABEL_START;
	LABEL_END:
		push	dword ptr [ebx];
		call	edx;
		mov		esp, ebp;
		pop		ebp;
		popad;
	}
#else
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
		"cmpl	$4, (%%eax);"
		"jle	LABEL_INT8_INT16_INT32_ARRAY;"
		"cmpl	$5, (%%eax);"
		"je		LABEL_INT64;"
		"cmpl	$6, (%%eax);"
		"jge	LABEL_INT8_INT16_INT32_ARRAY;"
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
	"LABEL_INT64:"
		"subl	$8, %%esp;"
		"movl	(%%ebx), %%edi;"
		"movl	%%edi, (%%esp);"
		"movl	4(%%ebx), %%edi;"
		"movl	%%edi, 4(%%esp);"
		"addl	$8, %%ebx;"
		"jmp	LABEL_PUSH_END;"
	"LABEL_INT8_INT16_INT32_ARRAY:"
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
#endif
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
  		env->SetIntArrayRegion(innernodes, 0, innerNodeIndex, (const jint*)innerNode);
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