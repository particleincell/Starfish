/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import starfish.core.common.Starfish.Log;

/** xml file parser*/
public class InputParser implements Iterable
{
    Node root_element;
	
    /** constructo
     * @param file_name*/
    public InputParser(String file_name)
    {
	/*recursively load the entire file*/
	root_element = Load(file_name);
    }
	
    /**
    * Recursively loads an input file, 
    * replaces {@code <load>} XML elements with the content from the specified file
    * @param file_name file to load
    * @return root node of the loaded element
    */
    protected final Node Load(String file_name)
    {	
	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder;
	Document document=null;
		
	Log.log("Loading "+file_name);
					
	try 
	{
	    builder = builderFactory.newDocumentBuilder();
	    document = builder.parse(new FileInputStream(file_name));
	} catch (Exception e) 
	{
	    Log.error("Failed to open input file " + file_name);
	} 

	Element root = document.getDocumentElement();

	/*iterate through the tree and replace  <load> commands*/
	NodeList nodes = root.getChildNodes();
	for(int i=0;i<nodes.getLength();i++)
	{
	    Node node = nodes.item(i);
	    if (node instanceof Element)
	    {
		if (node.getNodeName().equalsIgnoreCase("load"))
		{
		    /*load this new file and add element to the tree*/
		    String name = node.getFirstChild().getNodeValue();
		    Node new_node = document.importNode(Load(name),true);
		    root.replaceChild(new_node, node);
		}
	    }
	}

	/*return root element*/
	return root;
    }

    /** 
     * @param element parent element 
     * @return first child, i.e. the text in {@code <tag>text</tag>}*/
    public static String getFirstChild(Element element) 
    {
	return element.getFirstChild().getNodeValue();
    }

    /**
     * @param key key name* 
     * @param element source element
     * @return first element named key that is a child of element */
    public static Element getChild(String key, Element element) 
    {
	Iterator<Element> iterator = InputParser.iterator(element);

	while(iterator.hasNext())
	{
	    Element el = iterator.next();
	    if (el.getNodeName().equalsIgnoreCase(key))
		return el;
	}
	return null;		
    }

    /**@return all children with the named key
     @param key key name
     @param element source element*/
    public static Element[] getChildren(String key, Element element)
    {
	ArrayList<Element> list = new ArrayList<Element>();

	Iterator<Element> iterator = InputParser.iterator(element);
	while(iterator.hasNext())
	{
	    Element el = iterator.next();
	    if (el.getNodeName().equalsIgnoreCase(key))
		list.add(el);
	}
	return list.toArray(new Element [0]);
    }

    /**Searches source element for the specified key which can be given as an attribute or a node and returns the value
     * @param key key name
     * @param element source element
     * @return key value
     */
    private static String getValueInternal(String key, Element element) throws NoSuchElementException
    {
	/*make sure element is not null*/
	if (element==null) throw new NoSuchElementException("element is null");
	
	/*check if attribute with this key exists*/
	String attr = element.getAttribute(key);
	if (!attr.isEmpty())
	    return attr;

	/*next search among nodes*/
	NodeList nodes = element.getChildNodes();

	/*loop through elements and search for node_name*/
	for (int i=0;i<nodes.getLength();i++)
	{
	    Node node = nodes.item(i);
	    if (node.getNodeName().equalsIgnoreCase(key))
	    {
		/*if some text is defined*/
		String str = "";
		Node sub = node.getFirstChild();
		
		while (sub!=null)
		{
		    if (sub.getNodeType()!=Node.COMMENT_NODE) 
			str += sub.getNodeValue();
		    sub = sub.getNextSibling();
		}
		 
		return str;
	    }
	}
	
	throw new NoSuchElementException("Could not find <"+key+"> in <"+element.getNodeName()+">");
    }

    /**returns string value associated with the given key or the default if not found
     * @param key key to search for
     * @param element source element
     * @param default_value default value to return if key not found
     * @return key value*/
    public static String getValue(String key, Element element, String default_value)
    {
	try
	{
	    return getValueInternal(key,element);	
	}
	catch(NoSuchElementException e)
	{
	    return default_value;
	}	
    }

    /**
     * Returns string value associated with a certain key within a parent element. 
     * Function will error out if key not found
     * @param key key to search for
     * @param element source element
     * @return string value of the key if found
     */
    public static String getValue(String key, Element element) 
    {
	try{ return getValueInternal(key,element);}
	catch(NoSuchElementException e) {Log.error(e.getMessage());}
	return null;	/*this will never happen*/
    }
	
    /**convenience method to parse "true" or "yes" as boolean, any other value results in false
     * @param key key to search for
     * @param element source element
     * @return boolean if found or throws an exception if not found*/
    public static boolean getBoolean(String key, Element element) throws NoSuchElementException
    {
	String val = getValueInternal(key,element);
	return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes");
    }	

    /**convenience method to parse a boolean. Returns default value if key not found.
     * @param key key to search for
     * @param element source element
     * @param default_value default value if key not found
     * @return boolean value or default if key not found*/
    public static boolean getBoolean(String key, Element element, boolean default_value) 
    {
	try {
	    return getBoolean(key,element);
	}
	catch (NoSuchElementException e)
	{
	    return default_value;
	}
    }

