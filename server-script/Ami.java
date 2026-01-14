import java.util.ArrayList;

public class Ami {

    private String username;       // le nom de l’ami
    private boolean accepte;       // il t’a accepté ou pas
    private ArrayList<String> messages; // messages échangés

    public Ami(String username, boolean accepte) {
        this.username = username;
        this.accepte = accepte;
        this.messages = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public boolean isAccepte() {
        return accepte;
    }

    public void setAccepte(boolean accepte) {
        this.accepte = accepte;
    }

    public void ajouterMessage(String msg) {
        messages.add(msg);
    }

    public ArrayList<String> getMessages() {
        return messages;
    }
}
