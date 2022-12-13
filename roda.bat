@echo off
if "%1" NEQ "" sqlite\sqlite3.exe %1.db ".read bd.sql"
java -cp lib/*; eleicoes.Run %1