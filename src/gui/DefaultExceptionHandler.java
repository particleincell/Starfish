package starfish.core.gui;

import javax.swing.*;
import java.awt.*;
// did you know that you could import inner classes?
import java.lang.Thread.*;

/**
 *
 * @author Lubos Brieda
 */
public class DefaultExceptionHandler implements UncaughtExceptionHandler {
  public void uncaughtException(Thread t, Throwable e) {
    // Here you should have a more robust, permanent record of problems
    JOptionPane.showMessageDialog(findActiveFrame(),
        e.toString(), "Exception Occurred", JOptionPane.OK_OPTION);
    e.printStackTrace();
  }
  private Frame findActiveFrame() {
    Frame[] frames = JFrame.getFrames();
    for (int i = 0; i < frames.length; i++) {
      if (frames[i].isVisible()) {
        return frames[i];
      }
    }
    return null;
  }
}