package com.example.mobileaf;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

public class Listar extends AppCompatActivity {
    private ListView lstVisitas;
    private FirebaseFirestore db;
    private ArrayList<Visita> listaVisitas;
    private ArrayList<String> listaTextos;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar);

        lstVisitas = findViewById(R.id.lstVisitas);
        db = FirebaseFirestore.getInstance();
        listaVisitas = new ArrayList<>();
        listaTextos = new ArrayList<>();
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                listaTextos
        );

        lstVisitas.setAdapter(adapter);
        carregarVisitas();
        lstVisitas.setOnItemClickListener((parent, view, position, id) -> {
            Visita visita = listaVisitas.get(position);
            mostrarDetalhes(visita);
        });
        lstVisitas.setOnItemLongClickListener((parent, view, position, id) -> {
            Visita visita = listaVisitas.get(position);
            confirmarExclusao(visita);
            return true;
        });
    }

    private void carregarVisitas() {

        db.collection("visitas").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    listaVisitas.clear();
                    listaTextos.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visita visita = doc.toObject(Visita.class);
                        visita.setId(doc.getId());
                        listaVisitas.add(visita);
                        String texto =
                                "-" + visita.getTitulo()
                                        + "\nData: " + visita.getData()
                                        + "\nCategoria: " + visita.getCategoria()
                                        + "\nTemperatura: " + visita.getTemperatura() + "°C"
                                        + "\nCondição: " + visita.getCondicao()
                                        + "\nLocalização: "
                                        + String.format(
                                        Locale.getDefault(),
                                        "%.5f, %.5f",
                                        visita.getLatitude(),
                                        visita.getLongitude()
                                );
                        listaTextos.add(texto);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Listar.this, "Erro ao carregar visitas.", Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDetalhes(Visita visita) {

        String detalhes =
                "Título: " + visita.getTitulo()
                        + "\n\nDescrição: " + visita.getDescricao()
                        + "\n\nData: " + visita.getData()
                        + "\nCategoria: " + visita.getCategoria()
                        + "\nFavorito: "
                        + (visita.isFavorito() ? "Sim" : "Não")
                        + "\n\nLatitude: "
                        + String.format(
                        Locale.getDefault(),
                        "%.5f",
                        visita.getLatitude()
                )
                        + "\nLongitude: "
                        + String.format(
                        Locale.getDefault(),
                        "%.5f",
                        visita.getLongitude()
                )
                        + "\n\nTemperatura: "
                        + visita.getTemperatura() + "°C"
                        + "\nVento: "
                        + visita.getVento() + " km/h"
                        + "\nCondição: "
                        + visita.getCondicao();
        new AlertDialog.Builder(this).setTitle("Detalhes da Visita").setMessage(detalhes).setPositiveButton("Fechar", null).show();
    }

    private void confirmarExclusao(Visita visita) {
        new AlertDialog.Builder(this).setTitle("Excluir visita").setMessage("Deseja realmente excluir a visita:\n\n" + visita.getTitulo() + "?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    excluirVisita(visita);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirVisita(Visita visita) {
        db.collection("visitas").document(visita.getId()).delete().addOnSuccessListener(unused -> {
                    Toast.makeText(Listar.this, "Visita excluída com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarVisitas();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Listar.this, "Erro ao excluir visita.", Toast.LENGTH_LONG).show();
                });
    }
}