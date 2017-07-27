gradlew.bat clean fatjar
dx --dex --force-jumbo --output=driver.dex build/libs/OracleDriver-1.0.0.jar
rm -r build