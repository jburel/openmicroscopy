/*
 * org.openmicroscopy.shoola.agents.measurement.util.ColorCellRenderer 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.measurement.util;


//Java imports
import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;

//Third-party libraries

//Application-internal dependencies
import org.jdesktop.swingx.JXTree;
import org.openmicroscopy.shoola.agents.measurement.IconManager;
import org.openmicroscopy.shoola.agents.measurement.view.ROITable;
import org.openmicroscopy.shoola.util.roi.figures.ShapeTypes;
import org.openmicroscopy.shoola.util.roi.model.ROI;
import org.openmicroscopy.shoola.util.roi.model.ROIShape;
import org.openmicroscopy.shoola.util.roi.model.util.FigureType;
import org.openmicroscopy.shoola.util.ui.PaintPot;
import org.openmicroscopy.shoola.util.ui.drawingtools.figures.FigureUtil;

/** 
 * Basic cell renderer displaying color in a cell.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class ROITableCellRenderer 
	extends JXTree 
	implements TreeCellRenderer, TableCellRenderer
{

	/**
	 * Creates a new instance. Sets the opacity of the label to 
	 * <code>true</code>.
	 */
	public ROITableCellRenderer()
	{
		setOpaque(true);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent
	 * (javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, 
	 * int, boolean)
	 */
	public Component getTreeCellRendererComponent(JTree tree, 
			Object value, boolean selected, boolean expanded, 
			boolean leaf, int row, boolean hasFocus)
	{
		Component thisComponent = new JLabel();
		Object thisObject = ((ROINode)value).getUserObject();
		if ( (thisObject instanceof Double) || (thisObject instanceof String) ||
			 (thisObject instanceof FigureType) || thisObject instanceof Integer || 
			 thisObject instanceof Long)
		{
			JLabel label = new JLabel();
			label.setOpaque(true);
    		label.setText(thisObject+"");
    		thisComponent = label;
    	} 
		else if (thisObject instanceof Color) 
		{
    		PaintPot paintPot = new PaintPot((Color)thisObject);
    		thisComponent = paintPot;
    	}
    	else if( thisObject instanceof Boolean)
    	{
    		JCheckBox checkBox = new JCheckBox();
    		checkBox.setSelected((Boolean)thisObject);
    		thisComponent = checkBox;
    	}
    	else if( thisObject instanceof ROI)
    	{
    		JLabel label = new JLabel();
    		label.setIcon(IconManager.getInstance().getIcon(IconManager.ROISTACK));
    		thisComponent = label;
    	}
    	else if( thisObject instanceof ROIShape)
    	{
    		JLabel label = new JLabel();
    		label.setIcon(IconManager.getInstance().getIcon(IconManager.ROISHAPE));
    		thisComponent = label;    	
    	}
    	if (!(value instanceof Color)) 
			RendererUtils.setRowColor(thisComponent, selected, row);
		
		return thisComponent;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(
	 * javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column)
	{
		Component thisComponent = new JLabel();
		Object thisObject = value;
		if ( (thisObject instanceof Double) || (thisObject instanceof String) ||
			 (thisObject instanceof FigureType) || thisObject instanceof Integer || 
			 thisObject instanceof Long)
		{
			JLabel label = new JLabel();
			label.setOpaque(true);
			if( ((ROITable)table).isShapeTypeColumn(column))
			{
				makeShapeIcon(label, (String)thisObject);
			}
			else
				label.setText(thisObject+"");
			thisComponent = label;
    	} 
		else if (thisObject instanceof Color) {
    		PaintPot paintPot = new PaintPot((Color)thisObject);
    		thisComponent = paintPot;
    	}
    	else if( thisObject instanceof Boolean)
    	{
    		JCheckBox checkBox = new JCheckBox();
    		checkBox.setSelected((Boolean)thisObject);
    		thisComponent = checkBox;
    	}
    	else if( thisObject == null)
    	{
    		JLabel label = new JLabel();
    		thisComponent = label;
    	}

		if (!(value instanceof Color)) {
			int[] selectionRows = table.getSelectedRows();
			for(int i = 0 ; i < selectionRows.length ; i++)
			
				RendererUtils.setRowColor(thisComponent, selectionRows[i], 
									row);
		}
	 
		return thisComponent;
	}
		
	/**
	 * is the str representing a shapes object.
	 * @param str see above.
	 * @return see above.
	 */
	private boolean isShape(String str)
	{
		for(int i = 0 ; i < ShapeTypes.shapeList.size() ; i++)
			if(ShapeTypes.shapeList.get(i).equals(str))
				return true;
		return false;
	}
	
	/**
	 * Add the approriate shape icon to the label.
	 * @param label see above.
	 * @param shape above.
	 */
	private void makeShapeIcon(JLabel label, String shape)
	{
		if(shape.equals(FigureUtil.SCRIBBLE_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.SCRIBBLE16));
		if(shape.equals(FigureUtil.LINE_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.LINE16));
		if(shape.equals(FigureUtil.LINE_CONNECTION_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.CONNECTION16));
		if(shape.equals(FigureUtil.POLYGON_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.POLYGON16));
		if(shape.equals(FigureUtil.POINT_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.POINT16));
		if(shape.equals(FigureUtil.RECTANGLE_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.RECTANGLE16));
		if(shape.equals(FigureUtil.ELLIPSE_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.ELLIPSE16));
		if(shape.equals(FigureUtil.TEXT_TYPE))
			label.setIcon(IconManager.getInstance().getIcon(IconManager.TEXT16));
	}
}


