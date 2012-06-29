package fr.utc.assos.payutc;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JApplet;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

//The applet code
public class Concerto extends JApplet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CardTerminal MyReader;
	private JSObject jso;
	private byte[] ApduArray = {
			(byte) 0xFF,
			(byte) 0xCA,
			(byte) 0x00,
			(byte) 0x00,
			(byte) 0x00
	};
	private Thread tr;
		
	public static String getHexString(byte[] b) {
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}
	
	private void updateJS(String status){
		this.updateJS(status, "");
	}
	
	private void updateJS(String status, String data){
		System.out.println("Appel javascript : " + status + "/" + data);
		if(jso != null){
			try {
				jso.call("updateJS", new String[] {status, data});
			}
			catch (JSException e){
				System.out.println("Impossible de faire un appel javascript.");
				e.printStackTrace();
			}
		}
	}
	
	public void init(){
		try {
			jso = JSObject.getWindow(this);
		} catch (JSException e) {
			System.out.println("Impossible de trouver l'objet javascript.");
			e.printStackTrace();
		}
		
		try {
			/* Get the list of readers */
			TerminalFactory factory = TerminalFactory.getDefault();
			List<CardTerminal> terminals = factory.terminals().list();
	        System.out.println("Liste des lecteurs détectés : " + terminals);

			/* Choose the first reader */
	        if(terminals.size() > 0){
	        	MyReader = terminals.get(0);
	        	System.out.println("Connecté au premier lecteur.");
	        	
	        	tr = new Thread(new MyThread());
	    		tr.start();
	        }
	        else {
	        	System.out.println("Aucun lecteur détecté.");
	        }
		}  catch (CardException e) {
			System.out.println("Impossible de récupérer le premier lecteur.");
			e.printStackTrace();
		}
	}
	
	private class MyThread implements Runnable {
		
		@Override
		public void run() {
			CommandAPDU GetData = new CommandAPDU(ApduArray);
			while(true){
				try {
					System.out.println("Attente carte...");
					MyReader.waitForCardPresent(0);
					
					System.out.println("Carte détectée.");
					/* Connect to the card currently in the reader */
					Card card = MyReader.connect("*");
					/* Exchange APDUs with the card */
					CardChannel channel = card.getBasicChannel();
					ResponseAPDU CardApduResponse = channel.transmit(GetData);
					/* Disconnect */
					card.disconnect(true);
					
					System.out.println("Carte lue");
					String carduid = Concerto.getHexString(CardApduResponse.getData());
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					Concerto.this.updateJS("cardInserted", carduid);
					
					MyReader.waitForCardAbsent(0);
					/*while (MyReader.isCardPresent()) {
						System.out.println(MyReader.isCardPresent());
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							System.out.println("sleep error: " + e.getMessage());
						}
					}*/
					
					Concerto.this.updateJS("cardRemoved");
				} catch (CardException e) {
					System.out.println("Impossible de communiquer avec la carte.");
					e.printStackTrace();
				}
			}
		}
	}
} 
