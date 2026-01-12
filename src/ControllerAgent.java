import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ControllerAgent extends Agent {
    private JTextPane textPane;
    private StyledDocument doc;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Map<String, String> lastValues = new HashMap<>();
    private Map<String, String> lastAiResponse = new HashMap<>();

    @Override
    protected void setup() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ALARM SYSTEM (CONTROL CENTRAL)");
            frame.setSize(850, 500);
            frame.setLayout(new BorderLayout());

            JLabel lblTitlu = new JLabel(" MONITORIZARE ACTIVĂ DE SIGURANȚĂ ", SwingConstants.CENTER);
            lblTitlu.setOpaque(true);
            lblTitlu.setBackground(new Color(150, 50, 0));
            lblTitlu.setForeground(Color.WHITE);
            lblTitlu.setFont(new Font("Segoe UI", Font.BOLD, 18));
            frame.add(lblTitlu, BorderLayout.NORTH);

            textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setBackground(new Color(10, 10, 10));
            doc = textPane.getStyledDocument();

            JScrollPane scroll = new JScrollPane(textPane);
            frame.add(scroll, BorderLayout.CENTER);

            // butonul de oprire care declanseaza secventa
            JButton btnKill = new JButton("OPRIRE SECVENTIALA (SENZORI -> LOGGER -> OFF)");
            btnKill.setBackground(Color.RED);
            btnKill.addActionListener(e -> {
                // rulam oprirea pe un thread separat ca sa nu inghete interfata
                new Thread(this::oprireSecventiala).start();
            });
            frame.add(btnKill, BorderLayout.SOUTH);
            frame.setVisible(true);
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    procesareHibrida(msg.getContent());
                    trimiteLaLogger(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    // functia care opreste agentii pe rand
    private void oprireSecventiala() {
        try {
            logManual(">>> INITIERE PROCEDURA OPRIRE...");

            // pas 1: oprim senzorii
            logManual("1. Trimit comanda oprire la SENZORI...");
            ACLMessage msgSenzori = new ACLMessage(ACLMessage.REQUEST);
            msgSenzori.addReceiver(new AID("Senzori", AID.ISLOCALNAME));
            msgSenzori.setContent("SHUTDOWN_NOW");
            send(msgSenzori);

            // asteptam 1 secunda sa fim siguri ca s-au inchis
            Thread.sleep(1000);

            // pas 2: oprim loggerul
            logManual("2. Trimit comanda oprire la LOGGER...");
            // cautam loggerul in df pentru ca numele lui poate varia, dar serviciul e unic
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("logger-service");
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage msgLogger = new ACLMessage(ACLMessage.REQUEST);
                msgLogger.addReceiver(result[0].getName());
                msgLogger.setContent("SHUTDOWN_NOW");
                send(msgLogger);
            }

            // asteptam 1 secunda sa salveze fisierul
            Thread.sleep(1000);

            // pas 3: ne oprim pe noi si platforma
            logManual("3. Oprire CONTROLLER si Platforma JADE.");
            Thread.sleep(500);

            // inchidem platforma
            getContainerController().getPlatformController().kill();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // functie mica sa scriem in gui cand oprim
    private void logManual(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet stil = new SimpleAttributeSet();
                StyleConstants.setForeground(stil, Color.YELLOW);
                doc.insertString(doc.getLength(), text + "\n", stil);
            } catch (Exception e){}
        });
    }

    private void procesareHibrida(String json) {
        String tip = extrage(json, "tip");
        String valStr = extrage(json, "valoare");
        String unit = extrage(json, "unitate");
        double val = 0;
        try { val = Double.parseDouble(valStr); } catch (Exception e) { return; }

        String contextPericol = null;
        if (tip.equalsIgnoreCase("Temperatura")) {
            if (val < -10) contextPericol = "life threatening freeze";
            else if (val > 50) contextPericol = "imminent fire explosion";
            else if (val < 18) contextPericol = "too cold";
            else if (val > 30) contextPericol = "too hot";
        } else if (tip.equalsIgnoreCase("Umiditate")) {
            if (val < 0 || val > 100) contextPericol = "sensor electrical failure";
            else if (val > 80) contextPericol = "extreme humidity short circuit risk";
        } else if (tip.equalsIgnoreCase("Presiune")) {
            if (val < 0) contextPericol = "vacuum implosion risk";
            else if (val > 1200) contextPericol = "structural failure imminent";
        }

        String prevVal = lastValues.get(tip);
        boolean isChanged = (prevVal == null || !prevVal.equals(valStr));

        if (contextPericol == null) {
            lastValues.put(tip, valStr);
            lastAiResponse.put(tip, "OK");
            afiseaza(tip, valStr, unit, "OK", false);
            return;
        }

        if (isChanged) {
            lastValues.put(tip, valStr);
            String finalContext = contextPericol;
            new Thread(() -> {
                String mesajAI = OllamaService.genereazaMesajAlerta(tip, valStr, finalContext);
                lastAiResponse.put(tip, mesajAI);
                afiseaza(tip, valStr, unit, mesajAI, true);
                if (mesajAI.contains("FIRE") || mesajAI.contains("EXPLOSION") || mesajAI.contains("EVACUATE")) {
                    alertaVizuala(mesajAI);
                }
            }).start();
        } else {
            String oldMsg = lastAiResponse.get(tip);
            if (oldMsg == null) oldMsg = "ALERT: PERSISTENT DANGER";
            afiseaza(tip, valStr, unit, oldMsg, true);
        }
    }

    private void alertaVizuala(String mesaj) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "⚠️ CRITICAL ALERT ⚠️\n" + mesaj, "AI SAFETY MONITOR", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void afiseaza(String tip, String val, String unit, String mesaj, boolean isAlert) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = LocalTime.now().format(dtf);
                SimpleAttributeSet stil = new SimpleAttributeSet();
                StyleConstants.setFontFamily(stil, "Monospaced");
                StyleConstants.setFontSize(stil, 14);

                if (!isAlert) {
                    StyleConstants.setForeground(stil, Color.GREEN);
                    String line = String.format("[%s] %-12s: %5s %-4s\n", timestamp, tip.toUpperCase(), val, unit);
                    doc.insertString(doc.getLength(), line, stil);
                } else {
                    StyleConstants.setForeground(stil, Color.RED);
                    StyleConstants.setBold(stil, true);
                    String line = String.format("[%s] %-12s: %5s %-4s %s\n", timestamp, tip.toUpperCase(), val, unit, mesaj);
                    doc.insertString(doc.getLength(), line, stil);
                }
                textPane.setCaretPosition(doc.getLength());
            } catch (Exception e) {}
        });
    }

    private String extrage(String json, String cheie) {
        int start = json.indexOf("\"" + cheie + "\":");
        if (start == -1) return "0";
        start = json.indexOf(":", start) + 1;
        if (json.indexOf("\"", start) == start || json.indexOf("\"", start) == start + 1) {
            int q1 = json.indexOf("\"", start);
            int q2 = json.indexOf("\"", q1 + 1);
            return json.substring(q1 + 1, q2);
        } else {
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
    }

    private void trimiteLaLogger(String data) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("logger-service");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                m.addReceiver(result[0].getName());
                m.setContent(data);
                send(m);
            }
        } catch (Exception e) {}
    }
}