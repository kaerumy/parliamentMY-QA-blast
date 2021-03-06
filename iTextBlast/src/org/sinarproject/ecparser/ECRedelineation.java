/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sinarproject.ecparser;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import itextblast.ITextBlast;
import java.io.IOException;
import static java.lang.System.out;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leow
 */
public class ECRedelineation {

    public static String SOURCE_2ND = "./example/Sarawak_2ndSchedule.pdf";
    public static String SOURCE = "./example/Sarawak_Proposal.pdf";
    public static String RESULTS = "./results/%s.csv";
    static PdfReader my_reader;
    static int currentScheduleBlock = 0;
    static String currentPARCode;
    static String currentPARName;
    static String currentDUNCode;
    static String currentDUNName;
    static String currentDMCode;
    static String currentDMErrorLabel = "";
    static String currentDMMisaligned = "";
    static int countedDM = 0;
    static int DUNerrors = 0;
    static int DMerrors = 0;
    static int fixedDMs = 0;
    static Map<String, String> final_mapped_data;
    static Map<String, String> error_while_parsing;
    static String ec_filename;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        out.println("Sinar Project's EC Parser ..");
        PdfReader reader;
        reader = null;
        try {
            reader = new PdfReader(SOURCE);
        } catch (IOException ex) {
            Logger.getLogger(ECRedelineation.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Assign it for later reuse ..
        ECRedelineation.my_reader = reader;
        ECRedelineation.ec_filename = "Sarawak_Proposal";
        processECStructure("json");
    }

    public static void processECFile(String ec_filename, String mymeta) {
        if ((mymeta == null) || ("all".equals(mymeta))) {
            out.println("Inside the function processECFile!!");
            mymeta = "csv"; // Available metas: "csv"; maybe next time JSOn??
        } else {
            out.println("Inside the function processECFile with meta of " + mymeta);
        }
        out.println("PROCESSING " + ec_filename + " in " + ITextBlast.working_dir);
        // Fill up the reader link
        PdfReader reader = null;
        try {
            reader = new PdfReader(String.format(ITextBlast.working_dir + ITextBlast.SOURCE, ec_filename));
        } catch (IOException ex) {
            Logger.getLogger(ECRedelineation.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Assign it for later reuse ..
        ECRedelineation.my_reader = reader;
        ECRedelineation.ec_filename = ec_filename;
        processECStructure(mymeta);
    }

    private static void processECStructure(String mymeta) {
        // TODO: Restructure all the stuff from main into here ..
        int n = my_reader.getNumberOfPages();
        int i;
        // Maps init .. Key is <PAR_CODE>/<DUN_CODE>/<DM_CODE>
        // <Key> -> Name:Population
        // Errors -> Map<ErrKey, Original String>; ErrKey is Nxx
        final_mapped_data = new TreeMap<>();
        error_while_parsing = new TreeMap<>();
        // Loop through each page ..
        for (i = 1; i < n; i++) {
            try {
                String content;
                content = PdfTextExtractor.getTextFromPage(my_reader, i,
                        new LocationTextExtractionStrategy()
                );
                // content = PdfTextExtractor.getTextFromPage(reader, i);
                describePage(content, i);
            } catch (IOException ex) {
                Logger.getLogger(ECRedelineation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // Dump out error hash .. if any
        if (error_while_parsing.size() > 0) {
            out.println("==============================");
            out.println(" PARSING ERRORS --> "
                    + error_while_parsing.size() + " errors!!!");
            out.println("DUN: " + DUNerrors
                    + " DM: " + DMerrors
                    + " Fixed:" + fixedDMs);
            out.println("==============================");
            // TODO: Shift to file output
                /*
             for (Map.Entry<String, String> single_report_entry : error_while_parsing.entrySet()) {
             out.println("CODE: " + single_report_entry.getKey());
             out.println("UNMATCHED: " + single_report_entry.getValue());
             }
             */
        } else {
            out.println("========================");
            out.println("      ALL OK!!!         ");
            out.println("========================");
        }

        out.println("========================");
        out.println("  @@@@ Data!!! @@@@     ");
        out.println("========================");
        out.println("Final DM count: " + countedDM);
        // Detect if there any population not being able to be detected; so action can be taken
        for (Map.Entry<String, String> single_data_entry : final_mapped_data.entrySet()) {
            // Output those that were not able to be auto-corrected ..
            if (single_data_entry.getValue().endsWith(":0")) {
                out.print("KEY:" + single_data_entry.getKey());
                out.println(" ==> " + single_data_entry.getValue());
            };
        }
        if (null != mymeta) // write down Output
        // No JSON output in this cut :( Leave it for interaction with golang
        //  and shapefile mapping and manipulatons ..
        {
            switch (mymeta) {
                case "json":
                    Utils.writeJSONMappedData();
                    break;
                case "csv":
                    // go direct to CSV ..
                    Utils.writeCSVFinalData();
                    break;
            }
        }

        out.println("xxxxxxxXXXXXXXXXXxxxxxxxxx");
    }

    private static void describePage(String raw_extracted_content, int current_page) {
        // DEBUG: 
        // out.print(raw_extracted_content);
        if (Utils.isStartOfSchedule(raw_extracted_content)) {
            out.println("Mark page " + current_page + " as a NEW Block");
        };
        switch (currentScheduleBlock) {
            case 1:
                // DO nothing .. for now ..
                break;
            case 2:
                /*
                 Map<String, Map<String, Map<String, Int>>> m;
                 m = null;
                 Map<Character, Map<Character, Map<Character, Integer>>> n;
                 n = map('d',
                 map('c',
                 map('s', 1)
                 )
                 );
                 */
                // Esier code 123/12/33
                //  returns a snippet instead ..
                // Arrays of DMs, DMs[] = ""
                // Init PAR structure??
                // [PAR][DUN][DM]
                // Map<String, Map<String,Map<String,Int>>>
                // Do something ..
                // Look for PAR Header; extract out the value ..
                // Break apart the content by line ..
                // match ..
                String[] lines_of_content = raw_extracted_content.split("\\r?\\n");
                for (String single_line_of_content : lines_of_content) {
                    // DEBUG: For a look at the raw line to be process ..
                    // out.println("LINE:" + single_line_of_content);
                    if (Utils.isStartOfPAR(single_line_of_content)) {
                        // Anything to do with PAR??
                        // DEBUG: 
                        /*
                         out.println(single_line_of_content);
                         out.println("========^^^ MATCHED PAR ^^^^=======");
                         */
                    } else if (Utils.isStartOfDUN(single_line_of_content)) {
                        // ANything to do DUN
                        // DEBUG:
                        /*
                         out.println(single_line_of_content);
                         out.println("========^^^ MATCHED DUN ^^^^=======");
                         */
                    } else if (Utils.containsPossibleMisalignedDM(single_line_of_content)) {
                        // ORDER matters; needs to be called before containsDMData as this
                        //  is a specific rule ..
                        // DO NOT count DM as it will be counted inside DUN starter
                    } else if (Utils.containsDMData(single_line_of_content)) {
                        // Extract DM
                        Utils.mapDMData(single_line_of_content);
                        countedDM++;
                        // DEBUG:
                        /*
                         out.println(single_line_of_content);
                         out.println("========^^^ MATCHED DM ^^^^=======");
                         */
                    } else if (Utils.containsPossibleDMPopulation(single_line_of_content)) {
                        // Stuff done inside function already
                        // out.println("Found population!");
                    } else {
                        // Nothing to do ..
                    };
                }
                break;
            case 3:
                // Do nothing ... for now ..
                break;
            default:
                break;
        }
    }

}
