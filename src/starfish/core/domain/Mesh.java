/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Segment;
import starfish.core.boundaries.Spline;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.DomainModule.DomainType;

/** abstract implementation of a mesh with a structured topology */
public abstract class Mesh {
	/**
	 * general 2D structured constructor
	 * 
	 * @param nn          number of nodes;
	 * @param name        mesh identifier
	 * @param domain_type RZ/XY
	 */
	public Mesh(int nn[], String name, DomainType domain_type) {
		this.name = name;
		this.domain_type = domain_type;
		this.ni = nn[0];
		this.nj = nn[1];

		this.index = 0;
		if (Starfish.domain_module != null)
			this.index = Starfish.getMeshList().size();

		n_nodes = ni * nj;
		n_cells = (ni - 1) * (nj - 1);

		/* allocate nodes */
		node = new Node[ni][nj];

		for (int i = 0; i < ni; i++)
			for (int j = 0; j < nj; j++) {
				node[i][j] = new Node();
				node[i][j].type = NodeType.UNKNOWN;
			}

		/* initialize boundary mesh data */
		boundary_data[Face.RIGHT.val()] = new MeshBoundaryData[nj];
		boundary_data[Face.LEFT.val()] = new MeshBoundaryData[nj];
		boundary_data[Face.TOP.val()] = new MeshBoundaryData[ni];
		boundary_data[Face.BOTTOM.val()] = new MeshBoundaryData[ni];

		for (int j = 0; j < nj; j++) {
			boundary_data[Face.LEFT.val()][j] = new MeshBoundaryData();
			boundary_data[Face.RIGHT.val()][j] = new MeshBoundaryData();
		}
		for (int i = 0; i < ni; i++) {
			boundary_data[Face.BOTTOM.val()][i] = new MeshBoundaryData();
			boundary_data[Face.TOP.val()][i] = new MeshBoundaryData();
		}
	}

	/** called by DomainModule during initialization */
	public void init() {
		if (!virtual)
			setMeshNeighbors();
	}

	/*
	 * mesh index corresponds to the DomainModule list, meshes not in the main
	 * mesh_list will have duplicate index
	 */
	protected int index;

	/**
	 * @return index identifying this mesh
	 */
	public int getIndex() {
		return index;
	}

	/** Named constants for mesh faces */
	public enum Face {
		RIGHT(0), TOP(1), LEFT(2), BOTTOM(3);
		private final int v;

		Face(int val) {
			v = val;
		}

		/** @return associated value */
		public int val() {
			return v;
		}
	}

	/** data structure defined on each node */
	public static class Node {
		public NodeType type;

		public double bc_value;

		/** list of splines in this control volume */
		public ArrayList<Segment> segments = new ArrayList<>();
	}

	Field2D node_vol;

	Field2D getNodeVol() {
		return node_vol;
	}

	/* data type to specify the type of mesh boundary */
	static public enum DomainBoundaryType {
		OPEN(-1), DIRICHLET(0), NEUMANN(1), PERIODIC(2), SYMMETRY(3), MESH(4), SINK(5), CIRCUIT(6);

		protected int val;

		DomainBoundaryType(int val) {
			this.val = val;
		}

		/**
		 * @return associated value
		 */
		public int value() {
			return val;
		}
	}

	/**
	 * Data structure for storing information on mesh neighbors on boundary nodes
	 */
	public static class MeshBoundaryData {
		public Mesh neighbor[] = new Mesh[2]; // corner nodes can have two neighbors
		public DomainBoundaryType type = DomainBoundaryType.OPEN;
		double bc_value; // optional value for Dirichlet/Neumann boundaries
		public double buffer; // buffer for syncing boundaries
	}

	MeshBoundaryData boundary_data[][] = new MeshBoundaryData[4][]; /* [face][node_index] */

	/**
	 */
	static public enum NodeType {
		BAD(-99), UNKNOWN(-2), OPEN(-1), DIRICHLET(0);

		protected int val;

		NodeType(int val) {
			this.val = val;
		}

		/**
		 * @return associated value
		 */
		public int value() {
			return val;
		}
	};

	/**
	 *
	 * @param face
	 * @param type
	 * @param value
	 */
	public void setMeshBCType(Face face, DomainBoundaryType type, double value) {
		if (face == Face.LEFT || face == Face.RIGHT)
			for (int j = 0; j < nj; j++) {
				boundary_data[face.val()][j].type = type;
				boundary_data[face.val()][j].bc_value = value;
			}
		else
			for (int i = 0; i < ni; i++) {
				boundary_data[face.val()][i].type = type;
				boundary_data[face.val()][i].bc_value = value;
			}
	}

	/**
	 * Returns mesh boundary type at the node of the specified face
	 * 
	 * @param face  mesh face
	 * @param index node index
	 * @return boundary type
	 */
	public DomainBoundaryType boundaryType(Face face, int index) {
		return boundary_data[face.val()][index].type;
	}

	/* mesh definition */
	public int ni, nj; /* number of nodes */
	public int n_nodes, n_cells;

	public Node node[][];

	/* geometry */
	private DomainType domain_type;

	boolean virtual = false;

