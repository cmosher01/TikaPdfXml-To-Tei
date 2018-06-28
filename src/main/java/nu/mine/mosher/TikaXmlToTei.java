package nu.mine.mosher;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Intended usage:
 * <code>java -jar tika-app.jar -x PDF.pdf PARSED.xml</code>
 * <code>java TikaXmlToTei PARSED.xml >PARSED.tei.xml</code>
 */
public class TikaXmlToTei {
    public static void main(final String... args) throws ParserConfigurationException, IOException, SAXException {
        if (args.length != 3) {
            System.out.println("usage: java TikaXmlToTei FILE.xml 'link-format' first-page-number");
            return;
        }
        final File xmlFile = new File(args[0]);
        final String formatLink = args[1];
        final int firstPage = Integer.parseInt(args[2]);

        final List<List<String>> pages = new ArrayList<>();

        final DocumentBuilderFactory factoryBuilder = DocumentBuilderFactory.newInstance();
        factoryBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final DocumentBuilder builderDoc = factoryBuilder.newDocumentBuilder();
        final Document pdf = builderDoc.parse(xmlFile);
        final Element root = pdf.getDocumentElement();
        final NodeList childrenOfRoot = root.getChildNodes();
        for (int i = 0; i < childrenOfRoot.getLength(); ++i) {
            final Node childOfRoot = childrenOfRoot.item(i);
            if (childOfRoot.getNodeName().equals("body")) {
                final Node body = childOfRoot;
                final NodeList childrenOfBody = body.getChildNodes();
                for (int j = 0; j < childrenOfBody.getLength(); ++j) {
                    final Node childOfBody = childrenOfBody.item(j);
                    if (childOfBody.getNodeName().equals("div")) {
                        final Node div = childOfBody;
                        boolean isPage = false;
                        final NamedNodeMap attributes = div.getAttributes();
                        for (int a = 0; a < attributes.getLength(); ++a) {
                            final Attr attribute = (Attr)attributes.item(a);
                             if (attribute.getName().equals("class") && attribute.getValue().equals("page")) {
                                 isPage = true;
                             }
                        }
                        if (isPage) {
                            pages.add(buildPage(div));
                        }
                    }
                }
            }
        }
        printTei(pages, formatLink, firstPage);
    }

    private static void printTei(final List<List<String>> pages, final String formatLink, final int firstPage) {

    }

    private static ArrayList<String> buildPage(final Node div) {
        final ArrayList<String> lines = new ArrayList<>();

        final NodeList paragraphs = div.getChildNodes();
        for (int i = 0; i < paragraphs.getLength(); ++i) {
            final Node paragraph = paragraphs.item(i);
            final String text = paragraph.getTextContent().trim();
            if (!text.isEmpty() && FindWords.isWords(text)) {
                try (final BufferedReader reader = new BufferedReader(new StringReader(text))) {
                    for (String line = reader.readLine(); Objects.nonNull(line); line = reader.readLine()) {
                        final String t = line.trim();
                        if (!t.isEmpty()) {
                            lines.add(t);
                        }
                    }
                } catch (IOException cantHappen) {
                    throw new IllegalStateException(cantHappen);
                }
            }
        }

        return lines;
    }
}
