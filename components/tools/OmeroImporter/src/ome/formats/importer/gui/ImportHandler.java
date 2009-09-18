/*
 * ome.formats.importer.gui.ImportHandler
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *------------------------------------------------------------------------------
 */

package ome.formats.importer.gui;

import static omero.rtypes.rlong;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

import loci.formats.FormatException;
import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.model.InstanceProvider;
import omero.ResourceError;
import omero.model.IObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Importer is master file format importer for all supported formats and imports
 * the files to an OMERO database
 * 
 * @author Brian Loranger brain at lifesci.dundee.ac.uk
 * @basedOnCodeFrom Curtis Rueden ctrueden at wisc.edu
 */
public class ImportHandler
{

    private ImportConfig     config;
    private ImportLibrary    library;
    private ImportCandidates candidates;

    private GuiImporter      viewer;
    private static boolean   runState = false;
    private Thread runThread;
    HistoryDB db = null;

    
    //private ProgressMonitor monitor;
    
    private FileQueueTable  qTable;

    private static Log      log = LogFactory.getLog(ImportHandler.class);
    
    private int numOfPendings = 0;
    private int numOfDone = 0;
    
    private String[] files = null;
    private File selected_file = null;
    protected boolean errorsCollected = false;

    public ImportHandler(GuiImporter viewer, FileQueueTable qTable,
            ImportConfig config,
            ImportLibrary library, 
            ImportCandidates candidates)
    {
        this.config = config;
        this.library = library;
        this.candidates = candidates;

        db = HistoryDB.getHistoryDB();
        
        if (runState == true)
        {
            log.error("ImportHandler running twice");
            if (runThread != null) log.error(runThread);
            throw new RuntimeException("ImportHandler running twice");
        }
        runState = true;
        try {
            this.viewer = viewer;
            this.qTable = qTable;
            this.library = library;
            library.addObserver(qTable);
            library.addObserver(viewer);
                       
            runThread = new Thread()
            {

                public void run()
                {
                    importImages();
                }
            };
            runThread.start();
        }
        finally {
            runState = false;
        }
}

