FROM openjdk:16-jdk-bullseye

# Download the VTK Library
WORKDIR /tmp
RUN curl https://vtk.org/files/release/9.2/VTK-9.2.2.tar.gz --output ./vtk.tar.gz
RUN tar -xf ./vtk.tar.gz --transform s/VTK-9.2.2/VTK-source/

# Install build tools and libraries
RUN apt update
RUN apt install -y build-essential cmake
RUN apt install -y libglu1-mesa-dev freeglut3-dev mesa-common-dev
RUN cmake -S ./VTK-source -B ./VTK-build
# Configure cmake to build the Java library
RUN sed -i 's/VTK_WRAP_JAVA:BOOL=OFF/VTK_WRAP_JAVA:BOOL=ON/g' ./VTK-build/CMakeCache.txt
RUN cmake -S ./VTK-source -B ./VTK-build
RUN sed -i 's/VTK_JAVA_SOURCE_VERSION:STRING=<DEFAULT>/VTK_JAVA_SOURCE_VERSION:STRING=16/g' ./VTK-build/CMakeCache.txt
RUN sed -i 's/VTK_JAVA_TARGET_VERSION:STRING=<DEFAULT>/VTK_JAVA_TARGET_VERSION:STRING=16/g' ./VTK-build/CMakeCache.txt
# This takes a while
# Modify -jN to the N of cores you want to use for the build process. More is better.
RUN cmake --build ./VTK-build -j12

RUN mv ./VTK-build/lib/java/ /vtk/ && mv ./VTK-build/lib/* /vtk/vtk-Linux-x86_64
# Cleanup
RUN rm -r ./VTK-build && rm -r ./VTK-source && rm -r ./vtk.tar.gz

# These packages are needed to download some libraries for the GUI
RUN apt update
RUN apt install -y x11-utils
# Needed to create GLX context (?)
RUN apt install -y mesa-utils
# It seems the only way to get the AWT binaries is to download the whole JRE package
RUN apt install -y openjdk-17-jre

ENV PATH="$PATH:/vtk:/vtk/vtk-Linux-x86_64"

WORKDIR /starfish
COPY . .

# Build and Run the project as a JAR file
RUN javac -cp /vtk/*.jar -d bin/ -sourcepath src/ src/**/*.java
#RUN jar cfm starfish.jar bin/META-INF/MANIFEST.MF -classpath /vtk/vtk.jar -C bin/ .
#CMD java -Djava.library.path="/vtk/vtk-Linux-x86_64" -jar starfish.jar
CMD java -Djava.library.path=/vtk/vtk-Linux-x86_64/ -classpath bin:/vtk/vtk.jar starfish.Main -gui=on