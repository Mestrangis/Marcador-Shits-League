package com.marcador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.marcador.Player.Equipo;

public class GestorDatos {

    private List<Player> listaTotalJugadores;
    private String rutaVideoDefault; 

    public GestorDatos() {
        listaTotalJugadores = new ArrayList<>();
        this.rutaVideoDefault = "gol.mp4"; 
        cargarDatosDesdeArchivo();
    }

    private void cargarDatosDesdeArchivo() {
        // --- AQUÍ ESTÁ EL TRUCO PARA QUE FUNCIONE CON DOBLE CLIC ---
        // 1. Averiguamos dónde está el archivo .jar (o la clase compilada)
        File archivoConfig = null;
        try {
            String pathJar = GestorDatos.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // Decodificar la ruta por si tiene espacios (%20)
            String decodedPath = URLDecoder.decode(pathJar, StandardCharsets.UTF_8.toString());
            File jarFile = new File(decodedPath);
            
            // Obtenemos la carpeta donde está el jar
            File carpetaDelJar = jarFile.getParentFile();
            
            // Buscamos el txt en ESA carpeta
            archivoConfig = new File(carpetaDelJar, "configuracion.txt");
            
        } catch (Exception e) {
            // Si falla el truco, usamos el método antiguo por si acaso
            archivoConfig = new File("configuracion.txt");
        }
        
        // --- CHIVATOS ---
        System.out.println("--- INICIANDO CARGA DE DATOS ---");
        System.out.println("Buscando archivo en: " + archivoConfig.getAbsolutePath());
        
        if (!archivoConfig.exists()) {
            System.out.println("❌ ERROR: El archivo no existe en la ruta detectada.");
            return;
        } else {
            System.out.println("✅ Archivo encontrado. Leyendo...");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivoConfig))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                if (linea.startsWith("VIDEO_DEFAULT")) {
                    String[] partes = linea.split("=");
                    if (partes.length >= 2) {
                        this.rutaVideoDefault = procesarRuta(partes[1], archivoConfig.getParent());
                        System.out.println("-> Configurado Video Default: " + this.rutaVideoDefault);
                    }
                    continue;
                }

                // JUGADORES
                String[] datos = linea.split(",");
                if (datos.length >= 2) {
                    String corto = datos[0].trim();
                    String largo = datos[1].trim();
                    String img = (datos.length > 2) ? procesarRuta(datos[2], archivoConfig.getParent()) : null;
                    String vid = (datos.length > 3) ? procesarRuta(datos[3], archivoConfig.getParent()) : null;

                    Player p = new Player(corto, largo, img, vid);
                    listaTotalJugadores.add(p);
                    System.out.println("-> Jugador cargado: " + corto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error leyendo configuración: " + e.getMessage());
        }
        System.out.println("--- FIN CARGA (" + listaTotalJugadores.size() + " jugadores) ---");
    }

    // He mejorado este método para usar la carpeta del JAR como base
    private String procesarRuta(String rutaBruta, String carpetaBaseJar) {
        if (rutaBruta == null) return null;
        String limpia = rutaBruta.trim().replace("\"", ""); 
        if (limpia.equalsIgnoreCase("null") || limpia.isEmpty()) return null;

        File f = new File(limpia);
        if (f.isAbsolute()) {
            return limpia;
        } else {
            // Si es relativa, la pegamos a la carpeta donde está el JAR
            return carpetaBaseJar + File.separator + limpia;
        }
    }

    public List<Player> getTodos() { return listaTotalJugadores; }
    
    public void agregarJugadorPersonalizado(String nombre, String rutaImg, String rutaVid) {
        listaTotalJugadores.add(new Player(nombre, nombre, rutaImg, rutaVid));
    }
    
    public String getRutaVideoDefault() { return rutaVideoDefault; }
    
    public List<Player> getJugadoresPorEquipo(Equipo equipo) {
        List<Player> filtro = new ArrayList<>();
        for (Player p : listaTotalJugadores) {
            if (p.getEquipoAsignado() == equipo) filtro.add(p);
        }
        return filtro;
    }
}