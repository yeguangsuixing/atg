
#gcc -shared -fpic -o CppPathRecorder.so cn_nju_seg_atg_CppPathRecorder.c

default: cn_nju_seg_atg_cppmanager_CppManagerUtil.c cn_nju_seg_atg_CppPathRecorder.c
	g++ -rdynamic -shared -o CppManagerUtil.so cn_nju_seg_atg_cppmanager_CppManagerUtil.c -I./jniheader
	