	public void makeVirtual() {
		virtual = true;
	}

	/* name */
	String name;

	/**
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 *
	 * @return
	 */
	public Node[][] getNodeArray() {
		return node;
	}

	/* accessors */

	/**
	 *
	 * @param i
	 * @param j
	 * @return node data type at i,j
	 */
	public Node getNode(int i, int j) {
		return node[i][j];
	}

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	public NodeType nodeType(int i, int j) {
		return node[i][j].type;
	}

	/**
	 * Returns mesh boundary normal vector at corresponding face and position
	 * 
	 * @param face mesh face of interest
	 * @param pos  position, must be along the boundary
	 * @return boundary normal vector, pointing into the domain
	 */
	abstract public double[] faceNormal(Face face, double pos[]);

	/**
	 *
	 * @param face
	 * @param index
	 * @return
	 */
	public MeshBoundaryData boundaryData(Face face, int index) {
		return boundary_data[face.val()][index];
	}

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	public boolean isFarFromSurfNode(int i, int j) {
		if (node[i][j].segments == null || node[i][j].segments.isEmpty())
			return true;
		else
			return false;
	}

	/**
	 * @return true if node i,j is a Dirichlet node
	 * @param i
	 * @param j
	 */
	public boolean isDirichletNode(int i, int j) {
		return nodeType(i, j) == NodeType.DIRICHLET;
	}

	/**
	 * @return true if node i,j is on a mesh boundary and is of the specified type
	 * @param i    node index
	 * @param j    node index
	 * @param type type to check against
	 */
	public boolean isMeshBoundaryType(int i, int j, DomainBoundaryType type) {
		// need to check corners where both i==0 and j==0 and so on
		boolean check = false;
		if (i == 0)
			check = check |= this.boundaryType(Face.LEFT, j) == type;
		else if (i == ni - 1)
			check |= this.boundaryType(Face.RIGHT, j) == type;
		else if (j == 0)
			check |= this.boundaryType(Face.BOTTOM, i) == type;
		else if (j == nj - 1)
			check |= this.boundaryType(Face.TOP, i) == type;
		else
			check = false;
		return check;
	}

	/**
	 * @return true if node i,j is a boundary node shared by meshes
	 * @param i
	 * @param j
	 */
	public boolean isMeshBoundary(int i, int j) {
		return isMeshBoundaryType(i, j, DomainBoundaryType.MESH);
	}

	/* constructor */

	/** allocates memory, computes node volumes, and sets boundary cells */
	void initNodes() {
		int i, j;

		node_vol = Starfish.getFieldCollection("NodeVol").getField(this);

		/* compute node volumes on open nodes */
		for (i = 0; i < ni; i++)
			for (j = 0; j < nj; j++)
				node_vol.set(i, j, nodeVol(i, j));

		/* now repeat, but use monte carlo on interface nodes to correct volumes */
		for (i = 0; i < ni; i++)
			for (j = 0; j < nj; j++)
				computeInterfaceNodeVol(i, j);

		/* set Dirichlet flag on mesh boundaries */
		for (i = 0; i < ni; i++) {
			if (boundaryData(Face.BOTTOM, i).type == DomainBoundaryType.DIRICHLET) {
				node[i][0].type = NodeType.DIRICHLET;
				node[i][0].bc_value = boundaryData(Face.BOTTOM, i).bc_value;
			}
			if (boundaryData(Face.TOP, i).type == DomainBoundaryType.DIRICHLET) {
				node[i][nj - 1].type = NodeType.DIRICHLET;
				node[i][nj - 1].bc_value = boundaryData(Face.TOP, i).bc_value;
			}
		}

		for (j = 0; j < nj; j++) {
			if (boundaryData(Face.LEFT, j).type == DomainBoundaryType.DIRICHLET) {
				node[0][j].type = NodeType.DIRICHLET;
				node[0][j].bc_value = boundaryData(Face.LEFT, j).bc_value;
			}
			if (boundaryData(Face.RIGHT, j).type == DomainBoundaryType.DIRICHLET) {
				node[ni - 1][j].type = NodeType.DIRICHLET;
				node[ni - 1][j].bc_value = boundaryData(Face.RIGHT, j).bc_value;
			}
		}

	}

