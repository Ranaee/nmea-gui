package controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.ZDASentence;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import parser.PacketParser;
import parser.Record;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Controller {


    @FXML
    public Tab ellipseTab;

    @FXML
    public Tab geoTab;

    @FXML
    public Tab parsingTab;

    @FXML
    public TabPane tabPane;

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
    private AnchorPane parsingPane;

    @FXML
    private AnchorPane ellipsePane;

    @FXML
    private AnchorPane geofactorsPane;

    @FXML
    private WebView geofactorsWebView;

    private final List<Record> sourceRecords = new ArrayList<>();

    private static final String INFO_FILE_NAME = "./info.csv";

    private static final String[] INFO_CSV_HEADER = {"longitude","latitude","altitude", "time", "hdop", "vdop", "pdop"};

    private static final String HDOP_HTML = "index.html";

    @FXML
    private void initialize(){
        WebEngine engine = geofactorsWebView.getEngine();
        URL url = getClass().getClassLoader().getResource(HDOP_HTML);
        if (url != null){
            engine.load(url.toString());
        } else {
            throw new IllegalStateException("Resource not found: index.html");
        }
    }

    @FXML
    private void parseAll(ActionEvent event) {
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
        recordView.setItems(FXCollections.observableList(sourceRecords));
    }

    @FXML
    private void parseFromInterval(ActionEvent event) {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        List<Record> filtered = sourceRecords.stream().filter(record -> {
            Optional<Sentence> optional = record.getSentences().stream().filter(x -> "ZDA".equals(x.getSentenceId())).findFirst();
            if (optional.isEmpty()) {
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
    public void selectRecord(MouseEvent mouseEvent) {
        Record currentRecord = recordView.getSelectionModel().getSelectedItem();
        sentenceView.setItems(FXCollections.observableList(currentRecord.getSentences()));
    }

    @FXML
    public void getSentenceDescription(MouseEvent mouseEvent){
        Sentence currentSentence = sentenceView.getSelectionModel().getSelectedItem();
        String description = PacketParser.getSentenceDescription(currentSentence);
        String legend = PacketParser.getSentenceLegend(currentSentence);
        legendText.setText(legend);
        recordDescription.setText(description);

    }

    @FXML
    public void pickFile(ActionEvent actionEvent){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files", "*.txt"));
        File f = fileChooser.showOpenDialog(null);

        if (f != null){
            nmeaPath.setText(f.getAbsolutePath());
        }
    }

    @FXML
    public void createOutputFile(ActionEvent actionEvent){
        if (sourceRecords.isEmpty()){
            System.out.println("Данные отсутствуют!!!");
            return;
        }
        List<PacketParser.DopDTO> sources = PacketParser.getDopDTOList(sourceRecords);
        File outputFile = new File(INFO_FILE_NAME);
        try (FileWriter output = new FileWriter(outputFile); CSVPrinter printer = new CSVPrinter(output, CSVFormat.DEFAULT.withHeader(INFO_CSV_HEADER))){
            sources.forEach(x->{
                try {
                    printer.printRecord(x.getLongitude(), x.getLatitude(), x.getAltitude(), x.getTime().toInstant(OffsetDateTime.now().getOffset())
                            .toEpochMilli(), x.gethDOP(), x.getvDOP(), x.getpDOP());
                } catch (IOException e) {
                    System.out.println("Error occurred during writing line");
                }
            });
        } catch (IOException e) {
            System.out.println("Error occurred during output file creation");
        }
    }
}
