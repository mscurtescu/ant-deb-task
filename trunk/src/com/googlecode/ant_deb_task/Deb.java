package com.googlecode.ant_deb_task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.resources.FileResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

public class Deb extends Task
{
    public static class Description extends Task
    {
        private String _synopsis;
        private String _extended = "";

        public String getSynopsis ()
        {
            return _synopsis;
        }

        public void setSynopsis (String synopsis)
        {
            _synopsis = synopsis;
        }

        public void addText (String text)
        {
            _extended += text;
        }

        public String getExtended ()
        {
            return _extended;
        }

        public String getExtendedFormatted ()
        {
            StringBuffer buffer = new StringBuffer (_extended.length ());

            String lines[] = _extended.split ("\n");

            int start = 0;

            for (int i = 0; i < lines.length; i++)
            {
                String line = lines[i].trim ();

                if (line.length () > 0)
                    break;

                start++;
            }

            int end = lines.length;

            for (int i = lines.length - 1; i >= 0; i--)
            {
                String line = lines[i].trim ();

                if (line.length () > 0)
                    break;

                end--;
            }

            for (int i = start; i < end; i++)
            {
                String line = lines[i].trim ();

                buffer.append (' ');
                buffer.append (line.length () == 0 ? "." : line);
                buffer.append ('\n');
            }

            buffer.deleteCharAt (buffer.length () - 1);

            return buffer.toString ();
        }
    }

    private File _toDir;

    private String _package;
    private String _version;
    private String _section;
    private String _priority = "extra";
    private String _architecture = "all";
    private String _depends;
    private String _preDepends;
    private String _recommends;
    private String _suggest;
    private String _enhances;
    private String _conflicts;
    private String _maintainer;
    private Deb.Description _description;

    private TarFileSet _data;

    private File _tempFolder;

    private static final Tar.TarCompressionMethod GZIP_COMPRESSION_METHOD = new Tar.TarCompressionMethod ();

    static
    {
        GZIP_COMPRESSION_METHOD.setValue ("gzip");
    }

    public void setToDir (File toDir)
    {
        _toDir = toDir;
    }

    public void setPackage (String packageName)
    {
        _package = packageName;
    }

    public void setVersion (String version)
    {
        _version = version;
    }

    public void setSection (String section)
    {
        _section = section;
    }

    public void setPriority (String priority)
    {
        _priority = priority;
    }

    public void setArchitecture (String architecture)
    {
        _architecture = architecture;
    }

    public void setDepends (String depends)
    {
        _depends = depends;
    }

    public void setPreDepends (String preDepends)
    {
        _preDepends = preDepends;
    }

    public void setRecommends (String recommends)
    {
        _recommends = recommends;
    }

    public void setSuggest (String suggest)
    {
        _suggest = suggest;
    }

    public void setEnhances (String enhances)
    {
        _enhances = enhances;
    }

    public void setConflicts (String conflicts)
    {
        _conflicts = conflicts;
    }

    public void setMaintainer (String maintainer)
    {
        _maintainer = maintainer;
    }

    public void addDescription (Deb.Description description)
    {
        _description = description;
    }

    public void add (TarFileSet resourceCollection)
    {
        _data = resourceCollection;
    }

    private void writeControlFile (File controlFile, long installedSize) throws FileNotFoundException
    {
        log ("Generating control file to: " + controlFile.getAbsolutePath ());

        PrintWriter control = new PrintWriter (controlFile);

        control.print ("Package: ");
        control.println (_package);

        control.print ("Version: ");
        control.println (_version);

        if (_section != null)
        {
            control.print ("Section: ");
            control.println (_section);
        }

        if (_priority != null)
        {
            control.print ("Priority: ");
            control.println (_priority);
        }

        control.print ("Architecture: ");
        control.println (_architecture);

        if (_depends != null)
        {
            control.print ("Depends: ");
            control.println (_depends);
        }

        if (_preDepends != null)
        {
            control.print ("Pre-Depends: ");
            control.println (_preDepends);
        }

        if (_recommends != null)
        {
            control.print ("Recommends: ");
            control.println (_recommends);
        }

        if (_suggest != null)
        {
            control.print ("Suggests: ");
            control.println (_suggest);
        }

        if (_enhances != null)
        {
            control.print ("Enhances: ");
            control.println (_enhances);
        }

        if (_conflicts != null)
        {
            control.print ("Conflicts: ");
            control.println (_conflicts);
        }

        if (installedSize > 0)
        {
            control.print ("Installed-Size: ");
            control.println (installedSize / 1024L);
        }

        control.print ("Maintainer: ");
        control.println (_maintainer);

        control.print ("Description: ");
        control.println (_description.getSynopsis ());
        control.println (_description.getExtendedFormatted ());

        control.close ();
    }

    private File createMasterControlFile () throws IOException
    {
        File controlFile = new File (_tempFolder, "control");

        writeControlFile (controlFile, getInstalledSize ());

        File masterControlFile = new File (_tempFolder, "control.tar.gz");

        Tar controlTar = new Tar ();
        controlTar.setProject (getProject ());
        controlTar.setTaskName (getTaskName ());
        controlTar.setDestFile (masterControlFile);
        controlTar.setCompression (GZIP_COMPRESSION_METHOD);

        addFileToTar (controlTar, controlFile, "control");

        controlTar.perform ();

        controlFile.delete ();

        return masterControlFile;
    }

    private void addFileToTar(Tar tar, File file, String fullpath)
    {
        TarFileSet controlFileSet = tar.createTarFileSet ();

        controlFileSet.setFile (file);
        controlFileSet.setFullpath (fullpath);
    }

    public void execute () throws BuildException
    {
        try
        {
            _tempFolder = createTempFolder();
            
            File debFile = new File (_toDir, _package + "_" + _version + "_" + _architecture + ".deb");

            File dataFile = createDataFile ();

            File masterControlFile = createMasterControlFile ();

            BuildDeb.buildDeb (debFile, masterControlFile, dataFile);

            masterControlFile.delete ();
            dataFile.delete ();
        }
        catch (IOException e)
        {
            throw new BuildException (e);
        }
    }

    private File createDataFile () throws IOException
    {
        File dataFile = new File (_tempFolder, "data.tar.gz");

        Tar dataTar = new Tar ();
        dataTar.setProject (getProject ());
        dataTar.setTaskName (getTaskName ());
        dataTar.setDestFile (dataFile);
        dataTar.setCompression (GZIP_COMPRESSION_METHOD);
        dataTar.add (_data);

        dataTar.perform ();

        return dataFile;
    }

    private File createTempFolder() throws IOException
    {
        File tempFile = File.createTempFile ("deb", ".dir");
        String tempFolderName = tempFile.getAbsolutePath ();
        tempFile.delete ();

        tempFile = new File (tempFolderName, "removeme");
        tempFile.mkdirs ();
        tempFile.delete ();

        log ("Temp folder: " + tempFolderName, Project.MSG_VERBOSE);
        
        return new File (tempFolderName);
    }

    private long getInstalledSize()
    {
        long total = 0;

        Iterator resouces = _data.iterator ();
        while (resouces.hasNext ())
        {
            FileResource resource = (FileResource) resouces.next ();
            File file = resource.getFile ();
            
            total += file.length ();
        }

        return total;
    }
}
