/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.solver.Matrix;

/**
 *
 * @author Lubos Brieda
 */
public class BoundaryModule extends CommandModule {
	/* variables */

	/**
	 *
	 */

	protected ArrayList<Boundary> boundary_list = new ArrayList<>();

	/**
	 *
	 */
	protected FieldManager1D field_manager;

	/* methods */

	/**
	 *
	 * @param boundary
	 * @param var_name
	 * @return
	 */

	public Field1D getField(Boundary boundary, String var_name) {
		return field_manager.getField(boundary, var_name);
	}

	/**
	 *
	 * @return
	 */
	public ArrayList<Boundary> getBoundaryList() {
		return boundary_list;
	}

	/**
	 *
	 * @return
	 */
	public FieldManager1D getFieldManager() {
		return field_manager;
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public FieldCollection1D getFieldCollection(String name) {
		return field_manager.getFieldCollection(name);
	}

	/**
	 *
	 */
	protected ArrayList<Segment> seg_list;

	/**
	 *
	 * @param boundary
	 */
	public void addBoundary(Boundary boundary) {
		boundary_list.add(boundary);
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public Boundary getBoundary(String name) {
		for (Boundary boundary : boundary_list)
			if (boundary.name.equalsIgnoreCase(name))
				return boundary;
		Log.error("boundary " + name + " not found");
		return null; // this won't actually happen
	}

	/**
	 * Updates boundary values and also calls domain module if any boundary values have changed
	 */
	public void updateBoundaries() {
		boolean changed = false;
		for (Boundary boundary : boundary_list)
			changed |= boundary.update();
		if (changed)
			Starfish.domain_module.updateBCvalues();
	}

	@Override
	public void process(Element element) {

		// check for units
		double def_scale = 1;
		String units_name = InputParser.getValue("units", element, "m").toUpperCase();
		if (units_name.startsWith("IN"))
			def_scale = 0.0254;
		else if (units_name.startsWith("CM"))
			def_scale = 0.01;
		else if (units_name.startsWith("MM"))
			def_scale = 0.001;

		/* grab transformation if set */
		double scaling[] = { def_scale, def_scale };
		double translation[] = { 0, 0 };
		double rotation = 0;
		boolean flip_normals = false;

		/* default */
		Matrix G = Matrix.makeTransformationMatrix(scaling, rotation, translation);

		/* process commands */
		Iterator<Element> iterator = InputParser.iterator(element);

		while (iterator.hasNext()) {
			Element el = iterator.next();
			if (el.getNodeName().equalsIgnoreCase("BOUNDARY"))
				NewBoundary(el, G, flip_normals);
			else if (el.getNodeName().equalsIgnoreCase("TRANSFORM")) {
				scaling = InputParser.getDoubleList("scaling", el, scaling);
				translation = InputParser.getDoubleList("translation", el, translation);
				rotation = InputParser.getDouble("rotation", el, rotation);
				/* update global matrix */
				G = Matrix.makeTransformationMatrix(scaling, rotation, translation);

				flip_normals = InputParser.getBoolean("reverse", el, false);
			} else
				Log.warning("Unknown <boundaries> element " + el.getNodeName());
		}

	}

	/**
	 * ads a new boundary
	 *
	 * @param element             Parent element for this boundary
	 * @param G                   global transformation matrix
	 * @param flip_normals_global
	 */
	public void NewBoundary(Element element, Matrix G, boolean flip_normals_global) {
		Boundary boundary = new Boundary(element);
		/* grab local transformation if set */
		Element transf = InputParser.getChild("transform", element);
		double scaling[] = { 1, 1 };
		double translation[] = { 0, 0 };
		double rotation = 0;
		/* read reverse flag if defined */
		boolean flip_normals = InputParser.getBoolean("reverse", element, flip_normals_global);

		if (transf != null) {
			scaling = InputParser.getDoubleList("scaling", transf, scaling);
			translation = InputParser.getDoubleList("translation", transf, translation);
			rotation = InputParser.getDouble("rotation", transf, rotation);
			/* read reverse flag if defined inside transform element */
			flip_normals = InputParser.getBoolean("reverse", element, flip_normals);

		}

		/* make local transformation matrix */
		Matrix L = Matrix.makeTransformationMatrix(scaling, rotation, translation);

		/* update total matrix, T=G*L */
		Matrix T = G.mult(L);

		/* add points */
		String path = InputParser.getValue("path", element);
		boundary.setPath(path, T, flip_normals);

		/* add boundary to the list */
		addBoundary(boundary);
	}

	@Override
	public void init() {
		/* nothing to do */
	}

	@Override
	public void exit() {
		/* do nothing */
	}

	@Override
	public void start() {
		if (has_started)
			return;

		for (Boundary boundary : boundary_list)
			boundary.init();

		/* build a list of all segments for visibility checking */
		seg_list = new ArrayList<>();
		for (Boundary boundary : boundary_list)
			for (int i = 0; i < boundary.numSegments(); i++)
				seg_list.add(boundary.getSegment(i));

		/* init field manager */
		field_manager = new FieldManager1D(boundary_list);

		/* setup fields, these are the summations of individual materials */

		/*
		 * grab field manager for the first material, the data lists are the same on all
		 */
		/* manually start materials module if not yet started */
		Starfish.materials_module.start();

		FieldManager1D material_fm = Starfish.materials_module.getMaterialsList().get(0).getFieldManager1d();

		for (String name : material_fm.getNames()) {
			String unit = material_fm.getUnits(name);
			field_manager.add(name, unit);
		}

		has_started = true;

	}

	/**
	 * @param xp * @return true if the point xp is internal to any boundary spline
	 * @return true if point xp is on the internal side of boundaries
	 */
	public boolean isInternal(double xp[]) {
		Segment seg = Spline.visibleSegment(xp, seg_list);
		if (seg == null)
			return false;

		return Spline.isInternal(xp, seg);
	}

}