    /**convenience method to parse an integer.
     * @param key key to search for
     * @param element source element
     * @return key value, or an exception if data cannot be converted to an integer*/
    public static int getInt(String key, Element element) throws NumberFormatException
    {
	String str = getValueInternal(key,element);

	/*first convert to double to allow for "e" syntax of large numbers*/
	int i=0;

	try 
	{
	    double d = Double.parseDouble(str);
	    i = (int)d;
	}
	catch (NumberFormatException e)
	{
	    Log.error("error parsing integer value " + str);
	}

	return i;
    }	

    /**convenience method to parse an integer. Returns a default value if key not found.
     * @param key key to search for
     * @param default_value default value
     * @param element source element
     * @return key value, or default if key not found*/
    public static int getInt(String key, Element element, int default_value) 
    {
	try {
	    String str = getValueInternal(key,element);
	}
	catch (NoSuchElementException e)
	{
	    return default_value;
	}

	return getInt(key,element);
    }
    
    /**
     *
     * @param name
     * @param element
     * @return
     */
    public static int[] getIntList(String name, Element element) 
    {
	String list[] = getList(name,element);
	int c[] = new int[list.length];

	for (int i=0;i<list.length;i++)
	{
	    c[i] = Integer.parseInt(list[i]);
	}

	return c;
    }

    /**
     *
     * @param key
     * @param element
     * @param default_value
     * @return
     */
    public static int[] getIntegerList(String key, Element element, int[] default_value)
    {
	try {
	    return getIntList(key,element);
	}
	catch (NoSuchElementException e)
	{
	    return default_value;
	}		
    }


    /**convenience method to parse double
     * @param key
     * @param element
     * @return s*/
    public static double getDouble(String key, Element element) 
    {
	try {
	    return Double.parseDouble(getValueInternal(key,element));
	}
	catch (Exception e)
	{
	    Log.error(String.format("Could not convert value of <%s> to double",key));
	    return -1;
	}
    }

    /**convenience method for parsing double
     * @param key
     * @param element
     * @param default_value
     * @return */
    public static double getDouble(String key, Element element, double default_value)
    {
	try {
	    return Double.parseDouble(getValueInternal(key,element));
	}
	catch (NoSuchElementException e)
	{
	    return default_value;
	}		
    }

    /** returns values from a field such as &lt;node_name&gt;aa, bb, cc&lt;/node_name&gt; as string list
     * strings separator is "," and leading/trailing whitespace is removed
     * @param node_name
     * @param element
     * @return 
     */
    public static String[] getList(String node_name, Element element)
    {
	String text = getValue(node_name,element,"");
	ArrayList<String> list = new ArrayList<String>();

	StringTokenizer st = new StringTokenizer(text,",");
	while (st.hasMoreTokens()) 
	    list.add(st.nextToken().trim());
	
	return list.toArray(new String[0]);
    }

     /**
     * @param node_name
     * @param element *  @return values from a field such as {@code <node_name>[aa, bb], [cc, dd]</node_name>} as string list     
     * @return      
     */
    public static ArrayList<String[]> getListOfPairs(String node_name, Element element)
    {
	String text = getValue(node_name,element,"");
	ArrayList<String[]> list = new ArrayList<String[]>();

	StringTokenizer st = new StringTokenizer(text,",");
	boolean first=true;
	String pair[] = new String[2];
	while (st.hasMoreTokens()) 
	{
	    String token = st.nextToken().trim();
	    if (first)
	    {
		if (token.charAt(0)!='[') Log.error("Expected leading [ in "+token);
		token = token.substring(1);
		pair = new String[2];	//ArrayList add  only a shallow copy so need new object for each pair
		pair[0] = token;
	    }
	    else
	    {
		if (token.charAt(token.length()-1)!=']') Log.error("Expected trailing ] in "+token);
		token = token.substring(0,token.length()-1);
		pair[1] = token;
		list.add(pair);
		
	    }
	    first = !first;
	}
	
	return list;
    }

    /**
     *
     * @param name
     * @param element
     * @return
     */
    public static double[] getDoubleList(String name, Element element) 
    {
	String list[] = getList(name,element);
	double c[] = new double[list.length];

	for (int i=0;i<list.length;i++)
	{
	    c[i] = Double.parseDouble(list[i]);
	}

	return c;
    }

    /**
     *
     * @param key
     * @param element
     * @param default_value
     * @return
     */
    public static double[] getDoubleList(String key, Element element, double[] default_value)
    {
	
	double list[] = getDoubleList(key,element);
	
	if (list.length==0)
	    return default_value;
	
	return list;
    }

    @Override
    public Iterator<Element> iterator() 
    {
	return iterator (root_element);
    }

    /**
     *
     * @param node
     * @return
     */
    static public Iterator<Element> iterator(Node node) 
    {
	return new ElementIterator (node);
    }

    /** Element iterator, will return elements and ignore the rest*/
    public static class ElementIterator implements Iterator<Element> 
    {
	NodeList nodes;
	int count;
	int index = -1;		/*index of the last returned node*/

	private ElementIterator(Node root) 
	{
	    nodes = root.getChildNodes();
	    count = nodes.getLength();
	}

	/** returns true if more nodes exist*/
	@Override
	public boolean hasNext() 
	{
	    /*search for elements*/
	    for (int i=index+1;i<count;i++)
	    {
		if (nodes.item(i) instanceof Element) return true;
	    }
	    return false;
	}

	/** returns next node*/
	@Override
	public Element next() 
	{
	    Node node;
	    do{
		/*increment couner*/
		index++;

		node = nodes.item(index);	
	    } while(!(node instanceof Element));
	    return (Element) node;
	}

	@Override
	public void remove() 
	{
	    /*nothing to do, not supported*/
	}

    }	
}