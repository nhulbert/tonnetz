/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tonnetz;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;

import java.awt.Color;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import static tonnetz.Graphics.intdiv;
import static tonnetz.Graphics.mod;

// the graphical representation of the note, the position controlled by damped harmonic motion

class MovingNote {
    private int pitch; // pitch number in half steps above middle C
    private int voice; // which voice
    
    private boolean moving; // is the pitch moving
    private boolean sharp; // is the accidental a sharp
    private boolean addaccid; // is there an accidental
    
    // corrections for the note's position based on the canvas size
    static public float absx=0;
    static public float absy=0;
    
    static public int canvas_width=0;
    static public int canvas_height=0;
    
    // current note physics data
    private float posx;
    private float posy;
    private float speedx;
    private float speedy;
    private float rotate;
    
    // target position towards which the note is drawn
    private float targx;
    private float targy;
    private float targr;
    
    private int i; // place in C major scale the pitch is (ignoring accidentals)
    private int o; // octave of pitch
	
    public MovingNote(MovingNote m) {
        this.pitch = m.pitch;
        this.voice = m.voice;
        this.moving = m.moving;
        this.sharp = m.sharp;
        this.addaccid = m.addaccid;
        this.posx = m.posx;
        this.posy = m.posy;
        this.speedx = m.speedx;
        this.speedy = m.speedy;
        this.rotate = m.rotate;
        this.targx = m.targx;
        this.targy = m.targy;
        this.targr = m.targr;
        this.i = m.i;
        this.o = m.o;
    }
    
    public MovingNote(int pitch, int voice, boolean moving, boolean sharp, float modx, float mody){
        this(pitch,voice,moving,sharp);
        targx += modx;
        targx += mody;
        posx = targx;
        posy = targy;
    }
    
    public MovingNote(int pitch, int voice, boolean moving, boolean sharp){
        this.pitch = pitch;
        this.voice = voice;
        
        i = ChordProgression.scaledict.get((byte)0).indexOf((byte)mod(pitch,12));
        o = intdiv(pitch,12);
        
        if (i == -1){
            addaccid = true;
            if(sharp){
                i = ChordProgression.scaledict.get((byte)0).indexOf((byte)mod(pitch-1,12));
            }
            
            else{
                i = ChordProgression.scaledict.get((byte)0).indexOf((byte)mod(pitch+1,12));
            }
        }
        else{
            addaccid = false;
        }
        
        targx = 160+((voice+1)%2)*(-13)+absx;
        
        targy = -48+10*(14.25f)+
                ((voice+1)%2)*(6*(14.25f)-2)+
                (1-(int)(voice/2))*(6+7*(14.25f))-
                (i+7*o)*(14.25f)+absy;
        
        targr = ((voice+1)%2)*180;
        
        posx = targx;
        posy = targy;
        rotate = targr;
        
        speedx = 0;
        speedy = 0;
        
        this.moving = moving;
        this.sharp = sharp;
    }
    
    public void draw(GL2 gl, TextRenderer textRenderer, Texture texture){
        
        if (!moving){
            Graphics.drawSprite(gl, texture, targx, targy, 1, targr);
            
            if (addaccid){ // draw accidental
                if (sharp){
                    Graphics.drawText(textRenderer, canvas_width, canvas_height, "\u266F", (int)(targx-((voice+1)%2)*(-13)-32), (int)(targy-((voice+1)%2)*(6*(14.25f)-2)+125), Color.BLACK);
                }
                else{
                    Graphics.drawText(textRenderer,canvas_width, canvas_height, "\u266D", (int)(targx-((voice+1)%2)*(-13)-32), (int)(targy-((voice+1)%2)*(6*(14.25f)-2)+118), Color.BLACK);
                }
            }
        }
        else{
            Graphics.drawSprite(gl, texture, posx, posy, 1, rotate);
            
            if (addaccid){ // draw accidental
                if (sharp){
                    Graphics.drawText(textRenderer, canvas_width, canvas_height, "\u266F", (int)(posx-((voice+1)%2)*(-13)-32), (int)(posy-((voice+1)%2)*(6*(14.25f)-2)+125), Color.BLACK);
                }
                else{
                    Graphics.drawText(textRenderer,canvas_width, canvas_height, "\u266D", (int)(posx-((voice+1)%2)*(-13)-32), (int)(posy-((voice+1)%2)*(6*(14.25f)-2)+118), Color.BLACK);
                }
            }
        }
        
        // draw ledger lines
        if (voice == 2 || voice == 3){
            if (i+7*o < 1){
                for (int h = 0; h >= i+7*o; h -= 2){
                    Graphics.drawText(textRenderer, canvas_width, canvas_height,"__", (int)(targx-((voice+1)%2)*(-13)-4), (int)(48+10*(14.25f)+(1-(int)(voice/2))*(6+7*(14.25f))-(h)*(14.25f)+100), Color.BLACK);
                }
            }
            else if (i+7*o > 11){
                for (int h = 12; h <= i+7*o; h += 2){
                    Graphics.drawText(textRenderer, canvas_width, canvas_height,"__", (int)(targx+((voice+1)%2)*(-13)-4), (int)(48+10*(14.25f)+(1-(int)(voice/2))*(6+7*(14.25f))-(h)*(14.25f)+100), Color.BLACK);                }
            }
        }
        else {
            if (i+7*o < -11){
                for (int h = -12; h >= i+7*o; h -= 2){
                    Graphics.drawText(textRenderer, canvas_width, canvas_height,"__", (int)(targx-((voice+1)%2)*(-13)-4), (int)(48+10*(14.25f)+(1-(int)(voice/2))*(6+7*(14.25f))-(h)*(14.25f)+100), Color.BLACK);
                }
            }
            else if (i+7*o > -1){
                for (int h = 0; h <= i+7*o; h += 2){
                    Graphics.drawText(textRenderer, canvas_width, canvas_height,"__", (int)(targx-((voice+1)%2)*(-13)-4), (int)(48+10*(14.25f)+(1-(int)(voice/2))*(6+7*(14.25f))-(h)*(14.25f)+100), Color.BLACK);
                }
            }
        }
    }
    
