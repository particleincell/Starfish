[![GitHub forks](https://img.shields.io/github/forks/particleincell/Starfish-LE.svg)](https://github.com/particleincell/Starfish-LE/network)
[![Twitter Follow](https://img.shields.io/twitter/follow/espadrine.svg?style=social&label=Follow)](https://twitter.com/particleincell)

# Starfish-LE
![Starfish logo](starfish.png)

[Starfish](https://www.particleincell.com/starfish) is a 2D (Cartesian or axisymmetric) code for simulating a wide range of plasma and gas problems.
It implements the electrostatic Particle-in-Cell (ES-PIC) method along with several fluid solvers. Material interactions are included through
MCC or DSMC collisions, or via chemical reactions. The computational domain can be divided into multiple rectilinear or body-fitted meshes, and linear/cubic
splines represent the surface geometry. The code can be easily extended with plugins. Starfish is written in Java and is actively being developed.

## Getting Started
Start by taking a look at a five-step 
[ES-PIC](https://www.particleincell.com/2012/starfish-tutorial-part1/) and [DSMC](https://www.particleincell.com/2017/starfish-tutorial-dsmc/) tutorials.
Input files for the tutorials are included in the dat directory. On the PIC-C website you will also find
a [PowerPoint presentation](dat/tutorial/starfish-code-overview.pdf) with some introductory remarks about the source code. The associated video is [available here](https://www.youtube.com/watch?v=IDFeT_X-IsU).

# Examples
Input files for these examples are located in the dat/ folder.

![ion velocity](doc/plots/ion-vel.png)  
Axial velocity of ions streaming past a charged sphere

![temperature profile](doc/plots/dsmc-t.png)  
DSMC computation of temperature in an atmospheric jet expanding to a low pressure tank

![ion density](doc/plots/tube.png)  
Number density of ions flowing through a sectioned tube computed on a domain consisting of multiple meshes

# Bug Reporting
The code is under ongoing development and may contain numerous bugs. Please submit a bug report if you find anything odd!

# License
Please view LICENSE for license terms. 

(c) 2012-2018 Particle In Cell Consulting LLC

# Contact
Contact us [by visiting the website](https://www.particleincell.com/contact/) 
or on Twitter [@particleincell](https://twitter.com/particleincell).

# Revision History
- v0.19 Re-enables support for multi-domain simulations
- v0.16.2 Various bug fixes related to DSMC and ambient boundary source
- v0.16 Addition of a DSMC module (previously included in the full version)
- v0.15 Initial release of PIC-MCC code

