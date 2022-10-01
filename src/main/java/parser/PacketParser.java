package parser;

import exception.UnsupportedLineException;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.UnsupportedSentenceException;
import net.sf.marineapi.nmea.sentence.*;
import net.sf.marineapi.nmea.util.DataStatus;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.Nullable;
import parser.data.PositionWithTime;
import parser.data.Record;
import parser.sentence.UnknownParser;
import parser.sentence.UnknownSentence;

import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Парсер файлов протокола NMEA.
 * Данный парсер поддерживает входные файлы, разделяемые на записи/пакеты из N предложений, каждое запись начинается с предложения типа GGA;
 * Парсер позволяет создавать CSV-файлы, содержащий координаты, соответствующие каждой записи.
 */
public class PacketParser {

    public static final String OUTPUT_PREFIX = "./output/";
    private static final String POSITION_FILE_NAME = OUTPUT_PREFIX + "pos.csv";
    private static final String ACTUAl_POSITION_FILE_NAME = OUTPUT_PREFIX + "actual_pos.csv";
    private static final String DOP_FILE_NAME = OUTPUT_PREFIX + "dop.csv";

    private static final String[] POSITION_CSV_HEADER = {"pos_x", "pos_y"};
    private static final String[] DOP_CSV_HEADER = {"hdop", "vdop", "pdop"};
    private static final String[] DELTA_CSV_HEADER = {"latitude", "longitude", "time", "lat_m" ,"long_m"};

    private static final String GGA_STR = "GGA";
    private static final String GSA_STR = "GSA";
    private static final String GSV_STR = "GSV";
    private static final String ZDA_STR = "ZDA";
    private static final String RMC_STR = "RMC";
    private static final String VTG_STR = "VTG";
    private static final String GLL_STR = "GLL";

    private static final String UNKNOWN_SENTENCE_TYPE = "Неизвестный тип записи";

    private static final String WHITESPACE_SPLIT_PATTERN = "\\s+";

    private final static double WGS84az=6378137.00;
    private final static double WGS84e1=0.081819199;

    public static class ConvertedDTO {
        private final double longitudeD;
        private final double latitudeD;
        private final double altitudeD;
        private final double longitudeM;
        private final double latitudeM;
        private final double altitudeM;
        private final int satelliteCount;

        @Nullable
        private LocalDateTime dateTime;
        private LocalTime time;

        public ConvertedDTO(double longitudeD, double latitudeD, double altitudeD, double longitudeM, double latitudeM, double altitudeM, LocalDateTime dateTime, int satelliteCount) {
            this.longitudeD = longitudeD;
            this.latitudeD = latitudeD;
            this.altitudeD = altitudeD;
            this.longitudeM = longitudeM;
            this.latitudeM = latitudeM;
            this.altitudeM = altitudeM;
            this.dateTime = dateTime;
            this.time = dateTime.toLocalTime();
            this.satelliteCount = satelliteCount;
        }

        public ConvertedDTO(double longitudeD, double latitudeD, double altitudeD, double longitudeM, double latitudeM, double altitudeM, LocalDateTime dateTime) {
            this.longitudeD = longitudeD;
            this.latitudeD = latitudeD;
            this.altitudeD = altitudeD;
            this.longitudeM = longitudeM;
            this.latitudeM = latitudeM;
            this.altitudeM = altitudeM;
            this.dateTime = dateTime;
            this.time = dateTime.toLocalTime();
            this.satelliteCount = -1;
        }
        public ConvertedDTO(double longitudeD, double latitudeD, double altitudeD, double longitudeM, double latitudeM, double altitudeM, LocalTime time, int satelliteCount) {
            this.longitudeD = longitudeD;
            this.latitudeD = latitudeD;
            this.altitudeD = altitudeD;
            this.longitudeM = longitudeM;
            this.latitudeM = latitudeM;
            this.altitudeM = altitudeM;
            this.dateTime = null;
            this.time = time;
            this.satelliteCount = satelliteCount;
        }

        public ConvertedDTO(double longitudeD, double latitudeD, double altitudeD, double longitudeM, double latitudeM, double altitudeM, LocalTime time) {
            this.longitudeD = longitudeD;
            this.latitudeD = latitudeD;
            this.altitudeD = altitudeD;
            this.longitudeM = longitudeM;
            this.latitudeM = latitudeM;
            this.altitudeM = altitudeM;
            this.dateTime = null;
            this.time = time;
            this.satelliteCount = -1;
        }

