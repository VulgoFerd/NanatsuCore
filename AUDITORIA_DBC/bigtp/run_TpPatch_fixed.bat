@echo off
set ASM_PATH=C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re
set CP=%ASM_PATH%\asm.jar;%ASM_PATH%\asm-tree.jar;%ASM_PATH%\asm-util.jar;%ASM_PATH%\asm-commons.jar;%ASM_PATH%\asm-analysis.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\mc-1.7.10-srg-forge.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\tools

cd /d C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC

java -cp "%CP%" TpPatch --jrmcore "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\JRMCore-v1.3.51-original.jar" --dbc "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\DragonBlockC-v1.4.85.jar" --npcdbc "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\npcdbc-1.2.1-hotfix1.jar" --tputil "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\prod-classes\JinRyuu\JRMCore\TpUtil.class" --out "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\patched-output" --cp-file "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\classpath.txt" --optional "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\analysis\optional-deps.txt"