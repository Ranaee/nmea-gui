import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.sf.marineapi.nmea.sentence.Sentence;
import parser.PacketParser;
import parser.Record;

import java.io.File;
import java.net.URL;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        String schemaName = "scheme.fxml";
        URL schemeUrl = getClass().getClassLoader().getResource(schemaName);
        if (schemeUrl == null){
            throw new IllegalStateException("Schema " + schemaName + " not found");
        }
        Parent root = FXMLLoader.load(schemeUrl);
        primaryStage.setTitle("NMEA Reader");
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
//        parseConsole();
    }

    private static void parseConsole(){
        File testText = new File("small.TXT");
        List<Record> records = PacketParser.parse(testText);
        Sentence sn = records.get(0).getFields().get(0);
        String string = PacketParser.getSentenceDescription(sn);
        boolean t = true;
     }
}
