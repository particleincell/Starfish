package starfish.ui;

import starfish.core.common.Starfish;

public interface SimulationRunner {

    public Starfish getSim();

    public void start();
    public void stop();
    public void pause();

    public ConsoleOutputStream getConsole();
    public void updateProgress(double progress);
    
}
