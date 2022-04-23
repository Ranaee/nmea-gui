package parser;

import net.sf.marineapi.nmea.sentence.Sentence;

import java.util.List;

public class Record{

    private final String name;

    private final List<Sentence> sentences;

    public List<Sentence> getSentences() {
        return sentences;
    }

    public Record(List<Sentence> sentences, int number) {
        this.sentences = sentences;
        this.name = "Запись " + number;
    }

    @Override
    public String toString() {
        return name;
    }

}

