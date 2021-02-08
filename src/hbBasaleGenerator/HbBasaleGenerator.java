package hbBasaleGenerator;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class HbBasaleGenerator extends Application {

    public final String buttonStyle = "-fx-background-color: \n" +
            "        #c3c4c4,\n" +
            "        linear-gradient(#d6d6d6 50%, white 100%),\n" +
            "        radial-gradient(center 50% -40%, radius 200%, #e6e6e6 45%, rgba(230,230,230,0) 50%);\n" +
            "    -fx-background-radius: 30;\n" +
            "    -fx-background-insets: 0,1,1;\n" +
            "    -fx-text-fill: black;\n" +
            "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 3, 0.0 , 0 , 1 );" +
            "-fx-font:  14px 'Helvetica';";
    //root
    StackPane root = new StackPane();
    //Global util
    String calvalIdofTheFile;
    //Files
    File hbL0Ffile;
    List<File> hbL0xmls;
    File gecFile;
    List<File> gecXmls;
    File detailsFile;
    Map<Integer, List<String>> excelDetailsFileContent;
    Map<Integer, List<String>> excelBeamsFileContent;
    Map<Integer, List<String>> excelSlaveSiteRequested;
    List<BasaleDetail> basaleDetailList;
    List<BasaleDetail> di2s;
    File csgBeamsFile = new File("CSGBeams.xlsx");
    File slaveSiteRequested = new File("SlaveSiteRequested.xlsx");

    //Util lists
    Map<Integer, List<Element>> parsedFilePfqaAndD2S = new HashMap<>();
    Map<Integer, File> parsedFilePfqaAndD2SLocation = new HashMap<>();
    //Paths
    String mainFolder;
    //Buttons
    Button generateButton = new Button();
    Button selectHBL0FfileButton = new Button();
    Button selectGECfileButton = new Button();
    //Text
    Text l0fFileselectedText = new Text();
    Text gecFileSelectedText = new Text();
    Text detailsFileSelectedText = new Text();

    //Namespace

    public static void main(String[] args){
        launch(args);
    }
    @Override
    public void start(Stage stage) throws Exception {
        //Load CSGBeams.xlsx
        try {
            excelBeamsFileContent = readExcelFile(csgBeamsFile);
        } catch (IOException ioException) { ioException.printStackTrace(); }
        List<CSGBeam> csgBeams = createCSGBeamsList(excelBeamsFileContent);
        //Load SiteDetails.xlsx
        try {
            excelSlaveSiteRequested = readExcelFile(slaveSiteRequested);
        } catch (IOException ioException) { ioException.printStackTrace(); }
        List<RequestedLevel> slaveRequestedLevels = createSlaveRequestedLevelList(excelSlaveSiteRequested);
        root.setId("main");
        stage.setTitle("HB_Basale_Generator");
        //Button Read Excel Details
        Button selectDetailsButton = new Button();
        selectDetailsButton.setText("Select Details File");
        selectDetailsButton.setStyle(buttonStyle);
        selectDetailsButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser chooserDetailsExcelFile = new FileChooser();
                chooserDetailsExcelFile.setTitle("Select Details File");
                chooserDetailsExcelFile.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xls, *.xlsx)", "*.xls","*.xlsx"));
                detailsFile = chooserDetailsExcelFile.showOpenDialog(stage);
                detailsFileSelectedText.setText("Selected: " + detailsFile.getName());
                //Read Excel
                try {
                    excelDetailsFileContent = readExcelFile(detailsFile);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                //List of Basale Detail
                basaleDetailList = createBasaleDetails(excelDetailsFileContent);
                //Clean list and Save Di2s
                List<BasaleDetail> cleaner = new ArrayList<>();
                List<BasaleDetail> di2sTrue = new ArrayList<>();
                for(BasaleDetail b : basaleDetailList){
                    if(b.getPlan_id().equals(" ") && b.getTarget().equals(" ")){
                        cleaner.add(b);
                    }
                    else{
                        if(b.getDi2s())
                            di2sTrue.add(b);
                    }
                }
                basaleDetailList.removeAll(cleaner);
                di2s = di2sTrue;
                //Unlocks 2 buttons
                if(!basaleDetailList.isEmpty()){
                    selectGECfileButton.setDisable(false);
                    selectHBL0FfileButton.setDisable(false);
                }
            }
        });
        //Button Select and unzip the L0F Zip
        selectHBL0FfileButton.setText("Select HB_L0F File");
        selectHBL0FfileButton.setStyle(buttonStyle);
        selectHBL0FfileButton.setDisable(true);
        selectHBL0FfileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                parsedFilePfqaAndD2S.clear();
                FileChooser chooserHBL0Ffile = new FileChooser();
                chooserHBL0Ffile.setTitle("Select HB_L0F File");
                chooserHBL0Ffile.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip Files (*.zip)", "*.zip"));
                hbL0Ffile = chooserHBL0Ffile.showOpenDialog(stage);
                l0fFileselectedText.setText("Selected: " + hbL0Ffile.getName());
                try {
                    hbL0xmls = unZipFile(hbL0Ffile);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                generateButton.setDisable(false);
            }
        });
        //Button Select and unzip the GEC Zip
        selectGECfileButton.setText("Select GEC File");
        selectGECfileButton.setStyle(buttonStyle);
        selectGECfileButton.setDisable(true);
        selectGECfileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser chooserHBL0Ffile = new FileChooser();
                chooserHBL0Ffile.setTitle("Select GEC File");
                chooserHBL0Ffile.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip Files (*.zip)", "*.zip"));
                gecFile = chooserHBL0Ffile.showOpenDialog(stage);
                gecFileSelectedText.setText("Selected: " + gecFile.getName());
                try {
                    gecXmls = unZipFile(gecFile);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                generateButton.setDisable(false);
            }
        });
        //Button Generated
        generateButton.setDisable(true);
        generateButton.setText("Generate");
        generateButton.setStyle(buttonStyle);
        generateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //FOLDER PATH
                try { mainFolder = detailsFile.getCanonicalPath().substring(0, detailsFile.getCanonicalPath().lastIndexOf("/")) + "/";                }
                catch (IOException ioException) { ioException.printStackTrace(); }
                //Manage First ZIP
                try{
                    List<File> basaleTRV = new ArrayList<>();
                    List<File> basaleFILL = new ArrayList<>();
                    List<File> basaleFILLD2S = new ArrayList<>();
                    List<File> basalePFQCA = new ArrayList<>();
                    //1) Basale_TRV.zip and 2) Basale_FILL.zip
                    for(File f : hbL0xmls){
                        if(f.getName().contains("TIMEPERF_REVISIT")){
                            basaleTRV.add(f);
                        }
                        else if(f.getName().contains("Amazon")&&!f.getName().contains("MISURARE")){
                            basaleFILL.add(f);
                        }
                    }
                    //Zipping...
                    try {
                        zipMultipleFiles(basaleTRV, mainFolder + "Basale_TRV");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    //Clean list from Basale_TRV
                    hbL0xmls.removeAll(basaleTRV);
                    //Clean list from Basale_FILL
                    try {
                        zipMultipleFiles(basaleFILL, mainFolder + "Basale_FILL");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    hbL0xmls.removeAll(basaleFILL);
                    //Parsing remaining files
                    int fileCounter = 0;
                    for(File file : hbL0xmls){
                        SAXBuilder builder = new SAXBuilder();
                        List<Element> helperList = new ArrayList<>();
                        Document doc;
                        try{
                            doc = builder.build(file);
                            xmlToList(doc, 0, helperList);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        //Save Parsed XML and Location XML
                        parsedFilePfqaAndD2S.put(fileCounter, helperList);
                        parsedFilePfqaAndD2SLocation.put(fileCounter, file);
                        fileCounter++;
                    }
                    //The files are parsed and saved in an HashMap
                    //Start of Zipping basaleFILLD2S
                    int di2sCounter = 0;
                    List<Integer> positionOfDi2sInTheParsedFileList = new ArrayList<>();
                    HashMap<Integer, List<Element>> di2sRequests = new HashMap<>();
                    HashMap<Integer, File> di2sRequestsFilesLocation = new HashMap<>();
                    for (Map.Entry<Integer, List<Element>> entry : parsedFilePfqaAndD2S.entrySet()){
                        for(Element e : entry.getValue()){
                            if(e.getName().equals("orderReference")){
                                for(BasaleDetail di2s : di2s){
                                    if(di2s.getCalval_id().equals(e.getContent(0).getValue())){
                                        positionOfDi2sInTheParsedFileList.add(entry.getKey());
                                        di2sRequests.put(di2sCounter, entry.getValue());
                                        di2sRequestsFilesLocation.put(di2sCounter, parsedFilePfqaAndD2SLocation.get(entry.getKey()));
                                        di2sCounter++;
                                    }
                                }
                            }

                        }
                    }
                    //Zipping files...
                    for (Map.Entry<Integer, File> entry : di2sRequestsFilesLocation.entrySet()){
                        basaleFILLD2S.add(entry.getValue());
                    }
                    try {
                        zipMultipleFiles(basaleFILLD2S, mainFolder + "Basale_FILL_D2S");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    //Clean main lists from Di2s
                    positionOfDi2sInTheParsedFileList.forEach(i -> {
                        parsedFilePfqaAndD2S.remove(i);
                        parsedFilePfqaAndD2SLocation.remove(i);
                    });
                    for (Map.Entry<Integer, File> entry : parsedFilePfqaAndD2SLocation.entrySet()){
                        basalePFQCA.add(entry.getValue());
                    }
                    int calval_id_column = getNumOfColumn("calval_id",excelDetailsFileContent.get(0));
                    int beam_column = getNumOfColumn("BEAM",excelDetailsFileContent.get(0));
                    //Zipping files...
                    for(File file : basalePFQCA){
                        SimpleCALVALReq simpleCALVALReq = new SimpleCALVALReq();
                        SAXBuilder builder = new SAXBuilder();
                        Document doc = null;
                        try{
                            doc = builder.build(file);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        Boolean changed = false;
                        calvalIdofTheFile = null;
                        getElementValue((Element) doc.getContent(0), "orderReference");
                        try{
                            for(Map.Entry<Integer, List<String>> entry : excelDetailsFileContent.entrySet()){
                                if(entry.getKey() != 0){
                                    try{
                                        try{
                                            if(calvalIdofTheFile.equals(entry.getValue().get(calval_id_column))){
                                                String correctBeam = correctBeam(entry.getValue().get(beam_column));
                                                for(CSGBeam csgBeam : csgBeams){
                                                    if(csgBeam.getBeam().equals(correctBeam)){
                                                        changeElementValue((Element) doc.getContent(0), "beamId", csgBeam.getBeam());
                                                        changeElementValue((Element) doc.getContent(0), "minLookAngle", csgBeam.getNear_look_angle());
                                                        changeElementValue((Element) doc.getContent(0), "maxLookAngle", csgBeam.getFar_look_angle());
                                                        changed = true;
                                                    }
                                                    if(changed)
                                                        break;
                                                }
                                            }
                                        }catch(NullPointerException e){ }
                                    }
                                    catch(ArrayIndexOutOfBoundsException e){}
                                }
                                if(changed)
                                    break;
                            }
                        }catch (IndexOutOfBoundsException e){}
                        try {
                            FileWriter writer = new FileWriter(file);
                            XMLOutputter outputter = new XMLOutputter();
                            outputter.setFormat(Format.getPrettyFormat());
                            outputter.output(doc, writer);
                            outputter.output(doc, System.out);
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        zipMultipleFiles(basalePFQCA, mainFolder + "Basale_Calibrazione_PFQCA");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    //Export PDF slave_for_PFQCA
                    Map<Integer, List<Element>> parsedPFQCA = new HashMap<>();
                    Map<Integer, File> parsedPFQCAFileLocation = new HashMap<>();
                    int utilCounter = 0;
                    for(File file : basalePFQCA){
                        parsedPFQCAFileLocation.put(utilCounter, file);
                        SAXBuilder builder = new SAXBuilder();
                        List<Element> utilList = new ArrayList<>();
                        Document doc = null;
                        try{
                            doc = builder.build(file);
                            xmlToList(doc, 0, utilList);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        parsedPFQCA.put(utilCounter, utilList);
                        utilCounter++;
                    }
                    List<SlaveRequest> slaveRequestList = MatchPFQCAxLVLs(slaveRequestedLevels, parsedPFQCA, parsedPFQCAFileLocation);
                    try {
                        ExportPDF.exportPDF(basalePFQCA.get(0).getCanonicalPath(), true, slaveRequestList, null);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    //Clean Folder
                    deleteDirectory(new File(mainFolder + hbL0Ffile.getName().substring(0,hbL0Ffile.getName().indexOf("."))));
                    deleteFile(hbL0Ffile);
                }catch(NullPointerException e){}

//------------MANAGE SECOND ZIP--------------------------
                //File SubLists
                try{
                    List<File> respRtn = new ArrayList<>();
                    List<File> respVu = new ArrayList<>();
                    List<File> infoAgeNominal = new ArrayList<>();
                    List<File> infoAgeFast = new ArrayList<>();
                    for(File f : gecXmls){
                        if(f.getName().contains("RESP_RTN")){
                            respRtn.add(f);
                        }
                        else if(f.getName().contains("VU")){
                            respVu.add(f);
                        }
                        else if(f.getName().contains("INFO_AGE_NOMINAL")){
                            infoAgeNominal.add(f);
                        }
                        else if(f.getName().contains("INFO_AGE_FAST")&&!f.getName().contains("VU")){
                            infoAgeFast.add(f);
                        }
                    }
                    for(File file : respVu){
                        SimpleCALVALReq simpleCALVALReq = new SimpleCALVALReq();
                        SAXBuilder builder = new SAXBuilder();
                        Document doc = null;
                        try{
                            doc = builder.build(file);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        changeTimeStopValue((Element) doc.getContent(0), "timeStop");
                        try {
                            FileWriter writer = new FileWriter(file);
                            XMLOutputter outputter = new XMLOutputter();
                            outputter.setFormat(Format.getPrettyFormat());
                            outputter.output(doc, writer);
                            outputter.output(doc, System.out);
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    for(File file : respVu){
                        SimpleCALVALReq simpleCALVALReq = new SimpleCALVALReq();
                        SAXBuilder builder = new SAXBuilder();
                        Document doc = null;
                        try{
                            doc = builder.build(file);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        changeTimeStopValue((Element) doc.getContent(0), "timeStop");
                        try {
                            FileWriter writer = new FileWriter(file);
                            XMLOutputter outputter = new XMLOutputter();
                            outputter.setFormat(Format.getPrettyFormat());
                            outputter.output(doc, writer);
                            outputter.output(doc, System.out);
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    for(File file : infoAgeFast){
                        SimpleCALVALReq simpleCALVALReq = new SimpleCALVALReq();
                        SAXBuilder builder = new SAXBuilder();
                        Document doc = null;
                        try{
                            doc = builder.build(file);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        addElementForInfoAgeFast((Element) doc.getContent(0), "MissionOption");
                        //update files
                        try {
                            FileWriter writer = new FileWriter(file);
                            XMLOutputter outputter = new XMLOutputter();
                            outputter.setFormat(Format.getPrettyFormat());
                            outputter.output(doc, writer);
                            outputter.output(doc, System.out);
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    List<SimpleCALVALReq> simpleCALVALReqs = new ArrayList<>();
                    for(File file : respVu){
                        SimpleCALVALReq simpleCALVALReq = new SimpleCALVALReq();
                        SAXBuilder builder = new SAXBuilder();
                        List<Element> helperList = new ArrayList<>();
                        Document doc;
                        try{
                            doc = builder.build(file);
                            xmlToList(doc, 0, helperList);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        for(Element element : helperList){
                            if(element.getName().equals("orderReference")){
                                simpleCALVALReq.setCalval_id(element.getContent(0).getValue());
                            }
                            else if(element.getName().equals("timeStart")){
                                simpleCALVALReq.setTime_start(element.getContent(0).getValue());
                            }
                            else if(element.getName().equals("timeStop")){
                                simpleCALVALReq.setTime_stop(element.getContent(0).getValue());
                            }
                        }
                        simpleCALVALReqs.add(simpleCALVALReq);
                    }
                    for(File file : respRtn){
                        SimpleCALVALReq simpleCALVALReq = new SimpleCALVALReq();
                        SAXBuilder builder = new SAXBuilder();
                        List<Element> helperList = new ArrayList<>();
                        Document doc;
                        try{
                            doc = builder.build(file);
                            xmlToList(doc, 0, helperList);
                        }
                        catch (JDOMException | IOException jdomException) {
                            System.out.println(jdomException.toString());
                        }
                        for(Element element : helperList){
                            if(element.getName().equals("orderReference")){
                                simpleCALVALReq.setCalval_id(element.getContent(0).getValue());
                            }
                            else if(element.getName().equals("timeStart")){
                                simpleCALVALReq.setTime_start(element.getContent(0).getValue());
                            }
                            else if(element.getName().equals("timeStop")){
                                simpleCALVALReq.setTime_stop(element.getContent(0).getValue());
                            }
                        }
                        simpleCALVALReqs.add(simpleCALVALReq);
                    }
                    try {
                        ExportPDF.exportPDF(infoAgeFast.get(0).getCanonicalPath(), false, null, simpleCALVALReqs);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    //zipping files
                    try {
                        zipMultipleFiles(respRtn, mainFolder + "RESP_RTN");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    try {
                        zipMultipleFiles(respVu, mainFolder + "RESP_VU");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    try {
                        zipMultipleFiles(infoAgeNominal, mainFolder + "INFO_AGE_NOMINAL");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    try {
                        zipMultipleFiles(infoAgeFast, mainFolder + "INFO_AGE_FAST");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    deleteDirectory(new File(mainFolder + gecFile.getName().substring(0,gecFile.getName().indexOf("."))));
                    deleteFile(gecFile);
                }catch(NullPointerException e){}
                //Animation
                TranslateTransition animation = createAnimation();
                animation.pause();
                root.setEffect(new GaussianBlur());

                //Pop Up
                VBox exitPopUp = new VBox(10);
                exitPopUp.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);"+
                        "-fx-border-color: rgba(200,200,200);" +
                        "-fx-border-width: 3;" +
                        "-fx-border-style: solid;");
                exitPopUp.setAlignment(Pos.TOP_CENTER);
                exitPopUp.setPadding(new Insets(10,10,10,10));
                //Label
                Text okText = new Text("HB_BASALE SUCCESSFULLY GENERATED");
                okText.setFont(Font.font("sans-serif", FontPosture.ITALIC, 20));
                okText.setTextAlignment(TextAlignment.CENTER);
                exitPopUp.getChildren().add(okText);
                //Button
                Button okButton = new Button();
                okButton.setText("OK");
                okButton.setStyle("-fx-background-color: \n" +
                        "        #c3c4c4,\n" +
                        "        linear-gradient(#d6d6d6 50%, white 100%),\n" +
                        "        radial-gradient(center 50% -40%, radius 200%, #e6e6e6 45%, rgba(230,230,230,0) 50%);\n" +
                        "    -fx-background-radius: 30;\n" +
                        "    -fx-background-insets: 0,1,1;\n" +
                        "    -fx-text-fill: black;\n" +
                        "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 3, 0.0 , 0 , 1 );" +
                        "-fx-font:  20px 'Helvetica';");
                exitPopUp.getChildren().add(okButton);
                exitPopUp.setFillWidth(true);

                //Stage
                Stage popupStage = new Stage(StageStyle.TRANSPARENT);
                popupStage.initOwner(stage);
                popupStage.initModality(Modality.APPLICATION_MODAL);
                popupStage.setScene(new Scene(exitPopUp, Color.TRANSPARENT));
                popupStage.show();

                okButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        System.exit(0);
                    }
                });
            }
        });
        //Buttons GridPane
        GridPane buttonsGridPane = new GridPane();
        buttonsGridPane.setMinSize(400, 350);
        buttonsGridPane.setPadding(new Insets(110,10,10,10));
        buttonsGridPane.setAlignment(Pos.CENTER);
        buttonsGridPane.setVgap(10);
        buttonsGridPane.setHgap(10);
        buttonsGridPane.add(selectDetailsButton, 0,0);
        detailsFileSelectedText.setFill(Color.WHITE);
        buttonsGridPane.add(detailsFileSelectedText, 0, 1);
        buttonsGridPane.add(selectHBL0FfileButton,0,2);
        l0fFileselectedText.setFill(Color.WHITE);
        buttonsGridPane.add(l0fFileselectedText, 0,3);
        buttonsGridPane.add(selectGECfileButton,0,4);
        gecFileSelectedText.setFill(Color.WHITE);
        buttonsGridPane.add(gecFileSelectedText, 0,5);
        buttonsGridPane.add(generateButton, 0, 6);
        root.getChildren().add(buttonsGridPane);
        Scene mainScene = new Scene(root, 500, 450);
        mainScene.getStylesheets().addAll(this.getClass().getResource("scene.css").toExternalForm());
        stage.setScene(mainScene);
        stage.show();
    }
    //Parse an xml and save it in a list of Element
    public static void xmlToList(Object o, int depth, List<Element> elementList) {
        if (o instanceof Element) {
            Element element = (Element) o;
            //Eventuali ATTRUBUTI dell'elemento
            if (!((Element) o).getAttributes().isEmpty()) {
                Iterator iter = element.getAttributes().iterator();
                while (iter.hasNext()) {
                    Object attribute = iter.next();
                    xmlToList(attribute, depth + 1,elementList);
                }
            }
            elementList.add(element);
            //Eventuali FIGLI dell'elemento
            List children = element.getContent();
            Iterator iterator = children.iterator();
            while (iterator.hasNext()) {
                Object child = iterator.next();
                xmlToList(child, depth + 1,elementList);
            }
        }
        else if (o instanceof Document) {
            Document doc = (Document) o;
            List children = doc.getContent();
            Iterator iterator = children.iterator();
            while (iterator.hasNext()) {
                Object child = iterator.next();
                xmlToList(child, depth + 1,elementList);
            }
        }
    }
    //Unzip file
    public static List<File> unZipFile (File zipToUnarchive) throws FileNotFoundException, IOException {
        List<File> fileList = new ArrayList<>();
        byte[] buffer = new byte[1024];
        File destDir = new File(zipToUnarchive.getCanonicalPath().substring(0, zipToUnarchive.getCanonicalPath().indexOf("NewAcq")) + zipToUnarchive.getName().substring(0, zipToUnarchive.getName().indexOf(".")));
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipToUnarchive));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File file = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!file.isDirectory() && !file.mkdirs()) {
                    throw new IOException("Failed to create directory " + file);
                }
            }
            else {
                File parent = file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }
                FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            fileList.add(file);
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return fileList;
    }
    //Single file to zip
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
    //ZipFile
    public static void zipMultipleFiles(List<File> fileList, String zipName) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipName + ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        for(File file : fileList){
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();
    }
    //Read Excel
    public static Map<Integer, List<String>> readExcelFile(File excelFile) throws IOException {
        FileInputStream file = new FileInputStream(excelFile);
        Workbook workbook  = null;
        if(excelFile.getName().contains("xlsx")){
            workbook = new XSSFWorkbook(file);
        }
        else{
            workbook = new HSSFWorkbook(file);
        }
        Sheet sheet = workbook.getSheetAt(0);
        Map<Integer, List<String>> data = new HashMap<>();
        int i = 0;
        for (Row row : sheet) {
            data.put(i, new ArrayList<String>());
            for (Cell cell : row) {
                switch (cell.getCellType())
                {
                    case STRING:{
                        data.get(new Integer(i)).add(cell.getRichStringCellValue().getString());
                        break;
                    }

                    case NUMERIC:{
                        if (DateUtil.isCellDateFormatted(cell)) {
                            data.get(i).add(cell.getDateCellValue() + "");
                        } else {
                            data.get(i).add(cell.getNumericCellValue() + "");
                        }
                        break;
                    }

                    case BOOLEAN:{
                        data.get(i).add(cell.getBooleanCellValue() + "");
                        break;
                    }

                    case FORMULA:{
                        data.get(i).add(cell.getCellFormula() + "");
                        break;
                    }

                    default: data.get(new Integer(i)).add(" ");
                }
            }
            i++;
        }
        return data;
    }
    //Basale Details
    public static List<BasaleDetail> createBasaleDetails(Map<Integer, List<String>> excelFile){
        List<BasaleDetail> basaleDetails = new ArrayList<>();
        List<String> firstLine = excelFile.get(0);
        List<List<String>> rows = new ArrayList<>();
        for(int i=1; i<excelFile.size(); i++){
            rows.add(excelFile.get(i));
        }
        //Create Details
        for(List<String> row : rows){
            BasaleDetail detail = new BasaleDetail();
            for(int column=0; column<firstLine.size(); column++){
                if(firstLine.get(column).equals("plan_id")){
                    try{
                        detail.setPlan_id(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setPlan_id(null);
                    }
                }
                else if(firstLine.get(column).equals("target")){
                    try{
                        detail.setTarget(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setTarget(null);
                    }
                }
                else if(firstLine.get(column).equals("lat")){
                    try{
                        detail.setLat(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setLat(null);
                    }
                }
                else if(firstLine.get(column).equals("lon")){
                    try{
                        detail.setLon(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setLon(null);
                    }
                }
                else if(firstLine.get(column).equals("height")){
                    try{
                        detail.setHeight(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setHeight(null);
                    }
                }
                else if(firstLine.get(column).equals("start_validity_time")){
                    try{
                        detail.setStart_validity_time(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setStop_validity_time(null);
                    }
                }
                else if(firstLine.get(column).equals("stop_validity_time")){
                    try{
                        detail.setStop_validity_time(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setStop_validity_time(null);
                    }
                }
                else if(firstLine.get(column).equals("mission")){
                    try{
                        detail.setMission(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setMission(null);
                    }
                }
                else if(firstLine.get(column).equals("pass_mode")){
                    try{
                        detail.setPass_mode(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setPass_mode(null);
                    }
                }
                else if(firstLine.get(column).equals("operative_mode")){
                    try{
                        detail.setOperative_mode(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setOperative_mode(null);
                    }
                }
                else if(firstLine.get(column).equals("polarization")){
                    try{
                        detail.setPolarization(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setPolarization(null);
                    }                 }
                else if(firstLine.get(column).equals("product_level")){
                    try{
                        detail.setProduct_level(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setProduct_level(null);
                    }                 }
                else if(firstLine.get(column).equals("di2s")){
                    try{
                        if(row.get(column).equals("true")){
                            detail.setDi2s(true);
                        }
                        else{
                            detail.setDi2s(false);
                        }
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setDi2s(null);
                    }
                }
                else if(firstLine.get(column).equals("priority")){
                    try{
                        detail.setPriority(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setPriority(null);
                    }
                }
                else if(firstLine.get(column).equals("rppi")){
                    try{
                        detail.setRppi(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setRppi(null);
                    }
                }
                else if(firstLine.get(column).equals("calval_id")){
                    try{
                        detail.setCalval_id(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setCalval_id(null);
                    }
                }
                else if(firstLine.get(column).equals("day")){
                    try{
                        detail.setDay(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setDay(null);
                    }
                }
                else if(firstLine.get(column).equals("cycle")){
                    try{
                        detail.setCycle(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setCycle(null);
                    }                }
                else if(firstLine.get(column).equals("keyword")){
                    try{
                        detail.setKeyword(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setKeyword(null);
                    }
                }
                else if(firstLine.get(column).equals("beam")){
                    try{
                        detail.setBeam(row.get(column));
                    }
                    catch(IndexOutOfBoundsException e){
                        detail.setBeam(null);
                    }
                }
            }
            basaleDetails.add(detail);
        }
        return basaleDetails;
    }
    //Create Beams List
    public static List<CSGBeam> createCSGBeamsList(Map<Integer, List<String>> excelFile){
        List<CSGBeam> csgBeams = new ArrayList<>();
        List<String> firstLine = excelFile.get(0);
        List<List<String>> rows = new ArrayList<>();
        for(int i=1; i<excelFile.size(); i++){
            rows.add(excelFile.get(i));
        }
        for(List<String> row : rows){
            CSGBeam csgBeam = new CSGBeam();
            for(int column=0; column<firstLine.size(); column++){
                if(firstLine.get(column).equals("beam")){
                    try{ csgBeam.setBeam(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ csgBeam.setBeam(null); }
                }
                if(firstLine.get(column).equals("near_look_angle")){
                    try{ csgBeam.setNear_look_angle(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ csgBeam.setNear_look_angle(null); }
                }
                if(firstLine.get(column).equals("far_look_angle")){
                    try{ csgBeam.setFar_look_angle(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ csgBeam.setFar_look_angle(null); }
                }
                if(firstLine.get(column).equals("sensor_mode")){
                    try{ csgBeam.setSensor_mode(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ csgBeam.setSensor_mode(null); }
                }
            }
            csgBeams.add(csgBeam);
        }
        return csgBeams;
    }
    //Create Slave Requested Levels List
    public static List<RequestedLevel> createSlaveRequestedLevelList(Map<Integer, List<String>> excelFile){
        List<RequestedLevel> requestedLevels = new ArrayList<>();
        List<String> firstLine = excelFile.get(0);
        List<List<String>> rows = new ArrayList<>();
        for(int i=1; i<excelFile.size(); i++){
            rows.add(excelFile.get(i));
        }
        for(List<String> row : rows){
            RequestedLevel requestedLevel = new RequestedLevel();
            for(int column=0; column<firstLine.size(); column++){
                if(firstLine.get(column).equals("site")){
                    try{ requestedLevel.setSite(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ requestedLevel.setSite(null); }
                }
                if(firstLine.get(column).equals("sensor")){
                    try{ requestedLevel.setSensor(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ requestedLevel.setSensor(null); }
                }
                if(firstLine.get(column).equals("requested_level")){
                    try{ requestedLevel.setRequested_level(row.get(column)); }
                    catch(IndexOutOfBoundsException e){ requestedLevel.setRequested_level(null); }
                }
            }
            requestedLevels.add(requestedLevel);
        }
        return requestedLevels;
    }
    //Delete Folder
    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
    //Delete File
    public static void deleteFile(File file){
        file.delete();
    }
    //Animation Transition
    private TranslateTransition createAnimation() {
        TranslateTransition animation = new TranslateTransition(Duration.seconds(1), new Rectangle(500, 500, 300, 250));
        animation.setByX(400);
        animation.setCycleCount(Animation.INDEFINITE);
        animation.setAutoReverse(true);
        animation.play();
        return animation;
    }
    //Match Requeted Levels with PFQCA files to export pdf
    private List<SlaveRequest> MatchPFQCAxLVLs (List<RequestedLevel> slaveRequestedLevels, Map<Integer, List<Element>> parsedFilePfqa, Map<Integer, File> pfqcaFiles){
        List<SlaveRequest> listToExportPdf = new ArrayList<>();
        for(Map.Entry<Integer, File> xmlPFQCA : pfqcaFiles.entrySet()){
            String sensor = "";
            String calval_id = "";
            for(Element singleXmlElement : parsedFilePfqa.get(xmlPFQCA.getKey())){
                if(singleXmlElement.getName().equals("beamId")){
                    sensor = singleXmlElement.getValue();
                }
                if(singleXmlElement.getName().equals("orderReference")){
                    calval_id = singleXmlElement.getValue();
                }
            }
            sensor = sensor.substring(0,3);
            for(RequestedLevel requestedLevel : slaveRequestedLevels){
                if(requestedLevel.getSensor().equals(sensor)&&xmlPFQCA.getValue().getName().contains(requestedLevel.getSite())){
                    SlaveRequest slaveRequest = new SlaveRequest();
                    slaveRequest.setSite(requestedLevel.getSite());
                    slaveRequest.setSensor_mode(requestedLevel.getSensor());
                    slaveRequest.setRequested_level(requestedLevel.getRequested_level());
                    slaveRequest.setCalval_id(calval_id);
                    listToExportPdf.add(slaveRequest);
                }
            }
        }
        return listToExportPdf;
    }
    //Get num of a column by List<String> (excel)
    private int getNumOfColumn(String columnName, List<String> row){
        int counterHelper = 0;
        for (String s : row) {
            if(columnName.equals(s)){
                return counterHelper;
            }
            counterHelper++;
        }
        return -1;
    }
    //Change Element Value
    private void changeElementValue(Element e, String elementName, String elementValue){
        if(e.getName().equals(elementName)){
            e.getContent().clear();
            e.getContent().add(0, new org.jdom2.Text(elementValue));
            return;
        }
        for (Content content : e.getContent()) {
            if(content.getCType().toString().equals("Element"))
                changeElementValue((Element)content, elementName, elementValue);
        }
    }
    //Change Element Value
    private void changeTimeStopValue(Element e, String elementName){
        if(e.getName().equals(elementName)){
            String newDate = e.getContent().get(0).getValue().substring(0, 11) + "06:21:00.000Z";
            e.getContent().clear();
            e.getContent().add(0, new org.jdom2.Text(newDate));
            return;
        }
        for (Content content : e.getContent()) {
            if(content.getCType().toString().equals("Element"))
                changeTimeStopValue((Element)content, elementName);
        }
    }
    //Get Element Value
    private void getElementValue(Element e, String elementName){
        if(!e.getName().equals(elementName)){
            for (Content content : e.getContent()) {
                if(content.getCType().toString().equals("Element"))
                    getElementValue((Element)content, elementName);
            }
        }
        else{
            calvalIdofTheFile = e.getContent(0).getValue();
        }
    }
    //Correct Beam
    private String correctBeam(String wrongBeam){
        String correctBeam = wrongBeam.substring(0, wrongBeam.indexOf("."));
        if(correctBeam.equals("QPL")){
            correctBeam = "QPS";
        }
        String beamToBeEdit = wrongBeam.substring(wrongBeam.lastIndexOf(".")+1);
        char[] wrongBeamId = beamToBeEdit.toCharArray();
        char [] correctBeamId = new char[3];
        if(wrongBeamId.length == 1){
            correctBeamId[0] = '0';
            correctBeamId[1] = '0';
            correctBeamId[2] = wrongBeamId[0];
        }
        else if(wrongBeamId.length == 2){
            correctBeamId[0] = '0';
            correctBeamId[1] = wrongBeamId[0];
            correctBeamId[2] = wrongBeamId[1];
        }
        else if(wrongBeamId.length == 3){
            correctBeamId = wrongBeamId;
        }
        correctBeam = correctBeam + "-" + String.valueOf(correctBeamId);
        return correctBeam;
    }
    //Add Element
    private void addElementForInfoAgeFast(Element element, String elementName){
        if(element.getName().equals(elementName)){
            element.addContent(createTagGeolocationTrue());
            return;
        }
        for (Content content : element.getContent()){
            if(content.getCType().toString().equals("Element"))
                addElementForInfoAgeFast((Element)content, elementName);
        }
    }
    //Add Tag
    private Element createTagGeolocationTrue(){
        //<IOP:ProcessingOption>
        Element processingOption = new Element("processingOption");
        processingOption.setNamespace(Namespace.getNamespace("IOP", "http://www.telespazio.com/IOP/schemas/ordering"));
            //<IOPcm:name> value = geolocationLevel
            Element name = new Element("name");
            name.setNamespace(Namespace.getNamespace("IOPcm", "http://www.telespazio.com/IOP/schemas/common"));
            name.getContent().add(new org.jdom2.Text("geolocationLevel"));
            //</IOPcm:name>
            //<IOPcm:stringValue>
            Element stringValue = new Element("stringValue");
            stringValue.setNamespace(Namespace.getNamespace("IOPcm", "http://www.telespazio.com/IOP/schemas/common"));
            stringValue.getContent().add(new org.jdom2.Text("Fast"));
            //</IOPcm:stringValue>
        processingOption.getContent().add(new org.jdom2.Text("\n" + "                                        "));
        processingOption.getContent().add(name);
        processingOption.getContent().add(new org.jdom2.Text("\n" + "                                        "));
        processingOption.getContent().add(stringValue);
        processingOption.getContent().add(new org.jdom2.Text("\n" + "                                        "));
        //</IOP:ProcessingOption>
        return processingOption;
    }
}