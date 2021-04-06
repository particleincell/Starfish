package starfish.gui.viewer;

import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;
import starfish.core.io.OutputModule;
import starfish.core.io.VTKWriter;
import starfish.gui.runner.SimulationRunner;
import vtk.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimulationResultViewer extends JPanel {

    private SimulationRunner simRunner;

    private File vtkTempFile;
    private vtkXMLStructuredGridReader reader;
    private VTKWriter vtkWriter;

    private final ViewerWrapper main; // Pane that contains the vtkPanel and settings panel

    public SimulationResultViewer() {
        this(null);
    }

    public SimulationResultViewer(SimulationRunner simRunner) {
        super(new BorderLayout());
        this.simRunner = simRunner;

        createToolbar();

        main = new ViewerWrapper();

        main.getViewerSettings().setOnFormUpdate(arg0 -> updateVTKPanel());

        add(main, BorderLayout.CENTER);
    }
    private void createToolbar() {
        JButton loadFile = new JButton("Load file");
        loadFile.addActionListener(arg0 -> loadUserSelectedFile());

        JButton refreshVtkViewer = new JButton("Show current sim");
        refreshVtkViewer.addActionListener(arg0 -> loadCurrentlyRunningSimulation());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(loadFile);
        toolBar.add(refreshVtkViewer);

        add(toolBar, BorderLayout.NORTH);
    }

    private vtkActor previousActor = null;
    private static vtkScalarBarActor scalarBarActor = new vtkScalarBarActor();
    /**
     * Updates the VTK preview to display the currently loaded file using the settings set by the user in the
     * settings panel
     */
    private void updateVTKPanel() {
        if (fileIsLoaded() && main.getVtkPanel() != null) {

            String selectedVar = main.getViewerSettings().getSelectedVar();

            vtkStructuredGrid structuredGrid = reader.GetOutput();
            structuredGrid.GetPointData().SetActiveScalars(selectedVar);

            vtkDataSetMapper mapper = new vtkDataSetMapper();
            mapper.SetInputData(structuredGrid);
            mapper.SetScalarVisibility(1);
            mapper.SetScalarModeToUsePointData();
            mapper.SetColorModeToMapScalars();

            vtkLookupTable lookupTable = main.getViewerSettings().getLookupTable(selectedVar);

            mapper.SetLookupTable(lookupTable);
            mapper.SetUseLookupTableScalarRange(1);

            scalarBarActor.SetLookupTable(lookupTable);
            scalarBarActor.SetNumberOfLabels(4);

            vtkActor actor = new vtkActor();
            if (previousActor != null) {
                previousActor.Delete();
            }
            previousActor = actor;

            actor.SetMapper(mapper);

            vtkPanel vtkPanel = main.getVtkPanel();
            vtkPanel.GetRenderer().SetBackground(.5, .5, .5);
            vtkPanel.GetRenderer().RemoveAllViewProps();
            vtkPanel.GetRenderer().AddActor(actor);
            vtkPanel.GetRenderer().AddActor2D(scalarBarActor);
            vtkPanel.repaint();
        }
    }


    /**
     * Prompts the user to select a .vts file, and then shows it in the preview
     */
    public void loadUserSelectedFile() {
        File selectedFile = promptForVTKFile();
        if (selectedFile != null) {
            loadAndUpdateEverything(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Shows the state of the current simulation
     */
    public void loadCurrentlyRunningSimulation() {
        if (simRunner.getSim() != null && Starfish.getMeshList().size() > 0) {
            createVTKWriter();
            Mesh mesh = Starfish.getMeshList().get(Starfish.getMeshList().size() - 1);
            vtkWriter.write2DToFile(mesh, vtkTempFile.getAbsolutePath(), VTKWriter.VTK_Type.STRUCT);
            loadAndUpdateEverything(vtkTempFile.getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(this, "Start simulation to see preview");
        }
    }

    /**
     * Rewrites {@code vtkWriter} as a new vtkWriter to a new temp file
     */
    private void createVTKWriter() {
        try {
            vtkTempFile = File.createTempFile("StarfishViewer", ".vts");
            vtkTempFile.deleteOnExit();
            vtkWriter = new VTKWriter(vtkTempFile.getParent(), VTKWriter.OutputFormat.ASCII);

            String[] scalars = OutputModule.getAllScalars().toArray(new String[0]);
            List<String[]> vectors = new ArrayList<>(OutputModule.getAllVectors());
            String[] cellData = OutputModule.getAllCellData().toArray(new String[0]);
            vtkWriter.init2D(scalars, vectors, cellData, null);
        } catch (IOException e) {

        }
    }

    /**
     * Loads the file into {@code reader}, then updates the {@code vtkPanel}, updates the variables in the menu, etc.
     * @param vtkPath absolute path to .vts file
     */
    private void loadAndUpdateEverything(String vtkPath) {
        loadFile(vtkPath);
        updateVarChoices();
        updateVTKPanel();
        if (!main.isShowingVTKPanel()) {
            main.showVTKPanel();
        }
    }

    /**
     * Updates the choices for what variable to color with in the drop down menu using whatever file is loaded by
     * {@code reader}
     * precondition: file is already loaded in {@code reader}
     */
    private void updateVarChoices() {
        if (fileIsLoaded()) {
            String[] vars = getPointArrayVarNames();
            vtkPointData data = reader.GetOutputAsDataSet().GetPointData();
            for (String varName : vars) {
                main.getViewerSettings().putVar(varName, data.GetArray(varName));
            }
            main.getViewerSettings().setActiveVars(vars);
        }
    }
    private String[] getPointArrayVarNames() {
        int pointArrays = reader.GetNumberOfPointArrays();
        String[] vars = new String[pointArrays];
        for (int i = 0; i < pointArrays; i++) {
            String varName = reader.GetPointArrayName(i);
            vars[i] = varName;
        }
        return vars;
    }

    /**
     * Loads a VTS file into {@code reader}
     * @param vtsPath absolute path to .vts file
     */
    private void loadFile(String vtsPath) {
        if (reader == null) {
            reader = new vtkXMLStructuredGridReader();
        }
        reader.SetFileName(vtsPath);
        reader.Update();
    }

    private boolean fileIsLoaded() {
        return reader != null;
    }

    /**
     * Shows GUI dialog prompting user to select a .vts file
     * @return File object of selected file, null if no file selected
     */
    private File promptForVTKFile() {
        JFileChooser fileChooser;
        if (Starfish.options == null || Starfish.options.wd == null) {
            fileChooser = new JFileChooser();
        } else {
            fileChooser = new JFileChooser(Starfish.options.wd);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(".vts files", "vts"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile;
        }
        return null;
    }

}