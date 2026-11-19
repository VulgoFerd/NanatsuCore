@echo off
set ASM_PATH=C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re
set CP=%ASM_PATH%\asm.jar;%ASM_PATH%\asm-tree.jar;%ASM_PATH%\asm-util.jar;%ASM_PATH%\asm-commons.jar;%ASM_PATH%\asm-analysis.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\mc-1.7.10-srg-forge.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\JRMCore-v1.3.51-original.jar

mkdir build\test-compile 2>nul

javac -target 1.8 -source 1.8 -cp "%CP%" -d "build\test-compile" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpNbt.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpClientState.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpLogic.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpNetwork.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpNbtCore.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpTagView.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\TpUtil.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\p\JRMCorePTpSync.java" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\src\main\java\JinRyuu\JRMCore\p\JRMCorePTpSyncHandler.java"

echo Compilation done. Checking output:
dir build\test-compile\JinRyuu\JRMCore
dir build\test-compile\JinRyuu\JRMCore\p