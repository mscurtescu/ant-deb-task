package com.googlecode.ant_deb_task;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

public class BuildDeb
{
    private static final String FILE_HEADER_FORMAT = "{0}{1}0     0     100644  {2}`\n";
    
    private static final String DEBIAN_BINARY_CONTENT = "2.0\n";
    private static final String DEBIAN_BINARY_NAME = "debian-binary";
    private static final String CONTROL_NAME = "control.tar.gz";
    private static final String DATA_NAME = "data.tar.gz";

    public static void buildDeb(File debFile, File controlFile, File dataFile) throws IOException
    {
        long now = new Date().getTime() / 1000;
        PrintStream deb = new PrintStream(debFile);
        
        deb.print("!<arch>\n");
        deb.print(MessageFormat.format(FILE_HEADER_FORMAT, new Object[]{padd(DEBIAN_BINARY_NAME, 16), padd(now, 12), padd(DEBIAN_BINARY_CONTENT.length(), 10)}));
        deb.print(DEBIAN_BINARY_CONTENT);
        if (DEBIAN_BINARY_CONTENT.length() % 2 == 1)
            deb.print('\n');
        deb.print(MessageFormat.format(FILE_HEADER_FORMAT, new Object[]{padd(CONTROL_NAME, 16), padd(now, 12), padd(controlFile.length(), 10)}));

        FileInputStream control = new FileInputStream(controlFile);
        byte[] buffer = new byte[1024];
        while(true)
        {
            int read = control.read(buffer);
            if (read == -1)
                break;
            deb.write(buffer, 0, read);
        }
        control.close();
        if (controlFile.length() % 2 == 1)
            deb.print('\n');
        
        deb.print(MessageFormat.format(FILE_HEADER_FORMAT, new Object[]{padd(DATA_NAME, 16), padd(now, 12), padd(dataFile.length(), 10)}));

        FileInputStream data = new FileInputStream(dataFile);
        while(true)
        {
            int read = data.read(buffer);
            if (read == -1)
                break;
            deb.write(buffer, 0, read);
        }
        data.close();

        deb.close();
    }

    private static String padd(long number, int length)
    {
        return padd(Long.toString(number), length);
    }

    private static String padd(String text, int length)
    {
        StringBuffer buffer = new StringBuffer(text);

        for (int i = 0; i < length - text.length(); i++)
        {
            buffer.append(' ');
        }

        return buffer.toString();
    }

    public static void main(String[] args) throws IOException
    {
        buildDeb(new File(args[0]), new File(args[1]), new File(args[2]));
    }
}

