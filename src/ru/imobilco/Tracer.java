package ru.imobilco;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.DefaultErrorHandler;

import ru.imobilco.xalan.XSLTracer;

public class Tracer {

    private static final String TEMPLATE = "/template.html";

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String xmlFile = null;
        String xslFile = null;
        boolean measureTime = false;

        // if outFile is null, output data to console
        String outFile = null;
//		boolean useSaxon = false; // TODO implement

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-xml")) {
                if (i + 1 < args.length) {
                    xmlFile = args[++i];
                } else {
                    printInvalidOption("-xml");
                }
            } else if (args[i].equalsIgnoreCase("-xsl")) {
                if (i + 1 < args.length) {
                    xslFile = args[++i];
                } else {
                    printInvalidOption("-xsl");
                }
            } else if (args[i].equalsIgnoreCase("-out")) {
                if (i + 1 < args.length) {
                    outFile = args[++i];
                } else {
                    printInvalidOption("-out");
                }
            } else if (args[i].equalsIgnoreCase("-time")) {
                measureTime = true;
            }
//			else if (args[i].equalsIgnoreCase("-saxon")) {
//				useSaxon = true;
//			}
        }

        if (xmlFile == null) {
            exit("No input XML specified");
        }

        if (xslFile == null) {
            exit("No input XSL specified");
        }

        TransformerFactory tfactory = TransformerFactory.newInstance();
        tfactory.setErrorListener(new DefaultErrorHandler(false));
        String traceDoc = null;
        long totalTime = 0L;

        try {
            Transformer transformer = tfactory.newTransformer(new StreamSource(xslFile));
            if (measureTime) {
                ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
                Result result = new StreamResult(resultStream);
                int count = getTimes();
                long startTime = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    transformer.transform(new StreamSource(xmlFile), result);
                }
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime) / 1000000;
            } else if (transformer instanceof TransformerImpl) {

                // init XSL tracer
                XSLTracer tracer = new XSLTracer((TransformerImpl) transformer);

                // get tracer template
                InputStream templateStream = tracer.getClass().getResourceAsStream(TEMPLATE);
                String template = convertStreamToString(templateStream);

                // trace document
                traceDoc = tracer.traceDocument(new StreamSource(xmlFile), template);
            }
        } catch (Exception e) {
            exit(e.getMessage());
        }

        if (measureTime) {
            System.out.println("Time: " + totalTime + " ms");
            System.out.println("Mean time: " + totalTime / getTimes() + " ms");
        }

        if (traceDoc != null) {
            // output result
            if (outFile != null) {
                File f = new File(outFile);
                Writer output = new BufferedWriter(new FileWriter(f));
                try {
                    output.write(traceDoc);
                } finally {
                    output.close();
                }
            } else {
                System.out.print(traceDoc);
            }
        }
    }

    private static int getTimes() {
        String timesProp = System.getProperty("ru.imobilco.transform.times");
        int count = 1;
        if (timesProp != null && timesProp.length() > 0) {
            count = new Integer(timesProp);
        }
        return count;
    }

    private static void printInvalidOption(String option) {

    }

    private static void exit(String msg) {
        System.err.println(msg);
        throw new RuntimeException(msg);
    }

    public static String convertStreamToString(InputStream is)
            throws IOException {
        /*
           * To convert the InputStream to String we use the
           * BufferedReader.readLine() method. We iterate until the BufferedReader
           * return null which means there's no more data to read. Each line will
           * appended to a StringBuilder and returned as String.
           */
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }

}