	/** sets neighbors boundary cells */
	public void setMeshNeighbors() {
		int i, j;

		/*
		 * modify cells with neighbor meshes This is split into two parts: setting of
		 * the interior nodes [1:ni-2] and the corners The corners are set such that, in
		 * system with one mesh on top of another one, we do not get a mesh boundary on
		 * the (0,nj-1) left face, only on the top face.
		 */
		for (Mesh mesh : Starfish.getMeshList()) {
			/* skip self */
			if (mesh == this)
				continue;

			for (j = 1; j < nj - 1; j++) {
				if (mesh.containsPos(pos(0, j)))
					addMeshToBoundary(Face.LEFT, j, mesh);
				if (mesh.containsPos(pos(ni - 1, j)))
					addMeshToBoundary(Face.RIGHT, j, mesh);
			}

			for (i = 1; i < ni - 1; i++) {
				if (mesh.containsPos(pos(i, 0)))
					addMeshToBoundary(Face.BOTTOM, i, mesh);

				if (mesh.containsPos(pos(i, nj - 1)))
					addMeshToBoundary(Face.TOP, i, mesh);
			}
		}

		// now set the set corners
		for (Mesh mesh : Starfish.getMeshList()) {
			/* skip self */
			if (mesh == this)
				continue;

			// left boundary
			if (boundaryType(Face.LEFT, 1) == DomainBoundaryType.MESH && mesh.containsPos(pos(0, 0)))
				addMeshToBoundary(Face.LEFT, 0, mesh);
			if (boundaryType(Face.LEFT, nj - 2) == DomainBoundaryType.MESH && mesh.containsPos(pos(0, nj - 1)))
				addMeshToBoundary(Face.LEFT, nj - 1, mesh);

			// right boundary
			if (boundaryType(Face.RIGHT, 1) == DomainBoundaryType.MESH && mesh.containsPos(pos(ni - 1, 0)))
				addMeshToBoundary(Face.RIGHT, 0, mesh);
			if (boundaryType(Face.RIGHT, nj - 2) == DomainBoundaryType.MESH && mesh.containsPos(pos(ni - 1, nj - 1)))
				addMeshToBoundary(Face.RIGHT, nj - 1, mesh);

			// top boundary
			if (boundaryType(Face.TOP, 1) == DomainBoundaryType.MESH && mesh.containsPos(pos(0, nj - 1)))
				addMeshToBoundary(Face.TOP, 0, mesh);
			if (boundaryType(Face.TOP, ni - 2) == DomainBoundaryType.MESH && mesh.containsPos(pos(ni - 1, nj - 1)))
				addMeshToBoundary(Face.TOP, ni - 1, mesh);

			// bottom boundary
			if (boundaryType(Face.BOTTOM, 1) == DomainBoundaryType.MESH && mesh.containsPos(pos(0, 0)))
				addMeshToBoundary(Face.BOTTOM, 0, mesh);
			if (boundaryType(Face.BOTTOM, ni - 2) == DomainBoundaryType.MESH && mesh.containsPos(pos(ni - 1, 0)))
				addMeshToBoundary(Face.BOTTOM, ni - 1, mesh);
		}

	}

	/**
	 *
	 * @param face
	 * @param index
	 * @param mesh
	 */
	protected void addMeshToBoundary(Face face, int index, Mesh mesh) {
		MeshBoundaryData bc = boundary_data[face.val()][index];

		if (bc.neighbor[0] != null && bc.neighbor[1] != null) {
			Log.warning(
					String.format("Too many mesh neighbors for mesh %s, face %s, index %d", getName(), face, index));
			Log.warning(String.format("Current neighbors are %s and %s, found duplicate in %s",
					bc.neighbor[0].getName(), bc.neighbor[1].getName(), mesh.getName()));
		}

		bc.type = DomainBoundaryType.MESH;
		if (bc.neighbor[0] == null)
			bc.neighbor[0] = mesh;
		else
			bc.neighbor[1] = mesh;

	}

