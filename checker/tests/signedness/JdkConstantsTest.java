import org.checkerframework.checker.signedness.qual.PolySigned;

public class JdkConstantsTest {

    static @PolySigned int integerMinValue(@PolySigned int value) {
        // :: error: (return.type.incompatible)
        return Integer.MIN_VALUE;
    }

    static @PolySigned int flip(@PolySigned int value) {
        // :: error: (return.type.incompatible)
        return value ^ Integer.MIN_VALUE;
    }
}
