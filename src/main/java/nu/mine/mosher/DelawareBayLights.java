package nu.mine.mosher;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import static nu.mine.mosher.OfficeNamespaceContext.ns;

public class DelawareBayLights {

    public static void main(final String... args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        final var builder = factory().newDocumentBuilder();
        final var doc = builder.parse("DelawareBayLights.fods");

        final var xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new OfficeNamespaceContext());

        final var cols = new HashMap<String, Integer>();
        {
            final var rowHead = (NodeList)xpath.evaluate(
                "//office:document[1]/office:body[1]/office:spreadsheet[1]/table:table[1]" +
                    "/table:table-row[1]/table:table-cell[*]",
                doc,
                XPathConstants.NODESET);
            int iCol = 1;
            for (int i = 0; i < rowHead.getLength(); ++i) {
                final var nRow = rowHead.item(i);
                for (var cel = nRow.getFirstChild(); Objects.nonNull(cel); cel = cel.getNextSibling()) {
                    if (cel.getNodeType() == Node.ELEMENT_NODE) {
                        cols.put(cel.getTextContent().trim(), iCol);
                        ++iCol;
                    }
                }
            }
        }

        final var nlRows = (NodeList)xpath.evaluate(
            "//office:document[1]/office:body[1]/office:spreadsheet[1]/table:table[1]" +
            "/table:table-row[position()> 2][./table:table-cell/@office:value-type != '']",
            doc,
            XPathConstants.NODESET);

        final String XHTML_HEAD =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <link href="main.css" rel="stylesheet"/>
                <link href="lights.css" rel="stylesheet"/>
            </head>
            <body>
            """;
        final String XHTML_TAIL =
            """
            </body>
            </html>
            """;

        final var fileHtml = new File("docs/index.xhtml");
        final var outHtml = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileHtml), StandardCharsets.UTF_8)));
        outHtml.print(XHTML_HEAD);

        final var fileCss = new File("docs/lights.css");
        final var outCss = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileCss), StandardCharsets.UTF_8)));

        for (int i = 0; i < nlRows.getLength(); ++i) {
            final var nRow = nlRows.item(i);
            final var vals = new HashMap<Integer, String>();
            int iCol = 1;
            for (var cel = nRow.getFirstChild(); Objects.nonNull(cel); cel = cel.getNextSibling()) {
                if (cel.getNodeType() == Node.ELEMENT_NODE) {
                    var val = cel.getAttributes().getNamedItemNS(ns("office"), "value");
                    if (Objects.isNull(val)) {
                        val = cel.getAttributes().getNamedItemNS(ns("office"), "string-value");
                    }
                    String v;
                    if (Objects.isNull(val)) {
                        v = "";
                    } else {
                        v = val.getNodeValue().trim();
                    }
                    vals.put(iCol, v);
                    ++iCol;
                }
            }
            var quot = vals.get(cols.get("label"));
            if (Objects.isNull(quot) || quot.isEmpty()) {
                quot = "";
            } else {
                quot = "“"+quot+"”<br/>";
            }
            outHtml.printf(
                """
                <figure id="label_%03d">
                    <span class="light" id="light_%03d"/>
                    <figcaption>
                        %s
                        %s<br/>
                        <i>%s %s %1.1fs %3.0fft %dNMi</i><br/>
                        magnetic bearing %3.1f° (%d mils)<br/>
                    </figcaption>
                </figure>
                """,
                i+1,
                i+1,
                quot,
                vals.get(cols.get("name")),
                vals.get(cols.get("style")),
                color(vals.get(cols.get("color"))),
                pd(vals.get(cols.get("rate"))),
                pd(vals.get(cols.get("height"))),
                pint(vals.get(cols.get("visibility"))),
                pd(vals.get(cols.get("mag abs"))),
                pint(vals.get(cols.get("mag mils")))
            );

            final var r = pd(vals.get(cols.get("rate")));
            final var bearing = pd(vals.get(cols.get("true bearing")))+90.0D;
            final var y = bearing*20.0D;

            outCss.printf(
                """
                #label_%03d { left: %4.0fpx; }
                #light_%03d { animation-name: %s; background-color: %s; animation-duration: %1.2fs; }
                """,
                i+1,
                y,
                i+1,
                vals.get(cols.get("style")),
                vals.get(cols.get("color")),
                r);
        }

        outCss.flush();
        outCss.close();

        outHtml.print(XHTML_TAIL);
        outHtml.flush();
        outHtml.close();



        System.out.flush();
    }

    private static long pint(final String s) {
        final double d;
        try {
            d = Double.parseDouble(s);
        } catch (final Throwable e) {
            return 0;
        }
        return Math.round(Math.rint(d));
    }

    private static String color(String color) {
        if (Objects.isNull(color) || color.isEmpty()) {
            return "";
        }
        return color.substring(0,1).toUpperCase();
    }

    private static double pd(final String s) {
        try {
            return Double.parseDouble(s);
        } catch (final Throwable e) {
            return Double.NaN;
        }
    }

    private static DocumentBuilderFactory factory() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setValidating(false);
//        factory.setFeature("http://apache.org/xml/features/validation/schema", false);

        factory.setNamespaceAware(true);
//        factory.setExpandEntityReferences(true);
//        factory.setCoalescing(true);
//        factory.setIgnoringElementContentWhitespace(true);
//        factory.setIgnoringComments(true);
//
//        factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
//        factory.setFeature("http://apache.org/xml/features/warn-on-duplicate-entitydef", true);
//        factory.setFeature("http://apache.org/xml/features/standard-uri-conformant", true);
//        factory.setFeature("http://apache.org/xml/features/xinclude", true);
//        factory.setFeature("http://apache.org/xml/features/validate-annotations", true);
//        factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
//        factory.setFeature("http://apache.org/xml/features/validation/warn-on-duplicate-attdef", true);
//        factory.setFeature("http://apache.org/xml/features/validation/warn-on-undeclared-elemdef", true);
//        factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);

        return factory;
    }

//    public static void main(final String... args) throws InvocationTargetException, InterruptedException {
//        final DblGui gui = DblGui.create();
//        gui.waitForEventThread();
//    }
}
