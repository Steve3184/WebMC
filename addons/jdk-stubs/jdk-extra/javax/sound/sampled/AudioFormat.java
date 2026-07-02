package javax.sound.sampled;

public class AudioFormat {
    private final Encoding encoding;
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final int frameSize;
    private final float frameRate;
    private final boolean bigEndian;

    public AudioFormat(Encoding encoding, float sampleRate, int sampleSizeInBits, int channels, int frameSize, float frameRate, boolean bigEndian) {
        this.encoding = encoding; this.sampleRate = sampleRate; this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels; this.frameSize = frameSize; this.frameRate = frameRate; this.bigEndian = bigEndian;
    }
    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this(signed ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED, sampleRate, sampleSizeInBits, channels,
             (sampleSizeInBits + 7) / 8 * channels, sampleRate, bigEndian);
    }
    public Encoding getEncoding() { return encoding; }
    public float getSampleRate() { return sampleRate; }
    public int getSampleSizeInBits() { return sampleSizeInBits; }
    public int getChannels() { return channels; }
    public int getFrameSize() { return frameSize; }
    public float getFrameRate() { return frameRate; }
    public boolean isBigEndian() { return bigEndian; }

    public static class Encoding {
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");
        public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");
        public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");
        public static final Encoding ULAW = new Encoding("ULAW");
        public static final Encoding ALAW = new Encoding("ALAW");
        private final String name;
        public Encoding(String name) { this.name = name; }
        @Override public final boolean equals(Object obj) {
            return obj instanceof Encoding && ((Encoding) obj).name.equals(name);
        }
        @Override public final int hashCode() { return name.hashCode(); }
        @Override public final String toString() { return name; }
    }
}
