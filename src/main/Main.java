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

import java.awt.EventQueue;
import java.util.ArrayList;

import javax.swing.JFrame;

import starfish.collisions.CollisionsPlugin;
import starfish.core.common.Plugin;
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
		/* demo of starting Starfish with plugins */
		ArrayList<Plugin> plugins = new ArrayList<Plugin>();
		plugins.add(new CollisionsPlugin());

		/* command line settings */
		Options options = new Options(args);

		// if running from the console
		if (options.run_mode == RunMode.CONSOLE || System.console() != null) {
			new Starfish().start(options, plugins, null);
		} else {
			GUI.makeNewGUI(options, plugins);
		}
	}

	// list of various command line options
	public enum RunMode {
		CONSOLE, GUI, GUI_RUN
	};

	static public class Options implements Cloneable {

		public String wd = ""; // working directory
		public String sim_file = "starfish.xml";
		RunMode run_mode;
		public boolean randomize = true;
		public String log_level;
		public int max_cores;

		@Override 
		public Options clone() {
		   Options opt = new Options();
		   opt.wd = this.wd;
		   opt.sim_file = this.sim_file;
		   opt.run_mode = this.run_mode;
		   opt.randomize = this.randomize;
		   opt.log_level = this.log_level;
		   opt.max_cores = this.max_cores;
		   return opt;
		}
		
		public Options() { /*nothing*/}
		/*
		 * processes command line arguments -dir -sf -randomize -log_level -nr -serial
		 * -gui -nr -serial
		 * 
		 */
		public Options(String args[]) {

			// set some defaults
			if (System.console() != null)
				run_mode = RunMode.CONSOLE;
			else
				run_mode = RunMode.GUI;

			max_cores = Runtime.getRuntime().availableProcessors();

			// process arguments
			for (String arg : args) {
				// gui will set working dir and sim file so ignore those parameters
				if (arg.startsWith("-dir")) {
					wd = arg.substring(5);
					if (!wd.endsWith("/") && !wd.endsWith("\\"))
						wd += "/"; // add terminating slash if not present
					Log.log("Setting working directory to " + wd);
				} else if (arg.startsWith("-sf")) {
					sim_file = arg.substring(4);
				} else if (arg.startsWith("-randomize")) {
					String opt = arg.substring(11);
					if (opt.equalsIgnoreCase("true"))
						randomize = true;
				} else if (arg.startsWith("-log_level")) {
					log_level = arg.substring(10);
				} else if (arg.startsWith("-max_threads")) {
					String opt = arg.substring(12);
					max_cores = Integer.parseInt(opt);
				} else if (arg.startsWith("-gui")) {
					String opt = arg.substring(5);
					if (opt.equalsIgnoreCase("off"))
						run_mode = RunMode.CONSOLE;
					else if (opt.isEmpty() || opt.equalsIgnoreCase("on"))
						run_mode = RunMode.GUI;
					else if (opt.equalsIgnoreCase("run"))
						run_mode = RunMode.GUI_RUN;
				} else if (arg.startsWith("-nr")) {
					randomize = false;
				} else if (arg.startsWith("-serial")) {
					max_cores = 1;
				}

			}
		}
	}

}