        public double getLongitudeD() {
            return longitudeD;
        }

        public double getLatitudeD() {
            return latitudeD;
        }

        public double getAltitudeD() {
            return altitudeD;
        }

        public double getLongitudeM() {
            return longitudeM;
        }

        public double getLatitudeM() {
            return latitudeM;
        }

        public double getAltitudeM() {
            return altitudeM;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public void setDateTime(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }
    }

    public static class InfoDTO implements PositionWithTime {
        private final double hDOP;
        private final double vDOP;
        private final double pDOP;
        private final double longitude;
        private final double latitude;
        private double altitude;
        private final int satelliteCount;
        private final LocalDateTime time;


        public InfoDTO(double hDOP, double vDOP, double pDOP, LocalDateTime time, double longitude, double latitude, double altitude, int satelliteCount) {
            this.hDOP = hDOP;
            this.vDOP = vDOP;
            this.pDOP = pDOP;
            this.longitude = longitude;
            this.latitude = latitude;
            this.altitude = altitude;
            this.time = time;
            this.satelliteCount = satelliteCount;
        }

        public InfoDTO(double hDOP, double vDOP, double pDOP, LocalDateTime time) {
            this.hDOP = hDOP;
            this.vDOP = vDOP;
            this.pDOP = pDOP;
            this.time = time;
            this.longitude = 0;
            this.latitude = 0;
            this.altitude = 0;
            this.satelliteCount = 0;
        }

        public InfoDTO(double latitude, double longitude, LocalDateTime time){
            this.time = time;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = 0;
            this.hDOP = 0;
            this.vDOP = 0;
            this.pDOP = 0;
            this.satelliteCount = 0;
        }

        public double getHDOP() {
            return hDOP;
        }

        public double getVDOP() {
            return vDOP;
        }

        public double getPDOP() {
            return pDOP;
        }

        public int getSatelliteCount() {
            return satelliteCount;
        }

        public LocalDateTime getDateTime() {
            return time;
        }

        public LocalTime getTime(){
            return time.toLocalTime();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InfoDTO infoDTO = (InfoDTO) o;
            return Double.compare(infoDTO.hDOP, hDOP) == 0 && Double.compare(infoDTO.vDOP, vDOP) == 0 && Double.compare(infoDTO.pDOP, pDOP) == 0 && Double.compare(infoDTO.longitude, longitude) == 0 && Double.compare(infoDTO.latitude, latitude) == 0 && Double.compare(infoDTO.altitude, altitude) == 0 && time.equals(infoDTO.time);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hDOP, vDOP, pDOP, longitude, latitude, altitude, time);
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getAltitude() {
            return altitude;
        }

        public ConvertedDTO getConvertedDto(){
            double n = WGS84az/Math.sqrt(1 - Math.pow(WGS84e1,2)*Math.pow(Math.sin(latitude),2));
            double x = (n+altitude)*Math.cos(latitude)*Math.cos(longitude);
            double y = (n+altitude)*Math.cos(latitude)*Math.sin(longitude);
            double z = (n*(1-Math.pow(WGS84e1,2))+altitude)*Math.sin(latitude);
            return new ConvertedDTO(longitude, latitude, altitude, x,y,z,time);
        }
    }

    public static class InertialDTO implements PositionWithTime{

        private final LocalTime time;
        private final double latitude;
        private final double longitude;
        private final double hEll;
        private final double sdHoriz;
        private final double sdHeight;


        public InertialDTO(LocalTime time, double latitude, double longitude, double hEll, double sdHoriz, double sdHeight) {
            this.time = time;
            this.latitude = latitude;
            this.longitude = longitude;
            this.hEll = hEll;
            this.sdHoriz = sdHoriz;
            this.sdHeight = sdHeight;
        }


        public LocalTime getTime() {
            return time;
        }

        public LocalDateTime getDateTime(){
            return null;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getAltitude() {
            return hEll;
        }

        public double gethEll() {
            return hEll;
        }

        public double getSdHoriz() {
            return sdHoriz;
        }

        public double getSdHeight() {
            return sdHeight;
        }
    }

    public static class RTKPostDTO implements PositionWithTime{
        private final LocalDate date;
        private final LocalTime time;
        private final double latitude;
        private final double longitude;
        private final double height;
        private final int q;