    /**
     * Begin the import process, importing first the meta data, and then each
     * plane of the binary image data.
     */
    private void importImages()
    {
        long timestampIn;
        long timestampOut;
        long timestampDiff;
        long timeInSeconds;
        long hours, minutes, seconds;
        Date date = null;

        // record initial timestamp and record total running time for the import
        timestampIn = System.currentTimeMillis();
        date = new Date(timestampIn);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String myDate = formatter.format(date);

        viewer.appendToOutputLn("> Starting import at: " + myDate + "\n");
        viewer.statusBar.setStatusIcon("gfx/import_icon_16.png", "Now importing.");
        
        qTable.importBtn.setText("Cancel");
        qTable.importing = true;
        
        numOfPendings = 0;
        int importKey = 0;
        int importStatus = 0;
        
        try
        {
            if (db != null)
            {
                db.insertImportHistory(library.getExperimenterID(), "pending");
                importKey = db.getLastKey();
            }
        }
        catch (SQLException e)
        {  
        	log.error("SQL exception updating history DB.", e);
        }
        
        library.addObserver(new IObserver(){
            public void update(IObservable importLibrary, ImportEvent event) {
                
            }});
        
        library.importCandidates(config, candidates);
        
        for(int i = 0; i < importContainer.length; i++)
        {   
            selected_file = importContainer[i].file;
           	if (qTable.setProgressPending(i))
           	{
                numOfPendings++;
               	try {
               	    if (db != null)
               	    {
               	    	// FIXME: This is now "broken" with targets now able to be of type Screen or Dataset.
               	        db.insertFileHistory(importKey, library.getExperimenterID(), i, importContainer[i].imageName, 
               	         importContainer[i].projectID, importContainer[i].getTarget().getId().getValue(), 
               	         "pending", importContainer[i].file);
               	    }
               	}
               	catch (Exception e) 
               	{
               		log.error("Generic error while updating progress.", e);
               	}
           	}
        }
        
        if (db != null)
            db.notifyObservers("QUICKBAR_UPDATE", null);
        viewer.statusBar.setProgressMaximum(numOfPendings);
        
        numOfDone = 0;
        for (int j = 0; j < importContainer.length; j++)
        {
        	ImportContainer container = importContainer[j];
            if (qTable.table.getValueAt(j, 2).equals("pending") 
                    && qTable.cancel == false)
            {
                numOfDone++;
                String filename = container.file.getAbsolutePath();
                
                viewer.appendToOutputLn("> [" + j + "] Importing \"" + filename
                        + "\"");
                
                IObject target = container.getTarget();
                
                try
                {
                	library.importImage(importContainer[j].file, j,
                			    numOfDone, numOfPendings,
                			    importContainer[j].getUserSpecifiedName(),
                			    null,  // Description
                			    container.archive,
                			    true,  // Metadata file creation
                			           // (TODO: Enable in container and UI)
                			    container.userPixels, target);
                    try
                    {
                        if (db != null)
                            db.updateFileStatus(importKey, j, "done");
                    }
                    catch (SQLException e)
                    {
                    	log.error("SQL exception updating history DB.", e);
                    }

                }
                catch (FormatException fe)
                {
                    System.err.println(fe.getMessage());
                	log.error("Format exception while importing image.", fe);
                	qTable.setProgressFailed(j);
                    viewer.appendToOutputLn("> [" + j + "] Failure importing.");
                    if (importStatus < 0)   importStatus = -3;
                    else                    importStatus = -1;
                    
                    if (fe.getMessage() == "Cannot locate JPEG decoder")
                    {
                        viewer.appendToOutputLn("> [" + j + "] Lossless JPEG not supported.");
                                /*
                                See " +
                                "http://trac.openmicroscopy.org.uk/omero/wiki/LosslessJPEG for " +
                                "details on this error.");
                                */
                        JOptionPane.showMessageDialog(
                                viewer,
                                "\nThe importer cannot import the lossless JPEG images used by the file" +
                                "\n" + importContainer[j].imageName + "");
                                /*		"
                                "\n\nThere maybe be a native library available for your operating system" +
                                "\nthat will support this format. For details on this error, check:" +
                                "\nhttp://trac.openmicroscopy.org.uk/omero/wiki/LosslessJPEG");
                                */
                    }
                    else
                    {
                        String[] files = {importContainer[j].file.getAbsolutePath()};
                        addError(fe, importContainer[j].file, files, null);
                    }
                    
                    try
                    {
                        if (db != null)
                        {
                            db.updateImportStatus(importKey, "incomplete");
                            db.updateFileStatus(importKey, j, "failed");
                        }
                    }
                    catch (SQLException e)
                    {
                    	log.error("SQL exception updating history DB.", e);
                    }
                }
                catch (IOException ioe)
                {
                	log.error("I/O error while importing image.", ioe);
                    qTable.setProgressUnknown(j);
                    viewer.appendToOutputLn("> [" + j + "] Failure importing.");
                    if (importStatus < 0)   importStatus = -3;
                    else                    importStatus = -1;
                    
                    JOptionPane.showMessageDialog(
                            viewer,
                            "\nThe importer has encountered an error while attempting\n" +
                            "to read data from the hard drive. This could indicate a\n" +
                            "problem with a remote drive being inaccessable.\n\n" +
                            " The file in question is: " +
                            "\n" + importContainer[j].imageName + "");
                    try
                    {
                        if (db != null)
                        {
                            db.updateImportStatus(importKey, "incomplete");
                            db.updateFileStatus(importKey, j, "failed");
                        }
                    }
                    catch (SQLException e)
                    {
                    	log.error("SQL exception updating history DB.", e);
                    }
                }
                catch (ResourceError e)
                {
                	log.error("Resource error while importing image.", e);
                    JOptionPane.showMessageDialog(
                            viewer,
                            "The server is out of space and imports cannot continue.\n");
                    qTable.setProgressFailed(j);
                    if (importStatus < 0)   importStatus = -3;
                    else                    importStatus = -2;
                    qTable.cancel = true;
                    qTable.abort = true;
                    qTable.importing = false;
                    try
                    {
                        if (db != null)
                        {
                            db.updateImportStatus(importKey, "incomplete");
                            db.updateFileStatus(importKey, j, "failed");
                        }
                    }
                    catch (SQLException sqle)
                    {
                    	log.error("SQL exception updating history DB.", sqle);
                    }
                    
                }
                catch (Exception error)
                {
                    log.error("Generic error while importing image.", error);
                    qTable.setProgressFailed(j);
                    viewer.appendToOutputLn("> [" + j + "] Failure importing.");
                    
                    files = library.getUsedFiles();
                    String readerType = store.getReaderType();
                    
                    addError(error, importContainer[j].file, files, readerType);
                    
                    if (importStatus < 0)   importStatus = -3;
                    else                    importStatus = -2;
                    
                    try
                    {
                        if (db != null)
                        {
                            db.updateImportStatus(importKey, "incomplete");
                            db.updateFileStatus(importKey, j, "failed");
                        }
                    }
                    catch (SQLException sqle)
                    {
                    	log.error("SQL exception updating history DB.", sqle);
                    }
                }
            }
        }
        qTable.importBtn.setText("Import"); 
        qTable.importBtn.setEnabled(true);
        qTable.queue.setRowSelectionAllowed(true);
        qTable.removeBtn.setEnabled(true);
        if (qTable.failedFiles == true) 
            qTable.clearFailedBtn.setEnabled(true);
        if (qTable.doneFiles == true) 
            qTable.clearDoneBtn.setEnabled(true);
        qTable.importing = false;
        qTable.cancel = false;
        qTable.abort = false;
        
        viewer.statusBar.setProgress(false, 0, "");
        viewer.statusBar.setStatusIcon("gfx/import_done_16.png", "Import complete.");
        if (importStatus >= 0) try
        {
            if (db != null)
                db.updateImportStatus(importKey, "complete");
        } catch (SQLException e)
        {
        	log.error("SQL exception when updating import status.", e);
        }

        if (errorsCollected)
        {
            JOptionPane.showMessageDialog(
                    viewer,
                    "\nYour import has produced one or more errors, " +
                    "\nvisit the 'Import Errors' tab for details.", 
                    "Errors in import!", 
                    JOptionPane.WARNING_MESSAGE);
        }
        
        timestampOut = System.currentTimeMillis();
        timestampDiff = timestampOut - timestampIn;

        // calculate hour/min/sec time for the run
        timeInSeconds = timestampDiff / 1000;
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        seconds = timeInSeconds;

        viewer.appendToOutputLn("> Total import time: " + hours + " hour(s), "
                + minutes + " minute(s), " + seconds + " second(s).");

        viewer.appendToOutputLn("> Image import completed!");
    }
    


    /**
     * Instantiates an unloaded target object to feed to the import library for
     * usage during the import.
     * @param klass Target object class.
     * @param id Target object ID.
     * @return Target object instance.
     */
    private <T extends IObject> T instantiateTarget(Class<T> klass, long id)
    {
    	InstanceProvider provider = library.getInstanceProvider();
    	T o = provider.getInstance(klass);
    	o.setId(rlong(id));
    	o.unload();
    	return o;
    }
}
