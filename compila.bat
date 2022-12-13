rem cd "\batimentoEleicoes\leitorJava"
javac -d . Leitor.java entidades.java Impressor.java Run.java -cp lib/*
rem if %ERRORLEVEL% NEQ 0 pause
