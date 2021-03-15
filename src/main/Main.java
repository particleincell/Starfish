/** Starfish is a general 2D plasma/fluid hybrid Cartesian/axi-symmetric code
 * 
 * Contact Info: info@particleincell.com
 * 
 * The most recent version can be downloaded from:
 * https://www.particleincell.com/starfish
 * 
 * This software is governed by the following license:
 * 
 *  == Simplified BSD *** MODIFIED FOR NON-COMMERCIAL USE ONLY!!! *** ==
 * Copyright (c) 2012-2019, Particle In Cell Consulting LLC
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3. Any redistribution, use, or modification is done solely for academic, government,
 *    or personal benefit, and not for any commercial purpose or for monetary gain. 
 *    This version, or any derivation based upon it, may not be sold without a 
 *    prior approval of the copyright holder.
 */

package main;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

import starfish.collisions.CollisionsPlugin;
import starfish.core.common.Options;
import starfish.core.common.Plugin;
import starfish.core.common.SimStatus;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.gui.GUI;

/** wrapper to launch starfish with no plugin */
public class Main {

	/**
	 * Execution entry point. Creates a new instance of Starfish class and calls its
	 * start function with a list of optional plugins.
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String args[]) {
		Locale.setDefault(new Locale("en", "US"));	 //force the code to use dots for decimals

		/* demo of starting Starfish with plugins */
		ArrayList<Plugin> plugins = new ArrayList<Plugin>();
		plugins.add(new CollisionsPlugin());

		/* command line settings */
		Options options = new Options(args);

		// if running from the console
		if (options.run_mode == Options.RunMode.CONSOLE && System.out != null) {
			MainHeadless.run(options, plugins);
		} else {
			GUI.makeNewGUI(options, plugins);
		}
	}

}
