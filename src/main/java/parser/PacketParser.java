package parser;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.VTGSentence;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import sentence.UnknownParser;


import java.io.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
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
        StringBuilder builder = new StringBuilder();
        if (sentence.getSentenceId().equals(GGA.toString())){
            GGASentence ggaSentence = (GGASentence) sentence;
            Time time = ggaSentence.getTime();
            builder.append("Время UTC: ");
            builder.append(time.getHour());
            builder.append(":");
            builder.append(time.getMinutes());
            builder.append(":");
            builder.append(Math.round(time.getSeconds()));
            builder.append("\n");
            Position position = ggaSentence.getPosition();
            builder.append("Широта: ");
            builder.append(position.getLatitude());
            builder.append("\u00B0 ");
            builder.append(position.getLatitudeHemisphere());
            builder.append("\n");
            builder.append("Долгота: ");
            builder.append(position.getLongitude());
            builder.append("\u00B0" + " ");
            builder.append(position.getLongitudeHemisphere());
            builder.append("\n");
            builder.append("Высота над уровнем моря: ");
            builder.append(position.getAltitude());
            builder.append(" m");
            builder.append("\n");
            builder.append("Способ вычисления координат: ");
            builder.append(ggaSentence.getFixQuality());
            builder.append("\n");
            builder.append("Количество активных спутников: ");
            builder.append(ggaSentence.getSatelliteCount());
            builder.append("\n");
            builder.append("Горизонтальный геометрический фактор ухудшения точности (HDOP): ");
            builder.append(ggaSentence.getHorizontalDOP());
            builder.append("\n");
           /* builder.append("Высота над уровнем моря: ");
            builder.append(ggaSentence.getGeoidalHeight());
            builder.append(" ");
            builder.append(ggaSentence.getGeoidalHeightUnits());
            builder.append("\n");
            builder.append("Количество секунд прошедших с получения последней DGPS поправки: ");
            builder.append(ggaSentence.getDgpsAge());
            builder.append("\n");
            builder.append("ID базовой станции предоставляющей DGPS поправки: ");
            builder.append(ggaSentence.getDgpsStationId());
            builder.append("\n");*/
            return builder.toString();
        }
        if (sentence.getSentenceId().equals(VTG.toString())){
            VTGSentence vtgSentence = (VTGSentence) sentence;
            try {
                double magneticCourse = ((VTGSentence) sentence).getMagneticCourse();
            }
            catch (DataNotAvailableException e) {

            }
/*            builder.append("Магнитный курс: ");
            builder.append(vtgSentence.getMagneticCourse());
            builder.append("\n");*/
            builder.append("Курс на истинный полюс в градусах: ");
            builder.append(vtgSentence.getTrueCourse());
            builder.append("\n");
            builder.append("Скорость, км/ч: ");
            builder.append(vtgSentence.getSpeedKmh());
            builder.append("\n");
            builder.append("Скорость, узлы: ");
            builder.append(vtgSentence.getSpeedKnots());
            builder.append("\n");
            builder.append("Способ вычисления скорости и курса: ");
            builder.append(vtgSentence.getMode());
            builder.append("\n");
            return builder.toString();
        }
        return "";
    }
}
