import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class LoggerAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("Logger Agent " + getLocalName() + " a pornit.");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("logger-service");
        sd.setName("salvare-istoric");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    // verificam daca e comanda de oprire sau log normal
                    if (msg.getContent().equals("SHUTDOWN_NOW")) {
                        System.out.println("Logger: Am primit comanda de oprire. Salvez tot si ma inchid.");
                        doDelete(); // ne oprim
                    } else {
                        scrieInFisier(msg.getContent());
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void scrieInFisier(String continut) {
        try (FileWriter fw = new FileWriter("istoric_senzori.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            out.println("[" + LocalDateTime.now() + "] " + continut);
            System.out.println("LOGGED: " + continut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
        System.out.println("Agent Logger oprit.");
    }
}