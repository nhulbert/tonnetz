package tonnetz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// A Class that represents a sequence of chords, with methods for realizing them into harmony

public class ChordProgression {
	// defines the pitch (not java) classes, chord types, and scale types
	
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
    
    static final Map<Byte, ArrayList<Byte>> chorddict;
    
    static{
        HashMap<Byte, ArrayList<Byte>> temp = new HashMap<>();
        temp.put(MAJOR, new ArrayList<>(Arrays.asList(C,E,G)));
        temp.put(MINOR, new ArrayList<>(Arrays.asList(C,EB,G)));
        temp.put(DIMINISHED, new ArrayList<>(Arrays.asList(C,EB,GB)));
        temp.put(MAJMIN, new ArrayList<>(Arrays.asList(C,E,G,BB)));
        temp.put(HALFDIM, new ArrayList<>(Arrays.asList(C,EB,GB,BB)));
        
        chorddict = Collections.unmodifiableMap(temp);
    }
    
    static final Map<Byte, ArrayList<Byte>> scaledict;
    
    static{
        HashMap<Byte, ArrayList<Byte>> temp = new HashMap<>();
        temp.put(MAJOR, new ArrayList<>(Arrays.asList(C,D,E,F,G,A,B)));
        
        scaledict = Collections.unmodifiableMap(temp);
    }
    
    ArrayList<Byte> roots;
    ArrayList<Byte> chords;
    ArrayList<Byte> invs;
    
    private Byte numvoices=4; //number of voices upon realization
    private Byte low=-19; //lowest note in half-steps above middle C
    private Byte high=19; // highest note in half-steps above middle C
    private Byte overlap=13; // the overlap in the ranges of each voice
    private float width; // the width of the range of each voice, dependent on the previous four variables
    
    private int offset = 0; // offset of the searchrealize method's index of the voiceleading
    
    private ArrayList<Byte> lowest; // holds the lowest possible notes for each voice
    
    private ArrayList<Rating> topten; // when searchrealizing, holds the top ten realizations found for the current chord
    
    ArrayList<ArrayList<Byte>> voiceleading; // holds the generated sonority list
    
    private ArrayList<Integer> ratings; // the cost function's results for the chord possibilities
    
    private ArrayList<String> flags; // the specific factors that ended up influencing the cost function
    
    public ChordProgression(ArrayList<Byte> roots, ArrayList<Byte> chords, ArrayList<Integer> invs){
        this.invs = new ArrayList<Byte>();
        
        this.roots = roots;
        this.chords = chords;
        for (int i = 0; i < invs.size(); i++){
            this.invs.add(((Integer)(invs.get(i))).byteValue());
        }
    }
    
    public ChordProgression(ArrayList<Integer> roots, ArrayList<Integer> chords, ArrayList<Integer> invs, boolean isInteger){
        this.roots = new ArrayList<>();
        this.chords = new ArrayList<>();
        this.invs = new ArrayList<>();
        
        for (int i = 0; i < roots.size() && i < chords.size() && i < invs.size(); i++){
            this.roots.add(((Integer)(roots.get(i))).byteValue());
            this.chords.add(((Integer)(chords.get(i))).byteValue());
            this.invs.add(((Integer)(invs.get(i))).byteValue());
        }
    }
    
    // adds a chord to a given sonority list and chord
    public static ArrayList<Byte> addchord(byte root, byte chord, int inv, ArrayList<Byte> prev){
        ChordProgression cp = new ChordProgression(new ArrayList<>(Arrays.asList(root)),new ArrayList<>(Arrays.asList(chord)),new ArrayList<>(Arrays.asList(inv)));
        ArrayList<ArrayList<Byte>> result = cp.searchrealize(prev);
        return result.get(result.size()-1);
    }
    
    // a basic realization into closed position voicings of the chords
    public ArrayList<ArrayList<Byte>> naiverealize(){
        voiceleading = new ArrayList<>();
        ArrayList<Byte> curchord;
        byte curroot;
        byte curinv;
        ArrayList<Byte> cursonority;
        
        for (int i = 0; i < roots.size(); i++){
            voiceleading.add(new ArrayList<Byte>());
            
            curchord = chorddict.get(chords.get(i));
            curroot = roots.get(i);
            curinv = invs.get(i);
            cursonority = voiceleading.get(i);
            
            for (int h = 0; h < curchord.size(); h++){
                cursonority.add(((Integer)(curchord.get(mod(h+curinv,curchord.size()))+intdiv(h+curinv,curchord.size())*12+curroot)).byteValue());
            }
        }
        
        return voiceleading;
    }
    
