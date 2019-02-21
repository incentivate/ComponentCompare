package componentcompare;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class ComponentCompare {

    private static BufferedWriter BW;
    private static final JSONObject ROOT = new JSONObject();
    private static final JSONObject COMPAREMENT = new JSONObject();
    private static final JSONArray DIFF = new JSONArray();

//    private static int numDir = 0;
//    private static int numFiles = 0;
    public static void main(String[] args) {

        try {
            File rootDir = new File(args[0]);
            ROOT.put("root", rootDir.getAbsolutePath());
            JSONArray content = new JSONArray();
            ROOT.put("content", content);

            generateReport(rootDir, content);
            //System.out.println("root " + root);
//            System.out.println(rootDir.toString() + " Carpetas: " + numDir + ", Archivos: " + numFiles);

            System.out.println("\n\n\n\n\n\n\n\n");

            File dirToCompare = new File(args[1]);
            COMPAREMENT.put("root", dirToCompare.getAbsolutePath());
            JSONArray compareContent = new JSONArray();
            COMPAREMENT.put("content", compareContent);
            generateReport(dirToCompare, compareContent);
            System.out.println("\n\n\n\n\n\n");

            JSONObject reportComparison = compareContent(ROOT, COMPAREMENT, DIFF);
            System.out.println("\n\n\n Comparison: " + reportComparison);
//
//            File salida = new File("/home/incentivate/Desktop/resources/salida.txt");
//            BW = new BufferedWriter(new FileWriter(salida));
////            bw.write("Se han encontrado: " + numDir + " directorios \n");
////            bw.write("Se han encontrado: " + numFiles + " archivos \n");
//            BW.write(ROOT.toString());
//            BW.close();
        } catch (IOException e) {
            System.out.println("No existe la ruta de entrada!");
        } catch (ParseException ex) {
            Logger.getLogger(ComponentCompare.class.getName()).log(Level.SEVERE, null, ex);
        }

    } // end of main()

    public static String md5OfString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);

            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    } // end of getMD5()

    public static String md5OfFile(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fs = new FileInputStream(file);
            BufferedInputStream bs = new BufferedInputStream(fs);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte bite : digest) {
                sb.append(String.format("%02x", bite & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException ex) {
            Logger.getLogger(ComponentCompare.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    } // end of md5OfFile()

    public static void generateReport(File dir, JSONArray content) throws IOException, ParseException {
        File listFile[] = dir.listFiles();
        String md5;
        if (listFile != null && listFile.length > 0) {
            for (int i = 0; i < listFile.length; i++) {
                JSONObject obj = new JSONObject();
                content.add(obj);
                if (listFile[i].isDirectory()) {
                    md5 = md5OfString(listFile[i].getAbsolutePath());
                    obj.put("md5", md5);
                    obj.put("type", "folder");
                    obj.put("path", listFile[i].getAbsolutePath());
                    JSONArray contentSon = new JSONArray();
                    obj.put("content", contentSon);
                    generateReport(listFile[i], contentSon);
                    //numDir++;
                } else if (listFile[i].isFile()) {
                    md5 = md5OfFile(listFile[i]);
                    obj.put("md5", md5);
                    obj.put("type", "file");
                    obj.put("name", listFile[i].getAbsolutePath());
                    //numFiles++;
                }
            }
        }
    } // end of generateReport()    

    public static JSONObject compareContent(JSONObject root, JSONObject compare, JSONArray diff) throws IOException, ParseException {
        JSONObject differences = new JSONObject();
        JSONObject folderDifferences = new JSONObject();
        JSONObject fileDifferences = new JSONObject();
        differences.put("root", root.get("root"));
        differences.put("origin-report", compare.get("root"));
        differences.put("diff", diff);
        System.out.println(differences);
        JSONArray rootContent = (JSONArray) root.get("content");
        JSONArray compareContent = (JSONArray) compare.get("content");
        for (Object o : rootContent) {
            JSONObject ob = (JSONObject) o;
            //System.out.println("OB " + ob);
            System.out.println("\n\n\n");
            if (ob.get("type") == "file") {
                String fileName = (String) ob.get("name");
                String fileMd5 = (String) ob.get("md5");
                System.out.println("File encontrado " + fileName);
                JSONObject fileInDestination = getFirstElementFromContentByName(compareContent, fileName);
                if (fileInDestination != null) {
                    System.out.println("Found file " + fileName + " in destination.");
                    if (fileMd5.equals(fileInDestination.get("md5"))) {
                        System.out.println("Original file MD5 is equal to destination file MD5 for " + fileName);
                    } else {
                        System.out.println("Original file MD5 is DIFFERENT to destination file MD5 for " + fileName);
                    }
                } else {
                    System.out.println("Did NOT found file " + fileName + " in destination.");
                    fileDifferences.put("name", fileName);
                    fileDifferences.put("origin-md5", fileMd5);
                    fileDifferences.put("type", ob.get("type"));
                    diff.add(fileDifferences);
                }
            } else if (ob.get("type") == "folder") {
                String path = (String) ob.get("path");
                System.out.println("Found folder " + path);
                JSONObject folderInDestination = getFirstElementFromContentByName(compareContent, path);
                if (folderInDestination != null) {
                    int elementCount = ob.size();
                    if (elementCount > 0) {
                        System.out.println("Folder has " + elementCount + " elements inside.");
                        compareContent(ob, folderInDestination, diff);
                    } else{
                        System.out.println("No files inside this folder");
                    }
                } else {
                    System.out.println("Did NOT found folder " + path + " in destination.");
                    folderDifferences.put("origin-md5", ob.get("md5"));
                    folderDifferences.put("type", ob.get("type"));
                    folderDifferences.put("path", path);
                    diff.add(folderDifferences);
                }
            }
        }// end of for
        return differences;
    } // end of compareContent()

    public static JSONObject getFirstElementFromContentByName(JSONArray content, String _name) {
        if (content == null) {
            return null;
        }
        JSONArray contentFilter = new JSONArray();
        for (Object o : content) {
            JSONObject ob = (JSONObject) o;
            if (ob.get("type") == "folder") {
                if (ob.get("path").equals(_name)) {
                    contentFilter.add(ob);
                }
            } else if (ob.get("type") == "file") {
                if (ob.get("name").equals(_name)) {
                    contentFilter.add(ob);
                }
            }
        }
        if (contentFilter.isEmpty()) {
            return null;
        } else {
            return (JSONObject) contentFilter.get(0);
        }
    } // end of getFirstElementFromContentByName()

}// end of ComponentCompare class
