package com.example.vaadinupload;

import org.springframework.util.FastByteArrayOutputStream;
import com.vaadin.annotations.Push;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import java.io.*;
import java.time.Instant;
import java.util.Arrays;

/**
 * @author Alejandro Duarte
 */
@SpringUI
@Push // enable UI modifications from background threads in the server
public class VaadinUI extends UI implements Upload.Receiver, Upload.SucceededListener {

    private Upload upload;
    private ComboBox<String> comboBox = new ComboBox<String>();
    private ProgressBar progressBar;
    private Label labelHour;
    private Label labelResult;
    private final ProcessingService processingService;
    private FastByteArrayOutputStream outputStream;
    private File file;
    private String hash = "";
    public VaadinUI(ProcessingService processingService) { // processingService is injected by Spring
        this.processingService = processingService;
        labelHour = new Label("Hora actual: "+processingService.getHour());
    }

    @Override
    protected void init(VaadinRequest request) {
        processingService.beep(this::setBeep);
        // create an Upload component and set a Receiver and a SucceededListener
        upload = new Upload("Upload a file", this);
        upload.addSucceededListener(this);
        upload.setEnabled(false);
        // create an initially invisible and indeterminate ProgressBar component
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        progressBar.setCaption("Uploading...");

        //
        labelResult = new Label("Checksum: "+" leyendo");
        labelResult.addStyleName("h3");
        labelResult.addStyleName("bold");

        //Combobox
        comboBox.setItems(Arrays.asList("MD5","SHA1","SHA-256"));
        comboBox.addValueChangeListener( e -> {
            hash = e.getValue();
            boolean value = hash != null ? true : false;
            upload.setEnabled(value);
        });

        // configure the layout
        VerticalLayout mainLayout = new VerticalLayout(labelHour,comboBox,labelResult , upload, progressBar);
        setContent(mainLayout);
    }

    @Override
    public OutputStream receiveUpload(String s, String s1) {
        progressBar.setVisible(true);
        BufferedOutputStream bufferedOutputStream = null;
        file = new File("/tmp/",s);
        if(!file.exists()) {
            try {
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            }catch (IOException ex) {
                Notification.show("Not found "+file.getName());
            }
        }
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            
        }
        return bufferedOutputStream;
    }

    @Override
    public void uploadSucceeded(Upload.SucceededEvent succeededEvent) {
        upload.setVisible(false);

        progressBar.setCaption("Processing...");
        progressBar.setIndeterminate(false);

        // the actual job is started inside the service class in a new thread
        processingService.processData(file,hash, labelResult,
                this::processingUpdated, this::processingSucceeded);
    }

    private void processingUpdated(Float percentage) {
        // use access when modifying the UI from a background thread
        access(() -> progressBar.setValue(percentage));
    }

    private void processingSucceeded() {
        access(() -> {
            progressBar.setVisible(false);
            Notification.show("Done!");
        });
    }
    private void setBeep(final String hour) {
        access(()-> labelHour.setValue(hour));
    }
}
