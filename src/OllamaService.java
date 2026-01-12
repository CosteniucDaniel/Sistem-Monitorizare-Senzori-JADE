import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OllamaService {

    // functie statica sa o putem apela direct din controller
    public static String genereazaMesajAlerta(String tip, String valoare, String contextPericol) {
        try {
            // folosim modelul llama3.2 instalat local
            String modelName = "llama3.2";

            // conexiunea http catre serverul local ollama
            URL url = new URL("http://localhost:11434/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // promptul pentru ai: ii zicem sa tipe la noi
            // vrem doar mesajul de panica, fara alte explicatii
            String prompt = String.format(
                    "You are a Safety Alarm System. Sensor %s reads %s (%s). " +
                            "Task: SCREAM a short, panic warning to the human operator (Max 5 words). " +
                            "Examples: 'RUN AWAY! FIRE RISK!', 'EVACUATE ROOM NOW!', 'SENSOR DESTROYED! REPLACE!', 'GAS LEAK! DANGER!'. " +
                            "Output ONLY the warning text.",
                    tip, valoare, contextPericol
            );

            System.out.println("--> AI Analizează: " + prompt);

            // impachetam cererea pentru api-ul ollama
            String jsonInputString = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                    modelName, prompt
            );

            // trimitem datele
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // citim raspunsul
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            String raw = response.toString();

            // extragem textul relevant din json-ul primit de la ollama
            if (raw.contains("\"response\":\"")) {
                int start = raw.indexOf("\"response\":\"") + 12;
                String text = raw.substring(start);
                int end = text.indexOf("\",");
                // curatam caracterele speciale daca exista
                while (end != -1 && text.charAt(end - 1) == '\\') { end = text.indexOf("\",", end + 1); }

                if (end > 0) {
                    String clean = text.substring(0, end).replace("\\n", "").replace("\\\"", "").trim();
                    return ">>> ALARM: " + clean.toUpperCase();
                }
            }
            return ">>> ALARM: DANGER DETECTED";

        } catch (Exception e) {
            e.printStackTrace();
            return ">>> ALARM: ERROR";
        }
    }
}