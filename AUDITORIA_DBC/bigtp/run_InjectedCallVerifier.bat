@echo off
set ASM_PATH=C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re
set CP=%ASM_PATH%\asm.jar;%ASM_PATH%\asm-tree.jar;%ASM_PATH%\asm-util.jar;%ASM_PATH%\asm-commons.jar;%ASM_PATH%\asm-analysis.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\mc-1.7.10-srg-forge.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\tools

java -cp "%CP%" InjectedCallVerifier --tputil "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\prod-classes\JinRyuu\JRMCore\TpUtil.class" --tpnbtcore "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\prod-classes\JinRyuu\JRMCore\TpNbtCore.class" --jrmcoreh "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes\JinRyuu\JRMCore\JRMCoreH.class" --csv "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\fulltp-audit.csv"