package nu.mine.mosher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Year;
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
        if (args.length != 5) {
            System.out.println("usage: java TikaXmlToTei FILE.xml 'link-format' first-page-number conf.properties encodingDesc.xml");
            return;
        }
        final File xmlFile = new File(args[0]);
        final String formatLink = args[1];
        final int firstPage = Integer.parseInt(args[2]);
        final Properties props = new Properties();
        props.load(new FileInputStream(new File(args[3])));
        final String encDesc = new String(Files.readAllBytes(Paths.get(args[4])));

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
            } else if (childOfRoot.getNodeName().equals("head")) {
                final Node head = childOfRoot;
                final NodeList childrenOfHead = head.getChildNodes();
                for (int j = 0; j < childrenOfHead.getLength(); ++j) {
                    final Node childOfHead = childrenOfHead.item(j);
                    if (childOfHead.getNodeName().equals("meta")) {
                        final Node meta = childOfHead;
                        final NamedNodeMap attributes = meta.getAttributes();
                        boolean isRes = false;
                        for (int a = 0; a < attributes.getLength(); ++a) {
                            final Attr attribute = (Attr) attributes.item(a);
                            if (attribute.getName().equals("name") && attribute.getValue().equals("resourceName")) {
                                isRes = true;
                            }
                        }
                        if (isRes) {
                            for (int a = 0; a < attributes.getLength(); ++a) {
                                final Attr attribute = (Attr) attributes.item(a);
                                if (attribute.getName().equals("content")) {
                                    title = attribute.getValue();
                                }
                            }
                        }
                    }
                }
            }
        }

        title = title.replaceAll("_"," ").replaceAll("\\.pdf","");

        printTei(pages, formatLink, firstPage, title, props, encDesc);

        System.out.flush();
        System.err.flush();
    }

    private static void printTei(final List<List<String>> pages, final String formatLink, final int pageRealFirst, final String title, final Properties props, final String encDesc) {
        final String year = Year.now().toString();

        Fragment xml = new Fragment()
            .elem("TEI").attr("xml:lang", "en").attr("xmlns", "http://www.tei-c.org/ns/1.0")

            .elem("teiHeader")
                .elem("fileDesc")
                    .elem("titleStmt")
                        .elem("title").text(title).end()
                        .elem("author").end()
                        .elem("principal").text(props.getProperty("principal","")).end()
                    .end()
                    .elem("publicationStmt")
                        .elem("authority").text(props.getProperty("authority",props.getProperty("principal",""))).end()
                        .elem("pubPlace").text(props.getProperty("pubplace","")).end()
                        .elem("date")/*.attr("when", year)*/.text(year).end()
                        .elem("availability").attr("status","restricted")
                            .elem("p").text("Copyright \u00A9 "+year+", "+props.getProperty("copyright","")).end()
                            /* TEI misspells license as licence */
                            .elem("licence").attr("target", props.getProperty("licenseurl",""))
                                .text(props.getProperty("licensetext",""))
                            .end()
                        .end()
                    .end()
                    .elem("sourceDesc")
                        .elem("bibl").end()
                        /*.elem("msDesc").end()*/
                    .end()
                .end()
                .frag(encDesc) // cheat to put XML in
                .elem("profileDesc")
                    .elem("creation")
                        .elem("date")/*.attr("when")*/.end()
                        .elem("rs").attr("type", "place").end()
                    .end()
                    .elem("langUsage")
                        .elem("language").attr("ident",props.getProperty("langid","en"))
                            .text(props.getProperty("language","English"))
                        .end()
                    .end()
                .end()
            .end();



        xml = xml
            .elem("facsimile").text("\n");

        for (int pageInPdf = 1; pageInPdf <= pages.size(); ++pageInPdf) {
            final int pageReal = pageRealFirst + pageInPdf - 1;
            final String link = String.format(formatLink, pageReal);
            final String id = String.format("page-%03d", pageReal);
            xml = xml.elem("graphic").attr("xml:id", id).attr("url", link).end().text("\n");
        }

        xml = xml
            .end().text("\n");



        xml = xml
            .elem("text").attr("xml:lang", "en-US").text("\n")
            .elem("body").elem("ab").text("\n");

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
            final String text = paragraph.getTextContent();
            if (!text.isEmpty() && FindWords.isWords(text)) {
                try (final BufferedReader reader = new BufferedReader(new StringReader(text))) {
                    for (String line = reader.readLine(); Objects.nonNull(line); line = reader.readLine()) {
                        if (!line.isEmpty()) {
                            lines.add(line);
                        }
                    }
                } catch (IOException cantHappen) {
                    throw new IllegalStateException(cantHappen);
                }
            }
        }

        return filterRotated(lines);
    }

    /**
     * OCR of rotated text comes out with one or two characters per line.
     * Here we try to reassemble as best we can.
     * @param lines
     */
    private static ArrayList<String> filterRotated(final ArrayList<String> lines) {
        final ArrayList<String> result = new ArrayList<>(lines.size());
        StringBuilder s = new StringBuilder(100);
        for (int i = 0; i < lines.size(); ++i) {
            final String line = lines.get(i);
            if (line.length() <= 3 && (prev2short(lines, i) || next2short(lines, i))) {
                s.append(line);
            } else {
                if (s.length() > 0) {
                    result.add(s.toString());
                    s = new StringBuilder(100);
                }
                result.add(line);
            }
        }
        if (s.length() > 0) {
            result.add(s.toString());
        }
        return result;
    }

    private static boolean next2short(final ArrayList<String> lines, final int i) {
        if (lines.size() <= i+2) {
            return false;
        }
        return lines.get(i+1).length() <= 3 && lines.get(i+2).length() <= 3;
    }

    private static boolean prev2short(final ArrayList<String> lines, final int i) {
        if (i-2 < 0) {
            return false;
        }
        return lines.get(i-2).length() <= 3 && lines.get(i-1).length() <= 3;
    }
}
