/*
 * org.openmicroscopy.shoola.env.event.ChainExecutionsLoadedEvent
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.events;

//Java imports
import java.util.HashMap;
//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.env.event.AgentEvent;

/** 
 * 
  * 
 * An event indicating that the chain executions have been loaded. Source is set
 * to be a hash containing lists of executions, indexed by dataset id.
 *
 * @author  Harry Hochheiser &nbsp;&nbsp;&nbsp;&nbsp;
 *              <a href="mailto:hsh@nih.gov">hsh@nih.gov</a>
 * 
 * @version 2.2 
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */

public class ChainExecutionsLoadedEvent extends AgentEvent
{
    private HashMap executionsByChain;
    private HashMap executionsByID;
   
    
    public ChainExecutionsLoadedEvent(HashMap executionsByDataset,
    			HashMap executionsByChain,HashMap executionsByID) {
   		super();
   		setSource(executionsByDataset);
   		this.executionsByChain = executionsByChain;
   		this.executionsByID = executionsByID;
    }
 
    public HashMap getChainExecutionsByDatasetID() {
    	    return (HashMap) getSource();
    }
    
    public HashMap getChainExecutionsByChainID() {
    		return executionsByChain;
    }
    
    public HashMap getExecutionsByID() {
    		return executionsByID;
    }
}


