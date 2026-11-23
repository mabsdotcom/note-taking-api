package com.notesapp.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Rich text notes are stored as HTML. Before saving, the HTML is cleaned against an
 * allow-list so that stored XSS (script tags, event handlers, javascript: links) is impossible.
 */
@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("s", "mark", "hr")
            .addAttributes(":all", "class")
            .addProtocols("a", "href", "http", "https", "mailto");

    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, "", SAFELIST, OUTPUT);
    }

    /** Text without markup; used for searching and excerpts. */
    public String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }
}
