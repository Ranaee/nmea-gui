package parser;

import net.sf.marineapi.nmea.sentence.Sentence;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        File testText = new File("test_all_nmea.TXT");
        List<Record> records = PacketParser.parse(testText);
        List<Sentence> vtg = records.stream().flatMap(x->x.getFields().stream()).filter(x->x.getSentenceId().equals("VTG")).collect(Collectors.toList());/*.map(PacketParser::toString).collect(Collectors.joining("\n"));*/
        boolean t = true;
    }
}
