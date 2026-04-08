package it.unipi.Voyager.dto;

public class TripFrequencyDTO {
    private Double avgGapDays;       // Media giorni tra un viaggio e l'altro
    private Double daysSinceLastTrip;// Giorni passati dall'ultimo viaggio ad oggi
    private Double churnScore;      // Rapporto di velocità (Score > 1 indica rallentamento)
    private String status;           // "at_risk", "slowing", o "active"

    // Getter e Setter
    public Double getAvgGapDays() { return avgGapDays; }
    public void setAvgGapDays(Double avgGapDays) { this.avgGapDays = avgGapDays; }
    public Double getDaysSinceLastTrip() { return daysSinceLastTrip; }
    public void setDaysSinceLastTrip(Double daysSinceLastTrip) { this.daysSinceLastTrip = daysSinceLastTrip; }
    public Double getChurnScore() { return churnScore; }
    public void setChurnScore(Double churnScore) { this.churnScore = churnScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}