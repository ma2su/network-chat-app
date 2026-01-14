import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class ServeurUDP {

    static final int port = 6010;

    static ArrayList<Utilisateur> utilisateurs = new ArrayList<>();
    static ArrayList<Groupe> groupes = new ArrayList<>();

    // Pour login
    static String lastClientIP = "";
    static int lastClientPort = 0;

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket(port);
        byte[] buffer = new byte[1024];

        System.out.println("Serveur UDP en attente...");

        // ============================
        //   THREAD PING
        // ============================
        new Thread(() -> {

            while (true) {
                try {
                    Thread.sleep(5000);

                    long now = System.currentTimeMillis();

                    for (Utilisateur u : utilisateurs) {

                        if (!u.isConnecte()) continue;

                        // Timeout 6 sec
                        if (now - u.getLastPong() > 6000) {
                            u.setConnecte(false);
                            System.out.println("Déconnexion automatique : " + u.getUsername());
                            continue;
                        }

                        try {
                            String ping = "PING;" + u.getUsername();
                            byte[] data = ping.getBytes();

                            InetAddress addr = InetAddress.getByName(u.getIp());

                            DatagramPacket packetPing =
                                    new DatagramPacket(data, data.length, addr, u.getPort());

                            socket.send(packetPing);

                        } catch (Exception e) {
                            System.out.println("Erreur PING vers " + u.getUsername());
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
        // ============================


        // ============================
        //     BOUCLE PRINCIPALE
        // ============================
        while (true) {

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String CLIENT_IP = packet.getAddress().getHostAddress();
            int CLIENT_PORT = packet.getPort();

            lastClientIP = CLIENT_IP;
            lastClientPort = CLIENT_PORT;

            String msg = new String(packet.getData(), 0, packet.getLength()).trim();
            System.out.println("Reçu : " + msg);

            String reponse = traiterCommande(msg);

            byte[] data = reponse.getBytes();
            InetAddress addr = InetAddress.getByName(CLIENT_IP);

            DatagramPacket sendPacket =
                    new DatagramPacket(data, data.length, addr, CLIENT_PORT);

            socket.send(sendPacket);

            System.out.println("Réponse envoyée : " + reponse);
        }
    }

    // ============================
    //   ROUTEUR DE COMMANDES
    // ============================
    public static String traiterCommande(String message) {

        String[] parts = message.split(";");

        String cmd = parts[0];

        switch (cmd) {

            case "REGISTER":
                return register(parts[1]);

            case "LOGIN":
                return login(parts[1]);

            case "DISCONECT":
                return disconnect(parts[1]);

            case "GET_AMIS":
                return getAmis(parts[1]);

            case "GET_REQUESTS":
                return getRequests(parts[1]);

            case "GET_SENT":
                return getSentRequests(parts[1]);

            case "ADD_FRIEND":
                return addFriend(parts[1], parts[2]);

            case "ACCEPT_FRIEND":
                return AcceptFriend(parts[1], parts[2]);

            case "DECLINE_FRIEND":
                return declineFriend(parts[1], parts[2]);

            case "SEND_CHAT":
                return SEND_CHAT(parts[1], parts[2], parts[3]);

            case "GET_CHATS":
                return GET_CHAT(parts[1], parts[2]);

            case "PONG":
                return handlePong(parts[1]);

            case "SUPPR_FRIEND":
                return supprimerfriend(parts[1], parts[2]);

            case "CREATE_GROUP":
                return createGroup(parts[1], parts[2]);

        case "ADD_TO_GROUP":
            return addToGroup(parts[1], parts[2], parts[3]);

        case "REMOVE_FROM_GROUP":
            return removeFromGroup(parts[1], parts[2], parts[3]);

        case "DELETE_GROUP":
            return deleteGroup(parts[1], parts[2]);

        case "SEND_GROUP":
            return SEND_GROUP(parts[1], parts[2], parts[3]);
        case "GET_GROUP":
            return GET_GROUP(parts[1], parts[2]);

        case "GET_GROUPS":
            return GET_GROUPS(parts[1]);
        
        case "GET_GROUP_MEMBERS":
            return GET_GROUP_MEMBERS(parts[1], parts[2]);
        case "DELETE_CHAT":
            return supprdiscussion(parts [1], parts[2]);
            default:
                return "ERREUR:COMMANDE_INCONNUE";
        }
    }

    // ============================
    //          PONG
    // ============================
    public static String handlePong(String username) {

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(username)) {
                u.setLastPong(System.currentTimeMillis());
                return "OK:PONG_RECEIVED";
            }
        }
        return "ERROR:UNKNOWN_USER";
    }

   public static String connected(String ahah){
    Utilisateur u1 = null;
    
    for (Utilisateur u : utilisateurs) {
        if (u.getUsername().equals(ahah)) {
            u1 = u;
            break;
        }
    }

    if (u1 == null) {
        return "ERROR:USER_NOT_FOUND";
    }

    if (u1.isConnecte()) {
        return "YES:CONNECTED";
    } else {
        return "NO:CONNECTED";
    }
    }


    // ============================
    //     REGISTER + LOGIN
    // ============================
    public static String register(String username) {

        if (utilisateurs.size() >= 6)
            return "ERREUR:LISTE_COMPLET";

        for (Utilisateur u : utilisateurs)
            if (u.getUsername().equals(username))
                return "ERREUR:UTILISATEUR_EXISTE";

        utilisateurs.add(new Utilisateur(username));
        return "OK:UTILISATEUR_CREE";
    }

    public static String login(String username) {

        for (Utilisateur u : utilisateurs) {

            if (u.getUsername().equals(username)) {

                if (u.isConnecte())
                    return "ERREUR:DEJA_CONNECTE";

                u.setConnecte(true);
                u.setIp(lastClientIP);
                u.setPort(lastClientPort);
                u.setLastPong(System.currentTimeMillis());

                return "OK:CONNECTE";
            }
        }
        return "ERREUR:UTILISATEUR_INCONNU";
    }

    public static String disconnect(String username) {

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(username)) {

                if (!u.isConnecte())
                    return "ERROR:NOT_CONNECTE";

                u.setConnecte(false);
                return "OK:DECONNECTE";
            }
        }
        return "ERROR:UTILISATEUR_INCONNU";
    }

    // ============================
    //   AMIS
    // ============================
    public static String addFriend(String sender, String receiver) {

        Utilisateur u1 = null;
        Utilisateur u2 = null;

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(sender)) u1 = u;
            if (u.getUsername().equals(receiver)) u2 = u;
        }

        if (u1 == null || u2 == null)
            return "ERROR:USER_NOT_FOUND";

        if (u1.getAmis().contains(receiver))
            return "ERROR:ALREADY_FRIENDS";

        if (u1.getDemandesEnvoyees().contains(receiver))
            return "ERROR:ALREADY_SENT";

        if (u1.getDemandesRecues().contains(receiver))
            return "ERROR:THEY_SENT_YOU";

        u1.getDemandesEnvoyees().add(receiver);
        u2.getDemandesRecues().add(sender);

        return "OK:REQUEST_SENT";
    }

    public static String getAmis(String username) {

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(username)) {

                if (u.getAmis().isEmpty())
                    return "";

                return "" + String.join(",", u.getAmis());
            }
        }
        return "ERROR:UTILISATEUR_INCONNU";
    }

    public static String getRequests(String username) {

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(username)) {

                if (u.getDemandesRecues().isEmpty())
                    return "";

                return "" + String.join(",", u.getDemandesRecues());
            }
        }
        return "ERROR:UTILISATEUR_INCONNU";
    }

    public static String getSentRequests(String username) {

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(username)) {

                if (u.getDemandesEnvoyees().isEmpty())
                    return "SENT:VIDE";

                return "SENT:" + String.join(",", u.getDemandesEnvoyees());
            }
        }
        return "ERROR:UTILISATEUR_INCONNU";
    }

    public static String AcceptFriend(String receiver, String sender) {

        Utilisateur u1 = null;
        Utilisateur u2 = null;

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(receiver)) u1 = u;
            if (u.getUsername().equals(sender)) u2 = u;
        }

        if (u1 == null || u2 == null)
            return "ERROR:USER_NOT_FOUND";

        if (!u1.getDemandesRecues().contains(sender))
            return "ERROR:NO_REQUEST";

        u1.getAmis().add(sender);
        u2.getAmis().add(receiver);

        u1.getDemandesRecues().remove(sender);
        u2.getDemandesEnvoyees().remove(receiver);

        return "OK:NOW_FRIENDS";
    }

    public static String declineFriend(String receiver, String sender) {

        Utilisateur u1 = null;
        Utilisateur u2 = null;

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(receiver)) u1 = u;
            if (u.getUsername().equals(sender)) u2 = u;
        }

        if (u1 == null || u2 == null)
            return "ERROR:USER_NOT_FOUND";

        if (!u1.getDemandesRecues().contains(sender))
            return "ERROR:NO_REQUEST";

        u1.getDemandesRecues().remove(sender);
        u2.getDemandesEnvoyees().remove(receiver);

        return "OK:REQUEST_DECLINED";
    }

    public static String supprimerfriend(String supprimeur, String supprime) {

        Utilisateur u1 = null;
        Utilisateur u2 = null;

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(supprimeur)) u1 = u;
            if (u.getUsername().equals(supprime)) u2 = u;
        }

        if (u1 == null || u2 == null)
            return "ERROR:USER_NOT_FOUND";

        if (!u1.getAmis().contains(supprime) || !u2.getAmis().contains(supprimeur))
            return "ERROR:NOT_FRIENDS";

        u1.getAmis().remove(supprime);
        u2.getAmis().remove(supprimeur);

        return "OK:AMI_SUPPRIME";
    }

    // ============================
    //       CHAT PRIVÉ
    // ============================
    public static String SEND_CHAT(String sender, String receiver, String message) {

        Utilisateur u1 = null;
        Utilisateur u2 = null;

        for (Utilisateur u : utilisateurs) {
            if (u.getUsername().equals(sender)) u1 = u;
            if (u.getUsername().equals(receiver)) u2 = u;
        }

        if (u1 == null || u2 == null)
            return "ERROR:USER_NOT_FOUND";

        if (!u1.getAmis().contains(receiver) || !u2.getAmis().contains(sender))
            return "ERROR:NOT_FRIENDS";

        try {
            String userA = sender.compareTo(receiver) < 0 ? sender : receiver;
            String userB = sender.compareTo(receiver) < 0 ? receiver : sender;

            String fileName = "conversation_" + userA + "_" + userB + ".csv";

            String timestamp = java.time.LocalDateTime.now().toString();

            java.io.FileWriter fw = new java.io.FileWriter(fileName, true);

            fw.write(timestamp + ";" + sender + ";" + receiver + ";" + message + "\n");
            fw.close();

            return "OK:MESSAGE_SENT";

        } catch (Exception e) {
            return "ERROR:WRITE_FAILED";
        }
    }

    public static String GET_CHAT(String user1, String user2) {

        String userA = user1.compareTo(user2) < 0 ? user1 : user2;
        String userB = user1.compareTo(user2) < 0 ? user2 : user1;

        String fileName = "conversation_" + userA + "_" + userB + ".csv";

        java.io.File file = new java.io.File(fileName);

        if (!file.exists())
            return "CHAT:EMPTY";

        try {
            java.util.Scanner scanner = new java.util.Scanner(file);
            StringBuilder sb = new StringBuilder();

            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("|");
            }

            scanner.close();

            if (sb.length() > 0)
                sb.setLength(sb.length() - 1);

            return "CHAT:" + sb;

        } catch (Exception e) {
            return "ERROR:READ_FAILED";
        }
    }

    public static String supprdiscussion(String user1, String user2){
        String userA = user1.compareTo(user2) < 0 ? user1 : user2;
        String userB = user1.compareTo(user2) < 0 ? user2 : user1;

        String fileName = "conversation_" + userA + "_" + userB + ".csv";

        java.io.File file = new java.io.File(fileName);

        if (!file.exists())
            return "CHAT:EMPTY";

        file.delete();
        return "OK:CHAT_SUPPRIMER";
    }

    // ============================
    //         GROUPES
    // ============================
    public static String createGroup(String groupName, String createur) {

        for (Groupe g : groupes) {
            if (g.getNom().equals(groupName))
                return "ERREUR:GROUPE_EXISTE";
        }

        Utilisateur u = null;

        for (Utilisateur user : utilisateurs) {
            if (user.getUsername().equals(createur)) {
                u = user;
                break;
            }
        }

        if (u == null)
            return "ERREUR:CREATEUR_INCONNU";

        Groupe nouveau = new Groupe(groupName);
        nouveau.ajouterMembre(createur);

        groupes.add(nouveau);

        return "OK:GROUPE_CREE";
    }

    public static String addToGroup(String groupName, String demandeur, String newMember) {

        Groupe targetGroup = null;

        for (Groupe g : groupes) {
            if (g.getNom().equals(groupName)) {
                targetGroup = g;
                break;
            }
        }

        if (targetGroup == null)
            return "ERREUR:GROUPE_INCONNU";

        if (!targetGroup.getMembres().contains(demandeur))
            return "ERREUR:ACCES_REFUSE";

        Utilisateur u = null;

        for (Utilisateur user : utilisateurs) {
            if (user.getUsername().equals(newMember)) {
                u = user;
                break;
            }
        }

        if (u == null)
            return "ERREUR:UTILISATEUR_INCONNU";

        if (targetGroup.getMembres().contains(newMember))
            return "ERREUR:DEJA_DANS_LE_GROUPE";

        targetGroup.getMembres().add(newMember);

        return "OK:MEMBRE_AJOUTE";
    }

    public static String SEND_GROUP(String sender, String groupName, String message) {

        Groupe targetGroup = null;

        for (Groupe g : groupes) {
            if (g.getNom().equals(groupName)) {
                targetGroup = g;
                break;
            }
        }

        if (targetGroup == null)
            return "ERREUR:GROUPE_INCONNU";

        if (!targetGroup.getMembres().contains(sender))
            return "ERREUR:ACCES_REFUSE";

        try {
            String fileName = "group_" + groupName + ".csv";

            String timestamp = java.time.LocalDateTime.now().toString();

            java.io.FileWriter fw = new java.io.FileWriter(fileName, true);

            fw.write(timestamp + ";" + sender + ";" + groupName + ";" + message + "\n");
            fw.close();

            return "OK:MESSAGE_GROUPE_ENVOYE";

        } catch (Exception e) {
            return "ERREUR:ECRITURE_IMPOSSIBLE";
        }
    }

    public static String GET_GROUP(String username, String groupName) {

        Groupe g = null;

        for (Groupe grp : groupes) {
            if (grp.getNom().equals(groupName)) {
                g = grp;
                break;
            }
        }

        if (g == null)
            return "ERREUR:GROUPE_INCONNU";

        if (!g.getMembres().contains(username))
            return "ERREUR:ACCES_REFUSE";

        String fileName = "group_" + groupName + ".csv";
        java.io.File f = new java.io.File(fileName);

        if (!f.exists())
            return "GROUP:EMPTY";

        try {
            java.util.Scanner sc = new java.util.Scanner(f);
            StringBuilder sb = new StringBuilder();

            while (sc.hasNextLine()) {
                sb.append(sc.nextLine()).append("|");
            }

            sc.close();

            if (sb.length() > 0)
                sb.setLength(sb.length() - 1);

            return "GROUP:" + sb;

        } catch (Exception e) {
            return "ERREUR:LECTURE_IMPOSSIBLE";
        }
    }

    public static String GET_GROUPS(String username) {

        if (groupes.isEmpty())
            return "GROUPS:VIDE";

        ArrayList<String> groupesUser = new ArrayList<>();

        for (Groupe g : groupes) {
            if (g.getMembres().contains(username)) {
                groupesUser.add(g.getNom());
            }
        }

        if (groupesUser.isEmpty())
            return "GROUPS:VIDE";

        return "" + String.join(",", groupesUser);
    }

   public static String deleteGroup(String groupName, String demandeur) {

    Groupe g = null;

    for (Groupe grp : groupes) {
        if (grp.getNom().equals(groupName)) {
            g = grp;
            break;
        }
    }

    if (g == null) {
        return "ERREUR:GROUPE_INCONNU";
    }

    if (!g.getMembres().contains(demandeur)) {
        return "ERREUR:ACCES_REFUSE";
    }

    try {
        java.io.File f = new java.io.File("group_" + groupName + ".csv");
        if (f.exists()) f.delete();
    } catch (Exception ignored) {}

    groupes.remove(g);

    return "OK:GROUPE_SUPPRIME";
    }

    public static String removeFromGroup(String groupName, String demandeur, String membreASupprimer) {

    Groupe g = null;

    for (Groupe grp : groupes) {
        if (grp.getNom().equals(groupName)) {
            g = grp;
            break;
        }
    }

    if (g == null) {
        return "ERREUR:GROUPE_INCONNU";
    }

    if (!g.getMembres().contains(demandeur)) {
        return "ERREUR:ACCES_REFUSE";
    }

    if (!g.getMembres().contains(membreASupprimer)) {
        return "ERREUR:MEMBRE_ABSENT";
    }

    g.getMembres().remove(membreASupprimer);

    return "OK:MEMBRE_SUPPRIME";
    }

    public static String GET_GROUP_MEMBERS(String username, String groupName) {

    Groupe g = null;

    for (Groupe grp : groupes) {
        if (grp.getNom().equals(groupName)) {
            g = grp;
            break;
        }
    }

    if (g == null)
        return "ERREUR:GROUPE_INCONNU";

    if (!g.getMembres().contains(username))
        return "ERREUR:ACCES_REFUSE";

    if (g.getMembres().isEmpty())
        return "MEMBERS:VIDE";

    return "MEMBERS:" + String.join(",", g.getMembres());
    }

}
