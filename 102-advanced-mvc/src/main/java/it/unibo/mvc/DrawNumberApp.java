package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import static it.unibo.mvc.Configuration.Builder;
/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String PATH = "config.yml";
    private static final int ELEMS_PER_LINE_CONFIG_FILE = 2;

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        this.model = this.modelBuilder(PATH);
    }

    private DrawNumber modelBuilder(final String path) {
        Configuration model;
        final Builder builder = new Builder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(path)))) {
            String line;
            while (Objects.nonNull(line = reader.readLine())){
                StringTokenizer tokenizer = new StringTokenizer(line, ": ");
                if (tokenizer.countTokens() == ELEMS_PER_LINE_CONFIG_FILE) {
                    switch (tokenizer.nextToken()) {
                        case "minimum":
                            builder.setMin(Integer.parseInt(tokenizer.nextToken()));
                            break;
                        case "maximum":
                            builder.setMax(Integer.parseInt(tokenizer.nextToken()));
                            break;
                        case "attempts":
                            builder.setAttempts(Integer.parseInt(tokenizer.nextToken()));
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e1){
            for (final DrawNumberView view: views) {
                view.displayError("File not found");
            }
        } catch (NumberFormatException e2) {
            for (final DrawNumberView view: views) {
                view.displayError("File format not ok");
            }
        } finally {
            model = builder.build();
        }
        if (!model.isConsistent()) {
            model = new Builder().build();
        }
        return new DrawNumberImpl(model.getMin(), model.getMax(), model.getAttempts());
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.displayError(e.getMessage());
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        System.out.println(System.getProperty("user.dir"));
        new DrawNumberApp(
            new DrawNumberViewImpl(),
            new DrawNumberViewImpl(),
            new PrintStreamView(System.out),
            new PrintStreamView("file.txt")
        );
    }

}
