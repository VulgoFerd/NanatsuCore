@echo off
set ASM_PATH=C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re
set CP=%ASM_PATH%\asm.jar;%ASM_PATH%\asm-tree.jar;%ASM_PATH%\asm-util.jar;%ASM_PATH%\asm-commons.jar;%ASM_PATH%\asm-analysis.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\mc-1.7.10-srg-forge.jar

mkdir C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes 2>nul
javac -target 1.8 -source 1.8 -cp "%CP%" -d "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes" "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\stubs\JinRyuu\JRMCore\JRMCoreH.java"