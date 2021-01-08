/*
    Delaware Bay Lights

    Copyright © 2021, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see https://www.gnu.org/licenses/ .
*/
package nu.mine.mosher;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
                <meta http-equiv="Cache-Control" content="no-cache"/>
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

        {
            for (int y = -90; y <= +90; ++y) {
                var lab = "" + ((y + 360) % 360) + "°";
                var cls = "compass";
                if (((y+90) % 45) == 0) {
                    cls = "compass1";
                } else if (((y+90) % 5) == 0) {
                    cls = "compass0";
                } else {
                    lab = "";
                }
                final var p = (y+90)*30;
                outHtml.printf(
                    """
                    <span class="%s" id="comp_%03d"/>
                    <span class="compass_label" id="compl_%03d">%s</span>
                    """,
                    cls,
                    p,
                    p,
                    lab);
                outCss.printf(
                    """
                    #comp_%03d { left: %dpx; }
                    #compl_%03d { left: %dpx; }
                    """,
                    p,p,p,p+2);
            }
        }

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

            var link = vals.get(cols.get("link"));
            link = link.replace("&", "&amp;");

            outHtml.printf(
                """
                <figure id="label_%03d">
                    <%s class="light" id="light_%03d" %s/>
                    <figcaption>
                        %s
                        %s<br/>
                        <i>%s %s %1.1fs %3.0fft %dNMi</i><br/>
                        %s<br/>
                        true azimuth %3.1f° (%d mils)<br/>
                        magnetic azimuth %3.1f° (%d mils)<br/>
                    </figcaption>
                </figure>
                """,
                i+1,
                link.isEmpty() ? "span" : "a",
                i+1,
                link.isEmpty() ? "" : "href=\""+link+"\"",
                quot,
                vals.get(cols.get("name")),
                vals.get(cols.get("style")),
                color(vals.get(cols.get("color"))),
                pd(vals.get(cols.get("rate"))),
                pd(vals.get(cols.get("height"))),
                pint(vals.get(cols.get("visibility"))),
                latlon(pd(vals.get(cols.get("latitude"))), pd(vals.get(cols.get("longitude")))),
                pd(vals.get(cols.get("true abs"))),
                pint(vals.get(cols.get("true mils"))),
                pd(vals.get(cols.get("mag abs"))),
                pint(vals.get(cols.get("mag mils")))
            );

            final var r = pd(vals.get(cols.get("rate")));
            final var bearing = pd(vals.get(cols.get("true bearing")))+90.0D; // or mag?
            final var y = bearing*30.0D;
            final var randDelay = ThreadLocalRandom.current().nextInt(0, 4000);

            var anim = vals.get(cols.get("style"));
            if (anim.isBlank()) {
                anim = "none";
            }
            var color = vals.get(cols.get("color"));
            if (color.isBlank()) {
                color = "darkblue";
            }
            outCss.printf(
                """
                #label_%03d { left: %4.0fpx; }
                #light_%03d { animation-name: %s; background-color: %s; animation-duration: %1.2fs; animation-delay: %dms; }
                """,
                i+1,
                y,
                i+1,
                anim,
                color,
                r,
                randDelay);
        }

        outCss.flush();
        outCss.close();

        outHtml.print(XHTML_TAIL);
        outHtml.flush();
        outHtml.close();



        System.out.flush();
    }

    private static record DMS(
        long deg,
        long min,
        double sec,
        boolean neg
    ) {
        static DMS of(double d) {
            var xn = d < 0.0D;
            d = Math.abs(d);
            var xd = Math.round(Math.rint(Math.floor(d)));
            d -= xd;
            d *= 60.0D;
            var xm = Math.round(Math.rint(Math.floor(d)));
            d -= xm;
            d *= 60.0D;
            return new DMS(xd,xm,d,xn);
        }
        String fmt(final boolean lat) {
            if (Double.isNaN(sec())) {
                return "";
            }
            final String dir;
            final String lab;
            if (lat) {
                dir = neg() ? "S" : "N";
                lab = "latitude";
            } else {
                dir = neg() ? "W" : "E";
                lab = "longitude";
            }
            return String.format("%d° %d′ %2.3f″ %s %s", deg(), min(), sec(), dir, lab);
        }
    }

    private static String latlon(double latitude, double longitude) {
        var lat = DMS.of(latitude);
        var lon = DMS.of(longitude);
        final var flat = lat.fmt(true);
        final var flon = lon.fmt(false);
        if (flat.isBlank() || flon.isBlank()) {
            return "[unknown location]";
        }
        return flat +", "+ flon;
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
        factory.setNamespaceAware(true);
        return factory;
    }
}
