package org.texteditor;

import java.io.IOException;


//   the ESC [ (    written as \e[ or \033[   )
public class Views {
    public static void main(String[] args)throws IOException {
        makeTerminalRaw();

        while(true){
            int key = System.in.read();

            if(key == 'q') System.exit(0);

            System.out.print((char) key + " ("+ key +")\r\n");
        }
    }

    static void makeTerminalRaw() throws IOException{
        try{
            System.out.println("Please set your terminal to raw mode by typing 'stty raw' and press Enter.");
            System.out.println("Then run this program in the same terminal.");

            System.in.read();

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
