package starfish.core.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data class containing the configuration to run Starfish simulations with
 */
public class Options implements Cloneable {

    public String wd = ""; // working directory

    public String sim_file = "starfish.xml";
    public RunMode run_mode;
    public boolean randomize = true;
    public String log_level;
    public int max_cores;
    public Options() { /*nothing*/}

    /*
     * processes command line arguments -dir -sf -randomize -log_level -nr -serial
     * -gui -nr -serial
     *
     */
    public Options(String[] args) {

        // set some defaults
        if (System.console() != null)
            run_mode = RunMode.CONSOLE;
        else
            run_mode = RunMode.GUI;

        max_cores = Runtime.getRuntime().availableProcessors();


        String argPattern = "--?([a-z\\-\\_]+)=?(.*)"; // Matches -name, -name=val, --name, --name=val
        Pattern r = Pattern.compile(argPattern);

        for (String arg : args) {
            Matcher m = r.matcher(arg);
            if (!m.find()) {
                Starfish.Log.error("Unrecognized argument " + arg + ", skipped.");
                continue;
            }
            // Splits the string up into -(argName)=(argValue)
            String argName = m.group(1).toLowerCase();
            String argValue = m.group(2);

            switch (argName) {
                case "dir":
                    if (!wd.endsWith("/") && !wd.endsWith("\\"))
                        wd += "/"; // add terminating slash if not present
                        Starfish.Log.log("Setting working directory to " + wd);
                    break;
                case "sim_file":
                case "sf":
                    sim_file = argValue;
                    break;
                case "randomize":
                    randomize = argValue.equals("true");
                    break;
                case "log_level":
                    log_level = argValue;
                    break;
                case "max_threads":
                    int cores = max_cores;
                    try {
                        cores = Integer.parseInt(argValue);
                    } catch (NumberFormatException e) {
                        Starfish.Log.error(arg + ": " + argValue + " is not a valid integer! Skipped arg.");
                    }
                    max_cores = cores;
                    break;
                case "gui":
                    if (argValue.equalsIgnoreCase("off"))
                        run_mode = RunMode.CONSOLE;
                    else if (argValue.isEmpty() || argValue.equalsIgnoreCase("on"))
                        run_mode = RunMode.GUI;
                    else if (argValue.equalsIgnoreCase("run"))
                        run_mode = RunMode.GUI_RUN;
                    break;
                case "nr":
                    randomize = false;
                    break;
                case "serial":
                    max_cores = 1;
                    break;
                default:
                    System.out.println("Unrecognized arg " + arg + ", ignored.");
            }
        }
    }

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

    public enum RunMode {
        CONSOLE, GUI, GUI_RUN
    }
}
