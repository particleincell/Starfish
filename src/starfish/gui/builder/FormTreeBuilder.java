package starfish.gui.builder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import starfish.gui.builder.form.CustomBlueprintNode;
import starfish.gui.builder.form.FormNode;

import javax.swing.*;
import javax.swing.tree.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FormTreeBuilder extends JPanel {

    private Map<String, Supplier<FormNode>> typeToSupplierMap;

    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JTree tree;

    private JButton addNode;
    private JButton removeNode;

    public FormTreeBuilder() {
        super(new BorderLayout());
        setMinimumSize(new Dimension(200, 0));

        typeToSupplierMap = new HashMap<>();
        typeToSupplierMap.put("Custom", () -> new CustomBlueprintNode());

        rootNode = new DefaultMutableTreeNode(RootUserObject.create());
        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        tree.addTreeSelectionListener(arg0 -> onNodeSelectionChanged());

        addNode = new JButton("Add node");
        addNode.addActionListener(arg0 -> addNodeToSelectedNode());
        removeNode = new JButton("Remove node");
        removeNode.addActionListener(arg0 -> removeSelectedNode());

        add(new JScrollPane(tree), BorderLayout.CENTER);

        JPanel buttonContainer = new JPanel(new GridLayout(2, 1));
        buttonContainer.add(addNode);
        buttonContainer.add(removeNode);
        add(buttonContainer, BorderLayout.SOUTH);
    }

    public void addNodeType(String name, Supplier<FormNode> supplier) {
        typeToSupplierMap.put(name, supplier);
    }

    /** Remove all nodes except the root node. */
    public void clear() {
        rootNode.removeAllChildren();
        treeModel.reload();
    }

    private Consumer<JPanel> focusListener;

    /**
     * @param focusListener will be given the new item in focus
     */
    public void setOnNewNodeInFocus(Consumer<JPanel> focusListener) {
        this.focusListener = focusListener;
        focusListener.accept((JPanel) getSelectedNode().getUserObject());
    }


    private void onNodeSelectionChanged() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == rootNode) {
            addNode.setEnabled(true);
            removeNode.setEnabled(false);
        } else {
            FormNode formNode = (FormNode) selectedNode.getUserObject();
            addNode.setEnabled(formNode.allowsChildren());
            removeNode.setEnabled(true);
        }
        focusListener.accept((JPanel) selectedNode.getUserObject());
    }

    /** Remove the currently selected node. */
    private void removeSelectedNode() {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)
                    (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
            if (parent != null) {
                treeModel.removeNodeFromParent(currentNode);
                return;
            }
        }
    }

    private void addNodeToSelectedNode() {
        String[] types = typeToSupplierMap.keySet().toArray(new String[0]);
        Arrays.sort(types);
        Object selectionObject = JOptionPane.showInputDialog(this,
                "Choose command type of the new node", "Menu",
                JOptionPane.PLAIN_MESSAGE, null, types, types[0]);
        if (selectionObject != null) {
            String selection = selectionObject.toString();
            Supplier<FormNode> supplier = typeToSupplierMap.get(selection);
            FormNode newFormNode = supplier.get();
            addObject(newFormNode);
        }
    }

    /** Add child to the currently selected node. */
    private DefaultMutableTreeNode addObject(Object child) {
        DefaultMutableTreeNode parentNode = getSelectedNode();
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
        treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());
        tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        return childNode;
    }

    /**
     * @return Selected node, root node if no node is selected
     */
    private DefaultMutableTreeNode getSelectedNode() {
        DefaultMutableTreeNode node = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null) {
            node = rootNode;
        } else {
            node = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }
        return node;
    }

    public void outputToFile(File file) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element xmlRootElement = doc.createElement("simulation");
            doc.appendChild(xmlRootElement);

            for (int i = 0; i < rootNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                appendChild(xmlRootElement, child);
            }

            writeDocumentToFile(doc, file);
        } catch (Exception e) {
            System.out.println("uh oh " + e);
        }
    }
    private Element appendChild(Element parent, DefaultMutableTreeNode node) {
        FormNode formNode = (FormNode) node.getUserObject();
        Element newElem = formNode.outputSelfTo(parent);
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            appendChild(newElem, child);
        }
        return newElem;
    }


    private static void writeDocumentToFile(Document doc, File file) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

}
