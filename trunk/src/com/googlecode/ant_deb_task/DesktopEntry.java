package com.googlecode.ant_deb_task;

import org.apache.tools.ant.*;
import java.io.*;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class DesktopEntry extends Task
{
    private File _toFile;

    private Map _entries;

    public void setToFile(File toFile)
    {
        _toFile = toFile;
    }

    public void setType (String type)
    {
        _entries.put("Type", type);
    }

    public void setName(String name)
    {
        _entries.put("Name", name);
    }

    public void setGenericName(String genericName)
    {
        _entries.put("GenericName", genericName);
    }

    public void setNoDisplay(boolean noDisplay)
    {
        _entries.put("NoDisplay", noDisplay ? "true" : "false");
    }

    public void setComment(String comment)
    {
        _entries.put("Comment", comment);
    }

    public void setIcon(String icon)
    {
        _entries.put("Icon", icon);
    }

    public void setOnlyShowIn(String onlyShowIn)
    {
        _entries.put("OnlyShowIn", onlyShowIn);
    }

    public void setNotShowIn(String notShowIn)
    {
        _entries.put("NotShowIn", notShowIn);
    }

    public void setTryExec(String tryExec)
    {
        _entries.put("TryExec", tryExec);
    }

    public void setExec(String exec)
    {
        _entries.put("Exec", exec);
    }

    public void setPath(String path)
    {
        _entries.put("Path", path);
    }

    public void setTerminal(boolean terminal)
    {
        _entries.put("Terminal", terminal ? "true" : "false");
    }

    public void setMimeType(String mimeType)
    {
        _entries.put("MimeType", mimeType);
    }

    public void setCategories(String categories)
    {
        _entries.put("Categories", categories);
    }

    public void setUrl(String url)
    {
        _entries.put("Url", url);
    }

    public void init() throws BuildException
    {
        _entries = new LinkedHashMap();

        _entries.put("Version", "1.0");
        _entries.put("Type", "Application");
        _entries.put("Terminal", "false");
    }

    public void execute() throws BuildException
    {
        try
        {
            PrintWriter out = new PrintWriter(_toFile);

            out.println("[Desktop Entry]");

            Iterator keys = _entries.keySet().iterator();
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                String value = (String) _entries.get(key);

                out.print(key);
                out.print('=');
                out.println(value);
            }

            out.close();
        }
        catch (FileNotFoundException e)
        {
            throw new BuildException(e);
        }
    }
}

