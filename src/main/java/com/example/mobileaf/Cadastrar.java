package com.example.mobileaf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

public class Cadastrar extends AppCompatActivity {

    private static final int CodPermissaoLoc = 100;
    EditText edtTitulo, edtDescricao, edtData;
    Spinner spinnerCat;
    CheckBox CbFavorito;
    TextView txtClima;
    Button btnSalvar;
    Button btnBuscarClima;
    private String temperaturaAtual = "";
    private String ventoAtual = "";
    private String condicaoAtual = "";
    private boolean temClima = false;

    FirebaseFirestore db;
    FusedLocationProviderClient fusedLocationClient;

    double latitudeAtual = 0;
    double longitudeAtual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cadastrar);

        db = FirebaseFirestore.getInstance();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescricao = findViewById(R.id.edtDescricao);
        edtData = findViewById(R.id.edtData);

        spinnerCat = findViewById(R.id.spinnerCat);

        CbFavorito = findViewById(R.id.CbFavorito);

        txtClima = findViewById(R.id.txtClima);

        btnSalvar = findViewById(R.id.btnSalvar);

        btnBuscarClima = findViewById(R.id.btnBuscarClima);

        btnBuscarClima.setOnClickListener(v ->
                verificarPermissaoLocalizacao());

        btnSalvar.setOnClickListener(v ->
                salvarVisita());
    }

    private void verificarPermissaoLocalizacao() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            capturarLocalizacao();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CodPermissaoLoc);
        }
    }

    @SuppressLint("MissingPermission")
    private void capturarLocalizacao() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null) {
                latitudeAtual = location.getLatitude();
                longitudeAtual = location.getLongitude();
                buscarClima(latitudeAtual, longitudeAtual);
            } else {
                Toast.makeText(this, "Localização não encontrada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buscarClima(double latitude, double longitude) {
        new Thread(() -> {
            try {
                String urlString =
                        "https://api.open-meteo.com/v1/forecast?latitude="
                                + latitude
                                + "&longitude="
                                + longitude
                                + "&current=temperature_2m,wind_speed_10m,weather_code";
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conexao = (java.net.HttpURLConnection) url.openConnection();
                conexao.setRequestMethod("GET");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conexao.getInputStream()));
                StringBuilder resposta = new StringBuilder();
                String linha;

                while ((linha = reader.readLine()) != null) {
                    resposta.append(linha);
                }
                reader.close();
                String json = resposta.toString();
                runOnUiThread(() -> {

                    try {

                        JSONObject objeto = new JSONObject(json);

                        JSONObject current = objeto.getJSONObject("current");

                        double temperatura = current.getDouble("temperature_2m");

                        double vento = current.getDouble("wind_speed_10m");

                        int codigo = current.getInt("weather_code");

                        temperaturaAtual = String.valueOf(temperatura);

                        ventoAtual = String.valueOf(vento);

                        condicaoAtual = String.valueOf(codigo);

                        temClima = true;

                        txtClima.setText(
                                "Temperatura: "
                                        + temperaturaAtual
                                        + " °C\n"
                                        + "Vento: "
                                        + ventoAtual
                                        + " km/h\n"
                                        + "Condição: "
                                        + condicaoAtual
                        );

                    } catch (Exception e) {

                        txtClima.setText("Erro ao ler clima");

                    }

                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(Cadastrar.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void salvarVisita() {

        String titulo = edtTitulo.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();
        String data = edtData.getText().toString().trim();
        String categoria = spinnerCat.getSelectedItem().toString();
        boolean favorito = CbFavorito.isChecked();
        if (titulo.isEmpty()) {
            edtTitulo.setError("Informe o título");
            return;
        }
        Visita visita =
                new Visita(
                        titulo,
                        descricao,
                        data,
                        categoria,
                        favorito,
                        latitudeAtual,
                        longitudeAtual,
                        temperaturaAtual,
                        ventoAtual,
                        condicaoAtual
                );

        db.collection("visitas").add(visita).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Visita salva com sucesso!", Toast.LENGTH_SHORT).show();
            limparCampos();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void limparCampos() {
        edtTitulo.setText("");
        edtDescricao.setText("");
        edtData.setText("");
        CbFavorito.setChecked(false);
        txtClima.setText("Localização não obtida");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );
        if (requestCode == CodPermissaoLoc) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                capturarLocalizacao();
            }
        }
    }
}