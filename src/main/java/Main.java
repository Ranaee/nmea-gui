import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import parser.PacketParser;
import parser.Record;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Main extends Application {

    private static final int WIDTH = 1341;
    private static final int HEIGHT = 820;

    @Override
    public void start(Stage primaryStage) throws Exception {
        String schemaName = "scheme.fxml";
        URL schemeUrl = getClass().getClassLoader().getResource(schemaName);
        if (schemeUrl == null) {
            throw new IllegalStateException("Schema " + schemaName + " not found");
        }
        Parent root = FXMLLoader.load(schemeUrl);
        primaryStage.setTitle("NMEA Reader");
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        primaryStage.show();
    }


    public static void main(String[] args) {
//        launch(args);
        parseConsole();
    }

    /**
     * Use only for debug!!!
     */
    private static void parseConsole() {
        File testText = new File("test_all_nmea.txt");
        List<Record> records;
        try {
            records = PacketParser.parse(testText);
            PacketParser.createDOPCsv(records);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean b = true;
    }
}
