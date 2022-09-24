/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gdevice;

import com.fujitsu.pfu.fiscn.sdk.Fiscn;
import com.fujitsu.pfu.fiscn.sdk.FiscnException;

/**
 * @author sayso
 * scanner = new Fujitsu7180();
 * scanner.setGRider(instance);
 * scanner.NewTransaction();
 * scanner.setMaster("", value());
 * scanner.start_scan();
 * scanner.stop_scan();
 * scanner.SaveTransaction();
 * 
 */
public class Fujitsu7180 extends Fiscn{
    private String psBarCode;
    private String psFileNme;
    
    private String psMessage = "";
    private boolean mode_debug = true;
    
    public String getMessage(){
        return psMessage;
    }
    
    public void setDebugMode(boolean mode){
        mode_debug = mode;
    }
    
    public Fujitsu7180() throws FiscnException {
        this.psFileNme = "";
        this.psBarCode = "";
        this.psMessage = "";
    }
    
    public boolean start_scan(){
        try {
            this.initialize(this);
            
            //Open the scanner
            this.openScanner2();
            
            //Set the scan parameters
            this.setScanTo(0);              //Data output to files
            this.setCompressionType(5);     //Compression type to JPEG
            this.setFileType(3);            //File Type JPEG
            this.setPixelType(2);           //RGB
            this.setBarcodeDetection(true); //True
            this.setShowSourceUI(false);    //Do not display the source for User Interface()
            
            //Start the scanning process
            this.startScan();
            
        } catch (FiscnException ex) {
            if(mode_debug) ex.printStackTrace();
            psMessage = "start_scan:" + ex.getMessage();
            return false;
        }
        
        
        return true;
    }
    
    @Override
    public void eventScanToFile(long readCount, String fileName){
        psFileNme = fileName;
        
        try {
            if (this.getAIQCResult()) {
                psMessage = "A bad image was detected. Scanning image count:" + readCount;
            }else if (this.getBlankPageResult() == 1) {
                psMessage = "A blank page image was detected. Scanning image count:" + readCount;
            }
        } catch (FiscnException ex) {
            if(mode_debug) ex.printStackTrace();
            psMessage = ex.getMessage();
        }

        if(mode_debug){
            System.out.println("FileName:" + psFileNme + "XXXBarCode:" + psBarCode + "XXXCount:" + Long.toString(readCount));
        }
        
        //TODO: write to the detail table
        
        psBarCode = "";
    }
    
    @Override
    public void eventDetectBarcode(long readCount, long barcodeType, String barcodeText){
        psBarCode = barcodeText;
    }
    
    public boolean stop_scan(){
        try {
            this.closeScanner();
            this.unInitialize();
        } catch (FiscnException ex) {
            if(mode_debug) ex.printStackTrace();
            psMessage = "close_scan:" + ex.getMessage();
            return false;
        }
        
        return true;
    }

    public void setFilter(String filter){
        
    }
    
    public Object getMaster(String field){
        return null;
    }
    
    public void setMaster(String field, Object value){
        
    }
    
    public Object getDetail(int row, String field){
        return null;
    }
    
    public void setDetail(int row, String field, Object value){
        
    }
    
    public boolean NewTransaction(){
        
        return true;
    }
    
    public boolean SaveTransaction(){
        
        return true;
    }

    public boolean SearchTransaction(String value){
        
        return true;
    }
    
    public boolean OpenTransaction(){
        return true;
    }
    
    public boolean CloseTransaction(){
        
        return true;
    }
    
    public boolean CancelTransaction(){
        
        return true;
    }
}
