package hbBasaleGenerator;

import com.itextpdf.io.font.FontConstants;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;

public class ExportPDF {
    Paragraph emptyLine = new Paragraph("\n");
    public DeviceRgb LIGHT_YELLOW = new DeviceRgb(255, 255, 245);
    public DeviceRgb LIGHT_RED = new DeviceRgb(255, 235, 235);
    public DeviceRgb LIGHT_BLUE = new DeviceRgb(245, 245, 255);
    public DeviceRgb LIGHT_GREEN = new DeviceRgb(245, 255, 245);

    public static void exportPDF(String path, Boolean slave, List<SlaveRequest> slaveRequestList, List<SimpleCALVALReq> simpleCALVALReqs) throws IOException {
        if(slave){
            String p = path.substring(0, path.lastIndexOf("/"));
            p = p.substring(0, p.lastIndexOf("/")+1);
            path = p + "slave_for_PFQCA_" +LocalDate.now().toString()+".pdf";
            File file = new File(path);
            file.getParentFile().mkdirs();
            new ExportPDF().exportSlave(path, slaveRequestList);
        }
        if(!slave) {
            String p = path.substring(0, path.lastIndexOf("/"));
            p = p.substring(0, p.lastIndexOf("/")+1);
            path = p + "response_Time" +LocalDate.now().toString()+".pdf";
            File file = new File(path);
            file.getParentFile().mkdirs();
            new ExportPDF().exportcalvalReq(path, simpleCALVALReqs);
        }

    }
    public void exportcalvalReq(String path, List<SimpleCALVALReq> simpleCALVALReqs) throws IOException {
        //Layout of document
        Document document = createDocument(path);
        document.setMargins(5,5,5,5);
        Table firstLineTabel = new Table(new float[]{200,200});
        //Logo Cell
        try{firstLineTabel.addCell(createLogoCell());}catch(NullPointerException nullPointerException){firstLineTabel.addCell(new Cell());}
        //Info Cell
        firstLineTabel.addCell(createDetailsCellCalval(simpleCALVALReqs.size()));
        document.add(firstLineTabel);
        document.add(emptyLine);
        Table mainTable = new Table(new float[]{200,200,200});
        mainTable.addCell(new Cell().add(new Paragraph("Calval ID")).setBold());
        mainTable.addCell(new Cell().add(new Paragraph("Time Start")).setBold());
        mainTable.addCell(new Cell().add(new Paragraph("Time Stop")).setBold());
        for (SimpleCALVALReq req : simpleCALVALReqs) {
            mainTable.addCell(new Cell().add(new Paragraph(req.getCalval_id())));
            mainTable.addCell(new Cell().add(new Paragraph(req.getTime_start())));
            mainTable.addCell(new Cell().add(new Paragraph(req.getTime_stop())));
        }
        document.add(mainTable);
        document.close();
    }

    private Cell createDetailsCellCalval(int size) throws IOException {
        Cell cell = new Cell();
        Table table = new Table(new float[]{200});
        table.addCell("Info TIME PERF").setFont(PdfFontFactory.createFont(FontConstants.HELVETICA)).setBold().setBorder(Border.NO_BORDER);
        table.addCell(new Cell().add(
                new Table(new float[]{100,100}
                )
                        .addCell("Requests: ")
                        .addCell(String.valueOf(size))
        ));
        cell.add(table);
        return cell;
    }

    public void exportSlave(String path, List<SlaveRequest> slaveRequestList) throws IOException {
        //Layout of document
        Document document = createDocument(path);
        document.setMargins(5,5,5,5);
        Table firstLineTabel = new Table(new float[]{200,200});
        //Logo Cell
        try{firstLineTabel.addCell(createLogoCell());}catch(NullPointerException nullPointerException){firstLineTabel.addCell(new Cell());}
        //Info Cell
        firstLineTabel.addCell(createDetailsCell(slaveRequestList.size()));
        document.add(firstLineTabel);
        //Main table
        document.add(emptyLine);
        Table mainTable = mainTable(slaveRequestList);
        document.add(mainTable);
        document.close();
    }
    public Document createDocument(String path) throws FileNotFoundException {
        PdfWriter pdfWriter = new PdfWriter(path);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        return new Document(pdfDocument, PageSize.A4.rotate());
    }
    private Cell createLogoCell(){
        Cell logoCell = new Cell();
        Image logo = null;
        try {
            logo = new Image(ImageDataFactory.create("logo.tiff"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        logo.setHeight(80);
        logo.setWidth(190);
        logoCell.add(logo);
        return logoCell;
    }
    private Cell createDetailsCell(Integer size) throws IOException {
        Cell cell = new Cell();
        Table table = new Table(new float[]{200});
        table.addCell("Slave requested levels for PFQCA").setFont(PdfFontFactory.createFont(FontConstants.HELVETICA)).setBold().setBorder(Border.NO_BORDER);
        table.addCell(new Cell().add(
                new Table(new float[]{100,100}
                )
                .addCell("Requests: ")
                .addCell(String.valueOf(size))
        ));
        cell.add(table);
        return cell;
    }
    private Table mainTable(List<SlaveRequest> slaveRequestList){
        Table table = new Table(new float[]{100,100,100,100,100});
        table.addCell(new Cell().add(new Paragraph("Calval ID")).setBold())
             .addCell(new Cell().add(new Paragraph("Site")).setBold())
             .addCell(new Cell().add(new Paragraph("Sensor Mode")).setBold())
             .addCell(new Cell().add(new Paragraph("Requested Level")).setBold().setItalic())
             .addCell(new Cell().add(new Paragraph("Order ID")).setBold());
        for (SlaveRequest slaveRequest : slaveRequestList) {
            DeviceRgb deviceRgb = new DeviceRgb();
            if(slaveRequest.getSite().equals("MTR")) {
                deviceRgb = LIGHT_YELLOW;
            }
            else if(slaveRequest.getSite().equals("MET")) {
                deviceRgb = LIGHT_GREEN;
            }
            else if(slaveRequest.getSite().equals("BRE")) {
                deviceRgb = LIGHT_BLUE;
            }
            else if(slaveRequest.getSite().equals("Amazon")){
                deviceRgb = LIGHT_RED;
            }
                table.addCell(new Cell().add(new Paragraph(slaveRequest.getCalval_id())).setBackgroundColor(deviceRgb))
                    .addCell(new Cell().add(new Paragraph(slaveRequest.getSite())).setBackgroundColor(deviceRgb))
                    .addCell(new Cell().add(new Paragraph(slaveRequest.getSensor_mode())).setBackgroundColor(deviceRgb))
                    .addCell(new Cell().add(new Paragraph(slaveRequest.getRequested_level())).setBackgroundColor(deviceRgb))
                    .addCell("")
                ;
        }
        return table;
    }
}
