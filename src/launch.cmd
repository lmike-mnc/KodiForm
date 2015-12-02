::http://ss64.com/nt/for_cmd.html
::http://stackoverflow.com/questions/5615206/windows-batch-files-setting-variable-in-for-loop
@echo off
SETLOCAL ENABLEDELAYEDEXPANSION
set _cp=java -cp .\
FOR /F "tokens=*" %%J IN ('dir/b ^"*.jar^"') DO (set _cp=!_cp!;%%J)
echo !_cp!
!_cp! %1 %2