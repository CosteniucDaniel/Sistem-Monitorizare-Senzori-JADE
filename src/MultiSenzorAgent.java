import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import javax.swing.*;
import java.awt.*;

public class MultiSenzorAgent extends Agent {
    private JFrame frame;
    private JSlider sliderTemp, sliderUmid, sliderPres;
    private JLabel lblTemp, lblUmid, lblPres;

    @Override
    protected void setup() {
        // --- GUI CONFIGURATION ---
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Panou Control Senzori (Simulator)");
            frame.setSize(400, 500);
            frame.setLayout(new GridLayout(7, 1));
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            JLabel title = new JLabel("Configurare Valori Senzori", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 16));
            frame.add(title);

            // temperatura
            JPanel p1 = new JPanel(new BorderLayout());
            p1.setBorder(BorderFactory.createTitledBorder("Temperatura (°C)"));
            sliderTemp = new JSlider(JSlider.HORIZONTAL, -100, 200, 20);
            sliderTemp.setMajorTickSpacing(50);
            sliderTemp.setPaintTicks(true);
            sliderTemp.setPaintLabels(true);
            lblTemp = new JLabel("20 °C", SwingConstants.CENTER);
            sliderTemp.addChangeListener(e -> lblTemp.setText(sliderTemp.getValue() + " °C"));
            p1.add(sliderTemp, BorderLayout.CENTER);
            p1.add(lblTemp, BorderLayout.EAST);
            frame.add(p1);

            // umiditate
            JPanel p2 = new JPanel(new BorderLayout());
            p2.setBorder(BorderFactory.createTitledBorder("Umiditate (%)"));
            sliderUmid = new JSlider(JSlider.HORIZONTAL, -50, 150, 45);
            sliderUmid.setMajorTickSpacing(50);
            sliderUmid.setPaintTicks(true);
            sliderUmid.setPaintLabels(true);
            lblUmid = new JLabel("45 %", SwingConstants.CENTER);
            sliderUmid.addChangeListener(e -> lblUmid.setText(sliderUmid.getValue() + " %"));
            p2.add(sliderUmid, BorderLayout.CENTER);
            p2.add(lblUmid, BorderLayout.EAST);
            frame.add(p2);

            // presiune
            JPanel p3 = new JPanel(new BorderLayout());
            p3.setBorder(BorderFactory.createTitledBorder("Presiune (hPa)"));
            sliderPres = new JSlider(JSlider.HORIZONTAL, -500, 2000, 1013);
            sliderPres.setMajorTickSpacing(500);
            sliderPres.setPaintTicks(true);
            sliderPres.setPaintLabels(true);
            lblPres = new JLabel("1013 hPa", SwingConstants.CENTER);
            sliderPres.addChangeListener(e -> lblPres.setText(sliderPres.getValue() + " hPa"));
            p3.add(sliderPres, BorderLayout.CENTER);
            p3.add(lblPres, BorderLayout.EAST);
            frame.add(p3);

            JLabel info = new JLabel("<html><center>Datele se transmit automat la Controller<br>la fiecare 8 secunde.</center></html>", SwingConstants.CENTER);
            info.setForeground(Color.GRAY);
            frame.add(info);

            frame.setVisible(true);
        });

        // 1. comportament de transmisie date (ticker)
        addBehaviour(new TickerBehaviour(this, 8000) {
            @Override
            protected void onTick() {
                if (frame == null || !frame.isVisible()) return;
                trimiteMesaj("Temperatura", sliderTemp.getValue(), "°C");
                trimiteMesaj("Umiditate", sliderUmid.getValue(), "%");
                trimiteMesaj("Presiune", sliderPres.getValue(), "hPa");
            }
        });

        // 2. comportament de ascultare comanda oprire (cyclic)
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getContent().equals("SHUTDOWN_NOW")) {
                    System.out.println("Senzori: Am primit comanda de oprire. Ma inchid.");
                    doDelete(); // asta opreste agentul si apeleaza takedown
                } else {
                    block();
                }
            }
        });
    }

    private void trimiteMesaj(String tip, int valoare, String unitate) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("Controller", AID.ISLOCALNAME));
        String json = String.format("{\"tip\": \"%s\", \"valoare\": %d, \"unitate\": \"%s\"}", tip, valoare, unitate);
        msg.setContent(json);

        System.out.println(">> [Senzor-TEST] Trimit date: " + tip + " = " + valoare + " " + unitate);
        System.out.println("   Continut JSON: " + json);

        send(msg);
    }

    @Override
    protected void takeDown() {
        // inchidem fereastra gui
        SwingUtilities.invokeLater(() -> { if(frame!=null) frame.dispose(); });
        System.out.println("Agent Senzori oprit.");
    }
}