# OracleDriver [![Build Status](https://travis-ci.org/acgmohu/OracleDriver.svg?branch=master)](https://travis-ci.org/acgmohu/OracleDriver)

Fork from https://github.com/CalebFenton/dex-oracle

It works well for reflection calls.

### How to Build

```
gradlew clean fatjar
```



### Convert to dex

```
dx --dex --force-jumbo --output=driver.dex build/libs/OracleDriver-1.0.1.jar
```

