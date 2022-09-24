/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.rmj.gdevice;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/**
 *
 * @author kalyptus
 *
 * Notes:
 *    CommandAPDU(int cla, int ins, int p1, int p2);
 *    CommandAPDU(int cla, int ins, int p1, int p2, int ne);
 *    CommandAPDU(int cla, int ins, int p1, int p2, byte[] data)
 *    CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int dataOffset, int dataLength);
 *    CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int ne);
 *        int most = Integer.parseInt(pin.substring(0,2));
 *        int least = Integer.parseInt(pin.substring(2,4));
 */
public class SLE4428 {
   public boolean init(){
      System.out.println("init");
      psErrMsgx = "";
      
      try {
         // show the list of available terminals
         poFactory = TerminalFactory.getDefault();
         poTerminalList = poFactory.terminals().list();

         if(poTerminalList.isEmpty()){
            psErrMsgx = "No device has been detected!";
            return false;
         }

         // take the first terminal in the list
         poTerminal = (CardTerminal) poTerminalList.get(0);
      }
      catch (CardException ex) {
         psErrMsgx = ex.getMessage();
         return false;
      }

      return true;
   }

   public boolean connect(){
      System.out.println("connect");
      psErrMsgx = "";

      if(poTerminal == null){
         psErrMsgx = "GCard device is not yet initialized!";
         return false;
      }

      try {
         // establish a connection with the card
         poCard = poTerminal.connect("T=0");
         System.out.println("card: " + poCard);
         poChannel = poCard.getBasicChannel();
         //reset the card
         poAtr = poCard.getATR();
      }
      catch (CardException ex) {
         psErrMsgx = ex.getMessage();
         return false;
      }

      return true;
   }

   //Note:Make sure to reset the Error Counter to 0 after a successful verification.
   public boolean verifyPSC(String pin1, String pin2){
      System.out.println("SLE4428.verifyPSC(" + pin1 + ", " + pin2 + ")");
      psErrMsgx = "";

      if(poCard==null){
         psErrMsgx = "GCard is not yet connected!";
         System.out.println(psErrMsgx);
         return false;
      }

//      if(isValid("([0-9]{3})", pin1)){
//         psErrMsgx = "Pin 1 number should be a numeric value!";
//         System.out.println(psErrMsgx);
//         return false;
//      }
//
//      if(isValid("([0-9]{3})", pin2)){
//         psErrMsgx = "Pin 2 number should be a numeric value!";
//         System.out.println(psErrMsgx);
//         return false;
//      }

      try {
         int most = Integer.parseInt(pin1);
         int least = Integer.parseInt(pin2);

         System.out.println(String.format("%02X", most) + String.format("%02X", least));
         //Create the APDU command here
         byte[] cmd = new byte[7];
         cmd[0] = (byte) 0xFF;
         cmd[1] = (byte) 0x20;
         cmd[2] = (byte) 0x00;
         cmd[3] = (byte) 0x00;
         cmd[4] = (byte) 0x02;
         cmd[5] = (byte) most;
         cmd[6] = (byte) least;

         System.out.println(arrayToHex(cmd));
         ResponseAPDU r = poChannel.transmit(new CommandAPDU(cmd));

         if(!(r.getSW() == 0x90FF)){
            psErrMsgx = "Invalid pin number detected!";
            System.out.println(psErrMsgx);
            return false;
         }
      } catch (CardException ex) {
         Logger.getLogger(SLE4428.class.getName()).log(Level.SEVERE, null, ex);
         psErrMsgx = ex.getMessage();
         System.out.println(psErrMsgx);
         return false;
      }

      psPin1 = pin1;
      psPin2 = pin2;
      
      return true;
   }

   public boolean updatePSC(String pin1, String pin2){
      System.out.println("updatePSC");
      psErrMsgx = "";

      if(poCard==null){
         psErrMsgx = "GCard is not yet connected!";
         return false;
      }

      if(psPin1.isEmpty()){
         psErrMsgx = "Pin Number not verified!";
         return false;
      }

//      if(isValid("([0-9]{3})", pin1)){
//         psErrMsgx = "Pin number 1 should be a numeric value!";
//         return false;
//      }
//
//      if(isValid("([0-9]{3})", pin2)){
//         psErrMsgx = "Pin number 2 should be a numeric value!";
//         return false;
//      }

      int most = Integer.parseInt(pin1);
      int least = Integer.parseInt(pin2);

      byte pin[] = {(byte)most, (byte)least};

      if(!write(PCSC_AREA, pin)){
         psErrMsgx = "Pin number not updated!";
         return false;
      }
      else
         return true;
   }

