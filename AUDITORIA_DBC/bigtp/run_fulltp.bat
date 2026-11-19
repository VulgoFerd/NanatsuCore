@echo off
set ASM_PATH=C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re
set CP=%ASM_PATH%\asm.jar;%ASM_PATH%\asm-tree.jar;%ASM_PATH%\asm-util.jar;%ASM_PATH%\asm-commons.jar;%ASM_PATH%\asm-analysis.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\mc-1.7.10-srg-forge.jar
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\stub-classes
set CP=%CP%;C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\tools

java -cp "%CP%" FullTpAudit ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\dist\1.1.1\jars\JRMCore-patched.jar" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\dist\1.1.1\jars\DBC-patched.jar" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\dist\1.1.1\jars\NPCDBC-patched.jar" ^
    "C:\Users\Fernando\Desktop\NanatsuCore\AUDITORIA_DBC\bigtp\build\fulltp-audit.csv" 2>&1