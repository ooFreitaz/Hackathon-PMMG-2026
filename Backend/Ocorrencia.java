
import Backend.*;
import java.time.LocalTime;

public class Ocorrencia {
    private String id;
    private double latitude;
    private double longitude;
    private String gravidade; 
    private LocalTime horario;

    public Ocorrencia(String id, double latitude, double longitude, String gravidade, LocalTime horario) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gravidade = gravidade;
        this.horario = horario;
    }

    public int getPesoGravidade() {
        return switch (this.gravidade.toUpperCase()) {
            case "A" -> 4;
            case "B" -> 5;
            case "C" -> 1;
            case "D" -> 1;
            case "E" -> 2;
            case "F" -> 3;
            default ->1;
        };
    }

    // Getters e Setters
    public String getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getGravidade() { return gravidade; }
    public LocalTime getHorario() { return horario; }
}