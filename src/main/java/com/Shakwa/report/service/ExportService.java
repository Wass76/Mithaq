package com.Shakwa.report.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Shakwa.admin.dto.DashboardOverviewDTO;
import com.Shakwa.audit.dto.AuditEventDTO;
import com.Shakwa.report.dto.AverageResolutionTimeReportDTO;
import com.Shakwa.report.dto.ComplaintStatusReportDTO;
import com.Shakwa.report.dto.ComplaintTypeDistributionDTO;
import com.opencsv.CSVWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Service for exporting reports to CSV and PDF formats
 */
@Service
public class ExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Fonts for PDF (using default fonts, can be enhanced with Arabic font support)
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    
    // ========== CSV Export Methods ==========
    
    /**
     * Export complaint status report to CSV
     */
    public byte[] exportComplaintStatusReportToCSV(ComplaintStatusReportDTO report) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            // Write header
            csvWriter.writeNext(new String[]{
                "Complaint Status Report",
                "",
                ""
            });
            
            // Write metadata
            if (report.getAgency() != null) {
                csvWriter.writeNext(new String[]{"Agency", report.getAgency().getLabel(), ""});
            }
            if (report.getFromDate() != null) {
                csvWriter.writeNext(new String[]{"From Date", report.getFromDate().format(DATE_FORMATTER), ""});
            }
            if (report.getToDate() != null) {
                csvWriter.writeNext(new String[]{"To Date", report.getToDate().format(DATE_FORMATTER), ""});
            }
            csvWriter.writeNext(new String[]{"", "", ""});
            
            // Write statistics
            csvWriter.writeNext(new String[]{"Status", "Count", ""});
            csvWriter.writeNext(new String[]{"Total Complaints", String.valueOf(report.getTotalComplaints()), ""});
            csvWriter.writeNext(new String[]{"Resolved", String.valueOf(report.getResolvedCount()), ""});
            csvWriter.writeNext(new String[]{"In Progress", String.valueOf(report.getInProgressCount()), ""});
            csvWriter.writeNext(new String[]{"Pending", String.valueOf(report.getPendingCount()), ""});
            csvWriter.writeNext(new String[]{"Rejected", String.valueOf(report.getRejectedCount()), ""});
            csvWriter.writeNext(new String[]{"Closed", String.valueOf(report.getClosedCount()), ""});
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Export average resolution time report to CSV
     */
    public byte[] exportAverageResolutionTimeToCSV(AverageResolutionTimeReportDTO report) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[]{"Average Resolution Time Report", "", ""});
            
            if (report.getAgency() != null) {
                csvWriter.writeNext(new String[]{"Agency", report.getAgency().getLabel(), ""});
            }
            if (report.getFromDate() != null) {
                csvWriter.writeNext(new String[]{"From Date", report.getFromDate().format(DATE_FORMATTER), ""});
            }
            if (report.getToDate() != null) {
                csvWriter.writeNext(new String[]{"To Date", report.getToDate().format(DATE_FORMATTER), ""});
            }
            csvWriter.writeNext(new String[]{"", "", ""});
            
            csvWriter.writeNext(new String[]{"Metric", "Value", ""});
            csvWriter.writeNext(new String[]{"Average Days", String.format("%.2f", report.getAverageDays()), ""});
            csvWriter.writeNext(new String[]{"Average Hours", String.format("%.2f", report.getAverageHours()), ""});
            csvWriter.writeNext(new String[]{"Min Resolution Days", String.valueOf(report.getMinResolutionDays()), ""});
            csvWriter.writeNext(new String[]{"Max Resolution Days", String.valueOf(report.getMaxResolutionDays()), ""});
            csvWriter.writeNext(new String[]{"Total Resolved Complaints", String.valueOf(report.getTotalResolvedComplaints()), ""});
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Export complaint type distribution to CSV
     */
    public byte[] exportComplaintTypeDistributionToCSV(ComplaintTypeDistributionDTO report) throws IOException {
        // First, write CSV data to a temporary stream
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(tempStream, StandardCharsets.UTF_8);
        
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[]{"Complaint Type Distribution Report", "", ""});
            
            if (report.getAgency() != null) {
                csvWriter.writeNext(new String[]{"Agency", report.getAgency().getLabel(), ""});
            }
            if (report.getFromDate() != null) {
                csvWriter.writeNext(new String[]{"From Date", report.getFromDate().format(DATE_FORMATTER), ""});
            }
            if (report.getToDate() != null) {
                csvWriter.writeNext(new String[]{"To Date", report.getToDate().format(DATE_FORMATTER), ""});
            }
            csvWriter.writeNext(new String[]{"Total Complaints", String.valueOf(report.getTotalComplaints()), ""});
            csvWriter.writeNext(new String[]{"", "", ""});
            
            csvWriter.writeNext(new String[]{"Type", "Count", "Percentage"});
            if (report.getDistribution() != null) {
                for (ComplaintTypeDistributionDTO.TypeCount typeCount : report.getDistribution()) {
                    csvWriter.writeNext(new String[]{
                        typeCount.getType() != null && typeCount.getType().getLabel() != null 
                            ? typeCount.getType().getLabel() : "",
                        String.valueOf(typeCount.getCount()),
                        String.format("%.2f%%", typeCount.getPercentage())
                    });
                }
            }
        }
        
        // Now create final output with UTF-8 BOM for proper Excel encoding
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Add UTF-8 BOM
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        outputStream.write(bom);
        // Append the CSV data
        outputStream.write(tempStream.toByteArray());
        
        return outputStream.toByteArray();
    }
    
    /**
     * Export audit log to CSV
     */
    public byte[] exportAuditLogToCSV(List<AuditEventDTO> auditEvents) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[]{"Audit Log Export", "", "", "", "", "", ""});
            csvWriter.writeNext(new String[]{"", "", "", "", "", "", ""});
            
            csvWriter.writeNext(new String[]{
                "ID", "Action", "Target Type", "Target ID", "Actor", "Status", "IP Address", "Created At"
            });
            
            if (auditEvents != null) {
                for (AuditEventDTO event : auditEvents) {
                    csvWriter.writeNext(new String[]{
                        String.valueOf(event.getId()),
                        event.getAction(),
                        event.getTargetType(),
                        event.getTargetId() != null ? String.valueOf(event.getTargetId()) : "",
                        event.getActorName() != null ? event.getActorName() : "",
                        event.getStatus(),
                        event.getIpAddress() != null ? event.getIpAddress() : "",
                        event.getCreatedAt() != null ? event.getCreatedAt().toString() : ""
                    });
                }
            }
        }
        
        return outputStream.toByteArray();
    }
    
    // ========== PDF Export Methods ==========
    
    /**
     * Export complaint status report to PDF
     */
    public byte[] exportComplaintStatusReportToPDF(ComplaintStatusReportDTO report) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        // Title
        addTitle(document, "Complaint Status Report");
        
        // Metadata
        if (report.getAgency() != null) {
            addParagraph(document, "Agency: " + report.getAgency().getLabel(), NORMAL_FONT);
        }
        if (report.getFromDate() != null) {
            addParagraph(document, "From Date: " + report.getFromDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        if (report.getToDate() != null) {
            addParagraph(document, "To Date: " + report.getToDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        document.add(new Paragraph(" "));
        
        // Statistics table
        List<String> headers = List.of("Status", "Count");
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("Total Complaints", String.valueOf(report.getTotalComplaints())));
        data.add(List.of("Resolved", String.valueOf(report.getResolvedCount())));
        data.add(List.of("In Progress", String.valueOf(report.getInProgressCount())));
        data.add(List.of("Pending", String.valueOf(report.getPendingCount())));
        data.add(List.of("Rejected", String.valueOf(report.getRejectedCount())));
        data.add(List.of("Closed", String.valueOf(report.getClosedCount())));
        
        addTable(document, headers, data);
        
        document.close();
        return outputStream.toByteArray();
    }
    
    /**
     * Export average resolution time report to PDF
     */
    public byte[] exportAverageResolutionTimeToPDF(AverageResolutionTimeReportDTO report) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        addTitle(document, "Average Resolution Time Report");
        
        if (report.getAgency() != null) {
            addParagraph(document, "Agency: " + report.getAgency().getLabel(), NORMAL_FONT);
        }
        if (report.getFromDate() != null) {
            addParagraph(document, "From Date: " + report.getFromDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        if (report.getToDate() != null) {
            addParagraph(document, "To Date: " + report.getToDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        document.add(new Paragraph(" "));
        
        List<String> headers = List.of("Metric", "Value");
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("Average Days", String.format("%.2f", report.getAverageDays())));
        data.add(List.of("Average Hours", String.format("%.2f", report.getAverageHours())));
        data.add(List.of("Min Resolution Days", String.valueOf(report.getMinResolutionDays())));
        data.add(List.of("Max Resolution Days", String.valueOf(report.getMaxResolutionDays())));
        data.add(List.of("Total Resolved Complaints", String.valueOf(report.getTotalResolvedComplaints())));
        
        addTable(document, headers, data);
        
        document.close();
        return outputStream.toByteArray();
    }
    
    /**
     * Export complaint type distribution to PDF
     */
    public byte[] exportComplaintTypeDistributionToPDF(ComplaintTypeDistributionDTO report) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        addTitle(document, "Complaint Type Distribution Report");
        
        if (report.getAgency() != null) {
            addParagraph(document, "Agency: " + report.getAgency().getLabel(), NORMAL_FONT);
        }
        if (report.getFromDate() != null) {
            addParagraph(document, "From Date: " + report.getFromDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        if (report.getToDate() != null) {
            addParagraph(document, "To Date: " + report.getToDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        addParagraph(document, "Total Complaints: " + report.getTotalComplaints(), NORMAL_FONT);
        document.add(new Paragraph(" "));
        
        List<String> headers = List.of("Type", "Count", "Percentage");
        List<List<String>> data = new ArrayList<>();
        if (report.getDistribution() != null) {
            for (ComplaintTypeDistributionDTO.TypeCount typeCount : report.getDistribution()) {
                data.add(List.of(
                    typeCount.getType() != null && typeCount.getType().getLabel() != null 
                        ? typeCount.getType().getLabel() : "",
                    String.valueOf(typeCount.getCount()),
                    String.format("%.2f%%", typeCount.getPercentage())
                ));
            }
        }
        
        addTable(document, headers, data);
        
        document.close();
        return outputStream.toByteArray();
    }
    
    /**
     * Export dashboard overview to PDF
     */
    public byte[] exportDashboardOverviewToPDF(DashboardOverviewDTO dashboard) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        addTitle(document, "Dashboard Overview");
        
        // Metadata
        if (dashboard.getFromDate() != null) {
            addParagraph(document, "From Date: " + dashboard.getFromDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        if (dashboard.getToDate() != null) {
            addParagraph(document, "To Date: " + dashboard.getToDate().format(DATE_FORMATTER), NORMAL_FONT);
        }
        document.add(new Paragraph(" "));
        
        // Summary statistics
        addParagraph(document, "Summary Statistics", HEADER_FONT);
        List<String> summaryHeaders = List.of("Metric", "Value");
        List<List<String>> summaryData = new ArrayList<>();
        summaryData.add(List.of("Total Complaints", String.valueOf(dashboard.getTotalComplaints())));
        summaryData.add(List.of("Resolved", String.valueOf(dashboard.getResolvedComplaints())));
        summaryData.add(List.of("Open", String.valueOf(dashboard.getOpenComplaints())));
        summaryData.add(List.of("Rejected", String.valueOf(dashboard.getRejectedComplaints())));
        summaryData.add(List.of("Closed", String.valueOf(dashboard.getClosedComplaints())));
        if (dashboard.getAverageResolutionTimeDays() != null) {
            summaryData.add(List.of("Avg Resolution Time (Days)", String.format("%.2f", dashboard.getAverageResolutionTimeDays())));
        }
        summaryData.add(List.of("Overdue Complaints", String.valueOf(dashboard.getOverdueComplaints())));
        addTable(document, summaryHeaders, summaryData);
        
        document.add(new Paragraph(" "));
        
        // Top agencies
        if (dashboard.getTopAgenciesByComplaints() != null && !dashboard.getTopAgenciesByComplaints().isEmpty()) {
            addParagraph(document, "Top Agencies by Complaints", HEADER_FONT);
            List<String> agencyHeaders = List.of("Agency", "Count");
            List<List<String>> agencyData = new ArrayList<>();
            for (DashboardOverviewDTO.AgencyComplaintCount agency : dashboard.getTopAgenciesByComplaints()) {
                agencyData.add(List.of(agency.getAgencyLabel(), String.valueOf(agency.getComplaintCount())));
            }
            addTable(document, agencyHeaders, agencyData);
            document.add(new Paragraph(" "));
        }
        
        // Top complaint types
        if (dashboard.getTopComplaintTypes() != null && !dashboard.getTopComplaintTypes().isEmpty()) {
            addParagraph(document, "Top Complaint Types", HEADER_FONT);
            List<String> typeHeaders = List.of("Type", "Count");
            List<List<String>> typeData = new ArrayList<>();
            for (DashboardOverviewDTO.TypeComplaintCount type : dashboard.getTopComplaintTypes()) {
                typeData.add(List.of(type.getTypeLabel(), String.valueOf(type.getComplaintCount())));
            }
            addTable(document, typeHeaders, typeData);
        }
        
        document.close();
        return outputStream.toByteArray();
    }
    
    // ========== Helper Methods ==========
    
    private void addTitle(Document document, String title) throws DocumentException {
        Paragraph titlePara = new Paragraph(title, TITLE_FONT);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(20);
        document.add(titlePara);
    }
    
    private void addParagraph(Document document, String text, Font font) throws DocumentException {
        document.add(new Paragraph(text, font));
    }
    
    private void addTable(Document document, List<String> headers, List<List<String>> data) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        
        // Add headers
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
        
        // Add data rows
        for (List<String> row : data) {
            for (String cellData : row) {
                PdfPCell cell = new PdfPCell(new Phrase(cellData, NORMAL_FONT));
                cell.setPadding(5);
                table.addCell(cell);
            }
        }
        
        document.add(table);
    }
}

