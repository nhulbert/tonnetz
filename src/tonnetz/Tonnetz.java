/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tonnetz;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
/**
 *
 * @author neil
 */
public class Tonnetz {
    static final byte C = 0;
    static final byte CS = 1;
    static final byte DB = 1;
    static final byte D = 2;
    static final byte DS = 3;
    static final byte EB = 3;
    static final byte E = 4;
    static final byte F = 5;
    static final byte FS = 6;
    static final byte GB = 6;
    static final byte G = 7;
    static final byte GS = 8;
    static final byte AB = 8;
    static final byte A = 9;
    static final byte AS = 10;
    static final byte BB = 10;
    static final byte B = 11;
    
    static final byte MAJOR = 0;
    static final byte MINOR = 1;
    static final byte DIMINISHED = 2;
    static final byte MAJMIN = 3;
    static final byte MINMIN = 4;
    static final byte HALFDIM = 5;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        new Graphics().start(); // starts the interactive tonnetz
        
        /*ArrayList<Byte> roots = new ArrayList<>(Arrays.asList(DB,DB,BB,BB,EB,C,F,EB,EB,AB,DB));
        ArrayList<Byte> chords = new ArrayList<>(Arrays.asList(MAJOR,MAJMIN,MAJOR,MAJMIN,MAJOR,MINOR,HALFDIM,MAJOR,MINOR,MAJMIN,MAJOR));
        ArrayList<Integer> invs = new ArrayList<>(Arrays.asList(0,-1,0,1,0,0,1,0,-1,0,0));
        ChordProgression cp = new ChordProgression(roots, chords, invs);
        ArrayList<ArrayList<Byte>> voiceleading = cp.searchrealize();
        
        Sequencer seq = new Sequencer();
        seq.playmusic(voiceleading, 1000);
        
        System.out.print(voiceleading+"\n"+cp.ratings+"\n"+cp.flags);
        
        
        writetoly(voiceleading, "test.ly");
        
        seq.closewhendone();
        

        
        /*cp.ratetest(new ArrayList<Byte>(Arrays.asList((byte)-5,(byte)-2,(byte)7,(byte)14)),
                new ArrayList<Byte>(Arrays.asList((byte)-10,(byte)-3,(byte)6,(byte)12)),
                new ArrayList<Byte>(Arrays.asList(D,G)),
                new ArrayList<Byte>(Arrays.asList(MAJMIN,MINOR)),
                new ArrayList<Byte>(Arrays.asList((byte)0,(byte)0)));*/
    }
    
    // output a voiceleading to the LilyPond file format for graphical rendering
    @SuppressWarnings("unused")
	private static void writetoly(ArrayList<ArrayList<Byte>> vl, String filename){
        final String bp1 = "\\version \"2.14.0\"\n" +
            "\n" +
            "\\header{\n" +
            "  title = \"Computer Generated Voice Leading\"\n" +
            "}\n" +
            "\n" +
            "upper = {\n" +
            "	\\clef treble";
        final String bp3 = "}\n" +
            "\n" +
            "lower = {\n" +
            "	\\clef bass";
        final String bp5 = "}\n" +
            "\n" +
            "\\score {\n" +
            "  \\new PianoStaff <<\n" +
            "     \\new Staff = \"upper\" \\upper\n" +
            "     \\new Staff = \"lower\" \\lower\n" +
            "  >>\n" +
            "  \\layout { }\n" +
            "  \\midi { }\n" +
            "}";
        
     // write the pitches
        ArrayList<String> sb = new ArrayList<>();
        int pitch;
        for (int i = 0; i < vl.get(0).size(); i++){
            sb.add("");
            for (int h = 0; h < vl.size(); h++){
                pitch = vl.get(h).get(i);
                
                sb.set(i, sb.get(i)+pitchtolystring(pitch)+" ");
            }
        }
        
        String bp2 = "<<{"+sb.get(3)+"} \\\\ {"+sb.get(2)+"}>>";
        String bp4 = "<<{"+sb.get(1)+"} \\\\ {"+sb.get(0)+"}>>";
        
        stringtofile(bp1+bp2+bp3+bp4+bp5, filename);
    }
    
    // converts a short string in memory to a file on the disk
    private static void stringtofile(String s, String filename){
        Writer writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                  new FileOutputStream(filename), "utf-8"));
            writer.write(s);
        } catch (IOException ex){
        } finally {
           try {writer.close();} catch (Exception ex) {}
        }
    }
    
    // converts a pitch into its LilyPond equivalent
    private static String pitchtolystring(int pitch){
        String out;
        
        switch(mod(pitch,12)){
            case 0:
                out = "c";
                break;
            case 1:
                out = "cis";
                break;
            case 2:
                out = "d";
                break;
            case 3:
                out = "ees";
                break;
            case 4:
                out = "e";
                break;
            case 5:
                out = "f";
                break;
            case 6:
                out = "fis";
                break;
            case 7:
                out = "g";
                break;
            case 8:
                out = "aes";
                break;
            case 9:
                out = "a";
                break;
            case 10:
                out = "bes";
                break;
            default:
                out = "b";
        }
        
        int octave = intdiv(pitch,12)+1;
        String octstring = "";
        if (pitch < 0){
            for (int i = 0; i < Math.abs(octave); i++){
                octstring += ",";
            }
        }
        else {
            for (int i = 0; i < octave; i++){
                octstring += "'";
            }
        }
        
        return out+octstring;
    }
    

    // extends modulo and integer consistently to negative ints, % does it in a way not particularly useful
    static public int mod(int input, int mod){
        while (input > mod-1) input -= mod;
        while (input < 0) input += mod;
        return input;
    }
    
    static public int intdiv(int num, int den){
        if (num == 0) return 0;
        else if (Math.signum(num) == Math.signum(den)){
            return (num/den);
        }
        
        return ((num+1)/den-1);
    }
}
