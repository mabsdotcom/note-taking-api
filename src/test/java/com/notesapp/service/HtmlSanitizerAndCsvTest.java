package com.notesapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlSanitizerAndCsvTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void sanitize_keepsFormatting_removesScriptsAndHandlers() {
        String dirty = "<h1>Title</h1><p onclick=\"steal()\">Hi <b>bold</b> <a href=\"javascript:x()\">link</a></p>"
                + "<script>alert(1)</script><img src=\"https://x.com/a.png\" onerror=\"hack()\">";

        String clean = sanitizer.sanitize(dirty);

        assertThat(clean).contains("<h1>Title</h1>", "<b>bold</b>", "src=\"https://x.com/a.png\"");
        assertThat(clean).doesNotContain("script", "onclick", "onerror", "javascript:");
    }

    @Test
    void sanitize_nullOrBlank_returnsEmpty() {
        assertThat(sanitizer.sanitize(null)).isEmpty();
        assertThat(sanitizer.sanitize("  ")).isEmpty();
    }

    @Test
    void toPlainText_stripsTags() {
        assertThat(sanitizer.toPlainText("<p>Hello <b>world</b></p>")).isEqualTo("Hello world");
    }

    @Test
    void csvCell_quotesAndNeutralisesFormulas() {
        assertThat(CsvExportService.cell("plain")).isEqualTo("plain");
        assertThat(CsvExportService.cell("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvExportService.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvExportService.cell("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(CsvExportService.cell(null)).isEmpty();
    }
}
