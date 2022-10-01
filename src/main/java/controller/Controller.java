package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.ZDASentence;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import parser.PacketParser;
import parser.data.Record;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static parser.PacketParser.*;

public class Controller {

    @FXML
    private TextField nmeaPath;

    @FXML
    private ListView<Record> recordView;

    @FXML
    private ListView<Sentence> sentenceView;

    @FXML
    private DatePicker fromPicker;

    @FXML
    private DatePicker toPicker;

    @FXML
    private TextArea recordDescription;

    @FXML
    private TextArea legendText;

    @FXML
    private WebView geofactorsWebView;

    @FXML
    private WebView coordinatesWebView;

    @FXML
    private WebView deltasWebView;

    private final List<Record> sourceRecords = new ArrayList<>();

    private static final String INFO_FILE_NAME = OUTPUT_PREFIX + "info.csv";
    private static final String DELTA_FILE_NAME = OUTPUT_PREFIX + "delta.csv";

    private static final String[] INFO_CSV_HEADER = {"time", "longitude","latitude","altitude",  "hdop", "vdop", "pdop", "satellite_count"};

    private static final String HDOP_HTML = "dop-graph.html";
    private static final String POS_HTML = "coordinates.html";
    private static final String DELTA_HTML = "delta-graph.html";



    @FXML
    private void initialize(){
        getDeltaDeltaFile();
        WebEngine factorsEngine = geofactorsWebView.getEngine();
        factorsEngine.setJavaScriptEnabled(true);
        URL url = getClass().getClassLoader().getResource(HDOP_HTML);
        if (url != null){
            factorsEngine.load(url.toString());
        } else {
            throw new IllegalStateException("Resource not found: dop-graph.html");
        }
        WebEngine coordinatesEngine = coordinatesWebView.getEngine();
        coordinatesEngine.setJavaScriptEnabled(true);
        url = getClass().getClassLoader().getResource(POS_HTML);
        if (url != null){
            coordinatesEngine.load(url.toString());
        } else {
            throw new IllegalStateException("Resource not found: coordinates.html");
        }

        WebEngine deltasEngine = deltasWebView.getEngine();
        deltasEngine.setJavaScriptEnabled(true);
        url = getClass().getClassLoader().getResource(DELTA_HTML);
        if (url != null){
            deltasEngine.load(url.toString());
        } else {
            throw new IllegalStateException("Resource not found: " +DELTA_HTML);
        }
    }

    @FXML
    private void parseAll() {
        if (sourceRecords.isEmpty()){
            String path = nmeaPath.getText();
            if ("".equals(path) || path == null) {
                nmeaPath.setText("Не выбран файл!");
                return;
            }
            File file = new File(path);
            try {
                sourceRecords.addAll(PacketParser.parse(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File outputFolder = new File(OUTPUT_PREFIX);
        if (!outputFolder.exists()){
            boolean isCreated = outputFolder.mkdir();
            if (!isCreated){
                System.out.println("Error during output folder creation");
            }
        }
        PacketParser.createPositionCsv(sourceRecords);
        PacketParser.createDOPCsv(sourceRecords);
        File trackFile = new File("./input/track.txt");
        if (trackFile.exists()){
            List<PacketParser.InertialDTO> inertialDTOS = PacketParser.parseInertialExplorerFile(trackFile);
            List<PacketParser.InfoDTO> infoDTOS = PacketParser.getDopDTOList(sourceRecords);
            createActualPositionCsv(inertialDTOS);
            createDeltaFile(DELTA_FILE_NAME, infoDTOS, inertialDTOS);
        }
        recordView.setItems(FXCollections.observableList(sourceRecords));
    }

    private void getDeltaDeltaFile(){
        File trackFile = new File("./input/track.txt");
        if (!trackFile.exists()){
            return;
        }
        File comparedTrackFolder = new File("./input/compared/");
        if (!comparedTrackFolder.exists()){
            return;
        }
        List<File> comparedFiles = null;
        try (Stream<Path> paths = Files.list(comparedTrackFolder.toPath())){
            comparedFiles = paths.filter(x->!Files.isDirectory(x))
                .map(Path::toFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (comparedFiles == null){
            System.out.println("Error during track file collection");
            return;
        }
        if (comparedFiles.isEmpty()){
            return;
        }
        List<PacketParser.InertialDTO> inertialDTOS = PacketParser.parseInertialExplorerFile(trackFile);
        comparedFiles.forEach(
                file -> {
                    String fileName = file.getName();
                    String finalPath = OUTPUT_PREFIX + "compared/" +fileName;
                    List<RTKPostDTO> currentPwtList = parseRTKPostFile(file);
                    createDeltaFile(finalPath, inertialDTOS, currentPwtList);
                }
        );
    }

    @FXML
    private void parseFromInterval() {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        List<Record> filtered = sourceRecords.stream().filter(record -> {
            Optional<Sentence> optional = record.getSentences().stream().filter(x -> "ZDA".equals(x.getSentenceId())).findFirst();
            if (!optional.isPresent()) {
                return false;
            }
            Sentence sentence = optional.get();
            ZDASentence zda = (ZDASentence) sentence;
            LocalDate date = zda.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return fromDate.compareTo(date) <= 0 && toDate.compareTo(date) >= 0;
        }).collect(Collectors.toList());
        recordView.setItems(FXCollections.observableList(filtered));
    }

    @FXML
    public void selectRecord() {
        Record currentRecord = recordView.getSelectionModel().getSelectedItem();
        sentenceView.setItems(FXCollections.observableList(currentRecord.getSentences()));
    }

    @FXML
    public void getSentenceDescription(){
        Sentence currentSentence = sentenceView.getSelectionModel().getSelectedItem();
        String description = PacketParser.getSentenceDescription(currentSentence);
        String legend = PacketParser.getSentenceLegend(currentSentence);
        legendText.setText(legend);
        recordDescription.setText(description);

    }

    @FXML
    public void pickFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files", "*.txt"));
        File f = fileChooser.showOpenDialog(null);

        if (f != null){
            nmeaPath.setText(f.getAbsolutePath());
        }
    }

    @FXML
    public void createOutputFile(){
        if (sourceRecords.isEmpty()){
            System.out.println("Данные отсутствуют!!!");
            return;
        }
        List<PacketParser.InfoDTO> sources = PacketParser.getDopDTOList(sourceRecords);
        File outputFile = new File(INFO_FILE_NAME);
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withDelimiter(' ').withHeader(INFO_CSV_HEADER))){
            sources.forEach(x->{
                try {
                    printer.printRecord(x.getDateTime(), x.getLongitude(), x.getLatitude(), x.getAltitude(),  x.getHDOP(), x.getVDOP(), x.getPDOP(), x.getSatelliteCount());
                } catch (IOException e) {
                    System.out.println("Error occurred during writing line");
                }
            });
        } catch (IOException e) {
            System.out.println("Error occurred during output file creation");
        }
    }
}
