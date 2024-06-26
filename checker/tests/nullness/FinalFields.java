import org.checkerframework.checker.nullness.qual.Nullable;

class Upper {
    @Nullable String fs = "NonNull init";
    private final @Nullable String ffs = "NonNull init";

    void access() {
        // Error, because non-final field type is not refined
        // :: error: (dereference.of.nullable)
        fs.hashCode();
        // private final field is refined
        ffs.hashCode();
    }
}

public class FinalFields {
    public void foo(Upper u) {
        // Error, because final field in different class is not refined
        // :: error: (dereference.of.nullable)
        u.fs.hashCode();
    }

    public void bar(Lower l) {
        // Error, because final field in different class is not refined
        // :: error: (dereference.of.nullable)
        l.fs.hashCode();
    }

    public void local() {
        @Nullable String ls = "Locals";
        // Local variable is refined
        ls.hashCode();
    }
}

class Lower {
    @Nullable String fs = "NonNull init, too";
    private final @Nullable String ffs = "NonNull init, too";

    void access() {
        // Error, because non-final field type is not refined
        // :: error: (dereference.of.nullable)
        fs.hashCode();
        // private final field is refined
        ffs.hashCode();
    }
}