        public RTKPostDTO(LocalDate date, LocalTime time, double latitude, double longitude, double height, int satteliteCount) {
            this.date = date;
            this.time = time;
            this.latitude = latitude;
            this.longitude = longitude;
            this.height = height;
            this.q = satteliteCount;
        }


        public LocalTime getTime() {
            return time;
        }

        public LocalDateTime getDateTime(){
            return LocalDateTime.of(date, time);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
        public double getAltitude(){
            return height;
        }

        public double getHeight() {
            return height;
        }

        public int getSatteliteCount(){
            return q;
        }
    }

    public static String getSentenceLegend(Sentence sentence) {
        if (sentence instanceof UnknownSentence) {
            return UNKNOWN_SENTENCE_TYPE;
        }
        String sentenceId = sentence.getSentenceId();
        switch (sentenceId) {
            case GGA_STR:
                return getGGALegend();
            case GLL_STR:
                return getGLLegend();
            case GSA_STR:
                return getGSALegend();
            case ZDA_STR:
                return getZDALegend();
            case RMC_STR:
                return getRMCLegend();
            case GSV_STR:
                return getGSVLegend();
            case VTG_STR:
                return getVTGLegend();
            default:
                return UNKNOWN_SENTENCE_TYPE;
        }
    }


    private static String getVTGLegend() {
        return "Курс на истинный полюс в градусах - x.x\n" +
                "Флаг достоверности курса - 'T'-True(Достоверный) / 'F'-False(Недостоверный)\n" +
                "Магнитное склонение в градусах (может не использоваться) - x.x\n" +
                "Относительно северного магнитного полюса (может не использоваться) - М(Магнитный)\n" +
                "Скорость - s.ss\n" +
                "Единица измерения скорости, узлы - N\n" +
                "Скорость - s.ss\n" +
                "Единица измерения скорости, км/ч - К\n" +
                "Способ вычисления скорости и курса: 'A' - автономный.\n" +
                "'D' - дифференциальный;\n" +
                "'E' - аппроксимация;\n" +
                "'M' - фиксированные данные;\n" +
                "'N' - недостоверные данные;\n" +
                "Контрольная сумма строки - *hh\n";
    }

    private static String getGSALegend() {
        return "Режим выбора формата 2D/3D: 'M' — ручной, принудительно включен 2D или 3D режим;\n" +
                "A — автоматический, разрешено автоматически выбирать 2D или 3D режим.\n" +
                "Режим выбранного формата: '1'- Местоположение не определено / '2'- 2D / '3'- 3D\n" +
                "ID активного спутника (максимум 12 спутников) - xx\n" +
                "Пространственный геометрический фактор ухудшения точности (PDOP) - x.x\n" +
                "Горизонтальный геометрический фактор ухудшения точности (HDOP) - x.x\n" +
                "Вертикальный геометрический фактор ухудшения точности (VDOP) - x.x\n" +
                "Номер навигационной системы (1-GPS \"GP\", 2-Glonass \"GL\",, 3-Galileo \"GA\", 4-Beidu \"BD\"; \"GN\" - источник данных GPS+Glonass)\n" +
                "Контрольная сумма строки - *hh\n";
    }

    private static String getGLLegend() {
        return "Широта - xxxx.xx\n" +
                "Направление широты: 'N'-север / 'S'-юг\n" +
                "Долгота - yyyy.yy\n" +
                "Направление долготы: 'E'-восток / 'W'-запад\n" +
                "Время UTC - hhmmss.ss\n" +
                "Достоверность полученных координат: 'A' - данные достоверны;\n" +
                "'V' - ошибочные данные.\n" +
                "Способ вычисления координат: 'A' - автономный;\n" +
                "'D' - дифференциальный;\n" +
                "'E' - аппроксимация;\n" +
                "'M' - фиксированные данные;\n" +
                "'N' - недостоверные данные.\n" +
                "Контрольная сумма строки - *hh\n";
    }

