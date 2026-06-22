package com.marcador;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ControladorMarcador {

    @FXML private Label lblTiempo;
    @FXML private Label lblParte;
    @FXML private Label lblGolesLocal;      
    @FXML private Label lblGolesVisitante;  
    @FXML private VBox listaGoleadoresLocal;
    @FXML private VBox listaGoleadoresVisitante;
    @FXML private MediaView pantallaVideo;
    @FXML private ImageView imagenFondo;

    // CONFIGURACIÓN VISUAL
    private static final int MAX_GOLEADORES = 5; 
    private static final String SEPARADOR = "       "; 

    // --- REGISTRO DE DATOS (BACKEND) ---
    // Guarda: "MARCOS" -> ["3'", "15'", "80'"]
    private Map<String, List<String>> registroGolesLocal = new HashMap<>();
    private Map<String, List<String>> registroGolesVisitante = new HashMap<>();

    // --- HISTORIAL PARA DESHACER (Solo guardamos el NOMBRE del último que marcó) ---
    private Stack<String> historialLocal = new Stack<>();
    private Stack<String> historialVisitante = new Stack<>();

    // --- MÉTODOS BÁSICOS ---
    public void setTiempo(String tiempo) { if (lblTiempo != null) lblTiempo.setText(tiempo); }
    public void setParte(String textoParte) { if (lblParte != null) lblParte.setText(textoParte); }
    public void setMarcador(int local, int visitante) {
        if (lblGolesLocal != null) lblGolesLocal.setText(String.valueOf(local));
        if (lblGolesVisitante != null) lblGolesVisitante.setText(String.valueOf(visitante));
    }

    // --- LÓGICA DE AÑADIR GOL (CON MEMORIA) ---

    public void agregarGolLocal(String minuto, String nombreJugador) {
        if (listaGoleadoresLocal == null) return;

        // 1. AÑADIR AL REGISTRO DE DATOS
        registroGolesLocal.putIfAbsent(nombreJugador, new ArrayList<>());
        registroGolesLocal.get(nombreJugador).add(minuto + "'");

        // 2. AÑADIR AL HISTORIAL (Para poder deshacer luego)
        historialLocal.push(nombreJugador);

        // 3. ACTUALIZAR LA PANTALLA
        refrescarListaLocal(nombreJugador);
    }

    public void agregarGolVisitante(String minuto, String nombreJugador) {
        if (listaGoleadoresVisitante == null) return;

        // 1. AÑADIR AL REGISTRO
        registroGolesVisitante.putIfAbsent(nombreJugador, new ArrayList<>());
        registroGolesVisitante.get(nombreJugador).add(minuto + "'");

        // 2. HISTORIAL
        historialVisitante.push(nombreJugador);

        // 3. ACTUALIZAR PANTALLA
        refrescarListaVisitante(nombreJugador);
    }

    // --- MÉTODOS DE REFRESCO VISUAL ---
    // Esta lógica borra la etiqueta vieja del jugador y crea una nueva al final con TODOS sus goles
    
    private void refrescarListaLocal(String nombreJugador) {
        // A. Buscar si ya hay una etiqueta de este jugador visualmente y BORRARLA
        // (Para que al añadirla de nuevo aparezca abajo del todo)
        listaGoleadoresLocal.getChildren().removeIf(node -> {
            Label lbl = (Label) node;
            return lbl.getText().startsWith(nombreJugador + SEPARADOR);
        });

        // B. Construir el texto con TODOS los minutos del registro
        List<String> minutos = registroGolesLocal.get(nombreJugador);
        String textoMinutos = String.join(" ", minutos); // "3' 7' 25'"
        String textoFinal = nombreJugador + SEPARADOR + textoMinutos;

        // C. Crear etiqueta nueva
        Label nuevoLabel = new Label(textoFinal);
        estilizarLabel(nuevoLabel, Pos.CENTER_RIGHT);

        // D. Añadir al final
        listaGoleadoresLocal.getChildren().add(nuevoLabel);

        // E. Controlar el límite de 5 (borrar el de arriba del todo si nos pasamos)
        if (listaGoleadoresLocal.getChildren().size() > MAX_GOLEADORES) {
            listaGoleadoresLocal.getChildren().remove(0);
        }
    }

    private void refrescarListaVisitante(String nombreJugador) {
        // A. Borrar versión antigua visual
        listaGoleadoresVisitante.getChildren().removeIf(node -> {
            Label lbl = (Label) node;
            return lbl.getText().endsWith(SEPARADOR + nombreJugador);
        });

        // B. Construir texto (Primero minutos, luego nombre)
        List<String> minutos = registroGolesVisitante.get(nombreJugador);
        String textoMinutos = String.join(" ", minutos);
        String textoFinal = textoMinutos + SEPARADOR + nombreJugador;

        // C. Crear y añadir
        Label nuevoLabel = new Label(textoFinal);
        estilizarLabel(nuevoLabel, Pos.CENTER_LEFT);
        listaGoleadoresVisitante.getChildren().add(nuevoLabel);

        // D. Limitar
        if (listaGoleadoresVisitante.getChildren().size() > MAX_GOLEADORES) {
            listaGoleadoresVisitante.getChildren().remove(0);
        }
    }

    // --- LÓGICA DE DESHACER (REAL) ---

    public void quitarUltimoGolLocal() {
        if (historialLocal.isEmpty()) return;

        // 1. Recuperar quién marcó el último gol
        String nombreJugador = historialLocal.pop();

        // 2. Quitarle el último minuto de su registro
        List<String> minutos = registroGolesLocal.get(nombreJugador);
        if (minutos != null && !minutos.isEmpty()) {
            minutos.remove(minutos.size() - 1);
        }

        // 3. Actualizar visualmente
        // Primero borramos su etiqueta actual
        listaGoleadoresLocal.getChildren().removeIf(node -> {
            Label lbl = (Label) node;
            return lbl.getText().startsWith(nombreJugador + SEPARADOR);
        });

        // Si le quedan goles, volvemos a crear la etiqueta (pero no la movemos al final, 
        // idealmente debería quedarse donde estaba, pero para simplificar la recreamos)
        if (minutos != null && !minutos.isEmpty()) {
            // NOTA: Al deshacer, lo tratamos como un refresco. 
            // Si quieres que no baje al final al deshacer sería más complejo, 
            // pero esto cumple con actualizar los datos correctamente.
            refrescarListaLocal(nombreJugador); 
        } else {
            // Si no le quedan goles, borramos la entrada del mapa
            registroGolesLocal.remove(nombreJugador);
        }
    }

    public void quitarUltimoGolVisitante() {
        if (historialVisitante.isEmpty()) return;

        String nombreJugador = historialVisitante.pop();
        List<String> minutos = registroGolesVisitante.get(nombreJugador);
        
        if (minutos != null && !minutos.isEmpty()) {
            minutos.remove(minutos.size() - 1);
        }

        listaGoleadoresVisitante.getChildren().removeIf(node -> {
            Label lbl = (Label) node;
            return lbl.getText().endsWith(SEPARADOR + nombreJugador);
        });

        if (minutos != null && !minutos.isEmpty()) {
            refrescarListaVisitante(nombreJugador);
        } else {
            registroGolesVisitante.remove(nombreJugador);
        }
    }

    // --- UTILIDADES ---
    
    private void estilizarLabel(Label lbl, Pos alineacion) {
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 30px;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(alineacion);
    }
    
    // Este método ya cumple con lo de "Reiniciar Registro"
    public void limpiarGoleadores() {
        if (listaGoleadoresLocal != null) listaGoleadoresLocal.getChildren().clear();
        if (listaGoleadoresVisitante != null) listaGoleadoresVisitante.getChildren().clear();
        
        // Limpiamos los datos reales
        registroGolesLocal.clear();
        registroGolesVisitante.clear();
        historialLocal.clear();
        historialVisitante.clear();
    }

    public MediaView getPantallaVideo() { return pantallaVideo; }
    public ImageView getImagenFondo() { return imagenFondo; }
}