	/**
	 * evaluates position at topological coordinate i,j
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	abstract public double[] pos(double i, double j);

	/**
	 * returns position of node i,j with optional interpolation of ghost nodes on
	 * mesh boundaries
	 * 
	 * @param i                 i coordinate
	 * @param j                 j coordinate
	 * @param ghost_interpolate if true, will interpolate position of ghost nodes
	 *                          based on edge and one internal node
	 */
	public double[] pos(double i, double j, boolean ghost_interpolate) {
		// don't do anything if in bounds
		if (i >= 0 && i <= ni - 1 && j >= 0 && j <= nj - 1)
			return pos(i, j);

		// otherwise, reset to inbounds if interpolate flag not set
		if (!ghost_interpolate) {
			if (i < 0)
				i = 0;
			else if (i > ni - 1)
				i = ni - 1;
			if (j < 0)
				j = 0;
			else if (j > nj - 1)
				j = nj - 1;
			return pos(i, j);
		}

		// reset indexes to limits if no neighbor mesh along the boundary
		if (i < 0 && boundaryType(Face.LEFT, Utils.minmax((int) (j + 0.5), 0, nj - 1)) != DomainBoundaryType.MESH)
			return null;
		if (i > ni - 1 && boundaryType(Face.RIGHT, Utils.minmax((int) (j + 0.5), 0, nj - 1)) != DomainBoundaryType.MESH)
			return null;
		if (j < 0 && boundaryType(Face.BOTTOM, Utils.minmax((int) (i + 0.5), 0, ni - 1)) != DomainBoundaryType.MESH)
			return null;
		if (j > nj - 1 && boundaryType(Face.TOP, Utils.minmax((int) (i + 0.5), 0, ni - 1)) != DomainBoundaryType.MESH)
			return null;

		double x0[]; // node inside the mesh
		double x1[]; // node outside the mesh

		// there may be some recursive way to do this. Checking for 8+1 possible
		// scenarios
		if (i >= 0 && i <= ni - 1 && j >= 0 && j <= nj - 1)
			return pos(i, j); // trivial case - node is in bounds
		else if (i < 0 && j >= 0 && j <= nj - 1) // left face with j in bounds
		{
			double di = -i;
			x0 = pos(di, j);
			x1 = pos(0, j);
		} else if (i >= ni - 1 && j >= 0 && j <= nj - 1) // right face with j in bounds
		{
			double di = i - (ni - 1);
			x0 = pos(ni - 1 - di, j);
			x1 = pos(ni - 1, j);
		} else if (j < 0 && i >= 0 && i <= ni - 1) // bottom face with i in bounds
		{
			double dj = -j;
			x0 = pos(i, dj);
			x1 = pos(i, 0);
		} else if (j >= nj - 1 && i >= 0 && i <= ni - 1) // top face with i in bounds
		{
			double dj = j - (nj - 1);
			x0 = pos(i, nj - 1 - dj);
			x1 = pos(i, nj - 1);
		} else if (i < 0 && j < 0) // bottom left corner
		{
			// compute x0=(i,dj) and x1=(i,0)
			double di = -i;
			double dj = -j;
			double x00[] = pos(di, dj);
			double x01[] = pos(0, dj);
			x0 = new double[] { 2 * x01[0] - x00[0], 2 * x01[1] - x00[1] };

			double x10[] = pos(di, 0);
			double x11[] = pos(0, 0);
			x1 = new double[] { 2 * x11[0] - x10[0], 2 * x11[1] - x10[1] };

		} else if (i < 0 && j > nj - 1) // top left corner
		{
			// compute x0=(i,nj-1-dj) and x1=(i,nj-1)
			double di = -i;
			double dj = j - (nj - 1);
			double x00[] = pos(di, nj - 1 - dj);
			double x01[] = pos(0, nj - 1 - dj);
			x0 = new double[] { 2 * x01[0] - x00[0], 2 * x01[1] - x00[1] };

			double x10[] = pos(di, nj - 1);
			double x11[] = pos(0, nj - 1);
			x1 = new double[] { 2 * x11[0] - x10[0], 2 * x11[1] - x10[1] };
		} else if (i > ni - 1 && j < 0) // bottom right corner
		{
			// need to interpolate points at (i,dj) and (i,0)
			double di = i - (ni - 1);
			double dj = -j;
			double x00[] = pos(ni - 1 - di, dj);
			double x01[] = pos(ni - 1, dj);
			x0 = new double[] { 2 * x01[0] - x00[0], 2 * x01[1] - x00[1] };

			double x10[] = pos(ni - 1 - di, 0);
			double x11[] = pos(ni - 1, 0);
			x1 = new double[] { 2 * x11[0] - x10[0], 2 * x11[1] - x10[1] };
		} else if (i > ni - 1 && j > nj - 1) // top right corner
		{
			// need to interpolate points at (i,dj) and (i,nj-1)
			double di = i - (ni - 1);
			double dj = j - (nj - 1);
			double x00[] = pos(ni - 1 - di, nj - 1 - dj);
			double x01[] = pos(ni - 1, nj - 1 - dj);
			x0 = new double[] { 2 * x01[0] - x00[0], 2 * x01[1] - x00[1] };

			double x10[] = pos(ni - 1 - di, nj - 1);
			double x11[] = pos(ni - 1, nj - 1);
			x1 = new double[] { 2 * x11[0] - x10[0], 2 * x11[1] - x10[1] };
		} else // sanity check, this should never happen
		{
			Log.error("Shouldn't be here in pos");
			x1 = new double[2]; // to get rid of the error
			x0 = new double[2];
		}

		// we have x2=x0+2*(x1-x0)=2*x1-x0
		double x2[] = { 2 * x1[0] - x0[0], 2 * x1[1] - x0[1] };
		return x2;
	}

	/**
	 *
	 * @param lc
	 * @return
	 */
	public double[] pos(double lc[]) {
		return pos(lc[0], lc[1]);
	}

	/**
	 *
	 * @param lc
	 * @return
	 */
	public double[] pos(double lc[], boolean ghost_interpolate) {
		return pos(lc[0], lc[1], ghost_interpolate);
	}

