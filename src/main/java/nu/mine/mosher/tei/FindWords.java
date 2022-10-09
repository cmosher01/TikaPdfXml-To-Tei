package nu.mine.mosher.tei;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class FindWords {
    private static final float MIN_GOOD_RATE = 2f/3f;

    public static void main(final String... args) throws IOException {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            in.lines().filter(FindWords::isWords).forEach(System.out::println);
        } finally {
            System.out.flush();
        }
    }

    public static boolean isWords(final String s) {

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

        final float rateGood = ((float)cAlphaNum) / ((float)cNonSpace);

        // at least one alpha-numeric and too short, or high ratio of letters to symbols
        final boolean ok = (1 <= cAlphaNum && cAlphaNum+cNonSpace <= 5) ||MIN_GOOD_RATE <= rateGood;

        log.trace("{}: {}/{} {} \"{}\"", (ok?"OK":"NG"), cAlphaNum, cNonSpace, rateGood, s);

        return ok;
    }
}
