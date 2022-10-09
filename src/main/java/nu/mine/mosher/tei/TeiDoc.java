package nu.mine.mosher.tei;

import lombok.*;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

import static nu.mine.mosher.tei.XmlUtils.*;

public class TeiDoc {
    private static final double MIN_GOOD_RATE = 2D/3D;

    public static void main(final String... args) throws IOException, TransformerException {
        if (args.length != 2) {
            System.out.println("usage: tikapdfxml-to-tei conf.properties path/to/title <tesseract.txt >title.tei.xml");
            return;
        }

        val props = new Properties();
        props.load(new FileInputStream(args[0]));

        val pages = new ArrayList<List<String>>();
        val scanPages = new Scanner(new BufferedInputStream(new FileInputStream(FileDescriptor.in)), StandardCharsets.UTF_8)
            .useDelimiter("\f");
        while (scanPages.hasNext()) {
            pages.add(lines(scanPages.next()));
        }

        val link = props.getProperty("imgUrlPrefix")+args[1]+"/p%04d.ptif"+props.getProperty("imgUrlSuffix");
        printTei(pages, link, 1, titleFromPath(args[1]), props);

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

    private static String titleFromPath(final String pathToTitle) {
        val s = Objects.requireNonNull(pathToTitle.split("/"));
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

    @SneakyThrows
    private static ArrayList<String> lines(final String page) {
        val r = new ArrayList<String>();
        if (!page.isBlank() && isWords(page)) {
            try (val reader = new BufferedReader(new StringReader(page))) {
                for (var line = reader.readLine(); Objects.nonNull(line); line = reader.readLine()) {
                    if (!line.isBlank()) {
                        r.add(line.trim());
                    }
                }
            }
        }
        return r;
    }

    private static boolean isWords(final String s) {
        int cNonSpace = 0;
        int cAlphaNum = 0;
        for(int c : s.codePoints().toArray()){
            if (!Character.isWhitespace(c)) {
                ++cNonSpace;
                if (Character.isLetterOrDigit(c)) {
                    ++cAlphaNum;
                }
            }
        }

        val rateGood = ((double)cAlphaNum) / ((double)cNonSpace);

        return (1 <= cAlphaNum && cAlphaNum+cNonSpace <= 5) || MIN_GOOD_RATE <= rateGood;
    }
}
