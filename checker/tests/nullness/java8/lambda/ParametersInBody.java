// Test that parameter annotations are correct in the body of a lambda

import org.checkerframework.checker.nullness.qual.*;

interface ConsumerLPB {
    void method(@Nullable String s);
}

interface NNConsumerLPB {
    void method(@NonNull String s);
}

class LambdaParamBody {

    // :: error: (lambda.param.type.incompatible)
    ConsumerLPB fn0 = (String i) -> i.toString();
    ConsumerLPB fn2 =
            (@Nullable String i) -> {
                // :: error: (dereference.of.nullable)
                i.toString();
            };
    ConsumerLPB fn3 =
            // :: error: (lambda.param.type.incompatible)
            (String i) -> {
                i.toString();
            };
    ConsumerLPB fn3b =
            (i) -> {
                // :: error: (dereference.of.nullable)
                i.toString();
            };

    NNConsumerLPB fn4 =
            (String i) -> {
                i.toString();
            };
    NNConsumerLPB fn4b =
            (i) -> {
                i.toString();
            };
    NNConsumerLPB fn5 =
            (@Nullable String i) -> {
                // :: error: (dereference.of.nullable)
                i.toString();
            };
    NNConsumerLPB fn6 =
            (@NonNull String i) -> {
                i.toString();
            };
}
