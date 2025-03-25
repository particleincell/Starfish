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

package starfish;

import starfish.collisions.CollisionsPlugin;
import starfish.core.common.Options;
import starfish.core.common.Plugin;
import starfish.core.common.SimStatus;
import starfish.core.common.Starfish;
import starfish.plugins.plasma_dynamics.PlasmaDynamicsPlugin;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Exclude gui from files from the compilation and use this file as the main class to create a version of Starfish
 * that isn't dependent on any libraries
 */
public class MainHeadless {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));	 //force the code to use dots for decimals

        // demo of starting Starfish with plugins 
        ArrayList<Plugin> plugins = new ArrayList<Plugin>();
        plugins.add(new CollisionsPlugin());

        // command line settings
        Options options = new Options(args);

        run(options, plugins);
    }

    public static void run(Options options, ArrayList<Plugin> plugins) {
        Starfish sim = new Starfish();
    /*    Thread terminalControlThread = new Thread(() -> {
            if (System.console() == null) {
                terminalControlsNoConsole(sim);
            } else {
                terminalControlsWithConsole(sim);
            }
        });
        terminalControlThread.start();*/
        sim.start(options, plugins, null);
       //terminalControlThread.interrupt();
    }
    private static void terminalControlsNoConsole(Starfish sim) {
        try {
            while (sim.getStatus() != SimStatus.STOP) {
                int input = System.in.read();
                onInput(sim, input);
                System.out.println("Sim " + sim.getStatus());
            }
        } catch (IOException e) {

        }
    }
    
    /**
     * This listens for the user pressing specific keys while the simulation is running
     * so the user can control the simulation through the CLI.
     * See inInput() for implemented keybindings
     */
    private static void terminalControlsWithConsole(Starfish sim) {
        Console console = System.console();
        while (sim.getStatus() != SimStatus.STOP) {
            char[] rawInput = console.readPassword();
            // rawInput includes \n char at end, so non-empty input has 2 or more chars
            char input = rawInput.length > 1 ? Character.toLowerCase(rawInput[0]) : '-';
            onInput(sim, input);
            System.out.println("Sim " + sim.getStatus());
        }
    }
    private static void onInput(Starfish sim, int input) {
        if (input == 'r') {
            sim.setStatus(SimStatus.RUNNING);
        } else if (input == 'p') {
            sim.setStatus(SimStatus.PAUSED);
        } else if (input == 's') {
            sim.setStatus(SimStatus.STOP);
        }
        //System.err.println("est" + input);
    }

}
