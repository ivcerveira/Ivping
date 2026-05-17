package com.nuapps.ivping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class IpHistoryManager {
    private static final String PREFERENCE_KEY = "ping.ip.history";
    private final Preferences preferences;
    private final int maxSize;

    public IpHistoryManager() {
        this(10);
    }

    public IpHistoryManager(int maxSize) {
        this.preferences = Preferences.userNodeForPackage(IpHistoryManager.class);
        this.maxSize = maxSize;
    }

    public List<String> loadHistory() {
        String rawHistory = preferences.get(PREFERENCE_KEY, "");
        if (rawHistory.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Arrays.stream(rawHistory.split("\\R"))
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .forEach(unique::add);

        List<String> limitedHistory = new ArrayList<>(unique);
        if (limitedHistory.size() > maxSize) {
            return new ArrayList<>(limitedHistory.subList(0, maxSize));
        }
        return limitedHistory;
    }

    public void addIp(String ipAddress) {
        if (ipAddress == null) {
            return;
        }

        String normalizedIp = ipAddress.trim();
        if (normalizedIp.isEmpty()) {
            return;
        }

        List<String> history = new ArrayList<>(loadHistory());
        history.removeIf(existing -> existing.equals(normalizedIp));
        history.addFirst(normalizedIp);

        if (history.size() > maxSize) {
            history = new ArrayList<>(history.subList(0, maxSize));
        }

        saveHistory(history);
    }

    private void saveHistory(List<String> history) {
        preferences.put(PREFERENCE_KEY, String.join("\n", history));
        try {
            preferences.flush();
        } catch (BackingStoreException ignored) {
            // Keep running even if preferences cannot be flushed immediately.
        }
    }
}
