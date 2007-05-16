package com.googlecode.ant_deb_task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.types.resources.FileResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.*;

public class Deb extends Task
{
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9+\\-.]+");
    
    public static class Description extends ProjectComponent
    {
        private String _synopsis;
        private String _extended = "";

        public String getSynopsis ()
        {
            return _synopsis;
        }

        public void setSynopsis (String synopsis)
        {
            _synopsis = synopsis.trim ();
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
    
    public static class Version extends ProjectComponent
    {
        private static final Pattern UPSTREAM_VERSION_PATTERN = Pattern.compile("[0-9][A-Za-z0-9+\\-.:]*");
        private static final Pattern DEBIAN_VERSION_PATTERN = Pattern.compile("[A-Za-z0-9+\\-]+");
        
        private int _epoch = 0;
        private String _upstream;
        private String _debian = "1";

        public void setEpoch(int epoch)
        {
            _epoch = epoch;
        }

        public void setUpstream(String upstream)
        {
            _upstream = upstream.trim ();

            if (!UPSTREAM_VERSION_PATTERN.matcher (_upstream).matches ())
                throw new BuildException("Invalid upstream version number!");
        }

        public void setDebian(String debian)
        {
            _debian = debian.trim ();

            if (!DEBIAN_VERSION_PATTERN.matcher (_debian).matches ())
                throw new BuildException("Invalid debian version number!");
        }

        public String toString()
        {
            StringBuffer version = new StringBuffer();
            
            if (_epoch > 0)
            {
                version.append(_epoch);
                version.append(':');
            }
            else if (_upstream.indexOf(':') > -1)
                throw new BuildException("Upstream version can contain colons only if epoch is specified!");
            
            version.append(_upstream);
            
            if (_debian.length() > 0)
            {
                version.append('-');
                version.append(_debian);
            }
            else if (_upstream.indexOf('-') > -1)
                throw new BuildException("Upstream version can contain hyphens only if debian version is specified!");
            
            return version.toString();
        }
    }

    public static class Maintainer extends ProjectComponent
    {
        private String _name;
        private String _email;

        public void setName (String name)
        {
            _name = name.trim ();
        }

        public void setEmail (String email)
        {
            _email = email.trim ();
        }

        public String toString()
        {
            if (_name == null || _name.length () == 0)
                return _email;

            StringBuffer buffer = new StringBuffer (_name);

            buffer.append (" <");
            buffer.append (_email);
            buffer.append (">");

            return buffer.toString ();
        }
    }
    public static class Section extends EnumeratedAttribute
    {
        private static final String[] PREFIXES = new String[] {"", "contrib/", "non-free/"};
        private static final String[] BASIC_SECTIONS = new String[] {"admin", "base", "comm", "devel", "doc", "editors", "electronics", "embedded", "games", "gnome", "graphics", "hamradio", "interpreters", "kde", "libs", "libdevel", "mail", "math", "misc", "net", "news", "oldlibs", "otherosfs", "perl", "python", "science", "shells", "sound", "tex", "text", "utils", "web", "x11"};

        private List sections = new ArrayList (PREFIXES.length * BASIC_SECTIONS.length);

        public Section ()
        {
            for (int i = 0; i < PREFIXES.length; i++)
            {
                String prefix = PREFIXES[i];

                for (int j = 0; j < BASIC_SECTIONS.length; j++)
                {
                    String basicSection = BASIC_SECTIONS[j];

                    sections.add (prefix + basicSection);
                }
            }
        }

        public String[] getValues ()
        {
            return (String[]) sections.toArray (new String[]{});
        }
    }
    
    public static class Priority extends EnumeratedAttribute
    {
        public String[] getValues ()    
        {
            return new String[] {"required", "important", "standard", "optional", "extra"};
        }
    }
    
    private File _toDir;

    private String _package;
    private String _version;
    private Deb.Version _versionObj;
    private String _section;
    private String _priority = "extra";
    private String _architecture = "all";
    private String _depends;
    private String _preDepends;
    private String _recommends;
    private String _suggests;
    private String _enhances;
    private String _conflicts;
    private String _maintainer;
    private Deb.Maintainer _maintainerObj;
    private Deb.Description _description;

    private List _data = new ArrayList();

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
        if (!PACKAGE_NAME_PATTERN.matcher(packageName).matches())
            throw new BuildException("Invalid package name!");
            
        _package = packageName;
    }

    public void setVersion (String version)
    {
        _version = version;
    }

    public void setSection (Section section)
    {
        _section = section.getValue();
    }

    public void setPriority (Priority priority)
    {
        _priority = priority.getValue();
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

    public void setSuggests (String suggests)
    {
        _suggests = suggests;
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
        _data.add(resourceCollection);
    }

    public void addVersion(Deb.Version version)
    {
        _versionObj = version;
    }

    public void addMaintainer(Deb.Maintainer maintainer)
    {
        _maintainerObj = maintainer;
    }

    private void writeControlFile (File controlFile, long installedSize) throws FileNotFoundException
    {
        log ("Generating control file to: " + controlFile.getAbsolutePath (), Project.MSG_VERBOSE);

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

        if (_suggests != null)
        {
            control.print ("Suggests: ");
            control.println (_suggests);
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
            if (_versionObj != null)
                _version = _versionObj.toString ();

            if (_maintainerObj != null)
                _maintainer = _maintainerObj.toString ();

            _tempFolder = createTempFolder();
            
            File debFile = new File (_toDir, _package + "_" + _version + "_" + _architecture + ".deb");

            File dataFile = createDataFile ();

            File masterControlFile = createMasterControlFile ();

            log ("Writing deb file to: " + debFile.getAbsolutePath());
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
        Iterator filesets = _data.iterator();
        while (filesets.hasNext())
            dataTar.add ((TarFileSet) filesets.next());

        dataTar.execute ();

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

        Iterator filesets = _data.iterator();
        while (filesets.hasNext())
        {
            TarFileSet fileset = (TarFileSet) filesets.next();
            
            Iterator resouces = fileset.iterator ();
            while (resouces.hasNext ())
            {
                FileResource resource = (FileResource) resouces.next ();
                File file = resource.getFile ();
                
                total += file.length ();
            }
        }

        return total;
    }

    private void scanData()
    {
        Iterator filesets = _data.iterator();
        while (filesets.hasNext())
        {
            TarFileSet fileset = (TarFileSet) filesets.next();

            String fullPath = fileset.getFullpath (getProject ());
            String prefix = fileset.getPrefix (getProject ());

            if (!prefix.endsWith ("/"))
                prefix += '/';

            Iterator resouces = fileset.iterator ();
            while (resouces.hasNext ())
            {
                FileResource resource = (FileResource) resouces.next ();
                String targetName;

                if (fullPath.length () > 0)
                    targetName = fullPath;
                else
                    targetName = prefix + resource.getName ();

                if (resource.isDirectory ())
                {

                }
                else
                {
                    File file = resource.getFile ();

                    // todo:
                    // calculate installed size, just as in previous method, but save to instace var
                    // calculate and collect md5 sums
                    // get target folder names, and collect them (to be added to _data)
                }
            }
        }
    }
}
