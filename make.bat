
set Path=C:\Program Files\Microsoft Visual Studio 10.0\Common7\IDE;%Path%
set INCLUDE=C:\Program Files\Microsoft Visual Studio 10.0\VC\include;C:\Program Files\Microsoft SDKs\Windows\v7.0A\Include;%INCLUDE%
set LIB=C:\Program Files\Microsoft Visual Studio 10.0\VC\lib;C:\Program Files\Microsoft SDKs\Windows\v7.0A\Lib;%LIB%
"C:\Program Files\Microsoft Visual Studio 10.0\VC\bin\cl.exe" /DLL /LD /D __WIN32__ /Fe"lib\CppManagerUtil.dll" /I".\jniheader_win32" cn_nju_seg_atg_cppmanager_CppManagerUtil.cpp
del /f cn_nju_seg_atg_cppmanager_CppManagerUtil.obj lib\CppManagerUtil.exp lib\CppManagerUtil.lib
pause