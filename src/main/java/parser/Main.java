package parser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        File testText = new File("test_all_nmea.TXT");
        List<Record> records = PacketParser.parse(testText);
        boolean t = true;
    }
}