    // a recursive chord realization algorithm
    public ArrayList<ArrayList<Byte>> searchrealize(ArrayList<Byte> prev){        
        voiceleading = new ArrayList<>();
        ratings = new ArrayList<>();
        flags = new ArrayList<String>();
        offset = 0;
        
        // if there is a previous sonority specified from which the realization must follow, add that chord and increment the offset
        if (prev != null){
            voiceleading.add(prev);
            offset = 1;
        }
        
        // compute the voice ranges
        lowest = new ArrayList<>();
        
        width = (((float)high-(float)low+1)+((float)numvoices-1)*(float)overlap)/(float)numvoices;
        for (int i = 0; i < numvoices-1; i++){
            lowest.add(((Float)(low+i*(width-overlap))).byteValue());
        }
        lowest.add(((Float)(high-(width)+1)).byteValue());
        
        // perform the search
        int h; // number of equally good best choices-1
        int choice;
        for (int i = offset; i-offset < roots.size(); i++){
            topten = new ArrayList<>();
            search(new ArrayList<Byte>(), i); // the recursive part, with depth equivalent to the number of voices
            
            //record the top ten sonorities' statistics
            h=0;
            while(h < 9 && topten.get(0).rating == topten.get(h+1).rating) h++;
            choice = (int)(Math.random()*(h+1));
            voiceleading.add(topten.get(choice).sonority);
            ratings.add(topten.get(choice).rating);
            flags.add(topten.get(choice).flags);
        }
        
        return voiceleading;
    }
    
    // recursive part of the searchrealize method
    private void search(ArrayList<Byte> current, int pos){
        int searchcount = current.size(); // number of vertical notes in the sonority previously found
        int min = lowest.get(searchcount); // lowest note for the current voice
        int max; // highest note for the current voice
        if (searchcount != numvoices-1) {
            max = (int)(lowest.get(searchcount)+width-1);
        }
        else{
            max = high;
        }
        
        // adds pitches to sonority that would serve to complete the chord, then searches again for the next pitch
        for (int i = min; i <= max; i++){
            if (searchcount == 0) {
                if (mod(i,12) == mod(roots.get(pos-offset)+chorddict.get(chords.get(pos-offset)).get(mod(invs.get(pos-offset),chorddict.get(chords.get(pos-offset)).size())),12)){
                    current.add((byte)i);
                    search(current,pos);
                    current.remove(searchcount);
                }
            }
            else{
                if (searchcount != numvoices-1){
                    if (inchord(i,roots.get(pos-offset),chords.get(pos-offset))){
                        current.add((byte)i);
                        search(current,pos);
                        current.remove(searchcount);
                    }
                }
                else{
                    if (inchord(i,roots.get(pos-offset),chords.get(pos-offset))){
                        current.add((byte)i);
                        rateadd(current,pos);
                        current.remove(searchcount);
                    }
                }
            }
        }
    }
    
    // extends mod to negative integers consistently
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
    
    // returns whether a given pitch is in the given chord
    static public boolean inchord(int pitch, byte root, byte chord){
        ArrayList<Byte> pitches = new ArrayList<>(chorddict.get(chord));
        for (Byte b: pitches){
            if (mod(pitch,12) == mod((b+root),12)) {
                return true;
            }
        }
        return false;    
    }
    
    /*public void ratetest(ArrayList<Byte> current, ArrayList<Byte> prev, ArrayList<Byte> roots, ArrayList<Byte> chords, ArrayList<Byte> invs){
        voiceleading = new ArrayList<>();
        voiceleading.add(prev);
        
        this.roots = roots;
        this.chords = chords;
        this.invs = invs;
        
        rateadd(current, 1);
    }*/
    
