package com.opensource.yaml2json.tools;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Yaml2Json {
    static Map<String,Map<String,Object>> traitementFile= new HashMap<>();
    public static void main(String[] args) throws Exception {
        String path="C:\\work\\workspace-GITLAB\\devops\\irp-api\\swagger\\swagger.yml";
        String pathOutpu="C:\\work\\workspace-GITLAB\\devops\\irp-api\\swagger\\swagger.json";
        if(args != null && args.length>1){
            path=args[0];
            pathOutpu=args[1];
        }
        File file = new File(path);

        System.err.println("convertToJson");
        Map<String,Object> map= convertFileYamlToMap(file);
        JSONObject jsonObject=new JSONObject(map);
        writeFile(pathOutpu,jsonObject.toString());
    }

    public static void writeFile(String filePath, String fileContent) throws IOException {
        File file = new File(filePath);

        // if file doesnt exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(fileContent);
        bw.close();

    }

    public static String readFile(File file) throws IOException {
        String fileContent = "";
        FileReader fileReader = new FileReader(file.getAbsolutePath());
        // Always wrap FileReader in BufferedReader.
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line= bufferedReader.readLine();
        // get file details and get info you need.
        if(line != null)fileContent=line;
        while ((line = bufferedReader.readLine()) != null) {
            fileContent += ("\n" + line );
        }
        bufferedReader.close();
        return fileContent;
    }

    private static Map<String,Object> convertFileYamlToMap(File yamlFile) throws Exception {
        String absolutePath = yamlFile.getAbsolutePath();
        if(traitementFile.containsKey(absolutePath) && traitementFile.get(yamlFile.getAbsolutePath()) != null){
            return  traitementFile.get(yamlFile.getAbsolutePath());
        }else if(traitementFile.containsKey(absolutePath) && traitementFile.get(yamlFile.getAbsolutePath()) == null){
            throw new Exception("boucle recursive");
        }
        traitementFile.put(absolutePath,null);
        String yamlString=readFile(yamlFile);
        Yaml yaml= new Yaml();
        Map<String,Object> map= (Map<String, Object>) yaml.load(yamlString);
        traitementMap(null,map,yamlFile.getParentFile().getAbsolutePath());
        traitementFile.put(absolutePath,map);
        return map;
    }
    public static  void traitementMap(Map.Entry<String, Object> parent, Map<String,Object> map,String folderPath) throws Exception {
        for(Map.Entry<String, Object> entry : map.entrySet()){
            Object value = entry.getValue();
            if(value instanceof Map){
                traitementMap(entry,(Map)value,folderPath);
            }else if("$ref".equalsIgnoreCase(entry.getKey())){
                String stringValue = (String)value;
                if(stringValue.indexOf("#")==0){
                    if(stringValue.indexOf("#.")==0){
                        stringValue=  stringValue.replace("#.","");
                        entry.setValue("#"+stringValue.substring(stringValue.lastIndexOf("#")+1));
                    }
                }else{
                    String fileName=stringValue;
                    if(stringValue.indexOf("#")>-1){
                        fileName=stringValue.substring(0,stringValue.indexOf("#"));

                    }
                    File childYamlFile = new File(folderPath+File.separator+fileName);
                    Map<String,Object> childMap = convertFileYamlToMap(childYamlFile);
                    String newKey = entry.getKey();
                    if(stringValue.indexOf("#")>-1){
                        String[] subpath = stringValue.substring(stringValue.indexOf("#")+1).split("/");
                        //on comment avec 1 car le premier est un #
                        for(int i=0;i<subpath.length;i++){
                            if("#".equals(subpath[i]) || "".equals(subpath[i])){
                                continue;
                            }else if(childMap.get(subpath[i]) == null){
                                System.out.println("NullPointerException");
                            }else{
                                childMap = (Map<String,Object>)childMap.get(subpath[i]);
                                newKey = subpath[i];
                            }
                        }
                    }
                    parent.setValue(childMap);
                    //map.remove(entry.getKey());
                    //map.put(newKey,childMap);
                }
            }
        }
    }
}
