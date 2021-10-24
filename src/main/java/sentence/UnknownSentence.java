package sentence;

import net.sf.marineapi.nmea.sentence.Sentence;

public interface UnknownSentence extends Sentence {

    @Override
    default String toSentence() {
        return "Неизвестная строка";
    }

}
