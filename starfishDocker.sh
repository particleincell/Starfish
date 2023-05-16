#!/bin/bash

# This script will run Starfish in a Docker container and enables Starfish to use
# a GUI from within the container
# Works only on Linux OS‘s using X11

xhost +local:root # Allows programs in the docker container to display GUIs on the host machine

CONTAINER='starfish'

docker run -ti --rm \
        -e _JAVA_OPTIONS='-Dawt.useSystemAAFontSettings=lcd -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel' \
        -e DISPLAY=$DISPLAY \
        -v /tmp/.X11-unix:/tmp/.X11-unix \
        -v /usr/share/fonts:/usr/share/fonts:ro \
        --security-opt label=type:container_runtime_t \
        --network=host \
        "${CONTAINER}"