@echo off
echo === Command 1: java -version ===
java -version
echo.

echo === Command 2: javac -version ===
javac -version
echo.

echo === Command 3: where java ===
where java
echo.

echo === Command 4: mvn --version ===
mvn --version
echo.

if errorlevel 1 (
    echo === Command 5a: where mvn ===
    where mvn
    echo.
    
    echo === Command 5b: C:\Program Files\Apache\maven\bin\mvn --version ===
    C:\Program Files\Apache\maven\bin\mvn --version
    echo.
    
    echo === Command 5c: C:\Program Files\Maven\bin\mvn --version ===
    C:\Program Files\Maven\bin\mvn --version
)
