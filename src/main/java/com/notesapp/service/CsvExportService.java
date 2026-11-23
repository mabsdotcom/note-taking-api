package com.notesapp.service;

import java.util.List;
import java.util.stream.Collectors;

import com.notesapp.entity.Note;
import com.notesapp.entity.Tag;
import org.springframework.stereotype.Component;

/** Converts notes to RFC 4180 CSV. */
@Component
public class CsvExportService {

    private static final String HEADER = "id,title,category,tags,pinned,archived,createdAt,updatedAt,content";

    public String toCsv(List<Note> notes) {
        StringBuilder sb = new StringBuilder(HEADER).append("\r\n");
        for (Note n : notes) {
            String tags = n.getTags().stream().map(Tag::getName).sorted().collect(Collectors.joining(";"));
            sb.append(n.getId()).append(',')
                    .append(cell(n.getTitle())).append(',')
                    .append(cell(n.getCategory() == null ? "" : n.getCategory().getName())).append(',')
                    .append(cell(tags)).append(',')
                    .append(n.isPinned()).append(',')
                    .append(n.isArchived()).append(',')
                    .append(n.getCreatedAt()).append(',')
                    .append(n.getUpdatedAt()).append(',')
                    .append(cell(n.getPlainText()))
                    .append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Quotes a value when needed and neutralises spreadsheet formula injection
     * (cells starting with =, +, - or @ are prefixed with an apostrophe).
     */
    static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String v = value;
        char first = v.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
