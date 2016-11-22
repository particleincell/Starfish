package starfish.core.common;

/**
 * Starfish is a general 2D plasma/fluid hybrid Cartesian/axi-symmetric code
 * Copyright (c) 2012-2016, Particle In Cell Consulting LLC
 * 
 * Version 0.15 LE (academic or research light version), Development Version
 * Contact Info: info@particleincell.com
 * 
 * The most recent version can be downloaded from:
 * https://www.particleincell.com/starfish
 * 
 * This software is governed by the following license:
 * 
 *  == Simplified BSD *** MODIFIED FOR NON-COMMERCIAL USE ONLY!!! *** ==
 * Copyright (c) 2012-2016, Particle In Cell Consulting LLC
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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.BoundaryModule;
import starfish.core.diagnostics.AnimationModule;
import starfish.core.diagnostics.AveragingModule;
import starfish.core.diagnostics.ParticleTraceModule;
import starfish.core.domain.DomainModule;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.interactions.InteractionsModule;
import starfish.core.interactions.VolumeInteraction;
import starfish.core.io.InputParser;
import starfish.core.io.LoadFieldModule;
import starfish.core.io.LoggerModule;
import starfish.core.io.LoggerModule.Level;
import starfish.core.io.NoteModule;
import starfish.core.io.OutputModule;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.materials.MaterialsModule;
import starfish.core.solver.SolverModule;
import starfish.core.source.SourceModule;

public final class Starfish extends CommandModule implements UncaughtExceptionHandler
{    	
    /**simulation main loop*/
    public void MainLoop()
    {
	Log.message("Starting main loop");
	
	restart_module.load();
	
	/*compute initial field*/
	solver_module.updateFields();
	
	while(time_module.hasTime())
	{
	    /*add new particles*/
	    source_module.sampleSources();
			
	    /*update densities and velocities*/
	    materials_module.updateMaterials();
			
	    /*perform material interactions (collisions and the like)*/
	    interactions_module.performInteractions();
	    
	    /*solve potential and recompute electric field*/
	    solver_module.updateFields();

	    /*save restart data*/
	    restart_module.save();

	    /*save animations*/
	    animation_module.save();

	    /*save average data*/
	    averaging_module.sample();
	    
	    printStats();	    
	    
	    /*advance time*/
	    time_module.advance();	 
	}/*end of main loop*/
		
	/*save average data*/
	averaging_module.sample();
			
	/*check if we have reached the steady state*/
	if (!time_module.steady_state)
	    Log.warning("The simulation failed to reach steady state!");	
    }

    public void start(final String[] args, List<Plugin> plugins)
    {
	/*initialize logger*/
	logger_module = new LoggerModule();	
	modules.put("log",logger_module);
	
	/*
	SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	    GUI.createAndShowGUI(args);
	}});
	  */  
	PrintHeader();
			
	/*register modules*/
	RegisterModules();
	
	if (plugins!=null)
	    for (Plugin plugin:plugins)
		    plugin.register();	
	
	/*init modules*/
	InitModules();
	
	/*process input file*/
	ProcessInputFile("starfish.xml");
		
	/*exit modules*/
	ExitModules();
		
	/*terminate*/
	Log.message("Done!");
    }
		
    /** sequentially calls commands from the input file*/
    protected void ProcessInputFile(String file_name)
    {
	InputParser parser = new InputParser(file_name);
		
	/*process input file*/
	Iterator<Element> iterator = parser.iterator();
	while(iterator.hasNext())
	{
	    Element element = iterator.next();
			
	    /*look for the handler*/
	    String key = element.getNodeName();
	    if (modules.containsKey(key))
	    {
		Log.message("Processing <"+key+">");
		modules.get(key).process(element);
	    }
	    else
		Log.warning("Unknown command "+key);		
	}
    }

    /*module instances*/
    public static NoteModule note;
    public static DomainModule domain_module;
    public static BoundaryModule boundary_module;
    public static SourceModule source_module;
    public static SolverModule solver_module;
    public static OutputModule output_module;
    public static TimeModule time_module;
    public static MaterialsModule materials_module;
    public static InteractionsModule interactions_module;
    public static RestartModule restart_module;
    public static LoggerModule logger_module;
    public static AnimationModule animation_module;
    public static AveragingModule averaging_module;
    public static ParticleTraceModule particle_trace_module;

    /*iterable list of registered modules, using LinkedHashMap to get predictable ordering*/
    static LinkedHashMap<String,CommandModule> modules = new LinkedHashMap<String,CommandModule>();
    static HashMap<String,CommandModule> getModulesList() {return modules;}
    public static void register(String name, CommandModule module) {modules.put(name, module);}
	
    /*random number generator*/
    static Random random = new Random(0);
    static public double rnd() {return random.nextDouble();}
    static public double rnd2() {return -1.0+2*random.nextDouble();}
	
    /*code version*/
    static String VERSION = "Light v0.15  (Development)";
    static public String HEADER_MESSAGE = "General 2D Plasma / Gas Kinetic Code\n ";
    	
    /*accessors*/
    public static FieldCollection2D getFieldCollection(String name) {return domain_module.getFieldCollection(name);}
	
    public static Boundary getBoundary(String name) {return boundary_module.getBoundary(name);}
			
    public static DomainModule.DomainType getDomainType() {return domain_module.getDomainType();}
    
    public static ArrayList<Boundary> getBoundaryList() {return boundary_module.getBoundaryList();}
    public static ArrayList<Mesh> getMeshList() {return domain_module.getMeshList();}
	
    public static Material getMaterial(String name) {return materials_module.getMaterial(name);}
    public static Material getMaterial(int mat_index) {return materials_module.getMaterial(mat_index);}

    public static ArrayList<Material> getMaterialsList() {return materials_module.getMaterialsList();}
    public static ArrayList<VolumeInteraction> getInteractionsList() {return interactions_module.getInteractionsList();}

    public static double getDt() {return time_module.getDt();}
    public static int getIt() {return time_module.getIt();}
    public static boolean steady_state() {return time_module.steady_state;}

    /**returns number of available processors*/
    protected static int num_processors = -1;
    public static int getNumProcessors() {return num_processors;}
    
    /**convenience functions for logging*/
    public static class Log
    {
	static public void log (Level level, String message) {logger_module.log(level, message);}
	static public void log (String message) {logger_module.log(Level.LOG, message);}
	static public void log_low (String message) {logger_module.log(Level.LOG_LOW, message);}
	static public void message (String message) {logger_module.log(Level.MESSAGE, message);}
	static public void warning (String message) {logger_module.log(Level.WARNING, message);}
	static public void error (String message) {logger_module.log(Level.ERROR, message);}
	static public void debug (String message) {logger_module.log(Level.DEBUG, message);}
	static public void forced (String message) {logger_module.log(Level.FORCED, message);}
	static public void exception (String message) {logger_module.log(Level.EXCEPTION, message);}
    }

    /** registers simulation modules*/
    protected void RegisterModules()
    {
	/*note*/
	note = new NoteModule();
	modules.put("note", note);
		
	/*boundaries*/
	boundary_module = new BoundaryModule();
	modules.put("boundaries",boundary_module);

	/*mesh*/
	domain_module = new DomainModule();
	modules.put("domain", domain_module);
		
	/*materials*/
	materials_module = new MaterialsModule();
	modules.put("materials",materials_module);
	
	/*materials*/
	interactions_module = new InteractionsModule();
	modules.put("material_interactions",interactions_module);

	/*sources*/
	source_module = new SourceModule();
	modules.put("sources",source_module);

	/*solver*/
	solver_module = new SolverModule();
	modules.put("solver",solver_module);

	/*output*/
	output_module = new OutputModule();
	modules.put("output", output_module);

	/*time*/
	time_module = new TimeModule();
	modules.put("time", time_module);

	/*load field*/
	modules.put("load_field", new LoadFieldModule());

	/*restart*/
	restart_module = new RestartModule();
	modules.put("restart", restart_module);

	/*stop*/
	modules.put("stop", new StopModule());

	/*also register self*/
	modules.put("starfish", this);

	/*particle tracing*/
	particle_trace_module = new ParticleTraceModule();
	modules.put("particle_trace", particle_trace_module);

	/*animation*/
	animation_module = new AnimationModule();
	modules.put("animation", animation_module);

	/*averaging*/
	averaging_module = new AveragingModule();
	modules.put("averaging", averaging_module);
    }
	
    /** calls exit subroutines*/
    protected void InitModules()
    {
	Iterator<String> iter = modules.keySet().iterator();
	while(iter.hasNext())
	{
	    String key = iter.next();
			
	    Log.log_low("Initializing "+key);
	    modules.get(key).init();
	}
    }

    /** calls exit subroutines*/
    protected void StartModules()
    {
	Iterator<String> iter = modules.keySet().iterator();
	while(iter.hasNext())
	{
	    String key = iter.next();
		
	    Log.log_low("Starting "+key);
	    modules.get(key).start();
	}
    }

    /** calls exit subroutines*/
    protected void FinishModules()
    {
	Iterator<String> iter = modules.keySet().iterator();
	while(iter.hasNext())
	{
	    String key = iter.next();
    		
	    Log.log_low("Finishing "+key);
	    modules.get(key).finish();
	}
    }

    /** calls exit subroutines*/
    protected void ExitModules()
    {
	Iterator<String> iter = modules.keySet().iterator();
	while(iter.hasNext())
	{
	    String key = iter.next();
			
	    Log.log_low("Exiting "+key);
	    modules.get(key).exit();
	}
    }
	    
    @Override
    public void init()
    {
	/*do nothing*/
    }

    /**Process for <starfish> command*/
    @Override
    public void process(Element element) 
    {
	/*check for parameters*/
	if (InputParser.getBoolean("randomize", element,false))
	    random = new Random();  /*without the seed, will randomize*/
	
	/*read number of processors*/
	num_processors = InputParser.getInt("max_processors", element,Runtime.getRuntime().availableProcessors()-1);
	
	StartModules();
	MainLoop();
	FinishModules();
    }

    @Override
    public void exit() 
    {
	/*do nothing*/
    }

    public void printStats()
    {
	String msg = String.format("it: %d\t",getIt());
		
	for (Material mat:getMaterialsList())
	{
	    if (mat instanceof KineticMaterial)
	    {
		KineticMaterial km = (KineticMaterial)mat;
		msg += String.format("%s: %d ", km.getName(),km.getNp());
	    }
	}
	
	Log.message(msg);
    }
	
    /**outputs code header*/
    public void PrintHeader()
    {
	Log.forced("================================================================");
	Log.forced("> Starfish "+VERSION);
	Log.forced("> "+HEADER_MESSAGE);
	Log.forced("> (c) 2012-2016, Particle In Cell Consulting LLC");
	Log.forced("> info@particleincell.com, www.particleincell.com");
	Log.forced("");
	Log.forced("!! This is a development version. The software is provided as-is, \n"
		+ "!! with no implied or expressed warranties. Report bugs to \n"
		+ "!! bugs@particleincell.com");
	Log.forced("=================================================================");	
    }

    /**exception handler, logs message and stack trace*/
    @Override
    public void uncaughtException(Thread t, Throwable e) 
    {
	//e.printStackTrace(System.err);
	
	String message = (e.getMessage()!=null)?e.getMessage():e.toString();
		
	message = e.getClass().getName()+": "+message;
	
	StackTraceElement st[] = e.getStackTrace();
		
	for (int i=0;i<st.length;i++)
	    message += "\n"+i+": "+st[i].getLineNumber()+":"+st[i].getMethodName()+":"+st[i].getClassName()+":"+st[i].getFileName();
		
	Log.exception(message);
	System.exit(-1);
    }

    @Override
    public void start() 
    {
    }
	
    public static Field2D getField(Mesh mesh, String var_name) {return domain_module.getField(mesh, var_name);}
}
