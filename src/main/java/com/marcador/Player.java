package com.marcador;

import javafx.scene.image.Image;
import java.io.File;

public class Player {

    // Enumerado para saber de qué equipo es
    public enum Equipo { LOCAL, VISITANTE, SIN_ASIGNAR }

    private String id;
    private String nombreCorto;
    private String nombreCompleto;
    private String rutaCarta;        
    private String rutaVideoCelebracion; 
    private Equipo equipoAsignado; // Nuevo atributo

    private Image imagenCartaCache;

    // Constructor completo
    public Player(String nombreCorto, String nombreCompleto, String rutaCarta, String rutaVideoCelebracion) {
        this.id = "ID_" + System.currentTimeMillis() + "_" + Math.random();
        this.nombreCorto = nombreCorto;
        this.nombreCompleto = nombreCompleto;
        this.rutaCarta = rutaCarta;
        this.rutaVideoCelebracion = rutaVideoCelebracion;
        this.equipoAsignado = Equipo.SIN_ASIGNAR; // Por defecto no juegan
    }

    public String getNombreCorto() { return nombreCorto; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getRutaVideoCelebracion() { return rutaVideoCelebracion; }
    
    public Equipo getEquipoAsignado() { return equipoAsignado; }
    public void setEquipoAsignado(Equipo equipo) { this.equipoAsignado = equipo; }

    public Image getImagenCarta() {
        if (imagenCartaCache == null) {
            try {
                if (rutaCarta != null && !rutaCarta.isEmpty()) {
                    File file = new File(rutaCarta);
                    if (file.exists()) imagenCartaCache = new Image(file.toURI().toString());
                }
            } catch (Exception e) { }
        }
        return imagenCartaCache;
    }
    
    public boolean tieneVideoPersonalizado() {
        return rutaVideoCelebracion != null && !rutaVideoCelebracion.isEmpty();
    }

    @Override
    public String toString() { return nombreCorto; }
}