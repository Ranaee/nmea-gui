package parser;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.VTGSentence;
import sentence.UnknownParser;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static net.sf.marineapi.nmea.sentence.SentenceId.GGA;
import static net.sf.marineapi.nmea.sentence.SentenceId.VTG;

public class PacketParser {

    private static final int PACKET_LENGTH = 8;

    public static List<Record> parse(File nmeaFile) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(nmeaFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        SentenceFactory sentenceFactory = SentenceFactory.getInstance();
        List<Record> records = new ArrayList<>();
        boolean endOfTheLoop = false;
        int i = 0;
        while (true) {
            List<Sentence> sentences = new ArrayList<>();
            for (int j = 0; j < PACKET_LENGTH; j++) {
                String line = null;
                try {
                    if (reader != null) {
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (line == null) {
                    records.add(new Record(sentences, i));
                    endOfTheLoop = true;
                    break;
                }
                try {
                    Sentence sentence = sentenceFactory.createParser(line);
                    sentences.add(sentence);
                } catch (UnsupportedSentenceException e) {
                    sentences.add(new UnknownParser(line));
                }
            }
            if (endOfTheLoop) {
                break;
            }
            i++;
            records.add(new Record(sentences, i));
        }
        return records;
    }

    public static String getSentenceDescription(Sentence sentence){
        if (sentence.getSentenceId().equals(GGA.toString())){

        }
        if (sentence.getSentenceId().equals(VTG.toString())){
            StringBuilder builder = new StringBuilder();
            VTGSentence vtgSentence = (VTGSentence) sentence;
            try {
                double magneticCourse = ((VTGSentence) sentence).getMagneticCourse();
            }
            catch (DataNotAvailableException e) {

            }
/*            builder.append("Магнитный курс: ");
            builder.append(vtgSentence.getMagneticCourse());
            builder.append("\n");*/
            builder.append("Настоящий курс: ");
            builder.append(vtgSentence.getTrueCourse());
            builder.append("\n");
            return builder.toString();
        }
        return "";
    }
}
