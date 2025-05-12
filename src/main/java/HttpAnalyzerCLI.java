import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HttpAnalyzerCLI {

    public static void main(String[] args) {
        System.out.println("HTTP Protocol Analyzer (CSV Version)");

        String inputCsv = "http_input.csv";  // Input file should contain URL,Method
        String outputCsv = "http_output_results1.csv";

        List<String[]> entries = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputCsv))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.toLowerCase().startsWith("url")) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        entries.add(new String[]{parts[0].trim(), parts[1].trim().toUpperCase()});
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to read input CSV: " + e.getMessage());
            return;
        }

        try (PrintWriter writer = new PrintWriter(outputCsv)) {
            writer.println("URL,Method,Status,Time (ms),Content Length");

            for (String[] entry : entries) {
                String urlStr = entry[0];
                String method = entry[1];
                try {
                    long startTime = System.nanoTime();
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "JavaHttpAnalyzerCSV");
                    conn.setRequestProperty("Content-Type", "application/json");

                    if (method.equals("POST") || method.equals("PUT")) {
                        conn.setDoOutput(true);
                        try (OutputStream os = conn.getOutputStream()) {
                            byte[] input = "{}".getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }
                    }

                    int statusCode = conn.getResponseCode();
                    InputStream is = (statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (is != null) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        is.close();
                    }
                    int actualLength = baos.size();

                    long endTime = System.nanoTime();
                    long timeMs = (endTime - startTime) / 1_000_000;

                    System.out.printf("URL: %s | Method: %s | Status: %d | Time: %dms | Length: %d bytes%n",
                            urlStr, method, statusCode, timeMs, actualLength);
                    writer.printf("%s,%s,%d,%d,%d%n", urlStr, method, statusCode, timeMs, actualLength);

                } catch (Exception e) {
                    System.out.printf("Error with URL: %s (%s)%n", urlStr, e.getMessage());
                    writer.printf("%s,%s,Error,-,-%n", urlStr, method);
                }
            }

            System.out.println("Results saved to " + outputCsv);
        } catch (Exception e) {
            System.out.println("Failed to write to CSV: " + e.getMessage());
        }
    }
}
