
#gcc -shared -fpic -o CppPathRecorder.so cn_nju_seg_atg_CppPathRecorder.c

default: cn_nju_seg_atg_cppmanager_CppManagerUtil.cpp
	g++ -rdynamic -shared -o lib/CppManagerUtil.so cn_nju_seg_atg_cppmanager_CppManagerUtil.cpp -I./jniheader
	