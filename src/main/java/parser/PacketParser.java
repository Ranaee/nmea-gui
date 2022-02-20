package parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.VTGSentence;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import sentence.UnknownParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.sf.marineapi.nmea.sentence.SentenceId.*;

public class PacketParser {

    private static final int PACKET_LENGTH = 8;

    public static List<Record> parse(File nmeaFile, boolean parseGSV) {
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
                    if (parseGSV || !"GSV".equals(sentence.getSentenceId())) {
                        sentences.add(sentence);
                    }
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
            Sentence sentence = null;
            boolean start = true;
            while (start || sentence.getSentenceId() != null) {
                start = false;
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
                    sentence = sentenceFactory.createParser(line);
                    sentences.add(sentence);
                } catch (UnsupportedSentenceException e) {
                    sentence = new UnknownParser(line);
                    sentences.add(sentence);
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

    public static String getSentenceDescription(Sentence sentence) {
        StringBuilder builder = new StringBuilder();
        if (sentence.getSentenceId().equals(GGA.toString())) {
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
        if (sentence.getSentenceId().equals(GLL.toString())) {
            GLLSentence gllSentence = (GLLSentence) sentence;
            Position position = gllSentence.getPosition();
            String latitude = position.getLatitudeHemisphere().toChar() + String.valueOf(position.getLatitude());
            String longitude = position.getLongitudeHemisphere().toChar() + String.valueOf(position.getLongitude());
            builder.append("Широта: ");
            builder.append(latitude);
            builder.append("\n");
            builder.append("Долгота: ");
            builder.append(longitude);
            builder.append("\n");
            builder.append("Время: ");
            builder.append(gllSentence.getTime());
            builder.append("\n");
            builder.append("Достоверность получаемых координат: ");
            builder.append(gllSentence.getStatus());
            builder.append("\n");
            builder.append("Способ получения данных: ");
            //todo добавит легенду
            builder.append(gllSentence.getMode());
            builder.append("\n");
            return builder.toString();
        }
        if (sentence.getSentenceId().equals(VTG.toString())) {
            VTGSentence vtgSentence = (VTGSentence) sentence;
            try {
                double magneticCourse = ((VTGSentence) sentence).getMagneticCourse();
            } catch (DataNotAvailableException e) {

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

    public static List<parser.Position> getPositionList(File nmeaFile) {
        List<Record> record = PacketParser.parse(nmeaFile);
        return record.stream().map(x -> {
            GGASentence ggaSentence = (GGASentence) (x.getFields().get(1));
            Position position = ggaSentence.getPosition();
            return new parser.Position(String.format("%.2f", position.getLatitude()),position.getLatitudeHemisphere().toString(),String.format("%.2f", position.getLongitude()),position.getLongitudeHemisphere().toString());
        }).collect(Collectors.toList());

    }

    public static String getPositionFile(File from){
        List<parser.Position> positions = getPositionList(from);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(positions);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
