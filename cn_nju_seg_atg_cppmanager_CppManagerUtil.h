/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class cn_nju_seg_atg_cppmanager_CppManagerUtil */

#ifndef _Included_cn_nju_seg_atg_cppmanager_CppManagerUtil
#define _Included_cn_nju_seg_atg_cppmanager_CppManagerUtil
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT jstring JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_init
  (JNIEnv *, jobject);

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    load
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jstring JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_load
  (JNIEnv *, jobject, jstring);

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    unload
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_unload
  (JNIEnv *, jobject, jint);

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    unloadAll
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_unloadAll
  (JNIEnv *, jobject);

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    call
 * Signature: (ILjava/lang/String;I[I[Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_call
  (JNIEnv *, jobject, jint, jstring, jint, jintArray, jbyteArray, jcharArray, 
  	jintArray, jlongArray, jfloatArray, jdoubleArray);

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    getInnerNodePath
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_getInnerNodePath
  (JNIEnv *, jobject);

/*
 * Class:     cn_nju_seg_atg_cppmanager_CppManagerUtil
 * Method:    getInnerNodeValue
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_cn_nju_seg_atg_cppmanager_CppManagerUtil_getInnerNodeValue
  (JNIEnv *, jobject);


#ifdef __cplusplus
}
#endif
#endif
