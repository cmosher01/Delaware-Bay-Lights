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

import org.apache.commons.csv.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.Math.*;

public class DelawareBayLights {

    public record LatLng(double latitude, double longitude) {
        public double rlat() {
            return toRadians(latitude());
        }
        public double rlon() {
            return toRadians(longitude());
        }
    }

    public record Light(
        double bearing,
        Optional<LatLng> location,
        String style,
        String color,
        double rate,
        int offset,
        int height,
        int visibility,
        String label,
        String name,
        String link,
        double distance,
        double distanceNMi,
        double trueabs,
        double truemils,
        double magabs,
        double magmils
    ) {}

    public static double mod(final double x, final double m) {
        return ((x % m) + m) % m;
    }

    public static double wrap(final double n, final double min, final double max) {
        return
            (min <= n && n < max)
                ? n
                : (min + mod(n - min, max - min));
    }

    public static double computeHeading(final LatLng from, final LatLng to) {
        final double dLng = to.rlon()-from.rlon();
        final double heading = atan2(
            sin(dLng)*cos(to.rlat()),
            cos(from.rlat())*sin(to.rlat()) - sin(from.rlat())*cos(to.rlat())*cos(dLng));
        return wrap(toDegrees(heading), -180.0D, 180.0D);
    }

    public static final double EARTH_RADIUS_MILES = 3958.761D;