    private static String getGGALegend() {
        return "Время UTC - hhmmss.ss\n" +
        "Широта - xxxx.xx\n" +
        "Направление широты: 'N'-север / 'S'-юг\n" +
        "Долгота - xxxx.xx\n" +
        "Направление долготы: 'E'-восток / 'W'-запад\n" +
        "Индикатор качества GPS сигнала: '0' - недоступно.\n" +
        "'1' - автономно;\n" +
        "'2' - дифференциально;\n" +
        "'3' - PPS;\n" +
        "'4' - фиксированный RTK;\n" +
        "'5' - не фиксированный RTK;\n" +
        "'6' - экстраполяция;\n" +
        "'7' - фиксированные координаты;\n" +
        "'8' - режим симуляции;\n" +
        "Количество активных спутников, от \"00\" до \"12\"\n" +
        "Горизонтальный геометрический фактор ухудшения точности (HDOP)\n" +
        "Высота над уровнем моря - xxx\n" +
        "Единицы измерения высоты в метрах - M\n" +
        "Разница между эллипсоидом земли и уровнем моря - x.x\n" +
        "Единицы измерения в метрах - M\n" +
        "Количество секунд прошедших с получения последней DGPS поправки - x.x\n" +
        "ID базовой станции предоставляющей DGPS поправки (если включено DGPS) - xxxx\n" +
        "Контрольная сумма строки - *hh\n";
    }

    private static String getZDALegend() {
        return "Время UTC - hhmmss.ss\n" +
                "День - xx\n" +
                "Месяц - xx\n" +
                "Год - xxxx\n" +
                "Часовой пояс, смещение от GMT, от 00 до +-13 часов" +
                "Часовой пояс, смещение от GMT, минуты" +
                "Контрольная сумма строки - *hh\n";
    }

    private static String getRMCLegend() {
        return "Время UTC - hhmmss.ss\n" +
                "Статус:\n" +
                " А - данные верны;\n" +
                "V - данные не верны\n" +
                "Географическая широта -xxxx.xx\n" +
                "Север / Юг(N / S)\n" +
                "Географическая долгота -yyyy.yy\n" +
                "Запад / Восток(E / W)\n" +
                "Скорость в узлах - x.x\n" +
                "Направление движения в градусах -x.x\n" +
                "Дата на момент определения местоположения - ddmmyy\n" +
                "Магнитное склонения в градусах -x.x\n" +
                "Магнитное склонение на Запад/Восток(E / W)\n" +
                "Контрольная сумма строки - *hh\n";
    }

    private static String getGSVLegend() {
        return "Полное число сообщений - x\n" +
                "Номер сообщения - x\n" +
                "Полное число видимых спутников - xx\n" +
                "PRN номер спутника - xx\n" +
                "Высота, градусы - xx\n" +
                "Азимут истинный, градусы - xxx\n" +
                "Далее повторение для остальных спутников\n" +
                "Контрольная сумма строки - *hh\n";
    }

