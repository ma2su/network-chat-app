package com.example.sae302;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class MainActivity2 extends AppCompatActivity {

    private EditText inputUsername;
    private Button btnLogin;

    private static final String TAG = "MainActivity2";
    // Doit être la même IP que dans MainActivity
    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;
    public static final String PREFS_NAME = "Sae302Prefs";
    public static final String KEY_USERNAME = "LOGGED_IN_USERNAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Logique de connexion
        inputUsername = findViewById(R.id.inputUsername);
        btnLogin = findViewById(R.id.btnLogin);

        // Lien cliquable "Créer un compte"
        TextView textRegister = findViewById(R.id.textRegister);
        textRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, MainActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(MainActivity2.this, "Le nom d'utilisateur ne peut pas être vide", Toast.LENGTH_SHORT).show();
            } else {
                sendLoginRequestToServer(username);
            }
        });

        // --- AJOUT : Vérification du message de déconnexion forcée ---
        // Si le HeartbeatService nous renvoie ici, on affiche pourquoi.
        if (getIntent().hasExtra("DISCONNECT_REASON")) {
            String reason = getIntent().getStringExtra("DISCONNECT_REASON");
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
            // On vide le champ pour être propre
            inputUsername.setText("");
        }
        // -----------------------------------------------------------
    }

    private void sendLoginRequestToServer(String username) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String message = "LOGIN;" + username;
                byte[] data = message.getBytes();
                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                socket.send(packet);

                // Préparation à la réception d'une réponse
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.setSoTimeout(5000); // Timeout de 5 secondes

                socket.receive(receivePacket); // Attente d'une réponse

                String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());

                runOnUiThread(() -> {
                    switch(serverResponse) {
                        case "OK:CONNECTE":
                            // Enregistrer l'utilisateur dans les SharedPreferences
                            SharedPreferences.Editor editor = getSharedPreferences("PREFS_NAME", MODE_PRIVATE).edit();
                            editor.putString("KEY_USERNAME", username);
                            editor.apply();

                            // --- DEMARRAGE DU HEARTBEAT ---
                            // On lance le service qui va surveiller la connexion
                            Intent serviceIntent = new Intent(MainActivity2.this, HeartbeatService.class);
                            startService(serviceIntent);
                            // ------------------------------

                            Toast.makeText(MainActivity2.this, "Connexion réussie!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
                            startActivity(intent);
                            finish(); // Terminer l'activité de login
                            break;
                        case "ERREUR:DEJA_CONNECTE":
                            Toast.makeText(MainActivity2.this, "Utilisateur déjà connecté.", Toast.LENGTH_SHORT).show();
                            break;
                        case "ERREUR:UTILISATEUR_INCONNU":
                            Toast.makeText(MainActivity2.this, "Utilisateur inconnu.", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Log.w(TAG, "Réponse inattendue du serveur: " + serverResponse);
                            Toast.makeText(MainActivity2.this, "Utilisateur inconnu ou réponse inattendue.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                });

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Le serveur n'a pas répondu à temps", e);
                runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Le serveur n'a pas répondu.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de l'envoi/réception des données UDP", e);
                runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Erreur de communication avec le serveur.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}