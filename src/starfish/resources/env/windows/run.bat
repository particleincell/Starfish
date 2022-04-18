:: A path is required to be in the PATH. By launching Starfish using this bat file, you don't need to add a permanent
:: path variable. If you want to launch the Starfish Jar directly with gui enabled, you must add the "bin" folder created
:: by Starfish in this folder to the PATH
set PATH=%PATH%%CD%\bin
java -jar Starfish.jar -gui=on