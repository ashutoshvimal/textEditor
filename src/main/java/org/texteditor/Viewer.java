package org.texteditor;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.source.tree.ClassTree;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class Viewer {

    private static LibC.Termios originalAttributes;
    private static int rows = 5;
    private static int columns = 5;
    private static final int ARROW_UP = 1000
            ,ARROW_DOWN = 1001
            ,ARROW_LEFT = 1002,
            ARROW_RIGHT = 1003;

    private static int cursorX = 0, cursorY = 0, offSetY = 0 ;

    private static List<String> content = List.of();
//    private static BufferedReader content;

    public static void main(String[] args) throws IOException {

        openFile(args);
        enableRawMode();
        initEditor();

        while (true){
            refreshScreen();

            int key = readKey();

            handleKey(key);

        }
    }

    public static void openFile(String[] args){
        if(args.length >= 1){
            String filename = args[0];
            Path path = Path.of(filename);
            if(Files.exists(path)){
                try(InputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(path));
                    BufferedReader content = new BufferedReader(new InputStreamReader(inputStream))
                ){
//                    content = reader;
                }
                catch (IOException e){
                    System.exit(0);
                    System.out.println("\033[31m"+e);
                }
            }

        }
        else{
            System.out.println("\033[31mError: Please provide the file path as an argument.");
            System.exit(0);
        }
    }

    public static void refreshScreen(){
        StringBuilder stringBuffer = new StringBuilder();

        scroll();
        moveCursorToTopLeft(stringBuffer);
        drawContent(stringBuffer);
        drawStatusBar(stringBuffer);
        drawCursor(stringBuffer);

        System.out.print(stringBuffer);

    }

    public static void moveCursorToTopLeft(StringBuilder stringBuffer){
        stringBuffer.append("\033[H"); //moves cursor to home position (0, 0)
    }

