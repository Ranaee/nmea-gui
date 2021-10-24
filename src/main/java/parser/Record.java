package parser;


import net.sf.marineapi.nmea.sentence.Sentence;

import java.util.List;
//todo в записи может быть ни одной zda - это жопа
public class Record{

    private String name;

    private List<Sentence> fields;

    public List<Sentence> getFields() {
        return fields;
    }

    public String getName() {
        return name;
    }

    public Record(List<Sentence> fields, int number) {
        this.fields = fields;
        this.name = "Запись " + number;
    }

    @Override
    public String toString() {
        return name;
    }

}

