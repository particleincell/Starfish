/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;

/**
 *
 * @author Lubos Brieda
 */
public class LoggerModule extends CommandModule {

	/**
	 *
	 */
	public LoggerModule() {
		super();

		/* set default file name */
		file_name = "starfish.log";
	}

	/**
	 *
	 */
	protected PrintWriter log_file = null;

	/**
	 *
	 */
	protected String file_name;

	/**
	 *
	 */
	protected void openLogFile() {
		if (log_file != null)
			return;
		try {
			log_file = new PrintWriter(new FileWriter(Starfish.options.wd+file_name));
		} catch (IOException ex) {
			Log.error("Failed to open log file " + Starfish.options.wd+file_name);
		}

		/* save date and time */
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		log_file.println(dateFormat.format(date));
		log_file.flush();
	}

	@Override
	public void init() {
		/* do nothing */
	}

	@Override
	public void process(Element element) {
	
		String level_name = InputParser.getValue("level", element);
		setLevel(level_name);
	}
	
	public void setLevel(String level_name) {
		boolean found =false;
		for (Level level : Level.values()) {
			if (level.name().equalsIgnoreCase(level_name)) {
				logging_level = level;
				log(Level.FORCED, "Setting logging level to " + level.name());
				found = true;
				break;
			}
		}

		if (!found)
			log(Level.WARNING, "Unknown logging level " + level_name + ". Current level: " + logging_level.name());		
	}

	@Override
	public void exit() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		log_file.println(dateFormat.format(date));
		log_file.flush();
	}

	@Override
	public void start() {
	}

	/*
	 * output level in order of importance DEBUG: low-level screen and log file
	 * LOG_LOW: less important log file messages LOG: log file only MESSAGE: general
	 * screen and log output WARNING: warning message, screen and log ERROR: error
	 * message, screen (err) and log, terminates execution FORCED: like message but
	 * cannot be disabled
	 */

	/**
	 *
	 */

	public enum Level {

		DEBUG, LOG_LOW, LOG, MESSAGE, WARNING, ERROR, EXCEPTION, FORCED
	}

	/**
	 *
	 */
	protected Level logging_level = Level.LOG;

	/**
	 * outputs message to screen and/or log file
	 * 
	 * @param level
	 * @param message
	 */
	public void log(Level level, String message) {
		if (log_file == null)
			openLogFile();

		/* ignore non-error message below the current loging level */
		if (level.ordinal() < logging_level.ordinal() && level.ordinal() < Level.ERROR.ordinal()) {
			/* do nothing */
			return;
		}

		/* set message prefix */
		String prefix = "";
		if (level == Level.WARNING || level == Level.ERROR || level == Level.EXCEPTION || level == Level.DEBUG)
			prefix = level.toString() + ": ";

		String full_message = prefix + message;

		/* output to screen */
		if (level == Level.WARNING || level == Level.ERROR || level == Level.EXCEPTION)
		{
			if (Starfish.parent_gui==null)
				System.err.println(full_message);
			else
				Starfish.parent_gui.printErrorMessage(full_message);
		}
			else if (level.ordinal() > Level.LOG.ordinal())
		{
			if (Starfish.parent_gui==null)
				System.out.println(full_message);
			else
				Starfish.parent_gui.printMessage(full_message);
		}
		/* output to file */
		log_file.println(full_message);
		log_file.flush();

		/* exit if error */
		if (level == Level.ERROR)
			if (Starfish.parent_gui==null)
				System.exit(-1);
			else 
			{
				throw new RuntimeException(full_message);
			}
	}
}
