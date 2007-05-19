package com.googlecode.ant_deb_task;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.*;
import java.util.*;

public class DesktopEntry extends Task
{
    private File _toFile;

    private Map _entries;
    private List _localizedEntries = new ArrayList ();

    public static class LocalizedEntry
    {
        private String _key;
        private String _lang;
        private String _country;
        private String _encoding;
        private String _modifier;
        private String _value;

        public LocalizedEntry (String key)
        {
            _key = key;
        }

        public void setLang (String lang)
        {
            _lang = lang;
        }

        public void setCountry (String country)
        {
            _country = country;
        }

        public void setEncoding (String encoding)
        {
            _encoding = encoding;
        }

        public void setModifier (String modifier)
        {
            _modifier = modifier;
        }

        public void setValue (String value)
        {
            _value = value;
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer (_key);

            if (_lang != null)
            {
                buffer.append ('[');

                buffer.append (_lang.toLowerCase ());

                if (_country != null)
                {
                    buffer.append ('_');
                    buffer.append (_country.toUpperCase ());
                }

                if (_encoding != null)
                {
                    buffer.append ('.');
                    buffer.append (_encoding);
                }

                if (_modifier != null)
                {
                    buffer.append ('@');
                    buffer.append (_modifier);
                }

                buffer.append (']');
            }

            return buffer.toString ();
        }

        public String getValue ()
        {
            return _value;
        }
    }

    public static class Name extends LocalizedEntry
    {
        public Name ()
        {
            super ("Name");
        }
    }

    public static class GenericName extends LocalizedEntry
    {
        public GenericName ()
        {
            super ("GenericName");
        }
    }

    public static class Comment extends LocalizedEntry
    {
        public Comment ()
        {
            super ("Comment");
        }
    }

    public static class Icon extends LocalizedEntry
    {
        public Icon ()
        {
            super ("Icon");
        }
    }

    public static class Type extends EnumeratedAttribute
    {
        public String[] getValues ()
        {
            return new String[] {"Application", "Link", "Directory"};
        }
    }

    public static class OnlyShowIn extends EnumeratedAttribute
    {
        public String[] getValues ()
        {
            return new String[] {"GNOME", "KDE", "ROX", "XFCE", "Old"};
        }
    }

    public void setToFile(File toFile)
    {
        _toFile = toFile;
    }

    public void setType (Type type)
    {
        _entries.put("Type", type.getValue ());
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

    public void setOnlyShowIn(DesktopEntry.OnlyShowIn onlyShowIn)
    {
        if (_entries.containsKey ("NotShowIn"))
            throw new BuildException("Only one of either OnlyShowIn or NotShowIn can be set!");

        _entries.put("OnlyShowIn", onlyShowIn.getValue ());
    }

    public void setNotShowIn(DesktopEntry.OnlyShowIn notShowIn)
    {
        if (_entries.containsKey ("OnlyShowIn"))
            throw new BuildException("Only one of either OnlyShowIn or NotShowIn can be set!");

        _entries.put("NotShowIn", notShowIn.getValue ());
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

    public void addName(DesktopEntry.Name name)
    {
        _localizedEntries.add (name);
    }

    public void addGenericName(GenericName genericName)
    {
        _localizedEntries.add (genericName);
    }

    public void addComment(Comment comment)
    {
        _localizedEntries.add (comment);
    }

    public void addIcon(Icon icon)
    {
        _localizedEntries.add (icon);
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
            log ("Generating desktop entry to: " + _toFile.getAbsolutePath ());

            for (int i = 0; i < _localizedEntries.size (); i++)
            {
                LocalizedEntry localizedEntry = (LocalizedEntry) _localizedEntries.get (i);

                _entries.put (localizedEntry.toString (), localizedEntry.getValue ());
            }

            PrintWriter out = new UnixPrintWriter(_toFile);

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

