package com.snapverify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ScreenshotRepository {

    private final List<ScreenshotItem> items = new ArrayList<>();

    public void clear() {
        items.clear();
    }

    public void add(ScreenshotItem item) {
        items.add(item);
    }

    public void addAll(List<ScreenshotItem> newItems) {
        items.addAll(newItems);
    }

    public List<ScreenshotItem> search(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(items);
        }

        String q = query.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item ->
                        item.displayName().toLowerCase(Locale.ROOT).contains(q) ||
                                (item.ocrText() != null && item.ocrText().toLowerCase(Locale.ROOT).contains(q)))
                .collect(Collectors.toList());
    }
}