	/**
	 * Computes logical coordinates for a FVM control volume centered at i0,j0 On
	 * non-domain boundaries, the bounds collapse to half/quarter volume On domain
	 * boundaries, the full volume is retained on non-corner nodes, Corner nodes
	 * result in the appropriate half volume getting returned
	 * 
	 * @param i0    CV i index
	 * @param j0    CV j index
	 * @param delta CV size in logical coords in each direction
	 * @return [4][2] array with the first index being the node in BL, BR, TR, TL
	 *         and second index is i/j
	 */
	public double[][] controlVolumeLCs(double i0, double j0, double delta) {
		double i = -1, j = -1;
		double lcs[][] = new double[4][2];

		for (int k = 0; k < 4; k++) {
			// setting the starting node on each face
			switch (k) {
			case 0:
				i = i0 - delta;
				j = j0 - delta;
				break; // bottom left
			case 1:
				i = i0 + delta;
				j = j0 - delta;
				break; // bottom right
			case 2:
				i = i0 + delta;
				j = j0 + delta;
				break; // top right
			case 3:
				i = i0 - delta;
				j = j0 + delta;
				break; // top left
			}

			// first check for mesh boundaries
			if (i < 0 && boundaryType(Face.LEFT, Utils.minmax((int) (j0 + 0.5), 0, nj - 1)) != DomainBoundaryType.MESH)
				i = 0;
			if (i > ni - 1
					&& boundaryType(Face.RIGHT, Utils.minmax((int) (j0 + 0.5), 0, nj - 1)) != DomainBoundaryType.MESH)
				i = ni - 1;
			if (j < 0
					&& boundaryType(Face.BOTTOM, Utils.minmax((int) (i0 + 0.5), 0, ni - 1)) != DomainBoundaryType.MESH)
				j = 0;
			if (j > nj - 1
					&& boundaryType(Face.TOP, Utils.minmax((int) (i0 + 0.5), 0, ni - 1)) != DomainBoundaryType.MESH)
				j = nj - 1;

			// make sure this point exists in some mesh
			if (i < 0 || j < 0 || i > ni - 1 || j > nj - 1) {
				double x[] = pos(i, j, true);
				if (x == null || Starfish.domain_module.getMesh(x) == null) {
					if (i < 0)
						i = 0;
					if (j < 0)
						j = 0;
					if (i >= ni - 1)
						i = ni - 1;
					if (j >= nj - 1)
						j = nj - 1;
				}
			}

			lcs[k][0] = i;
			lcs[k][1] = j;

		}

		// check for cases with diagonal edges (left at j-0.5, right at j, etc..) These
		// happen on mesh boundary corners
		// bottom edge (0,1), need to have the same j
		lcs[0][1] = Math.max(lcs[0][1], lcs[1][1]); // keep larger values if one node is j-0.5 and other is j
		lcs[1][1] = lcs[0][1];

		// right edge (1,2), same i
		lcs[1][0] = Math.min(lcs[1][0], lcs[2][0]);
		lcs[2][0] = lcs[1][0];

		// top edge (2,3), same j
		lcs[1][0] = Math.min(lcs[1][0], lcs[2][0]);
		lcs[2][0] = lcs[1][0];

		// right edge (1,2), same j
		lcs[2][1] = Math.max(lcs[2][1], lcs[3][1]);
		lcs[3][1] = lcs[2][1];

		// left edge (2,3), same i
		lcs[3][0] = Math.max(lcs[3][0], lcs[0][0]);
		lcs[0][0] = lcs[3][0];

		return lcs;
	}

