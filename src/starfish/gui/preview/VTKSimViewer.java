package starfish.gui.preview;

import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;
import starfish.core.io.OutputModule;
import starfish.core.io.VTKWriter;
import vtk.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VTKSimViewer extends JPanel {

    /*
     * Keeps track of the number of instances so temp files created can be named accordingly if multiple GUIs are made
     * (temp1.vts, temp2.vts, ect...)
     */
    private static int instanceCount = 0;

    static {
        if (!vtkNativeLibrary.LoadAllNativeLibraries()) {
            JOptionPane.showMessageDialog(null, "Unable to load data visualization library");
        }
        vtkNativeLibrary.DisableOutputWindow(null);
    }

    private Showing currentlyShowing;
    private File vtkTempFile;
    private vtkXMLStructuredGridReader reader;
    private VTKWriter vtkWriter;

    private Starfish sim;
    private final JPanel preview; // Contains vtkPanel and settings panel
    private final vtkPanel vtkPanel;

    private VTKSimViewerSettings settings;
    private JComboBox<String> varSelector;

    private JToolBar toolbar;

    private JPanel previewPlaceholder; // Shown instead of preview when no sim has been run yet

    public VTKSimViewer(Starfish sim) {
        super(new BorderLayout());
        instanceCount += 1;
        this.sim = sim;

        settings = new VTKSimViewerSettings();

        createToolbar();
        add(toolbar, BorderLayout.NORTH);

        vtkPanel = new vtkPanel();
        // VTK Panel can't shrink unless you manually set the minimum size
        vtkPanel.setMinimumSize(new Dimension(0, 0));

        preview = new JPanel(new BorderLayout());
        preview.add(vtkPanel, BorderLayout.CENTER);

        createPreviewPlaceholder();
        add(previewPlaceholder, BorderLayout.CENTER);
        currentlyShowing = Showing.PLACEHOLDER;
    }
    private void createToolbar() {
        JButton loadFile = new JButton("Load file");
        loadFile.addActionListener(arg0 -> loadUserSelectedFile());

        JButton refreshVtkViewer = new JButton("Show current sim");
        refreshVtkViewer.addActionListener(arg0 -> loadPreviewOfCurrentlyRunningSim());

        JButton settings = new JButton("Settings");
        settings.addActionListener(arg0 -> showSettingsWindow());

        varSelector = new JComboBox<>();
        varSelector.addItem("Load simulation to select variable");
        varSelector.addActionListener(arg0 -> updateVTKPanel());
        varSelector.setPreferredSize(new Dimension(300, 0));
        varSelector.setMaximumSize(new Dimension(300, 300));
        // This needs to be disabled or else the drop down menu will be hidden under the vtkPanel
        // https://vtk.org/pipermail/vtkusers/2011-July/068586.html
        varSelector.setLightWeightPopupEnabled(false);

        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(refreshVtkViewer);
        toolbar.add(loadFile);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(varSelector);
        toolbar.add(settings);
    }
    private JDialog dialog;
    private void showSettingsWindow() {
        if (dialog == null) {
            dialog = new JDialog();
            dialog.setTitle("Settings");
            dialog.getContentPane().add(settings);
            dialog.setSize(500, 300);
            dialog.setMinimumSize(new Dimension(400, 300));
            dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            dialog.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    updateVTKPanel();
                }
            });
        }
        dialog.setVisible(true);
    }
    private void createPreviewPlaceholder() {
        previewPlaceholder = new JPanel(new BorderLayout());
        try {
            Image starfishImage = ImageIO.read(new File("starfish.png"));
            previewPlaceholder.add(new JLabel(new ImageIcon(starfishImage)), BorderLayout.CENTER);
        } catch (IOException e) {
            previewPlaceholder.add(new JLabel("Load file or start simulation to view"), BorderLayout.CENTER);
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
     * Function that is called when the "refresh preview" button is pressed in the GUI
     */
    public void loadPreviewOfCurrentlyRunningSim() {
        if (sim != null && Starfish.getMeshList().size() > 0) {
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
            vtkTempFile = File.createTempFile("starfishTemp" + instanceCount + "_", ".vts");
            vtkTempFile.deleteOnExit();
            vtkWriter = new VTKWriter(vtkTempFile.getParent(), VTKWriter.OutputFormat.ASCII);
            String[] scalars = OutputModule.getAllScalars().toArray(new String[0]);
            List<String[]> vectors = new ArrayList<>(OutputModule.getAllVectors());
            String[] cellData = OutputModule.getAllCellData().toArray(new String[0]);
            vtkWriter.init2D(scalars, vectors, cellData, null);
        } catch (IOException e) {

        }
    }

    // Color map legend must be a field so it can be kept between calls of updateVTKPanel(). If a new one is created
    // every time it is called, then it will add the new actor without removing the old one.
    private vtkScalarBarActor scalarBarActor = new vtkScalarBarActor();
    /**
     * Updates the VTK preview to display the currently loaded file using the settings set by the user in the
     * settings panel
     */
    private void updateVTKPanel() {
        if (fileIsLoaded()) {
            String selectedVar = (String) varSelector.getSelectedItem();

            vtkStructuredGrid structuredGrid = reader.GetOutput();
            structuredGrid.GetPointData().SetActiveScalars(selectedVar);

            vtkDataSetMapper mapper = new vtkDataSetMapper();
            mapper.SetInputData(structuredGrid);
            mapper.SetScalarVisibility(1);
            mapper.SetScalarModeToUsePointData();
            mapper.SetColorModeToMapScalars();

            vtkLookupTable lookupTable = settings.getLookupTable(selectedVar);

            mapper.SetLookupTable(lookupTable);
            mapper.SetUseLookupTableScalarRange(1);

            scalarBarActor.SetLookupTable(lookupTable);
            scalarBarActor.SetNumberOfLabels(4);

            vtkActor actor = new vtkActor();
            actor.SetMapper(mapper);

            vtkPanel.GetRenderer().SetBackground(.5, .5, .5);
            vtkPanel.GetRenderer().Clear();
            vtkPanel.GetRenderer().AddActor(actor);
            vtkPanel.GetRenderer().AddActor2D(scalarBarActor);
            vtkPanel.repaint();
        }
    }

    /**
     * Updates the choices for what variable to color with in the drop down menu using whatever file is loaded by
     * {@code reader}
     */
    private void updateVarChoices() {
        Object currentlySelectedItem = varSelector.getSelectedItem();
        varSelector.removeAllItems();
        vtkPointData data = reader.GetOutputAsDataSet().GetPointData();
        if (fileIsLoaded()) {
            int pointArrays = reader.GetNumberOfPointArrays();
            for (int i = 0; i < pointArrays; i++) {
                String varName = reader.GetPointArrayName(i);
                varSelector.addItem(varName);
                settings.putVar(varName, data.GetArray(varName));
            }
        }
        varSelector.setSelectedItem(currentlySelectedItem);
    }

    /**
     * Loads the file into {@code reader}, then updates the {@code vtkPanel}, updates the variables in the menu, etc.
     * @param vtkPath absolute path to .vts file
     */
    private void loadAndUpdateEverything(String vtkPath) {
        loadFile(vtkPath);
        updateVarChoices();
        updateVTKPanel();
        if (currentlyShowing != Showing.VTK_PREVIEW) {
            showPreview();
        }
    }

    /**
     * Loads a VTS file
     *
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

    public void hidePreview() {
        currentlyShowing = Showing.PLACEHOLDER;
        remove(preview);
        add(previewPlaceholder, BorderLayout.CENTER);
        repaint();
    }
    public void showPreview() {
        currentlyShowing = Showing.VTK_PREVIEW;
        remove(previewPlaceholder);
        add(preview, BorderLayout.CENTER);
        repaint();
    }

    public void setSim(Starfish sim) {
        this.sim = sim;
    }

    public boolean isAbleToShowPreview() {
        return vtkWriter != null;
    }

    /**
     * Shows GUI dialog prompting user to select a .vts file
     * @return File object of selected file, null if no file selected
     */
    private static File promptForVTKFile() {
        JFileChooser fileChooser;
        if (Starfish.options == null || Starfish.options.wd == null) {
            fileChooser = new JFileChooser();
        } else {
            fileChooser = new JFileChooser(Starfish.options.wd);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(".vts files", "vts"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile;
        }
        return null;
    }

    private enum Showing {
        VTK_PREVIEW, PLACEHOLDER
    }

}