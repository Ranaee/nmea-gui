package parser;


import net.sf.marineapi.nmea.sentence.Sentence;

import java.util.List;

public class Record {

    List<Sentence> fields;

    public Record(List<Sentence> fields) {
        this.fields = fields;
    }
}

