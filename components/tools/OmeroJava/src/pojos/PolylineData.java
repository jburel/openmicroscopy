/*
 * pojos.PolygonData
 *
Ê*------------------------------------------------------------------------------
 * Copyright (C) 2006-2009 University of Dundee. All rights reserved.
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package pojos;

//Java imports
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.awt.geom.Point2D;

//Third-party libraries

//Application-internal dependencies
import omero.RDouble;
import omero.RString;
import omero.rtypes;
import omero.model.Polygon;
import omero.model.PolygonI;
import omero.model.PolylineI;
import omero.model.Shape;
import omero.model.Polyline;

/**
 * Represents an Polyline shape in the Euclidean space <b>R</b><sup>2</sup>.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class PolylineData 
	extends ShapeData
{

	/** Regex for a data in block. */
	private static final String NUMREGEX = "\\[.*\\]";

	/** The points in the polyline as list. */
	List<Point2D> points;

	/** The points in the polyline as list. */
	List<Point2D> points1;

	/** The points in the polyline as list. */
	List<Point2D> points2;
	
	/** The points in the polyline as list. */
	List<Integer> mask;

	
	/**
	 * Creates a new instance.
	 * 
	 * @param shape The shape this object represents.
	 */
	public PolylineData(Shape shape)
	{
		super(shape);
		parseShapeStringToPointsList();
	}
	
	/**
	 * Create a new instance of polyline, creating a new PolylineI Object.
	 */
	public PolylineData()
	{
		this(new ArrayList<Point2D>(),new ArrayList<Point2D>(),
				new ArrayList<Point2D>(), new ArrayList<Integer>());
	}
	
	/**
	 * Create a new instance of the PolylineData, set the points in the polyline.
	 * @param points See Above.
	 */
	public PolylineData(List<Point2D> points, List<Point2D> points1, 
			List<Point2D> points2, List<Integer> maskList)
	{
		Polygon shape = new PolygonI();
		setValue(shape);
		setShapeSettings(shape);
		setPoints(points, points1, points2, maskList);
	}
		
	/**
	 * Returns the text of the shape.
	 * 
	 * @return See above.
	 */
	public String getText()
	{
		Polyline shape = (Polyline) asIObject();
		RString value = shape.getTextValue();
		if (value == null) return "";
		return value.getValue();
	}
	
	/**
	 * Sets the text of the shape.
	 * 
	 * @param text See above.
	 */
	public void setText(String text)
	{
		if(isReadOnly())
			throw new IllegalArgumentException("Shape ReadOnly");
		Polyline shape = (Polyline) asIObject();
		if (shape == null) 
			throw new IllegalArgumentException("No shape specified.");
		shape.setTextValue(rtypes.rstring(text));
	}

	/**
	 * Returns the points in the Polyline.
	 * 
	 * @return See above.
	 */
	public List<Point2D> getPoints()
	{
		String pts =  fromPoints("points");
		return parsePointsToPoint2DList(pts);
	}
	
	/**
	 * Returns the points in the Polyline.
	 * 
	 * @return See above.
	 */
	public List<Point2D> getPoints1()
	{
		String pts =  fromPoints("points1");
		return parsePointsToPoint2DList(pts);
	}

	/**
	 * Returns the points in the Polyline.
	 * 
	 * @return See above.
	 */
	public List<Point2D> getPoints2()
	{
		String pts = fromPoints("points2");
		return parsePointsToPoint2DList(pts);
	}
	
	/**
	 * Returns the points in the Polyline.
	 * 
	 * @return See above.
	 */
	public List<Integer> getMaskPoints()
	{
		String pts =  fromPoints("mask");
		return parsePointsToIntegerList(pts);
	}
	
	/**
	 * Set the points in the polyline.
	 * 
	 * @param points See above.
	 */
	public void setPoints(List<Point2D> points, List<Point2D> points1, 
			List<Point2D> points2, List<Integer> maskList)
	{
		if(isReadOnly())
			throw new IllegalArgumentException("Shape ReadOnly");
		Polygon shape = (Polygon) asIObject();
		if (shape == null) 
			throw new IllegalArgumentException("No shape specified.");
		
		String pointsValues=
			toPoints(points.toArray(new Point2D.Double[points.size()]));
		String points1Values=
			toPoints(points1.toArray(new Point2D.Double[points1.size()]));
		String points2Values=
			toPoints(points2.toArray(new Point2D.Double[points2.size()]));
		String maskValues = "";
		for( int i = 0 ; i < maskList.size()-1; i++)
			maskValues = maskValues + maskList.get(i)+",";
		maskValues = maskValues+maskList.get(maskList.size()-1)+"";
		String pts = "points["+pointsValues+"] ";
		pts = pts + "points1["+points1Values+"] ";
		pts = pts + "points2["+points2Values+"] ";
		pts = pts + "mask["+maskValues+"] ";
		
		shape.setPoints(rtypes.rstring(pts));
	}
	
	/**
	 * Parse out the type from the points string.
	 * @param type The value in the list to parse.
	 * @return See above.
	 */
	private String fromPoints(String type)
	{
		Polygon shape = (Polygon) asIObject();
		if (shape == null) 
			throw new IllegalArgumentException("No shape specified.");
		String pts = shape.getPoints().getValue();
		if(pts.length()==0)
			return "";
		String exp = type+NUMREGEX;
		String[] match = pts.split(exp);
		if(match.length!=1)
			return "";
		String list = match[0].substring(match[0].indexOf("["),
								match[0].indexOf("["));
		return list;
	}
	
	
	/** 
	* Parse the points list from the string to a list of point2d objects.
	* @param str the string to convert to points.
	*/
	private List<Point2D> parsePointsToPoint2DList(String str)
	{
		List<Point2D> points = new ArrayList<Point2D>();
	
		StringTokenizer tt=new StringTokenizer(str, " ,");
		int numTokens = tt.countTokens()/2;
		for (int i=0; i< numTokens; i++)
			points.add(
					new Point2D.Double(new Double(tt.nextToken()), new Double(
						tt.nextToken())));
		return points;
	}
	
	/** 
	* Parse the points list from the string to a list of integer objects.
	* @param str the string to convert to points.
	*/
	private List<Integer> parsePointsToIntegerList(String str)
	{
		List<Integer> points = new ArrayList<Integer>();
	
		StringTokenizer tt=new StringTokenizer(str, " ,");
		points.add(new Integer(tt.nextToken()));
		return points;
	}
	
	/**
	 * Returns a Point2D.Double array as a Points attribute value. as specified
	 * in http://www.w3.org/TR/SVGMobile12/shapes.html#PointsBNF
	 */
	private static String toPoints(Point2D.Double[] points)
	{
		StringBuilder buf=new StringBuilder();
		for (int i=0; i<points.length; i++)
		{
			if (i!=0)
			{
				buf.append(", ");
			}
			buf.append(toNumber(points[i].x));
			buf.append(',');
			buf.append(toNumber(points[i].y));
		}
		return buf.toString();
	}
	
	private void parseShapeStringToPointsList()
	{
		points = getPoints();
		points1 = getPoints();
		points2 = getPoints();
		mask = getMaskPoints();
	}
	
	/**
	 * Returns a double array as a number attribute value.
	 */
	private static String toNumber(double number)
	{
		String str=Double.toString(number);
		if (str.endsWith(".0"))
		{
			str=str.substring(0, str.length()-2);
		}
		return str;
	}

}
