package parser;

import exception.DataUnavailableException;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.*;
import net.sf.marineapi.nmea.util.*;
import net.sf.marineapi.nmea.util.Position;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.Nullable;
import sentence.UnknownParser;
import sentence.UnknownSentence;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.sf.marineapi.nmea.sentence.SentenceId.*;

/**
 * Парсер файлов протокола NMEA.
 * Данный парсер поддерживает входные файлы, разделяемые на записи/пакеты из N предложений (8 предложений по умолчанию);
 * Каждая запись состоит из следующих предложений в указанном порядке: RMC, GGA, GSA, GSA, VTG, ZDA, GLL, TKU;
 * Парсер позволяет создавать CSV-файл, содержащий координаты, соответствующие каждой записи.
 */
public class PacketParser {

    private static final int PACKET_LENGTH = 8;

    private static final String POSITION_FILE_NAME = "./pos.csv";
    private static final String DOP_FILE_NAME = "./dop.csv";

    private static final String[] POSITION_CSV_HEADER = {"pos_x", "pos_y"};
    private static final String[] DOP_CSV_HEADER = {"hdop", "vdop", "pdop"};

    private static final String GGA_STR = "GGA";
    private static final String GSA_STR = "GSA";
    private static final String GSV_STR = "GSV";
    private static final String ZDA_STR = "ZDA";
    private static final String RMC_STR = "RMC";
    private static final String VTG_STR = "VTG";
    private static final String GLL_STR = "GLL";

    private static final String UNKNOWN_SENTENCE_TYPE = "Неизвестный тип записи";


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
        boolean correctRecords = records.stream().allMatch(x -> x.getFields().get(0).getSentenceId().equals(RMC.toString()));
        if (!correctRecords) {
            System.out.println("Incorrect file format");
        }
        return records;
    }

    public static String getSentenceDescription(Sentence sentence) {
        if (sentence instanceof UnknownSentence) {
            return UNKNOWN_SENTENCE_TYPE;
        }
        String sentenceId = sentence.getSentenceId();
        switch (sentenceId) {
            case GGA_STR:
                return getGGADesription((GGASentence) sentence);
            case GLL_STR:
                return getGLLDesription((GLLSentence) sentence);
            case GSA_STR:
                return getGSADesription((GSASentence) sentence);
            case ZDA_STR:
                return getZDADesription((ZDASentence) sentence);
            case RMC_STR:
                return getRMCDesription((RMCSentence) sentence);
            case GSV_STR:
                return getGSVDesription((GSVSentence) sentence);
            case VTG_STR:
                return getVTGDesription((VTGSentence) sentence);
            default:
                return UNKNOWN_SENTENCE_TYPE;
        }
    }

    private static String getDirectionString(String enumString) {
        switch (enumString) {
            case "NORTH":
                return "Север";
            case "SOUTH":
                return "Юг";
            case "WEST":
                return "Запад";
            case "EAST":
                return "Восток";
            default:
                return "Неизвестно";
        }
    }

    private static String getGGADesription(GGASentence ggaSentence) {
        StringBuilder builder = new StringBuilder();
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
        return builder.toString();
    }

    private static String getGLLDesription(GLLSentence gllSentence) {
        StringBuilder builder = new StringBuilder();
        Time time = gllSentence.getTime();
        Position position = gllSentence.getPosition();
        String latitude = position.getLatitudeHemisphere().toChar() + " " + position.getLatitude();
        String longitude = position.getLongitudeHemisphere().toChar() + " " + position.getLongitude();
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
        builder.append(gllSentence.getMode());
        builder.append("\n");
        return builder.toString();
    }

    private static String getGSADesription(GSASentence gsaSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Режим выбора формата 2D/3D: ");
        builder.append(gsaSentence.getFixStatus());
        builder.append("\n");
        builder.append("Режим выбранного формата: ");
        builder.append(gsaSentence.getMode());
        builder.append("\n");
        builder.append("ID активных спутников: ");
        builder.append(Arrays.toString(gsaSentence.getSatelliteIds()));
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
    }

    private static String getZDADesription(ZDASentence zdaSentence) {
        StringBuilder builder = new StringBuilder();
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

    private static String getRMCDesription(RMCSentence rmcSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Статус: ");
        DataStatus status = rmcSentence.getStatus();
        switch (status) {
            case ACTIVE:
                builder.append("Активный");
            case VOID:
                builder.append("Недействителен");
        }
        builder.append("\n");
        builder.append("Скорость над землей в узлах: ");
        builder.append(rmcSentence.getSpeed());
        builder.append("\n");
        builder.append("Режим работы: ");
        builder.append(rmcSentence.getMode().toString());
        builder.append("\n");
        builder.append("ID активного спутника: ");
        builder.append(rmcSentence.getCourse());
        builder.append("\n");
        try {
            String directionOfVariation = rmcSentence.getDirectionOfVariation().toString();
            builder.append("Направление изменения: ");
            builder.append(getDirectionString(directionOfVariation));
            builder.append("\n");
        } catch (DataNotAvailableException ignored) {
        }
        return builder.toString();
    }

    private static String getGSVDesription(GSVSentence gsvSentence) {
        StringBuilder builder = new StringBuilder();
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

    private static String getVTGDesription(VTGSentence vtgSentence) {
        StringBuilder builder = new StringBuilder();
        try {
            double magneticCourse = vtgSentence.getMagneticCourse();
            builder.append("Магнитный курс: ");
            builder.append(magneticCourse);
            builder.append("\n");
        } catch (DataNotAvailableException ignored) {
        }
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


    @Nullable
    public static File createPositionCsv(List<Record> records) {
        File outputFile = new File(POSITION_FILE_NAME);
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withHeader(POSITION_CSV_HEADER))) {
            records.forEach(x -> {
                RMCSentence sentence = (RMCSentence) x.getFields().get(0);
                Position position = sentence.getPosition();
                try {
                    printer.printRecord(position.getLatitude(), position.getLongitude());
                } catch (IOException e) {
                    System.out.println("Error occurred during writing line");
                }
            });
            return outputFile;
        } catch (IOException e) {
            System.out.println("Error occurred during output file creation");
            return null;
        }
    }

    @Nullable
    public static File createDOPCsv(List<Record> records){
        File outputFile = new File(DOP_FILE_NAME);
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withHeader(DOP_CSV_HEADER))){
            records.forEach(x->{
                List<Sentence> fields = x.getFields();
                GSASentence gsaSentence = (GSASentence) fields.stream().filter(sentence->sentence.getSentenceId().equals(GSA_STR)).findFirst().orElseThrow(()->new DataUnavailableException("Отсутствует GSA-запись"));
                try {
                    printer.printRecord(gsaSentence.getHorizontalDOP(), gsaSentence.getVerticalDOP(), gsaSentence.getPositionDOP());
                } catch (IOException e) {
                    System.out.println("Error occurred during writing line");
                }
            });
            return outputFile;
        } catch (IOException e) {
            System.out.println("Error occurred during output file creation");
            return null;
        }
    }

}
