package nu.mine.mosher;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

public class OfficeNamespaceContext implements NamespaceContext {
    private static final String URI_OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    private static final String URI_TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String URI_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";

    public static String ns(final String prefix) {
        return new OfficeNamespaceContext().getNamespaceURI(prefix);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        switch (prefix) {
            case "office":
                return URI_OFFICE;
            case "table":
                return URI_TABLE;
            case "text":
                return URI_TEXT;
        }
        throw new IllegalStateException();
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }
}
