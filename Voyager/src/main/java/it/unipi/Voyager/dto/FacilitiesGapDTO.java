package it.unipi.Voyager.dto;

import java.util.List;

public class FacilitiesGapDTO {
    private List<String> missing;

    public List<String> getMissing() { return missing; }
    public void setMissing(List<String> missing) { this.missing = missing; }
}