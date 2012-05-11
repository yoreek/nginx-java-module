package org.nginx.servlet;

import java.io.IOException;

public class BufferedWriter {

    private static final String LINE_SEP = "\r\n";

    protected ServletContext context = null;

    void clear() {
        /*ob = null;*/
    }

    public void write(int i) throws IOException {
        /*ob.writeByte(i);*/
    }


    public void write(byte[] b) throws IOException {
        /*write(b, 0, b.length);*/
    }


    public void write(byte[] b, int off, int len) throws IOException {
        context.write(b, off, len);
        /*ob.write(b, off, len);*/
    }

    public void write(String s, int off, int len) throws IOException {
        write(s.getBytes(), off, len);
    }

    public void write(String s) throws IOException {
        write(s, 0, s.length());
    }

    public void flush() throws IOException {
        /* ob.flush(); */
    }


    public void close() {
        context.finalizeRequest();
    }

    public void print(String s) throws IOException {
        if (s == null) {
            s = "null";
        }
        write(s);
    }

    public void println() throws IOException {
        write(LINE_SEP);
    }

    public void println(String s) throws IOException {
        print(s);
        println();
    }

    public BufferedWriter(ServletContext context) {
        this.context = context;
    }
}