	/**
	 * @param i1
	 * @param j1 * @return distance between two points
	 * @param i2
	 * @param j2
	 */
	public double dist(double i1, double j1, double i2, double j2) {
		double x1[] = pos(i1, j1);
		double x2[] = pos(i2, j2);
		double dx = x1[0] - x2[0];
		double dy = x1[1] - x2[1];
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * @param i * @return random position in cell i,j
	 * @param j
	 */
	public double[] randomPosInCell(int i, int j) {
		double lc[] = new double[2];
		int c = 0;
		do {
			lc[0] = i + Starfish.rnd();
			lc[1] = j + Starfish.rnd();
		} while (++c < 10 && isInternalPoint(lc));

		if (c >= 10)
			Log.error("Failed to find external point in cell " + i + " " + j + " (" + pos1(i, j) + ", " + pos2(i, j)
					+ ")");
		return pos(lc);
	}

	/**
	 * returns true if mesh contains the poin
	 * 
	 * @param x
	 * @return t
	 */
	abstract public boolean containsPosStrict(double x[]);

	/* returns first component of position */

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */

	public double pos1(double i, double j) {
		double x[] = pos(i, j);
		return x[0];
	}

	/* return second component of position */

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */

	public double pos2(double i, double j) {
		double x[] = pos(i, j);
		return x[1];
	}

	/**
	 * returns radius, taking into account domain type, will be 1 for X
	 * 
	 * @param i
	 * @param j
	 * @return Y
	 */
	public double R(double i, double j) {
		if (domain_type == DomainType.RZ)
			return pos1(i, j);
		if (domain_type == DomainType.ZR)
			return pos2(i, j);
		else
			return 1;
	}

	/*
	 * /*evaluates logical coordinates at spatial x1,x2
	 *
	 * @param x1
	 * 
	 * @param x2
	 * 
	 * @return
	 */

	abstract public double[] XtoL(double x1, double x2);

	/**
	 * Returns logical coordinate of point x
	 *
	 * @param x physical coordinate
	 * @return
	 */
	public double[] XtoL(double x[]) {
		return XtoL(x[0], x[1]);
	}

	/* returns integral logical coordinate at spatial location d1,d2 */

	/**
	 *
	 * @param d1
	 * @param d2
	 * @return
	 */

	public int[] XtoI(double d1, double d2) {
		int i[] = new int[2];
		double l[] = XtoL(d1, d2);
		i[0] = (int) l[0];
		i[1] = (int) l[1];
		return i;
	}

	/* returns integral logical coordinate at spatial location d[] */

	/**
	 *
	 * @param d
	 * @return
	 */

	public int[] XtoI(double d[]) {
		return XtoI(d[0], d[1]);
	}

	/* converts i,j to node index */

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */

	public int IJtoN(int i, int j) {
		if (i >= 0 && i < ni && j >= 0 && j < nj)
			return j * ni + i;
		return -1;
	}

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	public int IJtoN(double i, double j) {
		return IJtoN((int) i, (int) j);
	}

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	public int IJtoC(int i, int j) {
		if (i >= 0 && i < ni - 1 && j >= 0 && j < nj - 1)
			return j * (ni - 1) + i;
		return -1;
	}

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	public int IJtoC(double i, double j) {
		return IJtoC((int) i, (int) j);
	}

	/**
	 * returns area for node/cell centered at i,
	 * 
	 * @param i0
	 * @param j0
	 * @return j
	 */
	public double area(double i0, double j0) {
		return area(i0, j0, false, 0.5);
	}

	/**
	 * returns area for node/cell centered at i,
	 * 
	 * @param i0
	 * @param j0
	 * @param with_ghosts
	 * @return j
	 */
	public double area(double i0, double j0, boolean with_ghosts) {
		return area(i0, j0, with_ghosts, 0.5);
	}

	/**
	 * returns area around a node/cell centered at i,j
	 * 
	 * @param i0
	 * @param j0
	 * @param with_ghosts if true, will extrapolate neighbor nodes on mesh
	 *                    boundaries
	 * @param delta       indicates the width in each direction, 0.5 will give a
	 *                    node volume
	 * @return
	 */
	public double area(double i0, double j0, boolean with_ghosts, double delta) {
		double V[][] = new double[4][];

		double lcs[][] = this.controlVolumeLCs(i0, j0, delta);

		/* first set the four corner positions, these may not be nodes */
		for (int k = 0; k < 4; k++) {
			// set vertex position
			V[k] = pos(lcs[k], with_ghosts);
		}

		/* compute cell area */
		double A;
		A = 0.5 * Math.abs(V[0][0] * V[1][1] - V[0][0] * V[2][1] + V[1][0] * V[2][1] - V[1][0] * V[0][1]
				+ V[2][0] * V[0][1] - V[2][0] * V[1][1]);

		A += 0.5 * Math.abs(V[0][0] * V[2][1] - V[0][0] * V[3][1] + V[2][0] * V[3][1] - V[2][0] * V[0][1]
				+ V[3][0] * V[0][1] - V[3][0] * V[2][1]);
		return A;
	}

	/**
	 * Computes node volume
	 * 
	 * @param i0             *
	 * @param j0
	 * @param include_ghosts if true will include ghost node positions on mesh
	 *                       boundaries
	 * @return volume for node i,j
	 */
	final double XY_depth = 1; // this is the z-dimension of XY cells used for cell volume calculation

	public double nodeVol(double i0, double j0, boolean include_ghosts) {
		if (domain_type == DomainType.XY)
			return XY_depth * area(i0, j0); // TODO: need better way to set domain width to simplify comparison of RZ to
											// XY

		/* axisymmetric mesh */

		double r = R(i0, j0);
		// on centerline, we revolve (half area) through circle at r+0.25dr
		if (domain_type == DomainType.RZ) {
			if (i0 == 0)
				r = R(i0 + 0.25, j0);
			else if (i0 == ni - 1)
				r = R(i0 - 0.25, j0);
		} else if (domain_type == DomainType.ZR) {
			if (j0 == 0)
				r = R(i0, j0 + 0.25);
			else if (j0 == nj - 1)
				r = R(i0, j0 - 0.25);
		}

		return 2 * Math.PI * area(i0, j0, include_ghosts) * r;
	}

	/**
	 * Returns node volume, defaults to no ghost layers
	 * 
	 * @param i0
	 * @param j0
	 * @return node volume
	 */
	public double nodeVol(double i0, double j0) {
		return nodeVol(i0, j0, false);
	}

	/**
	 * @param i * @return volume for cell i,j
	 * @param j
	 * @return volume of a cell centered at i+0.5, j+0.5
	 */
	public double cellVol(int i, int j) {
		return nodeVol(i + 0.5, j + 0.5);
	}

	/*
	 * uses monte carlo approach to compute node volumes in interface node control
	 * volumes
	 */

	/**
	 *
	 * @param i
	 * @param j
	 */

	public void computeInterfaceNodeVol(int i, int j) {
		// only process nodes with surface elements
		if (node[i][j].segments.isEmpty())
			return;

		// also only process "gas" nodes - otherwise internal interface nodes
		// are given small volume resulting in high density islands
		if (isInternalPoint(i, j))
			return;

		int inside = 0;
		int good = 0;
		for (int d = 0; d < 1000; d++) {
			/* pick random position from [i-0.5,j-0.5] to [i+0.5,j+0.5] */
			double lc[] = { i + 0.5 * Starfish.rnd2(), j + 0.5 * Starfish.rnd2() };

			// is point inside the mesh?
			if (lc[0] < 0 || lc[1] < 0 || lc[0] >= ni - 1 || lc[1] >= nj - 1)
				continue;

			inside++;

			if (!isInternalPoint(lc))
				good++;
		}

		// scale node volume, but only on interface nodes (fully internal are left alone
		// so can visualize leaks)
		if (good > 0)
			node_vol.data[i][j] *= good / (double) (inside);

	}

	/*
	 * returns the two nodes making up edge number e, ordering is counter clockwise
	 * from "Right" (R->T->L->B)
	 */

	/**
	 *
	 * @param i
	 * @param j
	 * @param face
	 * @param first
	 * @return
	 */

	public double[] edge(double i, double j, Face face, boolean first) {
		double fi = 0, fj = 0; /* first node */
		double si = 0, sj = 0; /* second node */
		double ri, rj;

		switch (face) {
		case RIGHT:
			fi = i + 0.5;
			fj = j - 0.5;
			si = i + 0.5;
			sj = j + 0.5;
			break;
		case TOP:
			fi = i + 0.5;
			fj = j + 0.5;
			si = i - 0.5;
			sj = j + 0.5;
			break;
		case LEFT:
			fi = i - 0.5;
			fj = j + 0.5;
			si = i - 0.5;
			sj = j - 0.5;
			break;
		case BOTTOM:
			fi = i - 0.5;
			fj = j - 0.5;
			si = i + 0.5;
			sj = j - 0.5;
			break;
		default:
			throw new UnsupportedOperationException("Wrong edge in call to Edge");
		}

		if (first) {
			ri = fi;
			rj = fj;
		} else {
			ri = si;
			rj = sj;
		}

		if (ri < 0)
			ri = 0;
		else if (ri > ni - 1)
			ri = ni - 1;
		if (rj < 0)
			rj = 0;
		else if (rj >= nj - 1)
			rj = nj - 1;
		return pos(ri, rj);
	}

	/**
	 * returns i,j index of node offset by di,dj from im,jm if the offset brings it
	 * to a new node. Truncated original index will be returned if only a partial
	 * distance
	 * 
	 * @param im
	 * @param jm
	 * @param di
	 * @param dj
	 * @return
	 */
	public int[] NodeIndexOffset(double im, double jm, double di, double dj) {
		double i, j;
		double fi, fj;
		int ii[] = new int[2];

		i = im + di;
		j = jm + dj;

		/* is this a fractional index? if so, keep original */
		fi = i - (int) i;
		fj = j - (int) j;

		if (fi != 0)
			ii[0] = (int) im;
		else
			ii[0] = (int) i;

		if (fj != 0)
			ii[1] = (int) jm;
		else
			ii[1] = (int) j;

		/* make sure we are in bounds */
		if (ii[0] < 0 && boundaryType(Face.LEFT, Utils.minmax(ii[1], 0, nj - 1)) != DomainBoundaryType.MESH)
			ii[0] = 0;
		if (ii[1] < 0 && boundaryType(Face.BOTTOM, Utils.minmax(ii[0], 0, ni - 1)) != DomainBoundaryType.MESH)
			ii[1] = 0;
		if (ii[0] >= ni && boundaryType(Face.RIGHT, Utils.minmax(ii[1], 0, nj - 1)) != DomainBoundaryType.MESH)
			ii[0] = ni - 1;
		if (ii[1] >= nj && boundaryType(Face.TOP, Utils.minmax(ii[0], 0, ni - 1)) != DomainBoundaryType.MESH)
			ii[1] = nj - 1;

		return ii;
	}

	/**
	 * computes node cuts and performs flood fill
	 * 
	 * @param boundary_list
	 */
	public void setBoundaries(ArrayList<Boundary> boundary_list) {
		if (!boundary_list.isEmpty()) {
			setNodeControlVolumes(boundary_list);
			setInterfaceNodeLocation();
			performFloodFill();
		}
	}

	/**
	 * marks boundaries located in a volume centered about each node
	 * 
	 * @param boundary_list
	 */
	protected void setNodeControlVolumes(ArrayList<Boundary> boundary_list) {
		int i, j;

		/* set node control volumes */
		for (Boundary boundary : boundary_list) {
			for (Segment segment : boundary.getSegments()) {
				/* get spline range */
				double box[][] = segment.getBox();

				/* convert to logical coordinates */
				int lcm[] = XtoI(box[0]);
				int lcp[] = XtoI(box[1]);

				/* expand */
				lcm[0]--;
				lcm[1]--;

				lcp[0] += 2;
				lcp[1] += 2;

				/* make sure we are in domain */
				if (lcm[0] < 0)
					lcm[0] = 0;
				if (lcm[1] < 0)
					lcm[1] = 0;

				if (lcp[0] >= ni - 1)
					lcp[0] = ni - 1;
				if (lcp[1] >= nj - 1)
					lcp[1] = nj - 1;

				/*
				 * TODO: this algorithm will not detect a boudaries in neighbor meshes, need
				 * some post set "all reduce" operation or some way to grow the mesh into the
				 * neighbor one
				 */

				/* loop through all nodes and set volumes */
				for (j = lcm[1]; j <= lcp[1]; j++)
					for (i = lcm[0]; i <= lcp[0]; i++) {
						/*
						 * number of cells the box will grow in each direction, want this to be >1.0 so
						 * that we capture elements terminating at the cell boundary
						 */
						final double bsize = 1.01;

						/* node to bottom left */
						double i2 = i - bsize;
						double j2 = j - bsize;

						double ncv_m[] = pos(i2, j2);

						i2 = i + bsize;
						j2 = j + bsize;
						double ncv_p[] = pos(i2, j2);

						if (!segment.segmentInBox(ncv_m, ncv_p))
							continue;

						boolean found = false;

						/* see if we already have this boundary */
						for (Segment seg : node[i][j].segments) {
							if (seg.getBoundary() == boundary && seg.id() == segment.id()) {
								found = true;
								break;
							}
						}

						/* not found, add */
						if (!found) {
							if (node[i][j].segments == null)
								node[i][j].segments = new ArrayList<>();
							node[i][j].segments.add(segment);
						}

					} /* node loop */
			} /* segment */
		} /* boundary */
	}

	public boolean isInternalPoint(double i, double j) {
		double lc[] = { i, j };
		return isInternalPoint(lc);
	}

	/**
	 * return if point is located inside or outside a surface in interface cell
	 *
	 * @param lc
	 * @return
	 */

	public boolean isInternalPoint(double lc[]) {
		/*
		 * don't remember anymore how segments are set so check all four cell vertices
		 */
		int i1 = (int) lc[0];
		int j1 = (int) lc[1];

		int i2 = i1 + 1;
		int j2 = j1 + 1;
		if (i2 >= ni)
			i2 = ni - 1;
		if (j2 >= nj)
			j2 = nj - 1;

		double x[] = pos(lc);

		for (int i = i1; i <= i2; i++)
			for (int j = j1; j <= j2; j++) {
				ArrayList<Segment> blist = node[i][j].segments;
				if (blist.isEmpty())
					continue;

				Segment seg = Spline.visibleSegment(x, blist);
				if (seg == null)
					continue;

				return Spline.isInternal(x, seg);
			}
		return false;
	}

	/** uses boundaries located in a node control volume to set node locations */
	protected void setInterfaceNodeLocation() {
		int i, j;

		/* now loop through all nodes and set the ones with cuts */
		for (i = 0; i < ni; i++)
			for (j = 0; j < nj; j++) {
				ArrayList<Segment> blist = node[i][j].segments;
				if (blist.isEmpty())
					continue;

				Segment seg = Spline.visibleSegment(pos(i, j), blist);
				if (seg == null) {
					/* no internal Dirichlet splines */
					continue;
				}
						
				if (Spline.isInternal(pos(i, j), seg)) {
					node[i][j].type = NodeType.DIRICHLET;
					node[i][j].bc_value = seg.boundary.getValue();
				} else if (node[i][j].type == NodeType.UNKNOWN) // do not overwrite MESH nodes
				{
					node[i][j].type = NodeType.OPEN;
					node[i][j].bc_value = 0;
				}
			}
	}

	/**
	 *
	 */
	protected void performFloodFill() {
		int i, j;

		/*
		 * perform flood fill, set some maximum number of passes to avoid infinite loops
		 */
		int count = 0;
		for (int pass = 0; pass < 20 * ni * nj; pass++) {
			count = 0;

			for (i = 0; i < ni; i++)
				for (j = 0; j < nj; j++) {
					if (node[i][j].type != NodeType.UNKNOWN)
						continue;

					if (i > 0 && okToCopy(i - 1, j)) {
						node[i][j].type = node[i - 1][j].type;
						node[i][j].bc_value = node[i - 1][j].bc_value;
						count++;
					} else if (i < ni - 1 && okToCopy(i + 1, j)) {
						node[i][j].type = node[i + 1][j].type;
						node[i][j].bc_value = node[i + 1][j].bc_value;
						count++;
					} else if (j > 0 && okToCopy(i, j - 1)) {
						node[i][j].type = node[i][j - 1].type;
						node[i][j].bc_value = node[i][j - 1].bc_value;
						count++;
					} else if (j < nj - 1 && okToCopy(i, j + 1)) {
						node[i][j].type = node[i][j + 1].type;
						node[i][j].bc_value = node[i][j + 1].bc_value;
						count++;
					}
				} /* j */

			/* this indicates that we did not set any more nodes */
			if (count == 0)
				break;
		} /* pass */

		if (count > 0)
			throw (new RuntimeException("Failed to set all nodes"));
	}

	/**
	 * Check if mesh contains a point, inclusive of all boundaries
	 * 
	 * @param x
	 * @return
	 */
	public boolean containsPos(double x[]) {
		double lc[] = XtoL(x);
		if (lc[0] < -Constants.FLT_EPS || lc[1] < -Constants.FLT_EPS || lc[0] > (ni - 1 + Constants.FLT_EPS)
				|| lc[1] > (nj - 1 + Constants.FLT_EPS))
			return false;

		return true;
	}

	/**
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	protected boolean okToCopy(int i, int j) {
		if (node[i][j].type == NodeType.OPEN || node[i][j].type == NodeType.DIRICHLET)
			return true;

		return false;
	}

}