    // update the position based on the target position and damped harmonic motion
    public void update(){
        speedx += 0.01*(targx-posx)-0.14*speedx;
        speedy += 0.01*(targy-posy)-0.14*speedy;
        
        posx += speedx;
        posy += speedy;
    }
    
    public int getpitch(){
        return pitch;
    }
    
    public static ArrayList<Byte> ArrayToByteArray(ArrayList<MovingNote> in){
        if (in == null) return null;
        
        ArrayList<Byte> out = new ArrayList<>();
        for (MovingNote i : in){
            out.add((byte)i.getpitch());
        }
        
        return out;
    }
    
    public static ArrayList<MovingNote> ByteArrayToArray(ArrayList<Byte> in, boolean minor){
        ArrayList<MovingNote> out = new ArrayList<>();
        int h = 0;
        boolean sharp;
        int change=0;
        if (minor) change = 3;
        
        // determines whether the note has a sharp or flat based on the chord's root
        switch (mod((in.get(0)+change),12)){
            case 2:
                sharp = true;
                break;
            case 4:
                sharp = true;
                break;
            case 7:
                sharp = true;
                break;
            case 9:
                sharp = true;
                break;
            case 11:
                sharp = true;
                break;
            default:
                sharp = false;
        }
        
        for (Byte i : in){
            out.add(new MovingNote(i,h,true,sharp));
            h++;
        }
        
        return out;
    }
    
    static void set(ArrayList<ArrayList<MovingNote>> prev, int bufferpos, ArrayList<Byte> in, boolean minor) {
        
        boolean sharp;
        int change=0;
        if (minor) change = 3;

        // determines whether the note has a sharp or flat based on the chord's root
        switch (mod((in.get(0)+change),12)){
            case 2:
                sharp = true;
                break;
            case 4:
                sharp = true;
                break;
            case 7:
                sharp = true;
                break;
            case 9:
                sharp = true;
                break;
            case 11:
                sharp = true;
                break;
            default:
                sharp = false;
        }
        
        if (prev.get(bufferpos).isEmpty()){
            int h = 0;
            if (bufferpos == 0){
                for (Byte i : in){
                    prev.get(bufferpos).add(new MovingNote(i,h,true,sharp,bufferpos*100,0));
                    h++;
                }
            }
            else{
                for (Byte i : in){
                    prev.get(bufferpos).add(new MovingNote(prev.get(bufferpos-1).get(h)));
                    prev.get(bufferpos).get(h).setpitch(new MovingNote(i,h,true,sharp,bufferpos*100,0),0);
                    h++;
                }
            }
        }
        else{
            int h = 0;
            
            for (Byte i : in){
                prev.get(bufferpos).get(h).setpitch(new MovingNote(i,h,true,sharp,bufferpos*100,0),0);
                h++;
            }
        }
    }

    public void setpitch(MovingNote source, float xmod){
        this.pitch = source.pitch;
        this.voice = source.voice;
        
        this.i = source.i;
        this.o = source.o;
        
        this.addaccid = source.addaccid;
        this.sharp = source.sharp;
        
        this.targx = source.targx+xmod;
        this.targy = source.targy;
        this.targr = source.targr;
    }
}
