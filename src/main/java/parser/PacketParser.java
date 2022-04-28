package parser;

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
import sentence.UnknownParser;
import sentence.UnknownSentence;

import java.io.*;
import java.util.*;

import static net.sf.marineapi.nmea.sentence.SentenceId.RMC;

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

    private static class DopDTO {
        private final double hDOP;
        private final double vDOP;
        private final double pDOP;
        private final long time;

        public DopDTO(double hDOP, double vDOP, double pDOP, long time) {
            this.hDOP = hDOP;
            this.vDOP = vDOP;
            this.pDOP = pDOP;
            this.time = time;
        }

        public double gethDOP() {
            return hDOP;
        }

        public double getvDOP() {
            return vDOP;
        }

        public double getpDOP() {
            return pDOP;
        }

        public long getTime() {
            return time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DopDTO dopDTO = (DopDTO) o;
            return Double.compare(dopDTO.hDOP, hDOP) == 0 && Double.compare(dopDTO.vDOP, vDOP) == 0 && Double.compare(dopDTO.pDOP, pDOP) == 0 && time == dopDTO.time;
        }

        @Override
        public int hashCode() {
            return Objects.hash(hDOP, vDOP, pDOP, time);
        }
    }

    public static String getSentenceLegend(Sentence sentence) {
        if (sentence instanceof UnknownSentence) {
            return UNKNOWN_SENTENCE_TYPE;
        }
        String sentenceId = sentence.getSentenceId();
        switch (sentenceId) {
            case GGA_STR:
                return getGGALegend((GGASentence) sentence);
            case GLL_STR:
                return getGLLegend((GLLSentence) sentence);
            case GSA_STR:
                return getGSALegend((GSASentence) sentence);
            //case ZDA_STR:
            //    return getZDALegend((ZDASentence) sentence);
            //case RMC_STR:
             //   return getRMCLegend((RMCSentence) sentence);
            //case GSV_STR:
            //    return getGSVLegend((GSVSentence) sentence);
            case VTG_STR:
                return getVTGLegend((VTGSentence) sentence);
            default:
                return UNKNOWN_SENTENCE_TYPE;
        }
    }


    private static String getVTGLegend(VTGSentence vtgSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Курс на истинный полюс в градусах - x.x" +
                "\nФлаг достоверности курса - 'T'-True(Достоверный) / 'F'-False(Недостоверный)" +
                "\nМагнитное склонение в градусах (может не использоваться) - x.x" +
                "\nОтносительно северного магнитного полюса (может не использоваться) - М(Магнитный)" +
                "\nСкорость - s.ss" +
                "\nЕдиница измерения скорости, узлы - N" +
                "\nСкорость - s.ss" +
                "\nЕдиница измерения скорости, км/ч - К" +
                "\nСпособ вычисления скорости и курса: 'A' - автономный." +
                "\n'D' - дифференциальный;\n" +
                "'E' - аппроксимация;\n" +
                "'M' - фиксированные данные;\n" +
                "'N' - недостоверные данные;" +
                "\nКонтрольная сумма строки - *hh");
        builder.append("\n");
        return builder.toString();
    }

    private static String getGSALegend(GSASentence gsaSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Режим выбора формата 2D/3D: 'M' — ручной, принудительно включен 2D или 3D режим;\n" +
                "A — автоматический, разрешено автоматически выбирать 2D или 3D режим.\n" +
                "Режим выбранного формата: '1'- Местоположение не определено / '2'- 2D / '3'- 3D\n" +
                "ID активного спутника (максимум 12 спутников) - xx\n" +
                "Пространственный геометрический фактор ухудшения точности (PDOP) - x.x\n" +
                "Горизонтальный геометрический фактор ухудшения точности (HDOP) - x.x\n" +
                "Вертикальный геометрический фактор ухудшения точности (VDOP) - x.x\n" +
                "Номер навигационной системы (1-GPS \"GP\", 2-Glonass \"GL\",, 3-Galileo \"GA\", 4-Beidu \"BD\"; \"GN\" - источник данных GPS+Glonass)\n" +
                "Контрольная сумма строки - *hh");
        builder.append("\n");
        return builder.toString();
    }

    private static String getGLLegend(GLLSentence gllSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Широта - xxxx.xx\n" +
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
                "Контрольная сумма строки - *hh");
        builder.append("\n");
        return builder.toString();
    }

    private static String getGGALegend(GGASentence ggaSentence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Время UTC - hhmmss.ss\n" +
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
                "Контрольная сумма строки - *hh");
        builder.append("\n");
        return builder.toString();
    }



    //TODO Нужно разобраться оставить ли этот метод, он нигде не используется и ошибочный
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
        boolean correctRecords = records.stream().allMatch(x -> x.getSentences().get(0).getSentenceId().equals(RMC.toString()));
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

    private static String getGSADesription(GSASentence gsaSentence) {
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
        builder.append("\n")
        gsvSentence.getSatelliteInfo().stream().forEach(x->{
            builder.append("\tID спутника: " + x.getId());
            builder.append("\n");
            builder.append("\tВысота: " + x.getElevation());
            builder.append("\n");
            builder.append("\tАзимут истинный: " + x.getAzimuth());
            builder.append("\n");
            builder.append("\tОтношение «сигнал — шум»: " + x.getNoise());
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
                RMCSentence sentence = (RMCSentence) x.getSentences().get(0);
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
            HashSet<DopDTO> dopSet = new HashSet<>();
            for (Record x : records){
                List<Sentence> sentences = x.getSentences();
                Optional<Sentence> gsaSentenceOpt = sentences.stream().filter(sentence->sentence.getSentenceId().equals(GSA_STR)).findFirst();
                Optional<Sentence> zdaSentenceOpt = sentences.stream().filter(sentence -> sentence.getSentenceId().equals(ZDA_STR)).findFirst();
                if (gsaSentenceOpt.isPresent() && zdaSentenceOpt.isPresent()){
                    GSASentence gsaSentence = (GSASentence) gsaSentenceOpt.get();
                    ZDASentence zdaSentence = (ZDASentence) zdaSentenceOpt.get();
                    dopSet.add(new DopDTO(gsaSentence.getHorizontalDOP(), gsaSentence.getVerticalDOP(), gsaSentence.getPositionDOP(), zdaSentence.getDate().toDate().getTime()));
                }
            }
            dopSet.forEach(x->{
                try {
                    printer.printRecord(x.gethDOP(), x.getvDOP(), x.getpDOP(), x.getTime());
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
