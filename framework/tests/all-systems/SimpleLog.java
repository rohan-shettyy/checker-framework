@SuppressWarnings("ainfertest") // only check WPI for crashes
public class SimpleLog {
    public SimpleLog() {
        try {
            int i = 0;
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }
}
