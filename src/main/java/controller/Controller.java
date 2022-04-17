package controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.ZDASentence;
import parser.PacketParser;
import parser.Record;

import java.io.File;
import java.time.LocalDate;
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

    private final List<Record> sourceRecords = new ArrayList<>();

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
            Optional<Sentence> optional = record.getFields().stream().filter(x -> "ZDA".equals(x.getSentenceId())).findFirst();
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
        sentenceView.setItems(FXCollections.observableList(currentRecord.getFields()));
    }

    @FXML
    public void getSentenceDescription(MouseEvent mouseEvent){
        Sentence currentSentence = sentenceView.getSelectionModel().getSelectedItem();
        String description = PacketParser.getSentenceDescription(currentSentence);
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
}
