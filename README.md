#Starfish-LE

![Starfish logo](starfish.png)

[Starfish](https://www.particleincell.com/starfish) is a genearl 2D Cartesian or axisymmetric code for simulating plasmas or rarefied gases. 
It consists of two "editions", light and regular. The light edition is what is posted here.
It includes support for the Particle In Cell (PIC) method with MCC or DSMC collisions,
several gas injection sources, and preliminary support for fluid and kinetic materials. The
code can be easily extended with plugins. 

The full version implements a
Navier Stokes solver and sources specific to plasma thrusters. This version is not publicly available.


##Getting Started
First, take a look at the
[five step tutorial](https://www.particleincell.com/2012/starfish-tutorial-part1/) posted on the PIC-C site.
The input files are included in the dat/tutorial directory. There you will also find
a [PowerPoint presentation](dat/tutorial/starfish-code-overview.pdf) with some introductory remarks about the source code. The associated video is [available here](https://www.youtube.com/watch?v=IDFeT_X-IsU).

#Bug Reporting
This is an early version and is likely full of bugs. Please submit a bug report if you find anything odd!

#License
Please view LICENSE for license terms. 
(c) 2012-2017 Particle In Cell Consulting LLC

#Contact
Contact us [by visiting the website](https://www.particleincell.com/contact/) 
or on Twitter [@particleincell](https://twitter.com/particleincell).

#Revision History
v0.16 Addition of a DSMC module (previously included in the full version)
v0.15 Initial release of PIC-MCC code

