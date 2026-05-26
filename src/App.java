import database.DBConnection;
import javafx.application.Application;
import javafx.stage.Stage;
import ui.LoginScreen;

/**
 * App — JavaFX entry point.
 *
 * Initialises the database schema on startup, then shows the login screen.
 *
 * Run the SERVER first:
 *   java -cp ... server.ChatServer
 *
 * Then run the CLIENT (this class) on each machine:
 *   java -cp ... App
 *
 * For deployed server, set env var:
 *   SERVER_HOST=your-railway-url.railway.app
 *   SERVER_PORT=25565
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialise DB schema (creates tables if they don't exist)
        DBConnection.initSchema();

        // Show login screen
        new LoginScreen().show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
