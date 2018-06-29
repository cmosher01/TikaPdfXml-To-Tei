package nu.mine.mosher;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import nu.mine.mosher.patterns.fluent.Fragment;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

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

        String title = "";
        final List<List<String>> pages = new ArrayList<>();

        final DocumentBuilderFactory factoryBuilder = DocumentBuilderFactory.newInstance();
        factoryBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final DocumentBuilder builderDoc = factoryBuilder.newDocumentBuilder();
        final Document pdf = builderDoc.parse(xmlFile);
        final Element root = pdf.getDocumentElement();
        final NodeList childrenOfRoot = root.getChildNodes();
        for (int i = 0; i < childrenOfRoot.getLength(); ++i) {
            final Node childOfRoot = childrenOfRoot.item(i);
            if (childOfRoot
                .getNodeName()
                .equals("body")) {
                final Node body = childOfRoot;
                final NodeList childrenOfBody = body.getChildNodes();
                for (int j = 0; j < childrenOfBody.getLength(); ++j) {
                    final Node childOfBody = childrenOfBody.item(j);
                    if (childOfBody
                        .getNodeName()
                        .equals("div")) {
                        final Node div = childOfBody;
                        boolean isPage = false;
                        final NamedNodeMap attributes = div.getAttributes();
                        for (int a = 0; a < attributes.getLength(); ++a) {
                            final Attr attribute = (Attr) attributes.item(a);
                            if (attribute
                                .getName()
                                .equals("class") &&
                                attribute
                                    .getValue()
                                    .equals("page")) {
                                isPage = true;
                            }
                        }
                        if (isPage) {
                            pages.add(buildPage(div));
                        }
                    }
                }
            } else if (childOfRoot.getNodeName().equals("teiHeader")) {
                // TODO get title from header
                title = "TITLE";
            }
        }
        printTei(pages, formatLink, firstPage, title);

        System.out.flush();
        System.err.flush();
    }

    private static void printTei(final List<List<String>> pages, final String formatLink, final int pageRealFirst, final String title) {



        Fragment xml = new Fragment()
            .elem("TEI").attr("xml:lang", "en").attr("xmlns", "http://www.tei-c.org/ns/1.0")
            .elem("teiHeader")
                .elem("fileDesc")
                .elem("titleStmt").elem("title").text(title).end().end()
                .elem("publicationStmt").end() // TODO publication
            .end().end()
            .elem("facsimile").text("\n");

        for (int pageInPdf = 1; pageInPdf <= pages.size(); ++pageInPdf) {
            final int pageReal = pageRealFirst + pageInPdf - 1;
            final String link = String.format(formatLink, pageReal);
            final String id = String.format("page-%03d", pageReal);
            xml = xml.elem("graphic").attr("xml:id", id).attr("url", link).end().text("\n");
        }

        xml = xml
            .end().text("\n")
            .elem("text").attr("xml:lang", "en-US").text("\n")
            .elem("body").text("\n");

        for (int pageInPdf = 1; pageInPdf <= pages.size(); ++pageInPdf) {
            final int pageReal = pageRealFirst + pageInPdf - 1;
            final String id = String.format("#page-%03d", pageReal);
            xml = xml.elem("pb").attr("n",Integer.toString(pageInPdf)).attr("facs",id).end().text("\n");
            final List<String> lines = pages.get(pageInPdf - 1);
            if (lines.isEmpty()) {
                xml = xml.text("[A transcription of this page is not available.]\n");
            }
            for (final String line : lines) {
                xml = xml.elem("lb").end().text(line).text("\n");
            }
        }

        System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        System.out.println("<?xml-model href=\"http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/tei_all.rng\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>");
        System.out.print(xml.toString());
    }

    private static ArrayList<String> buildPage(final Node div) {
        final ArrayList<String> lines = new ArrayList<>();

        final NodeList paragraphs = div.getChildNodes();
        for (int i = 0; i < paragraphs.getLength(); ++i) {
            final Node paragraph = paragraphs.item(i);
            final String text = paragraph
                .getTextContent()
                .trim();
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
