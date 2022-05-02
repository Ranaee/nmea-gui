package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private StackPane stackPane;

    @FXML
    private AnchorPane parsingPane;

    @FXML
    private AnchorPane ellipsePane;

    @FXML
    private AnchorPane geofactorsPane;

    @FXML
    public Button parsingPaneBtn;

    @FXML
    public Button geofactorsPlotBtn;

    @FXML
    public Button ellipsePlotBtn;

    private final List<Record> sourceRecords = new ArrayList<>();

    private static final String INFO_FILE_NAME = "./info.csv";

    private static final String[] INFO_CSV_HEADER = {"longitude","latitude","altitude", "time", "hdop", "vdop", "pdop"};

    @FXML
    private void parseAll(ActionEvent event) {
        if (sourceRecords.isEmpty()){
            String path = nmeaPath.getText();
            if ("".equals(path) || path == null) {
                nmeaPath.setText("Не выбран файл!");
                return;
            }
            File file = new File(path);
            sourceRecords.addAll(PacketParser.parse(file));
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

    public void changeToParsingPane(ActionEvent actionEvent) {
        parsingPaneBtn.setDefaultButton(true);
        ellipsePlotBtn.setDefaultButton(false);
        geofactorsPlotBtn.setDefaultButton(false);
        ObservableList<Node> children = stackPane.getChildren();
        int i = children.indexOf(parsingPane);
        if (i == -1){
            children.add(parsingPane);
        }
        children.remove(ellipsePane);
        children.remove(geofactorsPane);
    }

    public void changeToGeofactorsPlotPane(ActionEvent actionEvent) {
        parsingPaneBtn.setDefaultButton(false);
        ellipsePlotBtn.setDefaultButton(false);
        geofactorsPlotBtn.setDefaultButton(true);
        ObservableList<Node> children = stackPane.getChildren();
        int i = children.indexOf(geofactorsPane);
        if (i == -1){
            children.add(geofactorsPane);
        }
        children.remove(parsingPane);
        children.remove(ellipsePane);
    }

    public void changeToEllipsePlotPane(ActionEvent actionEvent) {
        parsingPaneBtn.setDefaultButton(false);
        ellipsePlotBtn.setDefaultButton(true);
        geofactorsPlotBtn.setDefaultButton(false);
        ObservableList<Node> children = stackPane.getChildren();
        int i = children.indexOf(ellipsePane);
        if (i == -1){
            children.add(ellipsePane);
        }
        children.remove(geofactorsPane);
        children.remove(parsingPane);
    }
}
