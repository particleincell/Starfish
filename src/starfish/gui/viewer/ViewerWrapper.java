package starfish.gui.viewer;

import vtk.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

/**
 * Allows you to hide/unhide the actual vtkPanel
 */
class ViewerWrapper extends JPanel {

    private boolean showingPanel;

    private vtkPanel vtkPanel;
    private SimulationResultViewerSettings settings;
    private JSplitPane splitPane;
    private JPanel placeholder;

    public ViewerWrapper() {
        super(new BorderLayout());
        createPlaceholder();
        add(placeholder, BorderLayout.CENTER);
        showingPanel = false;
    }
    private void createPlaceholder() {
        placeholder = new JPanel(new BorderLayout());
        try {
            Image starfishImage = ImageIO.read(new File("starfish.png"));
            placeholder.add(new JLabel(new ImageIcon(starfishImage)), BorderLayout.CENTER);
        } catch (IOException e) {
            placeholder.add(new JLabel("Load file or start simulation to view"), BorderLayout.CENTER);
        }
    }
    private void createViewer() {
        if (vtkPanel == null) {
            createVTKPanel();
        }
        if (settings == null) {
            createSettings();
        }
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vtkPanel, settings);
        splitPane.setResizeWeight(1); // vtkPanel grows/shrinks when this is resized
    }
    private void createVTKPanel() {
        vtkPanel = new vtkPanel();
        // VTK Panel can't shrink unless you manually set the minimum size
        vtkPanel.setMinimumSize(new Dimension(0, 0));

        // Disable rotation, but doesn't work :(
        // https://stackoverflow.com/questions/50917165/vtk-disable-rotation-python
        // https://public.kitware.com/pipermail/vtkusers/2010-November/064111.html
        vtkRenderWindow renderWindow = new vtkRenderWindow();
        vtkRenderWindowInteractor interactor = new vtkRenderWindowInteractor();
        interactor.SetRenderWindow(renderWindow);
        renderWindow.SetInteractor(interactor);
        vtkInteractorStyleImage style = new vtkInteractorStyleImage();
        interactor.SetInteractorStyle(style);
        style.SetInteractor(interactor);

        vtkPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                vtkPanel.repaint();
            }
        });
    }
    private void createSettings() {
        settings = new SimulationResultViewerSettings();
    }

    public void showVTKPanel() {
        if (splitPane == null) {
            createViewer();
        }
        remove(placeholder);
        add(splitPane, BorderLayout.CENTER);
        revalidate(); // Need this or else the layout is initially messed up
        showingPanel = true;
        repaint();
    }
    public void hideVTKPanel() {
        remove(splitPane);
        add(placeholder, BorderLayout.CENTER);
        showingPanel = false;
        repaint();
    }

    public vtkPanel getVtkPanel() {
        if (vtkPanel == null) {
            createVTKPanel();
        }
        return vtkPanel;
    }
    public SimulationResultViewerSettings getViewerSettings() {
        if (settings == null) {
            createSettings();
        }
        return settings;
    }

    public boolean isShowingVTKPanel() {
        return showingPanel;
    }

}
