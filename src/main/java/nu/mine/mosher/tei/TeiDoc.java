package nu.mine.mosher.tei;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.TransformerException;

import lombok.val;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import static nu.mine.mosher.tei.XmlUtils.*;

public class TeiDoc {
    public static void main(final String... args) throws IOException, TransformerException {
        if (args.length != 2) {
            System.out.println("usage: tikapdfxml-to-tei conf.properties path/to/title <tesseract.txt >title.tei.xml");
            return;
        }

        val props = new Properties();
        props.load(new FileInputStream(args[0]));

        val pages = new ArrayList<List<String>>();
        val scanPages = new Scanner(new BufferedInputStream(new FileInputStream(FileDescriptor.in)), StandardCharsets.UTF_8).useDelimiter("\u000C");
        while (scanPages.hasNext()) {
            pages.add(lines(scanPages.next()));
        }

        val link = props.getProperty("imgUrlPrefix")+args[1]+"/p%04d.ptif"+props.getProperty("imgUrlSuffix");
        printTei(pages, link, 1, titleFromPath(args[1]), props);

        System.err.flush();
        System.out.flush();
    }

    private static String titleFromPath(String arg) {
        val s = Objects.requireNonNull(arg.split("/"));
        if (s.length == 0) {
            throw new IllegalArgumentException("missing title from path/to/title argument");
        }
        var raw = s[s.length-1].trim();
        if (raw.isBlank()) {
            throw new IllegalArgumentException("missing title from path/to/title argument");
        }
        raw = raw.replaceAll("_", " ").trim();
        raw = raw.substring(0, 1).toUpperCase() + raw.substring(1);
        return raw;
    }

    private static ArrayList<String> lines(final String page) {
        val r = new ArrayList<String>();
        addTextLines(page, r);
        return r;
    }

    public static void OLDmain(final String... args) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        if (args.length != 5) {
            System.out.println("usage: java TeiDoc FILE.xml 'link-format' first-page-number conf.properties image-link-format");
            return;
        }
        final File xmlFile = new File(args[0]);
        final String formatLink = args[1];
        final int firstPage = Integer.parseInt(args[2]);
        final Properties props = new Properties();
        props.load(new FileInputStream(args[3]));
        final String formatLink2 = args[4];

        String title = "";
        final List<List<String>> pages = new ArrayList<>();

