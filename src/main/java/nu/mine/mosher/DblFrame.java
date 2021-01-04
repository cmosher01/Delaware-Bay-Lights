package nu.mine.mosher;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class DblFrame extends Frame {
    private static final Component CENTER_ON_SCREEN = null;

    private List<Light> bearings;

    public static DblFrame create() throws IOException, CsvValidationException {
        final DblFrame frame = new DblFrame();
        frame.init();
        return frame;
    }

    private void init() throws IOException, CsvValidationException {
        setSize(1920,1080);
        setLocationRelativeTo(CENTER_ON_SCREEN);
        setBackground(Color.BLACK);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent ev) {
                EventQueue.invokeLater(() -> quit());
            }
        });

        read();

        setVisible(true);
    }

    private void quit() {
        dispose();
    }

    public void paint(final Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        this.bearings.forEach(b -> {
            g2.setPaint(b.color());
            g.fillOval((int)Math.round(b.bearing()), 100, 4, 4);
        });
    }

    void read() throws IOException, CsvValidationException {
        final CSVReaderHeaderAware csv = new CSVReaderHeaderAware(new FileReader("DelawareBayLights.csv"));
        final ArrayList<Light> bs = new ArrayList<>();

        for (var map = csv.readMap(); Objects.nonNull(map); map = csv.readMap()) {
            final double b = bearing(map.get("true bearing"));
            if (!Double.isNaN(b)) {
                final Color color = colorOf(map.get("color").trim().toUpperCase());
                bs.add(new Light(b, color));
            }
        }
        this.bearings = List.copyOf(bs);
    }

    private Color colorOf(final String s) {
        return switch(s) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "YELLOW" -> Color.YELLOW;
            case "WHITE" -> Color.WHITE;
            default -> Color.BLACK;
        };
    }

    private double bearing(final String s) {
        try {
            return 2.0D * (Double.parseDouble(s)*10.0D + 900.0D);
        } catch (final Throwable e) {
            return Double.NaN;
        }
    }
}
