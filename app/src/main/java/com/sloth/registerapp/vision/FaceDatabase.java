package com.sloth.registerapp.vision;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FaceDatabase {

    private static final String TAG = "FaceDatabase";
    private static final String EMBEDDINGS_FILE = "embeddings.json";

    private final File embeddingsFile;

    public FaceDatabase(Context context) {
        File facesDir = new File(context.getExternalFilesDir(null), "faces");
        if (!facesDir.exists()) {
            facesDir.mkdirs();
        }
        embeddingsFile = new File(facesDir, EMBEDDINGS_FILE);
        if (!embeddingsFile.exists()) {
            try {
                embeddingsFile.createNewFile();
                writeJSONToFile(new JSONObject());

            } catch (IOException e) {
                Log.e(TAG, "Erro ao criar embeddings.json: " + e.getMessage());
            }
        }
    }

    // Adiciona ou sobrescreve embedding
    public void addEmbedding(String name, float[] embedding) {
        try {
            JSONObject json = getAllAsJSON();
            JSONArray vector = new JSONArray();
            for (float f : embedding) {
                vector.put(f);
            }
            json.put(name, vector);
            writeJSONToFile(json);
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Erro ao adicionar embedding: " + e.getMessage());
        }
    }

    // Remove um embedding específico
    public void removeEmbedding(String name) {
        try {
            JSONObject json = getAllAsJSON();
            json.remove(name);
            writeJSONToFile(json);
        } catch (IOException e) {
            Log.e(TAG, "Erro ao remover embedding: " + e.getMessage());
        }
    }

    // Retorna todos os embeddings como mapa
    public Map<String, float[]> getAllEmbeddings() {
        Map<String, float[]> map = new HashMap<>();
        try {
            JSONObject json = getAllAsJSON();
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = json.getJSONArray(key);
                float[] vector = new float[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    vector[i] = (float) arr.getDouble(i);
                }
                map.put(key, vector);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao ler embeddings: " + e.getMessage());
        }
        return map;
    }

    // ================================
    // Utilitários de leitura/escrita
    // ================================

    private JSONObject getAllAsJSON() {
        try {
            FileInputStream fis = new FileInputStream(embeddingsFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return new JSONObject(sb.toString().isEmpty() ? "{}" : sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler JSON do arquivo: " + e.getMessage());
            return new JSONObject();
        }
    }

    private void writeJSONToFile(JSONObject json) throws IOException {
        FileWriter writer = new FileWriter(embeddingsFile, false);
        writer.write(json.toString());
        writer.flush();
        writer.close();
    }
}