        boolean isOcr = false;

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
                            final Attr attribute = (Attr) attributes.item(a);
                            if (attribute.getName().equals("class") && attribute.getValue().equals("page")) {
                                isPage = true;
                            }
                            if (attribute.getName().equals("class") && attribute.getValue().equals("ocr")) {
                                isOcr = true;
                            }
                        }
                        if (isPage) {
                            pages.add(buildPage(div));
                        } else if (isOcr) {
                            pages.add(buildOcrPage(div));
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

        printTei(pages, isOcr ? formatLink2 : formatLink, firstPage, title, props);

        System.err.flush();
        System.out.flush();
    }

    private static void printTei(final List<List<String>> pages, final String formatLink, final int pageRealFirst, final String title, final Properties props) throws IOException, TransformerException {
        val year = Year.now().toString();
        val doc = doc();

        val tei = e(doc, "TEI");
        tei.setAttributeNS(XML_NAMESPACE, "lang", "en");
            val teiHeader = e(tei, "teiHeader");
                val fileDesc = e(teiHeader, "fileDesc");

                    val titleStmt = e(fileDesc, "titleStmt");
                        val etitle = e(titleStmt, "title");
                        t(etitle, title);
                        e(titleStmt, "author");
                        val principal = e(titleStmt, "principal");
                        t(principal, props.getProperty("principal",""));
                    val publicationStmt = e(fileDesc, "publicationStmt");
                        val authority = e(publicationStmt, "authority");
                        t(authority, props.getProperty("authority",props.getProperty("principal","")));
                        val pubPlace = e(publicationStmt, "pubPlace");
                        t(pubPlace, props.getProperty("pubplace",""));
                        val date = e(publicationStmt, "date");
                        t(date, year);
                        val availability = e(publicationStmt, "availability");
                        availability.setAttribute("status", "restricted");
                            val availabilityp = e(availability, "p");
                            t(availabilityp, "Copyright \u00A9 "+year+", "+props.getProperty("copyright",""));
                            val licence = e(availability, "licence");
                            licence.setAttribute("target", props.getProperty("licenseurl",""));
                            t(licence, props.getProperty("licensetext",""));
                    val sourceDesc = e(fileDesc, "sourceDesc");
                        e(sourceDesc, "bibl");
                val encodingDesc = e(teiHeader, "encodingDesc");
                    val editorialDecl = e(encodingDesc, "editorialDecl");
                        val correction = e(editorialDecl, "correction");
                        correction.setAttribute("status", "low");
                        t(e(correction, "p"), "OCR generated transcript has not been corrected.");
                        val normalization = e(editorialDecl, "normalization");
                        normalization.setAttribute("method", "markup");
                        t(e(normalization, "p"), "Only minor normalization where needed, if any, is indicated via markup.");
                        val punctuation = e(editorialDecl, "punctuation");
                        punctuation.setAttribute("marks", "all");
                        punctuation.setAttribute("placement", "internal");
                        t(e(punctuation, "p"), "All punctuation marks in the source text have been retained, and are represented by appropriate Unicode code points. Punctuation may be marked up with pc elements for clarification, but not exclusively.");
                        val quotation = e(editorialDecl, "quotation");
                        quotation.setAttribute("marks", "all");
                        t(e(quotation, "p"), "All quotation marks have been retained, and are represented by appropriate Unicode code points.");
                        val hyphenation = e(editorialDecl, "hyphenation");
                        hyphenation.setAttribute("eol", "all");
                        t(e(hyphenation, "p"), "Hyphenated words that appear at the end of a line have been retained. The hyphen, if present, is represented by the Unicode code point, and marked up with a pc element. The break=\"no\" attribute on the lb element indicates that a single word is split across the lines.");
                        val interpretation = e(editorialDecl, "interpretation");
                        t(e(interpretation, "p"), "Dates, places, and names may be marked up, where useful.");

                val profileDesc = e(teiHeader, "profileDesc");
                    val creation = e(profileDesc, "creation");
                        e(creation, "date");
                        val rs = e(creation, "rs");
                        rs.setAttribute("type", "place");
                    val langUsage = e(profileDesc, "langUsage");
                        val language = e(langUsage, "language");
                        language.setAttribute("ident",props.getProperty("langid", "en"));
                        t(language, props.getProperty("language","English"));
            val facsimile = e(tei, "facsimile");

            for (int pageInPdf = 1; pageInPdf <= pages.size(); ++pageInPdf) {
                val pageReal = pageRealFirst + pageInPdf - 1;
                val link = String.format(formatLink, pageReal);
                val id = String.format("page-%03d", pageReal);
                val graphic = e(facsimile, "graphic");
                graphic.setAttributeNS(XML_NAMESPACE, "id", id);
                graphic.setAttribute("url", link);
            }

            val text = e(tei, "text");
                val body = e(text, "body");
                    val ab = e(body, "ab");

            for (int pageInPdf = 1; pageInPdf <= pages.size(); ++pageInPdf) {
                val pageReal = pageRealFirst + pageInPdf - 1;
                val id = String.format("#page-%03d", pageReal);
                val pb = e(ab, "pb");
                pb.setAttribute("n", Integer.toString(pageInPdf));
                pb.setAttribute("facs", id);
                val lines = pages.get(pageInPdf - 1);
                if (lines.isEmpty()) {
                    t(ab, "[A transcription of this page is not available.]");
                }
                for (val line : lines) {
                    e(ab, "lb");
                    t(ab, line);
                }
            }

        val out = new BufferedOutputStream(System.out);
        serialize(doc, out, true, true);
        out.flush();
    }

    private static ArrayList<String> buildPage(final Node div) {
        val lines = new ArrayList<String>();

        val paragraphs = div.getChildNodes();
        for (int i = 0; i < paragraphs.getLength(); ++i) {
            addTextLines(paragraphs.item(i), lines);
        }

        return filterRotated(lines);
    }

    private static ArrayList<String> buildOcrPage(final Node node) {
        val lines = new ArrayList<String>();
        addTextLines(node, lines);
        return filterRotated(lines);
    }

    private static void addTextLines(final Node readFrom, final Collection<String> addTo) {
        addTextLines(readFrom.getTextContent(), addTo);
    }

    private static void addTextLines(final String text, final Collection<String> addTo) {
        if (!text.isBlank() && FindWords.isWords(text)) {
            try (val reader = new BufferedReader(new StringReader(text))) {
                for (var line = reader.readLine(); Objects.nonNull(line); line = reader.readLine()) {
                    if (!line.isBlank()) {
                        addTo.add(line.trim());
                    }
                }
            } catch (final IOException cantHappen) {
                throw new IllegalStateException(cantHappen);
            }
        }
    }

    /**
     * OCR of rotated text comes out with one or two characters per line.
     * Here we try to reassemble as best we can.
     * @param lines
     */
    private static ArrayList<String> filterRotated(final ArrayList<String> lines) {
        val result = new ArrayList<String>(lines.size());
        var s = new StringBuilder(100);
        for (int i = 0; i < lines.size(); ++i) {
            val line = lines.get(i);
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
