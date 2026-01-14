package com.example.sae302;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private static final String TAG = "MainActivity";
    // Remplacez par l'adresse IP de votre serveur
    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.usernameEditText);
        Button createUserButton = findViewById(R.id.createUserButton);

        createUserButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(MainActivity.this, "Le nom d'utilisateur ne peut pas être vide", Toast.LENGTH_SHORT).show();
            } else {
                sendUsernameToServer(username);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getSystemWindowInsets().left, insets.getSystemWindowInsets().top, insets.getSystemWindowInsets().right, insets.getSystemWindowInsets().bottom);
            return insets;
        });

        // Lien cliquable "Connexion à un compte"
        TextView textRegister = findViewById(R.id.textRegister);
        textRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });
    }

    private void sendUsernameToServer(String username) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String message = "REGISTER;" + username;
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
                    Toast.makeText(MainActivity.this, "Réponse du serveur: " + serverResponse, Toast.LENGTH_LONG);
                    usernameEditText.setText("");
                    switch(serverResponse){
                        case "ERREUR:UTILISATEUR_EXISTE":
                            Toast.makeText(MainActivity.this, "L'utilisateur existe déjà", Toast.LENGTH_SHORT).show();
                            break;
                        case "ERREUR:LISTE_COMPLET":
                            Toast.makeText(MainActivity.this, "La liste est pleine", Toast.LENGTH_SHORT).show();
                            break;
                        case "OK:UTILISATEUR_CREE":
                            Toast.makeText(MainActivity.this, "L'utilisateur a été créé", Toast.LENGTH_SHORT).show();
                    }
                });



            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Le serveur n'a pas répondu à temps", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Le serveur n'a pas répondu.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de l'envoi/réception des données UDP", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur de communication avec le serveur.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
