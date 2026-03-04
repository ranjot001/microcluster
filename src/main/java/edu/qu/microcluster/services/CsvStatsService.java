package edu.qu.microcluster.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvStatsService implements Service {

    @Override
    public String execute(String action, String payload) {
        String a = norm(action);
        if (a.equals("DEFAULT")) a = "STATS";
        if (!a.equals("STATS")) throw new IllegalArgumentException("CSV supports STATS only");

        return computeStats(payload);
    }

    private String computeStats(String csvText) {
        String[] lines = csvText.trim().split("\\R");
        if (lines.length < 2) return "Not enough rows (need header + data)";

        String[] headers = lines[0].trim().split(",");
        int cols = headers.length;

        List<List<Double>> columns = new ArrayList<>();
        for (int i = 0; i < cols; i++) columns.add(new ArrayList<>());

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] values = line.split(",", -1);
            if (values.length != cols) {
                throw new IllegalArgumentException("Row " + (i + 1) + " has " + values.length + " cols, expected " + cols);
            }

            for (int j = 0; j < cols; j++) {
                columns.get(j).add(Double.parseDouble(values[j].trim()));
            }
        }

        StringBuilder out = new StringBuilder();
        for (int c = 0; c < cols; c++) {
            List<Double> data = columns.get(c);
            if (data.isEmpty()) continue;

            Collections.sort(data);

            double min = data.get(0);
            double max = data.get(data.size() - 1);
            double mean = mean(data);
            double median = median(data);
            double std = stddev(data, mean);

            out.append("Column: ").append(headers[c].trim()).append("\n")
                    .append("Min: ").append(min).append("\n")
                    .append("Max: ").append(max).append("\n")
                    .append("Mean: ").append(mean).append("\n")
                    .append("Median: ").append(median).append("\n")
                    .append("StdDev: ").append(std).append("\n\n");
        }

        return out.toString().trim();
    }

    private double mean(List<Double> x) {
        double s = 0;
        for (double v : x) s += v;
        return s / x.size();
    }

    private double median(List<Double> x) {
        int n = x.size();
        if (n % 2 == 1) return x.get(n / 2);
        return (x.get(n / 2 - 1) + x.get(n / 2)) / 2.0;
    }

    private double stddev(List<Double> x, double mean) {
        double s = 0;
        for (double v : x) {
            double d = v - mean;
            s += d * d;
        }
        return Math.sqrt(s / x.size());
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}