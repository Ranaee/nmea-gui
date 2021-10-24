package parser;

public enum SentenceTypes {

    GGA("GGA"),
    GSA("GSA"),
    VTG("VTG"),
    ZDA("ZDA"),
    GLL("GLL"),
    TKU("TLL"),
    RMC("RMC"),
    GSV("GSV");

    private String type;

    SentenceTypes(String type){
        this.type = type;
    }
}