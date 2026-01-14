import java.util.ArrayList;

public class Utilisateur {
    private String username;
    private boolean connecte;

    private String ip;
    private int port;

    private long lastPong;

    private ArrayList<String> amis;
    private ArrayList<String> DemandeRecues;
    private ArrayList<String> DemandeEnvoye;

    public Utilisateur(String username) {
        this.username = username;
        this.connecte = false;

        this.ip = "";
        this.port = 0;
        this.lastPong = 0;

        this.amis = new ArrayList<>();
        this.DemandeRecues = new ArrayList<>();
        this.DemandeEnvoye = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnecte() {
        return connecte;
    }

    public void setConnecte(boolean connecte) {
        this.connecte = connecte;
    }

    public ArrayList<String> getAmis() { 
        return amis; 
    }

    public ArrayList<String> getDemandesRecues() { 
        return DemandeRecues; 
    }

    public ArrayList<String> getDemandesEnvoyees() { 
        return DemandeEnvoye; 
    }

    // === NOUVEAU : IP / PORT ===
    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    // === NOUVEAU : Dernier PONG ===
    public void setLastPong(long lastPong) {
        this.lastPong = lastPong;
    }

    public long getLastPong() {
        return lastPong;
    }
}
