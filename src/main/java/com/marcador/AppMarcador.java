package com.marcador;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import com.marcador.Player.Equipo;

public class AppMarcador extends Application {

    // --- VARIABLES DEL PARTIDO ---
    private int golesLocal = 0;
    private int golesVisitante = 0;
    private GestorDatos gestorDatos; 

    // --- VARIABLES DEL CRONÓMETRO ---
    private Timeline timeline; 
    private int segundosTotales = 0; 
    private boolean esAscendente = true; 
    private int limiteMinutos = 45; 
    private int minutoInicioRef = 0; 
    private int duracionParte = 45;  

    // --- COMPONENTES UI ---
    private Spinner<Integer> spinInicio;
    private Spinner<Integer> spinFin;
    private Spinner<Integer> spinParte; 
    
    private Label lblMonitorTiempo;
    private Label lblMonitorMarcador;

    // --- COMPONENTES LÓGICOS ---
    private ControladorMarcador controlador; 
    private MediaPlayer mediaPlayer;         
    private FadeTransition currentTransition; 

    @Override
    public void start(Stage stageControl) {
        // 1. CARGAR DATOS DESDE EL ARCHIVO TXT
        gestorDatos = new GestorDatos();

        // 2. VENTANA DEL MARCADOR (TV)
        Stage stageTV = new Stage();
        try {
        	FXMLLoader loader = new FXMLLoader(getClass().getResource("marcador.fxml"));
            Region contenidoMarcador = loader.load();
            controlador = loader.getController();

            StackPane rootNegro = new StackPane();
            rootNegro.setStyle("-fx-background-color: black;");
            Group grupoContenido = new Group(contenidoMarcador);
            rootNegro.getChildren().add(grupoContenido);
            Scene sceneTV = new Scene(rootNegro);

            Runnable actualizadorEscala = () -> {
                double factor = Math.min(sceneTV.getWidth() / 1920, sceneTV.getHeight() / 1080);
                grupoContenido.setScaleX(factor);
                grupoContenido.setScaleY(factor);
            };
            sceneTV.widthProperty().addListener((o, old, newV) -> actualizadorEscala.run());
            sceneTV.heightProperty().addListener((o, old, newV) -> actualizadorEscala.run());
            
            sceneTV.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.F11) {
                    stageTV.setFullScreen(!stageTV.isFullScreen());
                    stageTV.setAlwaysOnTop(true);
                }
            });
            stageTV.iconifiedProperty().addListener((obs, was, is) -> {
                if (is) { stageTV.setIconified(false); stageTV.setAlwaysOnTop(true); }
            });

            stageTV.setTitle("Marcador Público");
            stageTV.setScene(sceneTV);
            stageTV.setX(1000); 
            stageTV.setAlwaysOnTop(true);
            stageTV.show();
            actualizadorEscala.run();

        } catch (IOException e) {
            e.printStackTrace();
            return; 
        }

        inicializarVideoConFade();

        // 4. PANEL DE CONTROL (PC)
        VBox panelControl = new VBox(15);
        panelControl.setAlignment(Pos.TOP_CENTER);
        panelControl.setPadding(new Insets(20));
        panelControl.setStyle("-fx-font-size: 14px;");

        // MONITOR
        HBox boxMonitor = new HBox(20);
        boxMonitor.setAlignment(Pos.CENTER);
        boxMonitor.setStyle("-fx-background-color: #333; -fx-padding: 10; -fx-background-radius: 5;");
        lblMonitorTiempo = new Label("00:00");
        lblMonitorTiempo.setStyle("-fx-text-fill: yellow; -fx-font-weight: bold; -fx-font-size: 20px;");
        lblMonitorMarcador = new Label("0 - 0");
        lblMonitorMarcador.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        boxMonitor.getChildren().addAll(new Label("EN PANTALLA: "), lblMonitorTiempo, new Label("|"), lblMonitorMarcador);

        // BOTÓN GESTIÓN
        Button btnGestionarEquipos = new Button("⚙ GESTIONAR VERDE / NEGRO");
        btnGestionarEquipos.setStyle("-fx-background-color: #87CEEB; -fx-font-weight: bold;");
        btnGestionarEquipos.setOnAction(e -> abrirGestionEquipos(stageControl));

        // CONFIG TIEMPO
        ToggleGroup grupoModo = new ToggleGroup();
        RadioButton rbAscendente = new RadioButton("Ascendente");
        RadioButton rbDescendente = new RadioButton("Descendente");
        rbAscendente.setToggleGroup(grupoModo); rbDescendente.setToggleGroup(grupoModo);
        rbAscendente.setSelected(true);

        spinInicio = new Spinner<>(0, 90, 0); spinInicio.setEditable(true);
        spinFin = new Spinner<>(0, 90, 45); spinFin.setEditable(true);
        spinParte = new Spinner<>(1, 4, 1); 

        HBox boxConfig = new HBox(10, new Label("Inicio:"), spinInicio, new Label("Fin:"), spinFin, new Label("Parte:"), spinParte);
        boxConfig.setAlignment(Pos.CENTER);
        Button btnAplicarConfig = new Button("Aplicar Tiempo");
        
        // RELOJ
        HBox boxReloj = new HBox(10);
        boxReloj.setAlignment(Pos.CENTER);
        Button btnPlay = new Button("▶");
        Button btnPause = new Button("⏸");
        Button btnReset = new Button("⏹");
        boxReloj.getChildren().addAll(btnPlay, btnPause, btnReset);

        Button btnStopVideo = new Button("⏹ DETENER VÍDEO");
        btnStopVideo.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStopVideo.setOnAction(e -> detenerVideoInmediatamente());

        // GOLES
        HBox boxGoles = new HBox(30);
        boxGoles.setAlignment(Pos.CENTER);
        
        VBox boxLocal = new VBox(5);
        Button btnGolLocal = new Button("GOL VERDE");
        btnGolLocal.setStyle("-fx-background-color: #90ee90; -fx-font-weight: bold; -fx-pref-width: 120; -fx-pref-height: 50;");
        Button btnUndoLocal = new Button("Deshacer Verde");
        boxLocal.getChildren().addAll(btnGolLocal, btnUndoLocal);

        VBox boxVisitante = new VBox(5);
        Button btnGolVisitante = new Button("GOL NEGRO");
        btnGolVisitante.setStyle("-fx-background-color: #AAAAAA; -fx-font-weight: bold; -fx-pref-width: 120; -fx-pref-height: 50;");
        Button btnUndoVisitante = new Button("Deshacer Negro");
        boxVisitante.getChildren().addAll(btnGolVisitante, btnUndoVisitante);
        
        boxGoles.getChildren().addAll(boxLocal, boxVisitante);
        Button btnResetGoles = new Button("Reset 0-0");

        // EVENTOS
        btnAplicarConfig.setOnAction(e -> {
            esAscendente = rbAscendente.isSelected();
            int ini = spinInicio.getValue();
            int fin = spinFin.getValue();
            this.limiteMinutos = fin; 
            this.minutoInicioRef = ini;
            this.duracionParte = Math.abs(fin - ini);
            if (duracionParte == 0) duracionParte = 45;
            this.segundosTotales = ini * 60;
            actualizarMonitorYTV();
            controlador.setParte(spinParte.getValue() + "ª PARTE");
            if(timeline != null) timeline.stop();
        });

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (esAscendente) {
                if (segundosTotales < limiteMinutos * 60) segundosTotales++;
                else timeline.stop();
            } else {
                if (segundosTotales > 0) segundosTotales--;
                else timeline.stop();
            }
            actualizarMonitorYTV();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        btnPlay.setOnAction(e -> timeline.play());
        btnPause.setOnAction(e -> timeline.stop());
        btnReset.setOnAction(e -> { timeline.stop(); segundosTotales = spinInicio.getValue() * 60; actualizarMonitorYTV(); });

        btnGolLocal.setOnAction(e -> mostrarSelectorGoleador(true)); 
        btnGolVisitante.setOnAction(e -> mostrarSelectorGoleador(false)); 

        btnUndoLocal.setOnAction(e -> { if (golesLocal > 0) { golesLocal--; controlador.quitarUltimoGolLocal(); actualizarMonitorYTV(); }});
        btnUndoVisitante.setOnAction(e -> { if (golesVisitante > 0) { golesVisitante--; controlador.quitarUltimoGolVisitante(); actualizarMonitorYTV(); }});
        btnResetGoles.setOnAction(e -> { golesLocal=0; golesVisitante=0; controlador.limpiarGoleadores(); actualizarMonitorYTV(); });

        panelControl.getChildren().addAll(
            boxMonitor, 
            new Separator(), btnGestionarEquipos, 
            new Separator(), rbAscendente, rbDescendente, boxConfig, btnAplicarConfig, 
            new Separator(), new Label("RELOJ"), boxReloj, 
            new Separator(), btnStopVideo, 
            new Separator(), new Label("MARCADOR"), boxGoles, btnResetGoles
        );

        Scene sceneControl = new Scene(panelControl, 500, 750);
        stageControl.setTitle("Control Panel");
        stageControl.setScene(sceneControl);
     // --- ASEGURAR CIERRE TOTAL ---
        // Esto mata todos los procesos al cerrar la ventana del Panel de Control
        stageControl.setOnCloseRequest(e -> {
            if (mediaPlayer != null) mediaPlayer.stop();
            if (timeline != null) timeline.stop();
            Platform.exit();
            System.exit(0); // <--- ESTO ES EL TIRO DE GRACIA
        });
        stageControl.setX(50); stageControl.show();
        
        actualizarMonitorYTV();
        controlador.setParte("1ª PARTE");
    }

    private void actualizarMonitorYTV() {
        int m = segundosTotales / 60;
        int s = segundosTotales % 60;
        String tiempo = String.format("%02d:%02d", m, s);
        
        if (controlador != null) {
            controlador.setTiempo(tiempo);
            controlador.setMarcador(golesLocal, golesVisitante);
        }
        lblMonitorTiempo.setText(tiempo);
        lblMonitorMarcador.setText(golesLocal + " - " + golesVisitante);
    }

    private void mostrarSelectorGoleador(boolean esGolDelLocal) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Selecciona Jugador (" + (esGolDelLocal ? "VERDE" : "NEGRO") + ")");

        FlowPane pane = new FlowPane();
        pane.setPadding(new Insets(10));
        pane.setHgap(10); pane.setVgap(10);
        pane.setPrefWidth(600);

        for (Player p : gestorDatos.getTodos()) {
            Button btn = new Button(p.getNombreCorto());
            btn.setPrefSize(100, 50);
            
            if (p.getEquipoAsignado() == Equipo.LOCAL) {
                btn.setStyle("-fx-base: #90ee90; -fx-text-fill: black; -fx-font-weight: bold;"); 
            } else if (p.getEquipoAsignado() == Equipo.VISITANTE) {
                btn.setStyle("-fx-base: #000000; -fx-text-fill: white; -fx-font-weight: bold;"); 
            } else {
                btn.setStyle("-fx-base: #FFFFFF; -fx-text-fill: black; -fx-border-color: #CCCCCC;"); 
            }

            btn.setOnAction(e -> {
                procesarGol(p, esGolDelLocal);
                dialog.close();
            });
            pane.getChildren().add(btn);
        }
        Scene scene = new Scene(pane);
        dialog.setScene(scene);
        dialog.show();
    }

    private void procesarGol(Player p, boolean esGolDelLocal) {
        String nombreMostrar = p.getNombreCorto();
        if (esGolDelLocal && p.getEquipoAsignado() == Equipo.VISITANTE) nombreMostrar += " (PP)";
        else if (!esGolDelLocal && p.getEquipoAsignado() == Equipo.LOCAL) nombreMostrar += " (PP)";

        if (esGolDelLocal) {
            golesLocal++;
            controlador.agregarGolLocal(obtenerMinutoCalculado(), nombreMostrar);
        } else {
            golesVisitante++;
            controlador.agregarGolVisitante(obtenerMinutoCalculado(), nombreMostrar);
        }
        actualizarMonitorYTV();
        lanzarVideoJugador(p);
    }

    private void abrirGestionEquipos(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Gestión de Equipos");

        HBox layout = new HBox(10); layout.setPadding(new Insets(10));
        ListView<Player> listSinAsignar = new ListView<>();
        ListView<Player> listLocal = new ListView<>();
        ListView<Player> listVisitante = new ListView<>();
        actualizarListasGestion(listSinAsignar, listLocal, listVisitante);

        VBox botones = new VBox(10); botones.setAlignment(Pos.CENTER);
        Button toLocal = new Button("➡ A VERDE");
        Button toVisitante = new Button("➡ A NEGRO");
        Button toNada = new Button("⬅ Quitar");
        Button btnCrear = new Button("➕ Nuevo Jugador"); 

        toLocal.setOnAction(e -> { Player p = listSinAsignar.getSelectionModel().getSelectedItem(); if (p!=null){p.setEquipoAsignado(Equipo.LOCAL); actualizarListasGestion(listSinAsignar, listLocal, listVisitante);}});
        toVisitante.setOnAction(e -> { Player p = listSinAsignar.getSelectionModel().getSelectedItem(); if (p!=null){p.setEquipoAsignado(Equipo.VISITANTE); actualizarListasGestion(listSinAsignar, listLocal, listVisitante);}});
        toNada.setOnAction(e -> { 
            Player p1 = listLocal.getSelectionModel().getSelectedItem(); if (p1!=null) p1.setEquipoAsignado(Equipo.SIN_ASIGNAR);
            Player p2 = listVisitante.getSelectionModel().getSelectedItem(); if (p2!=null) p2.setEquipoAsignado(Equipo.SIN_ASIGNAR);
            actualizarListasGestion(listSinAsignar, listLocal, listVisitante);
        });

        btnCrear.setOnAction(e -> {
             TextInputDialog td = new TextInputDialog("Invitado");
             td.setHeaderText("Nombre del nuevo jugador");
             td.showAndWait().ifPresent(nombre -> {
                 gestorDatos.agregarJugadorPersonalizado(nombre, null, null);
                 actualizarListasGestion(listSinAsignar, listLocal, listVisitante);
             });
        });

        botones.getChildren().addAll(new Label("Mover seleccionado:"), toLocal, toVisitante, toNada, new Separator(), btnCrear);
        layout.getChildren().addAll(new VBox(new Label("Sin Asignar"), listSinAsignar), botones, new VBox(new Label("VERDE (Local)"), listLocal), new VBox(new Label("NEGRO (Visitante)"), listVisitante));
        Scene scene = new Scene(layout, 800, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void actualizarListasGestion(ListView<Player> sin, ListView<Player> loc, ListView<Player> vis) {
        sin.getItems().clear(); loc.getItems().clear(); vis.getItems().clear();
        for (Player p : gestorDatos.getTodos()) {
            if (p.getEquipoAsignado() == Equipo.LOCAL) loc.getItems().add(p);
            else if (p.getEquipoAsignado() == Equipo.VISITANTE) vis.getItems().add(p);
            else sin.getItems().add(p);
        }
    }

    private String obtenerMinutoCalculado() {
        int minutoReloj = segundosTotales / 60;
        int jugados = esAscendente ? (minutoReloj - minutoInicioRef) : (minutoInicioRef - minutoReloj);
        int real = ((spinParte.getValue() - 1) * duracionParte) + jugados;
        return String.valueOf(Math.max(0, real));
    }

    private void detenerVideoInmediatamente() {
        if (currentTransition != null) currentTransition.stop();
        if (mediaPlayer != null) mediaPlayer.stop();
        if (controlador != null && controlador.getPantallaVideo() != null) {
            controlador.getPantallaVideo().setVisible(false);
            controlador.getPantallaVideo().setOpacity(0.0);
        }
    }

    private void inicializarVideoConFade() {
        // AHORA LA RUTA LA COGEMOS DEL GESTOR
        String rutaDefault = gestorDatos.getRutaVideoDefault();
        if (rutaDefault != null) {
            try { 
                File f = new File(rutaDefault); 
                if(f.exists()) { 
                    mediaPlayer = new MediaPlayer(new Media(f.toURI().toString())); 
                    controlador.getPantallaVideo().setMediaPlayer(mediaPlayer); 
                    controlador.getPantallaVideo().setVisible(false); 
                } 
            } catch(Exception e) {}
        }
    }

    private void lanzarVideoJugador(Player p) {
        if (controlador == null || controlador.getPantallaVideo() == null) return;
        
        if (currentTransition != null) currentTransition.stop();
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }

        // PRIORIDAD: 1. Video Jugador -> 2. Video Default (desde archivo) -> 3. Nada
        String ruta = gestorDatos.getRutaVideoDefault(); 
        if (p.tieneVideoPersonalizado()) ruta = p.getRutaVideoCelebracion();
        
        if (ruta == null) return; // No hay video configurado

        File f = new File(ruta);
        if (!f.exists()) {
            System.out.println("❌ Video no encontrado: " + ruta);
            return;
        }

        mediaPlayer = new MediaPlayer(new Media(f.toURI().toString()));
        controlador.getPantallaVideo().setMediaPlayer(mediaPlayer);

        MediaView mv = controlador.getPantallaVideo();
        mv.setVisible(true); mv.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), mv); fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        currentTransition = fadeIn;

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.8), mv); fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> { mv.setVisible(false); mediaPlayer.stop(); });

        mediaPlayer.setOnEndOfMedia(() -> { currentTransition = fadeOut; fadeOut.play(); });
        
        mediaPlayer.play();
        fadeIn.play();
    }

    public static void main(String[] args) { launch(args); }
}