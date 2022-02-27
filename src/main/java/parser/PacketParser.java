package parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.tk.TKPulseListener;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.*;
import net.sf.marineapi.nmea.util.Date;
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
            builder.append("ч:");
            builder.append(time.getMinutes());
            builder.append("м:");
            builder.append(Math.round(time.getSeconds()));
            builder.append("с");
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
            Time time = gllSentence.getTime();
            Position position = gllSentence.getPosition();
            String latitude = position.getLatitudeHemisphere().toChar() + " " + String.valueOf(position.getLatitude());
            String longitude = position.getLongitudeHemisphere().toChar() + " " + String.valueOf(position.getLongitude());
            builder.append("Широта: ");
            builder.append(latitude);
            builder.append("\u00B0 ");
            builder.append("\n");
            builder.append("Долгота: ");
            builder.append(longitude);
            builder.append("\u00B0 ");
            builder.append("\n");
            builder.append("Время UTC: ");
            builder.append(time.getHour());
            builder.append("ч:");
            builder.append(time.getMinutes());
            builder.append("м:");
            builder.append(Math.round(time.getSeconds()));
            builder.append("с");
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
        if (sentence.getSentenceId().equals(GSA.toString())) {
            GSASentence gsaSentence = (GSASentence) sentence;
            builder.append("Режим выбора формата 2D/3D: ");
            builder.append(gsaSentence.getFixStatus());
            builder.append("\n");
            builder.append("Режим выбранного формата: ");
            builder.append(gsaSentence.getMode());
            builder.append("\n");
            builder.append("ID активного спутника: ");
            builder.append(gsaSentence.getSatelliteIds());
            builder.append("\n");
            builder.append("Пространственный геометрический фактор ухудшения точности (PDOP): ");
            builder.append(gsaSentence.getPositionDOP());
            builder.append("\n");
            builder.append("Горизонтальный геометрический фактор ухудшения точности (HDOP): ");
            builder.append(gsaSentence.getHorizontalDOP());
            builder.append("\n");
            builder.append("Вертикальный геометрический фактор ухудшения точности (VDOP): ");
            builder.append(gsaSentence.getVerticalDOP());
            builder.append("\n");
            builder.append("Номер навигационной системы: ");
            //todo добавит легенду
            builder.append(gsaSentence.getTalkerId());
            builder.append("\n");
            return builder.toString();
        }
        /*if (sentence.getSentenceId().equals(TKU.toString())) {
            TKUSentence tkuSentence = (TKUSentence) sentence;
            builder.append("Режим выбора формата 2D/3D: ");
            builder.append(gsaSentence.getFixStatus());
            builder.append("\n");
            builder.append("Режим выбранного формата: ");
            builder.append(gsaSentence.getMode());
            builder.append("\n");
            builder.append("ID активного спутника: ");
            builder.append(gsaSentence.getSatelliteIds());
            builder.append("\n");
            builder.append("Пространственный геометрический фактор ухудшения точности (PDOP): ");
            builder.append(gsaSentence.getPositionDOP());
            builder.append("\n");
            builder.append("Горизонтальный геометрический фактор ухудшения точности (HDOP): ");
            builder.append(gsaSentence.getHorizontalDOP());
            builder.append("\n");
            builder.append("Вертикальный геометрический фактор ухудшения точности (VDOP): ");
            builder.append(gsaSentence.getVerticalDOP());
            builder.append("\n");
            builder.append("Номер навигационной системы: ");
            builder.append(gsaSentence.getTalkerId());
            builder.append("\n");
            return builder.toString();
        }*/
        if (sentence.getSentenceId().equals(ZDA.toString())) {
            ZDASentence zdaSentence = (ZDASentence) sentence;
            Time time = zdaSentence.getTime();
            Date date = zdaSentence.getDate();
            builder.append("Время UTC: ");
            builder.append(time.getHour());
            builder.append("ч:");
            builder.append(time.getMinutes());
            builder.append("м:");
            builder.append(Math.round(time.getSeconds()));
            builder.append("с");
            builder.append("\n");
            builder.append("Дата: ");
            builder.append(date.getDay());
            builder.append(".");
            builder.append(date.getMonth());
            builder.append(".");
            builder.append(date.getYear());
            builder.append("\n");
            return builder.toString();
        }
        if (sentence.getSentenceId().equals(RMC.toString())) {
            RMCSentence rmcSentence = (RMCSentence) sentence;
            builder.append("Время UTC: ");
            builder.append(rmcSentence.getTime());
            builder.append("\n");
            builder.append("Достоверность полученных координат: ");
            builder.append(rmcSentence.getCorrectedCourse());
            builder.append("\n");
            builder.append("ID активного спутника: ");
            builder.append(rmcSentence.getCourse());
            builder.append("\n");
            builder.append("Пространственный геометрический фактор ухудшения точности (PDOP): ");
            builder.append(rmcSentence.getSpeed());
            builder.append("\n");
            builder.append("Горизонтальный геометрический фактор ухудшения точности (HDOP): ");
            builder.append(rmcSentence.getDate());
            builder.append("\n");
            builder.append("Вертикальный геометрический фактор ухудшения точности (VDOP): ");
            builder.append(rmcSentence.getDirectionOfVariation());
            builder.append("\n");
            //todo добавит легенду
            return builder.toString();
        }
        if (sentence.getSentenceId().equals(GSV.toString())) {
            GSVSentence gsvSentence = (GSVSentence) sentence;
            builder.append("Количество выводимых сообщений: ");
            builder.append(gsvSentence.getSentenceCount());
            builder.append("\n");
            builder.append("Номер сообщения: ");
            builder.append(gsvSentence.getSentenceIndex());
            builder.append("\n");
            builder.append("Количество наблюдаемых спутников: ");
            builder.append(gsvSentence.getSatelliteCount());
            builder.append("\n");
            builder.append("Данные о спутнике: ");
            builder.append(gsvSentence.getSatelliteInfo());
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
