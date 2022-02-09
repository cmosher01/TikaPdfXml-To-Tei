package nu.mine.mosher.patterns.fluent;

import com.google.common.xml.XmlEscapers;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Simple XML fragment builder.
 *
 * Use {@link Fragment#elem(String)} for elements,
 * {@link Attribute#attr(String, String)} for attributes,
 * {@link Fragment#text(String)} for text.
 *
 * Use {@link Fragment#end()} to close the most recent element, if any.
 *
 * Use {@link Fragment#toString()} to build the fragment.
 *
 * Escapes text content and attributes.
 *
 * Does not check for malformed names of elements or attributes.
 */
public class Fragment {
    private final Deque<String> within = new LinkedList<>();
    private final StringBuilder out = new StringBuilder(128);

    public Attribute elem(final String name) {
        this.out.append("<").append(name);
        this.within.push(name);
        return new Attribute(this);
    }

    public Fragment text(final String text) {
        this.out.append(escText(text));
        return this;
    }

    public Fragment frag(final String frag) {
        this.out.append(frag);
        return this;
    }

    public Fragment end() {
        if (!this.within.isEmpty()) {
            final String elem = this.within.pop();
            this.out.append("</").append(elem).append(">");
        }
        return this;
    }

    @Override
    public String toString() {
        while (!this.within.isEmpty()) {
            end();
        }
        return this.out.toString();
    }

    public static class Attribute extends Fragment {
        private final Fragment outer;

        private Attribute(final Fragment fragment) {
            this.outer = fragment;
        }

        public Attribute attr(final String name) {
            return attr(name, "");
        }

        public Attribute attr(final String name, final String value) {
            this.outer.out.append(" ").append(name).append("=\"").append(escAttr(value)).append("\"");
            return this;
        }

        public Attribute elem(final String name) {
            this.outer.out.append(">");
            return this.outer.elem(name);
        }

        public Fragment text(final String text) {
            this.outer.out.append(">");
            return this.outer.text(text);
        }

        public Fragment frag(final String frag) {
            this.outer.out.append(">");
            return this.outer.frag(frag);
        }

        public Fragment end() {
            this.outer.within.pop();
            this.outer.out.append("/>");
            return this.outer;
        }

        @Override
        public String toString() {
            end();
            return this.outer.toString();
        }
    }

    private static String escText(final String s) {
        return XmlEscapers.xmlContentEscaper().escape(s);
    }

    private static String escAttr(final String s) {
        return XmlEscapers.xmlAttributeEscaper().escape(s);
    }
}