    // the cost function, rates a possible next sonority and adds it to the top ten if it qualifies
    private void rateadd(ArrayList<Byte> current, int pos) {
        String flags = "[";
        int rating = 0; // accumulates the rating
        int diff1; // difference between adjacent voices
        
        //checks for voice leading weaknesses and strengths and rates them accordingly
        //the flag strings describe what is being checked
        for (int i = 0; i<numvoices-1; i++){
                diff1 = current.get(i+1)-current.get(i);
            if (diff1 < 0){
                rating -= 5;
                flags = flags.concat("direct crossing, ");
            }
            if (i != 0 && diff1 > 12) // only punish excessive distance if it doesn't occur in the lowest pair of voices
            {
                rating -= 5;
                flags = flags.concat("voice distance, ");
            }

            if (pos > 0){ 
                if ((current.get(i) > voiceleading.get(pos-1).get(i+1) ||
                        current.get(i+1) < voiceleading.get(pos-1).get(i)))
                {
                    rating -= 5;
                    flags = flags.concat("indirect crossing, ");
                }
            }
        }
        
        if (pos-offset > 0){
            for (int i = 0; i<numvoices; i++){

                    if (chorddict.get(chords.get(pos-1-offset)).size()>3 &&
                        mod(voiceleading.get(pos-1).get(i),12) == mod(chorddict.get(chords.get(pos-1)).get(3)+roots.get(pos-1-offset),12) &&
                            current.get(i) > voiceleading.get(pos-1).get(i))
                    {
                        rating -= 10;
                        flags = flags.concat("unresolved 7th, ");
                    }
            }
        }
        
        if (pos > 0){
            int diff2;
            int line1;
            int line2;
            for (int i = 0; i < numvoices-1; i++){
                for (int h = i+1; h < numvoices; h++){
                    diff1 = current.get(h)-current.get(i);
                    diff2 = voiceleading.get(pos-1).get(h)-voiceleading.get(pos-1).get(i);
                    line1 = current.get(h)-voiceleading.get(pos-1).get(h);
                    line2 = current.get(i)-voiceleading.get(pos-1).get(i);
                    if (diff2 == diff1 && current.get(h) != voiceleading.get(pos-1).get(h) && (mod(diff1, 12) == 0 || mod(diff1, 12) == 7)){
                        rating -= 20;
                        flags = flags.concat("parallel perfects, ");
                    }
                    
                    if ((mod(diff1,12) == 7 || mod(diff1,12) == 12)){
                            if(!(Math.abs(line1) == 1 || line1 == 0)){ 
                            if((Math.signum(line1) == Math.signum(line2))){
                                rating -= 5;
                                flags = flags.concat("hidden perfects, ");
                            }
                    }
                    }
                }
            }
        }
        int rs=0;
        int ts=0;
        int fs=0;
        int ss=0;
        for (int i = 0; i<numvoices; i++){
            if (mod(current.get(i),12) == mod(chorddict.get(chords.get(pos-offset)).get(0)+roots.get(pos-offset),12))
                rs++;
            else if (mod(current.get(i),12) == mod(chorddict.get(chords.get(pos-offset)).get(1)+roots.get(pos-offset),12))
                ts++;
            else if (mod(current.get(i),12) == mod(chorddict.get(chords.get(pos-offset)).get(2)+roots.get(pos-offset),12))
                fs++;
            else if (chorddict.get(chords.get(pos-offset)).size() > 3 && mod(current.get(i),12) == mod (chorddict.get(chords.get(pos-offset)).get(3)+roots.get(pos-offset),12))
                ss++;
        }
        
        if (rs < 1){
            rating -= 10;
            flags = flags.concat("chord members, ");
        }
        if (ts != 1){
            rating -= 10;
            flags = flags.concat("chord members, ");
        }
        if (fs < 1){
            rating -= 3;
            flags = flags.concat("no fifth, ");
        }
        else if (fs > 1){
            rating -= 10;
            flags = flags.concat("chord members, ");
        }
        if (chorddict.get(chords.get(pos-offset)).size() > 3 && (ss > 1 || ss < 1)){
            rating -= 10;
            flags = flags.concat("chord members (7th), ");
        }
        
        // rates each voice's conjunctiveness
        if (pos > 0){
            for (int i = 0; i<numvoices; i++){
                int interval = current.get(i)-voiceleading.get(pos-1).get(i);
                switch (Math.abs(interval)){
                    case 6:
                        rating -= 5;
                        flags = flags.concat("tritone leap, ");
                        break;
                    case 0:
                        rating += 2;
                        break;
                    case 1:
                        rating += 2;
                        break;
                    case 2:
                        rating += 1;
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    default:
                        rating -= 2;
                        if (i != 0) flags = flags.concat("large leap, ");
                }
            }
        }
        
        flags = flags.concat("]");
        
        // updates the top ten
        int i = 0;
        while (i < topten.size() && topten.get(i).rating >= rating) i++;
        if (i != 10){
            topten.add(i, new Rating(current, rating, flags));
        }
        if (topten.size() == 11) topten.remove(10);
    }
    
    // a sonority with its rating and flags
    private class Rating{
        ArrayList<Byte> sonority;
        Integer rating;
        String flags;
        
        public Rating(ArrayList<Byte> sonority, Integer rating, String flags){
            this.sonority = new ArrayList<>(sonority);
            this.rating = rating;
            this.flags = flags;
        }
    }
}