    public static List<Record> parse(File nmeaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(nmeaFile));
        SentenceFactory sentenceFactory = SentenceFactory.getInstance();
        List<Record> records = new ArrayList<>();
        boolean endOfTheLoop = false;
        int i = 0;
        String line;
        String firstLine = null;
        while (true) {
            List<Sentence> sentences = new ArrayList<>();
            Sentence sentence;
            boolean packetStarted = false;
            boolean startOfPacket = true;
            while (startOfPacket || packetStarted){
                line = firstLine == null ? reader.readLine() : firstLine;
                firstLine = null;
                if (line == null) {
                    records.add(new Record(sentences, i));
                    endOfTheLoop = true;
                    break;
                }
                try {
                    sentence = sentenceFactory.createParser(line);
                    boolean isGGA = sentence.getSentenceId().equals(GGA_STR);
                    if (isGGA){
                        if (packetStarted) {
                            packetStarted = false;
                            firstLine = line;
                            continue;
                        } else {
                            packetStarted = true;
                        }
                    }
                    if (!packetStarted){
                        continue;
                    }
                    sentences.add(sentence);
                    startOfPacket = false;
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
        boolean correctRecords = records.stream().allMatch(x -> x.getSentences().get(0).getSentenceId().equals(GGA_STR));
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
                return getGSDDescription((GSASentence) sentence);
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
        builder.append("Индикатор качества GPS сигнала: ");
        builder.append(ggaSentence.getFixQuality());
        builder.append("\n");
        builder.append("Количество активных спутников: ");
        builder.append(ggaSentence.getSatelliteCount());
        builder.append("\n");
        builder.append("Горизонтальный геометрический фактор ухудшения точности (HDOP): ");
        builder.append(ggaSentence.getHorizontalDOP());
        builder.append("\n");
        builder.append("Высота над уровнем моря: ");
        builder.append(ggaSentence.getAltitude());
        builder.append(", ");
        builder.append((ggaSentence.getAltitudeUnits()));
        builder.append("\n");
        try {
            String dgps = ggaSentence.getDgpsStationId();
            builder.append("Разница между эллипсоидом земли и уровнем моря в метрах: ");
            builder.append(dgps);
            builder.append("\n");
        } catch (DataNotAvailableException ignored) {
        }
        try {
            double dgpsAge = ggaSentence.getDgpsAge();
            builder.append("Количество секунд прошедших с получения последней DGPS поправки: ");
            builder.append(dgpsAge);
            builder.append("\n");
        } catch (DataNotAvailableException ignored) {
        }
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

    private static String getGSDDescription(GSASentence gsaSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Режим выбора формата 2D/3D: ");
        builder.append(gsaSentence.getMode());
        builder.append("\n");
        builder.append("Режим выбранного формата: ");
        builder.append(gsaSentence.getFixStatus());
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
        Time time;
        try {
            time = zdaSentence.getTime();
        }
        catch (DataNotAvailableException exception) {
            zdaSentence.setLocalZoneHours(0);
            zdaSentence.setLocalZoneMinutes(0);
            time = zdaSentence.getTime();
        }
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
        Time time = rmcSentence.getTime();
        Date date = rmcSentence.getDate();
        builder.append("Время UTC: ");
        builder.append(time.getHour());
        builder.append("ч:");
        builder.append(time.getMinutes());
        builder.append("м:");
        builder.append(Math.round(time.getSeconds()));
        builder.append("с");
        builder.append("\n");
        builder.append("Статус: ");
        DataStatus status = rmcSentence.getStatus();
        switch (status) {
            case ACTIVE:
                builder.append("Активный");
                break;
            case VOID:
                builder.append("Недействителен");
                break;
        }
        builder.append("\n");
        Position position = rmcSentence.getPosition();
        String latitude = position.getLatitudeHemisphere().toChar() + " " + position.getLatitude();
        String longitude = position.getLongitudeHemisphere().toChar() + " " + position.getLongitude();
        builder.append("Широта: ");
        builder.append(latitude);
        builder.append("\u00B0 ");
        builder.append("\n");
        //builder.append("Направление широты: ");
        //builder.append("\n");
        builder.append("Долгота: ");
        builder.append(longitude);
        builder.append("\u00B0 ");
        builder.append("\n");
        //builder.append("Направление долготы: ");
        //builder.append("\n");
        builder.append("Скорость над землей в узлах: ");
        builder.append(rmcSentence.getSpeed());
        builder.append("\n");
        builder.append("Курс, в градусах: ");
        builder.append(rmcSentence.getCourse());
        builder.append("\u00B0 ");
        builder.append("\n");
        //builder.append("Магнитное склонение: ");
        //builder.append("\n");
        //builder.append("Направление магнитного склонения: ");
        //builder.append("\n");
        //builder.append("Индикатор режима системы позиционирования: ");
        //builder.append("\n");
        builder.append("Режим работы: ");
        builder.append(rmcSentence.getMode().toString());
        builder.append("\n");
        builder.append("ID активного спутника: ");
        builder.append(rmcSentence.getTalkerId());
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
        //SatelliteInfo satelliteInfo = (SatelliteInfo) gsvSentence.getSatelliteInfo();
        builder.append("Количество выводимых сообщений: ");
        builder.append(gsvSentence.getSentenceCount());
        builder.append("\n");
        builder.append("Номер сообщения: ");
        builder.append(gsvSentence.getSentenceIndex());
        builder.append("\n");
        builder.append("Количество наблюдаемых спутников: ");
        builder.append(gsvSentence.getSatelliteCount());
        builder.append("\n");
        builder.append("Данные о спутниках: ");
        builder.append("\n");
        gsvSentence.getSatelliteInfo().forEach(x->{
            builder.append("\tID спутника: ").append(x.getId());
            builder.append("\n");
            builder.append("\tВысота: ").append(x.getElevation());
            builder.append("\n");
            builder.append("\tАзимут истинный: ").append(x.getAzimuth());
            builder.append("\n");
            builder.append("\tОтношение «сигнал — шум»: ").append(x.getNoise());
            builder.append("\n");
        });
        //builder.append(elevation);
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
                GGASentence sentence = (GGASentence) x.getSentences().get(0);
                Position position = null;
                try {
                    position = sentence.getPosition();
                } catch (DataNotAvailableException dataNotAvailableException){
                    Optional<Sentence> gllOpt = x.getSentences().stream().filter(y -> GLL_STR.equals(y.getSentenceId())).findFirst();
                    if (gllOpt.isPresent()){
                        GLLSentence gllSentence = (GLLSentence) gllOpt.get();
                        try {
                            position = gllSentence.getPosition();
                        }  catch (DataNotAvailableException dataNotAvailableExceptionInner){
                            System.out.println("Couldn't find position for parser.sentence: " + gllSentence.toSentence());
                        }
                    }
                }
                if (position != null){
                    InfoDTO infoDTO = new InfoDTO(position.getLatitude(), position.getLongitude(), LocalDateTime.now());
                    infoDTO.altitude = position.getAltitude();
                    ConvertedDTO convertedDTO = infoDTO.getConvertedDto();
                    try {
                        printer.printRecord(convertedDTO.getLatitudeD(), convertedDTO.getLongitudeD(), convertedDTO.getLatitudeM(), convertedDTO.getLongitudeM());
                    } catch (IOException e) {
                        System.out.println("Error occurred during writing line");
                    }
                }
            });
            return outputFile;
        } catch (IOException e) {
            System.out.println("Error occurred during output file creation");
            return null;
        }
    }

