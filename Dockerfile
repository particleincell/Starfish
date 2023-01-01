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

RUN mv ./VTK-build/lib/java/ /vtk/
# Cleanup
RUN rm -r ./VTK-build && rm -r ./VTK-source && rm -r ./vtk.tar.gz