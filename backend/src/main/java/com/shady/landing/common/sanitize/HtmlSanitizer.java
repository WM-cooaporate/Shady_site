package com.shady.landing.common.sanitize;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Allowlist sanitizer for admin-authored rich text (descriptions, about/footer bodies). Anything not
 * explicitly allowed — scripts, event handlers, styles, iframes, javascript: URLs — is removed.
 */
@Component
public class HtmlSanitizer {

    private static final PolicyFactory RICH_TEXT = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "b", "em", "i", "u", "s", "ul", "ol", "li",
                    "h2", "h3", "h4", "blockquote", "a")
            .allowAttributes("href").onElements("a")
            .allowStandardUrlProtocols()
            .requireRelsOnLinks("noopener", "noreferrer", "nofollow")
            .allowAttributes("dir").matching(false, "rtl", "ltr", "auto").globally()
            .toFactory();

    private static final PolicyFactory NO_HTML = new HtmlPolicyBuilder().toFactory();

    /** Sanitizes rich text; {@code null} stays {@code null}, blank becomes {@code null}. */
    public String sanitizeRichText(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String clean = RICH_TEXT.sanitize(html).trim();
        return clean.isEmpty() ? null : clean;
    }

    /** Removes all markup (for fields that must be plain text). Output is HTML-escaped. */
    public String stripAll(String text) {
        return text == null ? null : NO_HTML.sanitize(text).trim();
    }
}
