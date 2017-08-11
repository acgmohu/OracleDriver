# OracleDriver [![Build Status](https://travis-ci.org/acgmohu/OracleDriver.svg?branch=master)](https://travis-ci.org/acgmohu/OracleDriver)

Fork from https://github.com/CalebFenton/dex-oracle

It works well for reflection calls.



### How to Build

```
# build.bat
gradlew clean fatjar
```



### Convert to dex

```
# todex.bat
dx --dex --force-jumbo --output=driver.dex build/libs/OracleDriver-1.0.1.jar
```



### Usage

```
# PC - run.bat
java -classpath OracleDriver.jar org.cf.oracle.Driver <class> <method> [<parameter type>:<parameter value json>]
java -classpath OracleDriver.jar org.cf.oracle.Driver @<json file>

# Android
Usage: export CLASSPATH=/data/local/od.zip; app_process /system/bin org.cf.oracle.Driver <class> <method> [<parameter type>:<parameter value json>]
       export CLASSPATH=/data/local/od.zip; app_process /system/bin org.cf.oracle.Driver @<json file>
```



### 参数形式例子

```
class method  "java.lang.String":"[85, 122, 85, ..., 61, 61]" "java.lang.String":"[32, 80, 68, ... 56, 32, 53]"

@jsonfile
[
    {
        "className":"com.x.x",
        "id":"sha256",
        "methodName":"foo",
        "arguments":
        [
            "java.lang.String:[85, 122, 85, ..., 61, 61]",
            "java.lang.String:[32, 80, 68, ... 56, 32, 53]"
        ]
    }
]
```