//    public static boolean lastLine(){
//        String line;
//
//    }

    public static void drawContent(StringBuilder stringBuffer){
        for (int i = offSetY; i < rows+offSetY; i++) {
            if(i >= content.size()){
                stringBuffer.append("~");
            }
            else{
                stringBuffer.append(content.get(i));
            }
            stringBuffer.append("\033[K\r\n");
        }
    }

    public static void drawCursor(StringBuilder stringBuffer){
        stringBuffer.append(String.format("\033[%d;%dH", cursorY+1-offSetY, cursorX+1)); // moves cursor to line #, column #
    }

    public static String statusMessage;
    public static void drawStatusBar(StringBuilder stringBuffer){

//        stringBuffer.append("\033[47m\033[30m"); //for highlight
        String message = (statusMessage != null) ? statusMessage : String.format(String.format("X = %d, Y = %d ", cursorX, cursorY)
                + "Soba's Text Editor" + String.format("%d, %d", rows, columns));

        stringBuffer.append("\033[7m\033[K") //set inverse/reverse mode
                    .append(message)
                    .append("\033[0m");
    }

    private static void initEditor() {
        LibC.Winsize windowSize = getWindowSize();
        columns = windowSize.ws_col;
        rows = windowSize.ws_row - 1;
    }

    private static void scroll(){
        if(cursorY >= rows + offSetY){
            offSetY = cursorY - rows + 1;
        }
        else if(cursorY < offSetY){
            offSetY = cursorY;
        }
    }

    private static void handleKey(int key) {
        if(cursorY > 0 && key == ARROW_UP){
            cursorY--; // down is +ve
        }
        else if(cursorY <= Math.max(rows, content.size()) && key == ARROW_DOWN){
            cursorY++;
        }
        else if(cursorX <= columns && key == ARROW_RIGHT){
            cursorX++;
        }
        else if(cursorX > 0 && key == ARROW_LEFT){
            cursorX--;
        }

        if(key == ctrl('q')){
            System.out.print("\033[2J");
            System.out.print("\033[H");
            LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);
            System.exit(0);
        }

        if(key == ctrl('f')){
            find();
        }
    }

    public static int ctrl(int key){
        return key & 0x1f;
    }

    static int lastMatch = -1;
    public static void find(){
        try {
            prompt("Search word", (query, lastKeyPress) -> {
//                if(lastKeyPress == ARROW_DOWN || lastKeyPress == ARROW_RIGHT)
                int currentIdx = lastMatch;

                for(int i=0;i<content.size();i++){
                    currentIdx ++;
                    if(currentIdx == content.size()) currentIdx = 0;

                    String currentLine = content.get(currentIdx);
                    int match = currentLine.indexOf(query);

                    if(match != -1){
                        lastMatch = currentIdx;
                        cursorX = match; //doesnt work 100%
                        cursorY = currentIdx;
                        offSetY = content.size(); //greater than cursorY
                        break;
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void prompt(String message, BiConsumer<String, Integer> consumer)  throws IOException{
        statusMessage = message;

        StringBuilder userInput = new StringBuilder();

        while (true){
            try{
                statusMessage = (userInput.isEmpty()) ? statusMessage : userInput.toString();
                refreshScreen();
                int key = readKey();

                if(key == '\r'){
                    statusMessage = null;
                    return;
                }
                else if (key == 127) { // Check if the user pressed the backspace key
                    if (!userInput.isEmpty()) {
                        userInput.deleteCharAt(userInput.length() - 1); // Remove the last character
                    }
                }
                else if(!Character.isISOControl(key) && key < 128){
                    userInput.append((char)key);
                }
                consumer.accept(userInput.toString(), key);
            }
            catch (IOException e){
                throw new IOException(e);
            }



        }
    }


    public static int readKey() throws IOException{
        int key = System.in.read();
        if(key != '\033') return key;

        int nextKey = System.in.read();

        if(nextKey != '[') return nextKey;

        int nextNextKey = System.in.read();

        if(nextNextKey == 'A') return ARROW_UP;
        if(nextNextKey == 'B') return ARROW_DOWN;
        if(nextNextKey == 'C') return ARROW_RIGHT;
        if(nextNextKey == 'D') return ARROW_LEFT;

        return key;
    }



























    private static void enableRawMode() {
        LibC.Termios termios = new LibC.Termios();
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);

        if (rc != 0) {
            System.err.println("There was a problem calling tcgetattr");
            System.exit(rc);
        }

        originalAttributes = LibC.Termios.of(termios);

        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);

       /* termios.c_cc[LibC.VMIN] = 0;
        termios.c_cc[LibC.VTIME] = 1;*/

        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
    }


    private static LibC.Winsize getWindowSize() {
        final LibC.Winsize winsize = new LibC.Winsize();
        final int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winsize);

        if (rc != 0) {
            System.err.println("ioctl failed with return code[={}]" + rc);
            System.exit(1);
        }

        return winsize;
    }

}

interface LibC extends Library {

    int SYSTEM_OUT_FD = 0;
    int ISIG = 1, ICANON = 2, ECHO = 10, TCSAFLUSH = 2,
            IXON = 2000, ICRNL = 400, IEXTEN = 100000, OPOST = 1, VMIN = 6, VTIME = 5, TIOCGWINSZ = 0x5413;

    // we're loading the C standard library for POSIX systems
    LibC INSTANCE = Native.load("c", LibC.class);

    @Structure.FieldOrder(value = {"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class Winsize extends Structure {
        public short ws_row, ws_col, ws_xpixel, ws_ypixel;
    }



    @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure {
        public int c_iflag, c_oflag, c_cflag, c_lflag;

        public byte[] c_cc = new byte[19];

        public Termios() {
        }

        public static Termios of(Termios t) {
            Termios copy = new Termios();
            copy.c_iflag = t.c_iflag;
            copy.c_oflag = t.c_oflag;
            copy.c_cflag = t.c_cflag;
            copy.c_lflag = t.c_lflag;
            copy.c_cc = t.c_cc.clone();
            return copy;
        }

        @Override
        public String toString() {
            return "Termios{" +
                    "c_iflag=" + c_iflag +
                    ", c_oflag=" + c_oflag +
                    ", c_cflag=" + c_cflag +
                    ", c_lflag=" + c_lflag +
                    ", c_cc=" + Arrays.toString(c_cc) +
                    '}';
        }
    }


    int tcgetattr(int fd, Termios termios);

    int tcsetattr(int fd, int optional_actions,
                  Termios termios);

    int ioctl(int fd, int opt, Winsize winsize);

}

/**/