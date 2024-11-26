module tzuchi.scaningroomsystem {
    requires javafx.controls;
    requires javafx.media;
    requires java.desktop;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    // Add these if you need them
    requires transitive javafx.graphics;
    requires transitive javafx.base;

    exports com.tzuchi.scaningroomsystem;
}