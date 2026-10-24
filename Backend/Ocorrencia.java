import java.time.LocalDate;
import java.time.LocalTime;

public class Ocorrencia {
    private String id;
    private double latitude;
    private double longitude;
    private char natureza; // Ex: "A", "B", "C"
    private LocalTime hora;
    private int pesoDoCrime;

    public Ocorrencia(double latitude, double longitude, LocalTime hora,char natureza){
        this.latitude=latitude;
        this.longitude=longitude;
        this.hora=hora;
        this.natureza=natureza;
        switch (natureza) {
            case 'A':
                pesoDoCrime=4;
                break;
            case 'B':
                pesoDoCrime=5;
                break;
                case 'C':
                pesoDoCrime=1;
                break;
            case 'D':
                pesoDoCrime=1;
                break;
                case 'E':
                pesoDoCrime=2;
                break;
            case 'F':
                pesoDoCrime=3;
                break;
            default:
                break;
        }
    }
    // Getters e Setters
    public double getLatitude(){
        return latitude;
    }
    public double getLongitude(){
        return longitude;
    }
    
    public LocalTime getHora(){
        return hora;
    }
    public char getNatureza(){
        return natureza;
    }
    public int getPeso(){
        return pesoDoCrime;
    }

    // Getters e Setters
    public String getId() 
    { return id; }
    
}