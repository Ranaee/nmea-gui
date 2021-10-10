package parser;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.Sentence;
import parser.sentence.UnknownParser;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketParser {

    private static final int PACKET_LENGTH = 8;

    public static List<Record> parse(File nmeaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(nmeaFile));
        SentenceFactory sentenceFactory = SentenceFactory.getInstance();
        List<Record> records = new ArrayList<>();
        boolean endOfTheLoop = false;
        while (true) {
            List<Sentence> sentences = new ArrayList<>();
            for (int j = 0; j < PACKET_LENGTH; j++) {
                String line = reader.readLine();
                if (line == null) {
                    records.add(new Record(sentences));
                    endOfTheLoop = true;
                    break;
                }
                try {
                    Sentence sentence = sentenceFactory.createParser(line);
                    sentences.add(sentence);
                } catch (UnsupportedSentenceException e) {
                    sentences.add(new UnknownParser());
                }
            }
            if (endOfTheLoop) {
                break;
            }
            records.add(new Record(sentences));
        }
        return records;
    }
}