    @Nullable
    public static File createActualPositionCsv(List<InertialDTO> inertialDTOS) {
        File outputFile = new File(ACTUAl_POSITION_FILE_NAME);
        List<ConvertedDTO> convertedDTOS = inertialDTOS.stream().map(PacketParser::convertInertialToInfo).map(InfoDTO::getConvertedDto).collect(Collectors.toList());
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withHeader(POSITION_CSV_HEADER))) {
            convertedDTOS.forEach(x -> {
                try {
                    printer.printRecord(x.getLatitudeD(), x.getLongitudeD(), x.getLatitudeM(), x.getLongitudeM());
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
       return createDOPCsv(records, null);
    }

    @Nullable
    public static File createDOPCsv(List<Record> records, @Nullable String path){
        String finalPath = path == null ? DOP_FILE_NAME : path;
        File outputFile = new File(finalPath);
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withHeader(DOP_CSV_HEADER))){
            List<InfoDTO> dopList = getDopDTOList(records).stream().map(x->new InfoDTO(x.getHDOP(), x.getVDOP(), x.getPDOP(), x.getDateTime())).collect(Collectors.toList());
            TreeSet<InfoDTO> sortedSet = new TreeSet<>(Comparator.comparing(InfoDTO::getDateTime));
            sortedSet.addAll(dopList);
            sortedSet.forEach(x->{
                try {
                    printer.printRecord(x.getHDOP(), x.getVDOP(), x.getPDOP(), x.getDateTime().toInstant(OffsetDateTime.now().getOffset())
                            .toEpochMilli());
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

    public static List<InfoDTO> getDopDTOList(List<Record> records){
        List<InfoDTO> result = new ArrayList<>();
        for (Record x : records){
            List<Sentence> sentences = x.getSentences();
            Optional<Sentence> gsaSentenceOpt = sentences.stream().filter(sentence -> sentence.getSentenceId() != null && sentence.getSentenceId().equals(GSA_STR)).findFirst();
            Optional<Sentence> zdaSentenceOpt = sentences.stream().filter(sentence -> sentence.getSentenceId() != null && sentence.getSentenceId().equals(ZDA_STR)).findFirst();
            Optional<Sentence> ggaSentenceOpt = sentences.stream().filter(sentence -> sentence.getSentenceId() != null && sentence.getSentenceId().equals(GGA_STR)).findFirst();
            if (gsaSentenceOpt.isPresent() && zdaSentenceOpt.isPresent() && ggaSentenceOpt.isPresent()){
                GSASentence gsaSentence = (GSASentence) gsaSentenceOpt.get();
                ZDASentence zdaSentence = (ZDASentence) zdaSentenceOpt.get();
                GGASentence ggaSentence = (GGASentence) ggaSentenceOpt.get();
                double hDOP;
                double vDOP;
                double pDOP;
                try {
                    hDOP = gsaSentence.getHorizontalDOP();
                    vDOP = gsaSentence.getVerticalDOP();
                    pDOP = gsaSentence.getPositionDOP();
                } catch (DataNotAvailableException exception) {
                    continue;
                }
                result.add(new InfoDTO(hDOP, vDOP, pDOP, mapNmeaTimeToJavaTime(zdaSentence), ggaSentence.getPosition().getLongitude(), ggaSentence.getPosition().getLatitude(), ggaSentence.getAltitude(), ggaSentence.getSatelliteCount()));
            }
        }
        return result;
    }

    public static LocalDateTime mapNmeaTimeToJavaTime(Date date, Time time){
        if (time != null) {
            return LocalDateTime.of(date.getYear(), Month.of(date.getMonth()), date.getDay(), time.getHour(), time.getMinutes(), (int) time.getSeconds());
        } else {
            return LocalDateTime.of(date.getYear(), Month.of(date.getMonth()), date.getDay(), 0, 0, 0);
        }
    }

    public static LocalDateTime mapNmeaTimeToJavaTime(ZDASentence zdaSentence){
        Time time;
        try {
            time = zdaSentence.getTime();
        }
        catch (DataNotAvailableException exception) {
            zdaSentence.setLocalZoneHours(0);
            zdaSentence.setLocalZoneMinutes(0);
            time = zdaSentence.getTime();
        }
        return mapNmeaTimeToJavaTime(zdaSentence.getDate(), time);
    }

    public static List<InertialDTO> parseInertialExplorerFile(File inertialFile){
        List<InertialDTO> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inertialFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(parseInertialLine(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<RTKPostDTO> parseRTKPostFile(File rtkFile){
        List<RTKPostDTO> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rtkFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("%")){
                    result.add(parseRTKPostLine(line));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static InertialDTO parseInertialLine(String line){
        String[] strArr = line.split(WHITESPACE_SPLIT_PATTERN);
        if (strArr.length != 7 ){
            throw new UnsupportedLineException("Incorrect Inertial Explorer line");
        }
        LocalTime time = LocalTime.parse(strArr[0]);
        double latitude = Double.parseDouble(strArr[1]);
        double longtitude = Double.parseDouble(strArr[2]);
        double hEll = Double.parseDouble(strArr[3]);
        double sdHoriz = Double.parseDouble(strArr[4]);
        double sdHeight = Double.parseDouble(strArr[5]);
        return new InertialDTO(time, latitude, longtitude, hEll, sdHoriz, sdHeight);
    }

    public static RTKPostDTO parseRTKPostLine(String line){
        String[] strArr = line.split(WHITESPACE_SPLIT_PATTERN);
        if (strArr.length<23){
            throw new UnsupportedLineException("Incorrect RTKPOST line");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH);
        LocalDate date = LocalDate.parse(strArr[0], formatter);
        LocalTime time = LocalTime.parse(strArr[1]);
        double latitude = Double.parseDouble(strArr[2]);
        double longitude = Double.parseDouble(strArr[3]);
        double height = Double.parseDouble(strArr[4]);
        int satelliteCount = Integer.parseInt(strArr[5]);
        return new RTKPostDTO(date, time, latitude, longitude, height, satelliteCount);
    }

    public static File createDeltaFile(String path, List<? extends PositionWithTime> pwtList1, List<? extends PositionWithTime> pwtList2){
        if (pwtList1.isEmpty()){
            throw new IllegalStateException("First list cannot be empty");
        }
        if (pwtList2.isEmpty()){
            throw new IllegalStateException("Second list cannot be empty");
        }
        List<ConvertedDTO> deltaList = getDeltaListUnsynced(pwtList1, pwtList2);
        File outputFile = new File(path);
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withHeader(DELTA_CSV_HEADER))) {
            deltaList.forEach(x -> {
                try {
                    String count = x.satelliteCount == -1 ? "" : String.valueOf(x.satelliteCount);
                    printer.printRecord(BigDecimal.valueOf(x.getLatitudeD()).toPlainString(), BigDecimal.valueOf(x.getLongitudeD()).toPlainString(), x.getDateTime().toInstant(OffsetDateTime.now().getOffset())
                            .toEpochMilli(), BigDecimal.valueOf(x.getLatitudeM()).toPlainString(), BigDecimal.valueOf(x.getLongitudeM()).toPlainString(), count);
                } catch (IOException e) {
                    System.out.println("Error occurred during writing line");
                }
            });
        } catch (IOException e) {
            System.out.println("Error occurred during delta file creation");
            return null;
        }
        return new File("");
    }
    private static List<ConvertedDTO> getDeltaListUnsynced(List<? extends PositionWithTime> pwtList1, List<? extends PositionWithTime> pwtList2){
        List<ConvertedDTO> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < pwtList1.size() && j < pwtList2.size()){
            PositionWithTime pwt1 = pwtList1.get(i);
            PositionWithTime pwt2 = pwtList2.get(j);
            LocalTime time1 = pwt1.getTime();
            LocalTime time2 = pwt2.getTime();
            int comparisonResult = time1.compareTo(time2);
            if (comparisonResult == 0){
                result.add(getDeltaDto(pwt1, pwt2));
                j++;
                i++;
            }
            if (comparisonResult > 0){
                if (j == pwtList2.size() -1){
                    break;
                } else {
                    j++;
                    continue;
                }
            }
            if (comparisonResult < 0){
                if (i == pwtList1.size() -1){
                    break;
                } else {
                    i++;
                }
            }
        }
        return result;
    }

    private static ConvertedDTO getDeltaDto(InfoDTO infoDTO, InertialDTO inertialDTO){
        ConvertedDTO track = convertInertialToInfo(inertialDTO).getConvertedDto();
        ConvertedDTO info = infoDTO.getConvertedDto();
        double latitudeDeltaD = Math.abs(info.latitudeD-track.latitudeD);
        double longitudeDeltaD = Math.abs(info.longitudeD-track.longitudeD);
        double altitudeDeltaD = Math.abs(info.altitudeD-track.altitudeD);
        double latitudeDeltaM = Math.abs(info.latitudeM-track.latitudeM);
        double longitudeDeltaM = Math.abs(info.longitudeM-track.longitudeM);
        double altitudeDeltaM = Math.abs(info.altitudeM-track.altitudeM);
        return new ConvertedDTO(latitudeDeltaD, longitudeDeltaD, altitudeDeltaD, latitudeDeltaM, longitudeDeltaM, altitudeDeltaM, infoDTO.time);
    }

    private static ConvertedDTO getDeltaDto(PositionWithTime pwt1, PositionWithTime pwt2){
        LocalDateTime time;
        if (pwt1.getDateTime() == null && pwt2.getDateTime() == null){
            throw new RuntimeException("Cannot find date in objects");
        }
        time = pwt1.getDateTime() == null ? pwt2.getDateTime() : pwt1.getDateTime();
        if (time == null){
            throw new RuntimeException("Cannot find date in objects");
        }
        ConvertedDTO first = getConvertedDTO(pwt1);
        ConvertedDTO second = getConvertedDTO(pwt2);
        double latitudeDeltaD = Math.abs(first.latitudeD-second.latitudeD);
        double longitudeDeltaD = Math.abs(first.longitudeD-second.longitudeD);
        double altitudeDeltaD = Math.abs(first.altitudeD-second.altitudeD);
        double latitudeDeltaM = Math.abs(first.latitudeM-second.latitudeM);
        double longitudeDeltaM = Math.abs(first.longitudeM-second.longitudeM);
        double altitudeDeltaM = Math.abs(first.altitudeM-second.altitudeM);
        int satCount = -1;
        if (pwt1 instanceof RTKPostDTO){
            satCount = ((RTKPostDTO) pwt1).q;
        } else if (pwt2 instanceof RTKPostDTO){
            satCount = ((RTKPostDTO) pwt2).q;
        }
        return new ConvertedDTO(latitudeDeltaD, longitudeDeltaD, altitudeDeltaD, latitudeDeltaM, longitudeDeltaM, altitudeDeltaM, time, satCount);
    }

    private static InfoDTO convertInertialToInfo(InertialDTO inertialDTO){
        InfoDTO infoDTO = new InfoDTO(inertialDTO.latitude, inertialDTO.longitude, LocalDateTime.now());
        infoDTO.altitude = inertialDTO.hEll;
        return infoDTO;
    }

    public static ConvertedDTO getConvertedDTO(PositionWithTime positionWithTime){
        double latitude = positionWithTime.getLatitude();
        double longitude = positionWithTime.getLongitude();
        double altitude = positionWithTime.getAltitude();
        LocalTime time = positionWithTime.getTime();
        double n = WGS84az/Math.sqrt(1 - Math.pow(WGS84e1,2)*Math.pow(Math.sin(latitude),2));
        double x = (n+altitude)*Math.cos(latitude)*Math.cos(longitude);
        double y = (n+altitude)*Math.cos(latitude)*Math.sin(longitude);
        double z = (n*(1-Math.pow(WGS84e1,2))+altitude)*Math.sin(latitude);
        return new ConvertedDTO(longitude, latitude, altitude, x,y,z,time);
    }
}
