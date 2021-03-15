package starfish.core.common;

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

        // process arguments
        for (String arg : args) {
            // gui will set working dir and sim file so ignore those parameters
            if (arg.startsWith("-dir")) {
                wd = arg.substring(5);
                if (!wd.endsWith("/") && !wd.endsWith("\\"))
                    wd += "/"; // add terminating slash if not present
                Starfish.Log.log("Setting working directory to " + wd);
            } else if (arg.startsWith("-sf")) {
                sim_file = arg.substring(4);
            } else if (arg.startsWith("-randomize")) {
                String opt = arg.substring(11);
                if (opt.equalsIgnoreCase("true"))
                    randomize = true;
            } else if (arg.startsWith("-log_level")) {
                log_level = arg.substring(10);
            } else if (arg.startsWith("-max_threads")) {
                String opt = arg.substring(13);
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
