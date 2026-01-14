import java.util.ArrayList;

public class Groupe {
    private String nom;
    private ArrayList<String> membres;

    public Groupe(String nom){
        this.nom = nom;
        this.membres = new ArrayList<>();
    }

    public String getNom() { return nom; }
    public ArrayList<String> getMembres() { return membres; }

    public void ajouterMembre(String username) {
        if (!membres.contains(username)) {
            membres.add(username);
        }
    }
}
