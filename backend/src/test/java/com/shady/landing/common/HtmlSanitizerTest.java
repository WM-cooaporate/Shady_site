package com.shady.landing.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.shady.landing.common.sanitize.HtmlSanitizer;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void keepsAllowedFormatting() {
        String html = "<p dir=\"rtl\">مرحبا <strong>bold</strong> <em>it</em></p><ul><li>one</li></ul>";
        assertThat(sanitizer.sanitizeRichText(html)).isEqualTo(html);
    }

    @Test
    void removesScriptsAndEventHandlers() {
        String dirty = "<p onclick=\"steal()\">hi<script>alert(1)</script><img src=x onerror=alert(1)></p>";
        assertThat(sanitizer.sanitizeRichText(dirty)).isEqualTo("<p>hi</p>");
    }

    @Test
    void removesJavascriptUrlsAndHardensLinks() {
        assertThat(sanitizer.sanitizeRichText("<a href=\"javascript:alert(1)\">x</a>")).doesNotContain("javascript");
        assertThat(sanitizer.sanitizeRichText("<a href=\"https://instagram.com/shady\">ig</a>"))
                .contains("href=\"https://instagram.com/shady\"")
                .contains("rel=\"noopener noreferrer nofollow\"");
    }

    @Test
    void removesStylesAndIframes() {
        String dirty = "<p style=\"background:url(x)\">a</p><iframe src=\"https://evil\"></iframe>";
        assertThat(sanitizer.sanitizeRichText(dirty)).isEqualTo("<p>a</p>");
    }

    @Test
    void blankBecomesNull() {
        assertThat(sanitizer.sanitizeRichText("   ")).isNull();
        assertThat(sanitizer.sanitizeRichText("<script>x</script>")).isNull();
        assertThat(sanitizer.sanitizeRichText(null)).isNull();
    }

    @Test
    void stripAllRemovesEveryTag() {
        assertThat(sanitizer.stripAll("<b>Mug</b> & <i>cup</i>")).isEqualTo("Mug &amp; cup");
    }
}
