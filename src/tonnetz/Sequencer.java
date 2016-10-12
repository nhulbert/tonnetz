/*8
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tonnetz;

import java.util.ArrayList;
import java.util.Arrays;

// converts the sonorities into sound

public class Sequencer {
    private SoundPlayer soundplayer;
    private Playertask pl;
    private boolean toclose = false;
    
    public Sequencer(){
        soundplayer = new SoundPlayer();
    }
    
    public void playpitch(int i, int length){        
        if (pl != null) pl.interrupt();
        pl = new Playertask((byte)i, length);
        pl.start();
    }
    
    public void playpitches(ArrayList<Byte> pitches, int length){       
        if (pl != null) pl.interrupt();
        pl = new Playertask(new ArrayList<>(Arrays.asList(pitches)), length);
        pl.start();
    }
    
    public void playmusic(ArrayList<ArrayList<Byte>> pitches, int length){
        if (pl != null) pl.interrupt();
        pl = new Playertask(pitches, length);
        pl.start();
    }
    
    // the task that plays the pitches
    private class Playertask extends Thread {
        ArrayList<ArrayList<Byte>> pitches;
        int length;

        public Playertask(ArrayList<ArrayList<Byte>> pitches, int length){
            this.pitches = pitches;
            this.length = length;
        }

        public Playertask(Byte pitch, int length){
            pitches = new ArrayList<ArrayList<Byte>>(Arrays.asList(new ArrayList<Byte>(Arrays.asList((byte)4))));
            this.length = length;
        }
        
        public void run(){
            ArrayList<String> nm = new ArrayList<>();
            for (ArrayList<Byte> chord : pitches){
                nm.clear();
                for (Byte pitch : chord){
                    nm.add("Note"+Integer.toString(pitch));
                    if (!Thread.interrupted()){
                        if (!soundplayer.isloaded(nm.get(nm.size()-1))){
                            soundplayer.load(nm.get(nm.size()-1),"Note1", true);
                            soundplayer.setPitch(nm.get(nm.size()-1), (float)(Math.pow(2, (-9f+(float)pitch)/12f))); // shift the sound to the correct pitch
                        }
                    }
                    else{
                        soundplayer.stopall();
                        return;
                    }
                }
                if (!Thread.interrupted()){
                    for (String pitchstr : nm){
                        soundplayer.play(pitchstr);
                    }
                }
                else{
                    soundplayer.stopall();
                    return;
                }
                try {
                    sleep(length);
                } catch (InterruptedException ex) {
                    soundplayer.stopall();
                    return;
                }
                soundplayer.stopall();
            }
            if (toclose) soundplayer.cleanUp();
        }
    }
    
    public void closewhendone(){
        if (pl.isAlive()) toclose = true;
        else cleanup();
    }
    
    public void cleanup(){
        soundplayer.cleanUp();
    }
}
