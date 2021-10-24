package sentence;

import net.sf.marineapi.nmea.sentence.TalkerId;

public class UnknownParser implements UnknownSentence {

    public String source;

    public char getBeginChar() {
        return 0;
    }

    public int getFieldCount() {
        return 0;
    }

    public String getSentenceId() {
        return null;
    }

    public TalkerId getTalkerId() {
        return null;
    }

    public boolean isAISSentence() {
        return false;
    }

    public boolean isProprietary() {
        return false;
    }

    public boolean isValid() {
        return false;
    }

    public void reset() {

    }

    public void setBeginChar(char c) {

    }

    public void setTalkerId(TalkerId talkerId) {

    }

    public String toSentence(int i) {
        return null;
    }

    @Override
    public String toString() {
        return source;
    }

    public UnknownParser(String source) {
        this.source = source;
    }
}
