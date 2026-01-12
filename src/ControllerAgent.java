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

            frame.add(btnKill, BorderLayout.SOUTH);
            frame.setVisible(true);
        });


}}