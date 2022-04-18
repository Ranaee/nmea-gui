package parser;

import net.sf.marineapi.nmea.sentence.Sentence;

import java.util.List;

public class Record{

    private final String name;

    private final List<Sentence> fields;

    public List<Sentence> getFields() {
        return fields;
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