   public boolean disconnect(){
      System.out.println("disconnect");
      psErrMsgx = "";

      if(poCard==null){
         psErrMsgx = "GCard is not connected!";
         return false;
      }

      try {
         poCard.disconnect(false);
      }
      catch (CardException ex) {
         psErrMsgx = ex.getMessage();
         return false;
      }

      poCard = null;
      return true;
   }

   public byte[] read(int address, int len){
      System.out.println("read");
      psErrMsgx = "";

      byte ret[] = {};

      if(poCard==null){
         psErrMsgx = "GCard is not yet connected!";
         return ret;
      }

      String sAddress = String.format("%04X", address);
      System.out.println(sAddress);
      int most = Integer.parseInt(sAddress.substring(0,2), 16);
      int least = Integer.parseInt(sAddress.substring(2,4), 16);

      byte cmd[] = new byte[5];
      cmd[0] = (byte) 0xFF;
      cmd[1] = (byte) 0xB0;
      cmd[2] = (byte) most;
      cmd[3] = (byte) least;
      cmd[4] = (byte) len;
      try {

         System.out.println(arrayToHex(cmd));
         ResponseAPDU r = poChannel.transmit(new CommandAPDU(cmd));

         if(r.getSW() != 0x9000){
            psErrMsgx = "Failed to read the data!";
            return ret;
         }
         System.out.println(r.getData());       
         return r.getData();

         
      } catch (CardException ex) {
         Logger.getLogger(SLE4428.class.getName()).log(Level.SEVERE, null, ex);
         psErrMsgx = ex.getMessage();
         return ret;
      }
   }

   public boolean write(int address, byte[] data){
      System.out.println("write");
      psErrMsgx = "";

      if(poCard==null){
         psErrMsgx = "GCard is not yet connected!";
         return false;
      }

      if(psPin1.isEmpty()){
         psErrMsgx = "Pin Number not verified!";
         return false;
      }

      String sAddress = String.format("%04X", address);
      System.out.println(sAddress);
      int most = Integer.parseInt(sAddress.substring(0,2), 16);
      int least = Integer.parseInt(sAddress.substring(2,4), 16);

      byte cmd[] = new byte[5 + data.length];
      cmd[0] = (byte) 0xFF;
      cmd[1] = (byte) 0xD0;
      cmd[2] = (byte) most;
      cmd[3] = (byte) least;
      cmd[4] = (byte) data.length;

      byte bdata[] = data;

      for(int x = 5; x < (5+data.length); x++)
         cmd[x] = bdata[x-5];
      try {
         System.out.println(arrayToHex(cmd));
         ResponseAPDU r = poChannel.transmit(new CommandAPDU(cmd));
         if(r.getSW() != 0x9000){
            psErrMsgx = "Failed to encode the data!";
            return false;
         }
      } catch (CardException ex) {
         Logger.getLogger(SLE4428.class.getName()).log(Level.SEVERE, null, ex);
         psErrMsgx = ex.getMessage();
         return false;
      }

      return true;
   }

   private boolean isValid(String spattern, String svalue){
      Pattern pattern;
      Matcher matcher;

       pattern = Pattern.compile(spattern);
       matcher = pattern.matcher(svalue);
       return matcher.matches();
   }

   public static String toString(byte[] bytes) {
      final String hexChars = "0123456789ABCDEF";
      StringBuffer sbTmp = new StringBuffer();
      char[] cTmp = new char[2];

      for (int i = 0; i < bytes.length; i++) {
         cTmp[1] = hexChars.charAt((bytes[i] & 0xF0) >>> 4);
         cTmp[2] = hexChars.charAt(bytes[i] & 0x0F);
         sbTmp.append(cTmp);
      }
      return sbTmp.toString();
   }

   public static String arrayToHex(byte[] data) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < data.length; i++) {
         String bs = Integer.toHexString(data[i] & 0xFF);
         if (bs.length() == 1) {
            sb.append(0);
         }
         sb.append(bs);
      }
      return sb.toString();
   }

   public String getErrMessage(){
      return psErrMsgx;
   }

   private byte[] READCTRX = {(byte) 0xFF, (byte) 0xB1, (0x00), (byte) 0x00, (byte)0x03};
   private byte[] SELECTCD = {(byte) 0xFF, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x05};

   private byte[] WRITEPRT = {(byte) 0xFF, (byte) 0xD1};
   private byte[] READPROT = {(byte) 0xFF, (byte) 0xB2};

   private String psErrMsgx="";
   private String psPin1="";
   private String psPin2="";
   private CardTerminal poTerminal=null;
   protected CardChannel poChannel=null;
   private TerminalFactory poFactory=null;
   private Card poCard=null;
   private List poTerminalList;
   private ATR poAtr=null;

   private static final int PCSC_AREA = 0x3FE;
}