    private static Optional<Integer> readInteger(final CSVRecord record, final String field) {
        try {
            return Optional.of(Integer.parseInt(record.get(field)));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Double> readDouble(final CSVRecord record, final String field) {
        try {
            return Optional.of(Double.parseDouble(record.get(field)));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<LatLng> readLatLng(final CSVRecord record, final String lat, final String lng) {
        try {
            return Optional.of(new LatLng(readDouble(record, lat).get(), readDouble(record, lng).get()));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    private static String readString(final CSVRecord record, final String field) {
        return Optional.ofNullable(record.get(field)).orElse("").trim();
    }

    public static void main(final String... args) throws IOException {
        final var fmt =
            CSVFormat.Builder.create()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
            .build();

        final var lights = new ArrayList<Light>(100);
        try (final var in = new FileReader("DelawareBayLights.csv")) {
            final var records = fmt.parse(in);

            LatLng home = null;
            double magoff = 0D;
            for (final var record : records) {
                final var br = readDouble(record, "bearing");
                final var latLng = readLatLng(record, "latitude", "longitude");

                if (Objects.isNull(home)) {
                    // first data row is special, it must represent the viewing location, and it must have
                    // latitude/longitude defined. Bearing should be the offset of true north from magnetic north.
                    home = latLng.get();
                    magoff = br.get();
                } else {
                    lights.add(readLight(home, magoff, record, br.get(), latLng));
                }
            }
        }





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

        final int YMIN = -95;
        final int YMAX = +90;
        {
            outHtml.println("<div>");
            for (int y = YMIN; y <= YMAX; ++y) {
                var lab = "" + ((y + 360) % 360) + "°";
                var cls = "compass";
                if (((y-YMIN) % 45) == 0) {
                    cls = "compass1";
                } else if (((y-YMIN) % 5) == 0) {
                    cls = "compass0";
                } else {
                    lab = " ";
                }
                final var p = (y-YMIN)*30;
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
            outHtml.println("</div>");
        }





        outHtml.println("<div>");

        int i = 0;
        for (final var light : lights) {
            var quot = light.label();//vals.get(cols.get("label"));
            if (Objects.isNull(quot) || quot.isEmpty()) {
                quot = "";
            } else {
                quot = " “" + quot + "”";
            }

            var link = light.link();//vals.get(cols.get("link"));
            if (Objects.isNull(link)) {
                link = "";
            }
            link = link.replace("&", "&amp;");

            outHtml.printf(
                """
                <figure id="label_%03d">
                    <%s class="light" id="light_%03d" %s/>
                    <figcaption>
                        <span class="lightinline" id="lightl_%03d"/><br/>
                        %s%s<br/>
                        <i>%s %s %1.1fs %3dft %dNMi</i><br/>
                        %s<br/>
                        %2.1f miles (%2.1f NMi)<br/>
                        azimuth: &#x2605; %.1f° (%+.1f°) (%.0f mils); &#x1F9ED; %.1f° (%.0f mils)<br/>
                    </figcaption>
                </figure>
                """,
                i + 1,
                link.isEmpty() ? "span" : "a",
                i + 1,
                link.isEmpty() ? "" : "href=\"" + link + "\"",
                i + 1,
                light.name(),
                quot,
                light.style(),
                color(light.color()),
                light.rate(),
                light.height(),
                light.visibility(),
                latlon(light.location()),
                light.distance(),
                light.distanceNMi(),
                light.trueabs(),
                light.bearing(),
                light.truemils(),
                light.magabs(),
                light.magmils()
            );

            final var r = light.rate();//pd(vals.get(cols.get("rate")));
            final var y = (light.bearing()-YMIN) * 30.0D;
            final var randDelay = light.offset();

            var anim = light.style();
            if (Objects.isNull(anim)) {
                anim = "";
            }
            if (anim.isBlank()) {
                anim = "none";
            }
            var color = light.color();
            if (Objects.isNull(color)) {
                color = "";
            }
            if (color.isBlank()) {
                color = "darkblue";
            }
            outCss.printf(
                """
                    #label_%03d { left: %4.0fpx; }
                    #light_%03d { animation-name: %s; background-color: %s; animation-duration: %1.2fs; animation-delay: %dms; }
                    #lightl_%03d { animation-name: %s; background-color: %s; animation-duration: %1.2fs; animation-delay: %dms; }
                    """,
                i + 1, y,
                i + 1, anim, color, r, randDelay,
                i + 1, anim, color, r, randDelay);
            ++i;
        }
        outHtml.println("</div>");

        outCss.flush();
        outCss.close();

        outHtml.print(XHTML_TAIL);
        outHtml.flush();
        outHtml.close();
    }

    private static Light readLight(final LatLng home, final double magoff, final CSVRecord record, final double br, final Optional<LatLng> latLng) {
        // if lat/long is given, use it to compute bearing; otherwise use the bearing field read in
        final double bearing = latLng.map(c -> computeHeading(home, c)).orElse(br);
        final var trueabs = mod(bearing, 360.0D);

        final var distance = distance(home, latLng);

        double magbrg = bearing-magoff;
        double magabs = mod(magbrg, 360.0D);
        double magmils = magabs*160D/9D;

        return new Light(
            bearing,
            latLng,
            readString(record, "style"),
            readString(record, "color"),
            readDouble(record, "rate").get(),
            readInteger(record, "offset").get(),
            readInteger(record, "height").get(),
            readInteger(record, "visibility").get(),
            readString(record, "label"),
            readString(record, "name"),
            readString(record, "link"),
            distance,
            distance*5280D/6076D,
            trueabs,
            trueabs*160D/9D,
            magabs,
            magmils
        );
    }

    private static double distance(LatLng home, Optional<LatLng> latLng) {
        if (latLng.isEmpty()) {
            return 0.0D;
        }

        final var loc = latLng.get();

        final var mid = (
            sin(home.rlat()) *
            sin( loc.rlat())
        ) + (
            cos(home.rlat()) *
            cos( loc.rlat()) *
            cos( loc.rlon() - home.rlon())
        );

        return acos(mid) * EARTH_RADIUS_MILES;
    }

    public record DMS(
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

    private static String latlon(final Optional<LatLng> location) {
        if (location.isEmpty()) {
            return "[unknown location]";
        }
        final var loc = location.get();
        if (abs(loc.latitude()) < 0.001D || abs(loc.longitude()) < 0.001D) {
            return "[unknown location]";
        }
        final var lat = DMS.of(loc.latitude());
        final var lon = DMS.of(loc.longitude());
        final var flat = lat.fmt(true);
        final var flon = lon.fmt(false);
        if (flat.isBlank() || flon.isBlank()) {
            return "[unknown location]";
        }
        return flat +", "+ flon;
    }

    private static String color(String color) {
        if (Objects.isNull(color) || color.isEmpty()) {
            return "";
        }
        return color.substring(0,1).toUpperCase();
    }
}
