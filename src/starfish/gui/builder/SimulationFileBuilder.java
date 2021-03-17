package starfish.gui.builder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import starfish.gui.builder.form.FormNode;
import starfish.gui.builder.form.FormNodeFactory;
import starfish.gui.builder.form.exceptions.MissingAttributeException;
import starfish.gui.builder.form.exceptions.UnknownConfigFileTagNameException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SimulationFileBuilder extends JPanel {

    private JSplitPane content;
    private FormTreeBuilder formTreeBuilder;
    private JScrollPane sectionEditorPanel;

    public SimulationFileBuilder()  {
        setLayout(new BorderLayout());
        content = new JSplitPane();
        formTreeBuilder = new FormTreeBuilder();
        content.setLeftComponent(formTreeBuilder);
        sectionEditorPanel = new JScrollPane();
        content.setRightComponent(sectionEditorPanel);
        add(content, BorderLayout.CENTER);

        createToolBar();

        formTreeBuilder.setOnNewNodeInFocus(jPanel -> sectionEditorPanel.setViewportView(jPanel));
    }
    private void createToolBar() {
        JButton saveToFileButton = new JButton("Save to File");
        saveToFileButton.addActionListener(arg0 -> promptToSaveToFile());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(saveToFileButton);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @param blueprintFile xml file of Starfish sim file command blueprints
     */
    public void addCommandsFrom(File blueprintFile) throws IOException, SAXException,
            UnknownConfigFileTagNameException, MissingAttributeException {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return;
        }
        Document document = builder.parse(blueprintFile); // IOException and SAXException thrown from here
        addBlueprintsFrom(document);
    }
    private void addBlueprintsFrom(Document document) {
        NodeList sections = document.getDocumentElement().getChildNodes();
        List<String> badElements = new LinkedList<>();
        for (int i = 0; i < sections.getLength(); i++) {
            Node node = sections.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                try {
                    // test if valid node
                    FormNode newNode = FormNodeFactory.makeNode(element);
                    // If it is, this part of the code would be reached
                    formTreeBuilder.addNodeType(newNode.getName(), FormNodeFactory.makeNodeSupplier(element));
                } catch (Exception e) {
                    badElements.add(e.getMessage());
                }
            }
        }
    }

    public void promptToSaveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() ||  f.getName().endsWith("xml");
            }
            @Override
            public String getDescription() {
                return "Starfish simulation file (*.xml)";
            }
        });

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            formTreeBuilder.outputToFile(fileToSave);
        }
    }


}
