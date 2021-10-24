package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.ZDASentence;
import parser.PacketParser;
import parser.Record;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Controller {

    @FXML
    private TextField nmeaPath;

    @FXML
    private ListView<Record> recordView;

    @FXML
    private ListView<Sentence> recordItems;

    @FXML
    private DatePicker fromPicker;

    @FXML
    private DatePicker toPicker;

    private List<Record> sourceRecords;

    @FXML
    private void parseAll(ActionEvent event) {
        String path = nmeaPath.getText();
        //TODO заменить на ошибку вида "не вввели путь"
        if ("".equals(path) || path == null) {
            path = "C:\\Users\\user\\IdeaProjects\\nmea-gui\\test_all_nmea.txt";
        }
        File file = new File(path);
        ObservableList<Record> records = FXCollections.observableArrayList();
        sourceRecords = new ArrayList<>();
        sourceRecords.addAll(PacketParser.parse(file));
        records.addAll(sourceRecords);
        recordView.setItems(records);
    }

    @FXML
    private void parseFromInterval(ActionEvent event) {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        List<Record> filtered = sourceRecords.stream().filter(record->{
            Optional<Sentence> optional = record.getFields().stream().filter(x->"ZDA".equals(x.getSentenceId())).findFirst();
            if (optional.isEmpty()) {
                return false;
            }
            Sentence sentence = optional.get();
            ZDASentence zda = (ZDASentence) sentence;
            LocalDate date = zda.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (fromDate.compareTo(date)<=0 && toDate.compareTo(date)>=0){
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        ObservableList<Record> records = FXCollections.observableArrayList();
        records.addAll(filtered);
        recordView.setItems(records);
    }

    @FXML
    public void selectRecord(MouseEvent mouseEvent) {
        Record currentRecord = recordView.getSelectionModel().getSelectedItem();
        List<Sentence> currentSentence = currentRecord.getFields();
        ObservableList<Sentence> sentences = FXCollections.observableArrayList();
        sentences.addAll(currentSentence);
        recordItems.setItems(sentences);
    }
}