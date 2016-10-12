package tonnetz;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2.GL_QUADS;
import javax.media.opengl.*;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import javax.media.opengl.glu.GLU;
 
public class Graphics implements GLEventListener, KeyListener, MouseListener {   
   private static String TITLE = "Euler's Tonnetz";  // window's title
   private int canvas_width;  // width of the drawable
   private int canvas_height; // height of the drawable
   private static final int LINE_WIDTH = 5;
   private static final int FPS = 60; // target frames per second
   private static final float SIN60 = 0.86602540f;
   private static final float TAN30 = 0.57735026f;
   private static final float HEXSIZE = 100f; // the edge length of the hexagons
   
   private TextRenderer textRenderer; // normal text renderer
   private TextRenderer smalltext; // small text renderer
   
   // keeps track of the mouse position
   private int mouseposx=0; 
   private int mouseposy=0;
   
   // keeps track of the previous grid position
   private int gridx=0;
   private int gridy=0;
   private boolean gridup=false;
   
   private Sequencer seq=new Sequencer(); // Converts the sonorities into sound
   
   private HashSet<OrdIntPair> litup=new HashSet<>(); // the hexagons that are selected
   
   private ArrayList<ArrayList<MovingNote>> prev = new ArrayList<>(); // the previous sonorities
   
   private GLWindow window; // OpenGL window object

   // textures
   private Texture note;
   private Texture staff;
   
   private int cooldown = 0; // wait counter for changing chords
   private static final int COOLDOWN_WAIT = 20;
   
   // on OpenGL start
   public void start(){
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        window = GLWindow.create(caps);
        final FPSAnimator animator = new FPSAnimator(window, FPS, true);
        
        window.addWindowListener(new WindowAdapter() {
           @Override
           public void windowDestroyNotify(WindowEvent arg0) {
              // Use a dedicated thread to run the stop() to ensure that the
              // animator stops before program exits.
              new Thread() {
                 @Override
                 public void run() {
                    animator.stop(); // stop the animator loop
                    System.exit(0);
                 }
              }.start();
           };
        });
        
        canvas_width = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
        canvas_height = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
        
        window.addGLEventListener(this);
        window.setSize(canvas_width, canvas_height);
        window.setFullscreen(true);
        window.setTitle(TITLE);
        window.setVisible(true);
        window.addMouseListener(this);
        window.addKeyListener(this);
        
        animator.start();
   }
 
   private GLU glu;  // for the GL Utility
 
   @Override
   public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
        gl.glClearColor(0.1f, 0f, 0.9f, 0.0f);
        glu.gluOrtho2D(0, canvas_width, canvas_height, 0);
       
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, canvas_width, canvas_height, 0);
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
        
        textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 50));
        smalltext = new TextRenderer(new Font("SansSerif", Font.PLAIN, 10));
        
        try {
            note = TextureIO.newTexture(this.getClass().getResourceAsStream("Textures/NoteHead.png"), false, ".png");
            staff = TextureIO.newTexture(this.getClass().getResourceAsStream("Textures/staff.png"), false, ".png");
        } catch (IOException ex) {
            Logger.getLogger(Graphics.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        MovingNote.canvas_width = canvas_width;
        MovingNote.canvas_height = canvas_height;
        MovingNote.absx = canvas_width-550f;
        MovingNote.absy = 100f;
   }
 
   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {   }
   
   @Override
   public void display(GLAutoDrawable drawable) {
      render(drawable);
      update();
   }
   
    private void render(GLAutoDrawable drawable){
       GL2 gl = drawable.getGL().getGL2();
       gl.glClear(GL.GL_COLOR_BUFFER_BIT);
       gl.glLoadIdentity();
       gl.glColor3f(1, 1, 1);

       gl.glEnable(GL_MULTISAMPLE);

       // draw the hexes and their pitches
        int i = -1;
        int g;
        for (float h = -10-1.5f*HEXSIZE; h < canvas_height; h += 1.5f*HEXSIZE){
            g=-(intdiv(i,2))-1;
            for (float w = 10+(mod(i,2)-3)*SIN60*HEXSIZE; w < canvas_width; w += SIN60*2*HEXSIZE){
               drawHex(gl,w,h,HEXSIZE);
               if (litup.contains(new OrdIntPair(g,i))){
                   drawFilledHex(gl,w,h,HEXSIZE);
                   drawText(textRenderer,canvas_width, canvas_height,pitchtostring((i+1)*4+g*7),(int)(SIN60*HEXSIZE+w+0.5)-15,(int)(HEXSIZE+h+0.5)+10,Color.WHITE);
               }
               else{
                   drawText(textRenderer,canvas_width, canvas_height,pitchtostring((i+1)*4+g*7),(int)(SIN60*HEXSIZE+w+0.5)-15,(int)(HEXSIZE+h+0.5)+10,Color.BLACK);
               }
               g++;
            }
            i++;
        }
        
        // draw the staff
        drawSprite(gl, staff, canvas_width-550, 0, 0.5f, 0);
        //drawSprite(gl, note, canvas_width-400, -48+4*(14.25f), 1, 180);
        
        // draw the chords
        if (!prev.isEmpty()){
            ArrayList<MovingNote> chord;
            for (int h = 0; h<prev.size(); h++){
                chord = prev.get(h);
                for (i = 0; i < 4; i++){
                    chord.get(i).draw(gl, textRenderer, note);
                }
            }
        }
        
        drawText(textRenderer, canvas_width, canvas_height, "current chord", canvas_width-400, 50, Color.BLACK);
        {
            gl.glLoadIdentity();
            
            int h = 0;
            if (!prev.isEmpty()) h = prev.size();
            gl.glBegin(GL_LINES);
                gl.glVertex2f(canvas_width-300+h*40, 70);
                gl.glVertex2f(canvas_width-440+h*90,130);
            gl.glEnd();
        }
        
        //new MovingNote(0,0,false,false).draw(gl, textRenderer, note);
        
        drawText(smalltext, canvas_width, canvas_height, "Written by Neil Hulbert", 0, canvas_height-20, Color.WHITE);
        
        gl.glDisable(GL_MULTISAMPLE);
    }
    
    public static void drawSprite(GL2 gl, Texture texture, float posx, float posy, float scale, float angle){
        TextureCoords textureCoords = texture.getImageTexCoords();
        gl.glColor3f(1, 1, 1);
        
        gl.glLoadIdentity();
        gl.glTranslatef(posx+scale*texture.getImageWidth()/2, posy+scale*texture.getImageHeight()/2, 0);
        gl.glRotatef(angle, 0, 0, 1f);
        gl.glTranslatef(-scale*texture.getImageWidth()/2, -scale*texture.getImageHeight()/2, 0);
        
        gl.glEnable(GL_TEXTURE_2D);
        texture.enable(gl);
        texture.bind(gl);
        gl.glBegin(GL_QUADS);
        gl.glTexCoord2f(textureCoords.left(), textureCoords.top());
        gl.glVertex2f(0, 0);
        gl.glTexCoord2f(textureCoords.left(), textureCoords.bottom());
        gl.glVertex2f(0, scale*(texture.getImageHeight()));
        gl.glTexCoord2f(textureCoords.right(), textureCoords.bottom());
        gl.glVertex2f(scale*texture.getImageWidth(), scale*texture.getImageHeight());
        gl.glTexCoord2f(textureCoords.right(), textureCoords.top());
        gl.glVertex2f(scale*texture.getImageWidth(), 0);
        gl.glEnd();
        gl.glDisable(GL_TEXTURE_2D);
    }
    
    public static void drawText(TextRenderer textRenderer, int canvas_width, int canvas_height, String msg, int x, int y, Color c){
        textRenderer.beginRendering(canvas_width, canvas_height, true);
        textRenderer.setColor(c);

        textRenderer.draw(msg, x, canvas_height-y);

        textRenderer.endRendering();
    }
   
    public void drawHex(GL2 gl, float x, float y, float sidelength){
        gl.glColor3f(0,0,0);
        gl.glLoadIdentity();
        gl.glTranslatef(x,y,0);
        gl.glScalef(sidelength, sidelength, 1);
        gl.glLineWidth(LINE_WIDTH);
        float addlen = TAN30*(LINE_WIDTH/sidelength)*0.5f;
        
        gl.glBegin(GL2.GL_LINES);
            gl.glVertex2f(0-(addlen*SIN60),0.5f+(addlen*0.5f));
            gl.glVertex2f(SIN60+(addlen*SIN60),0-(addlen*0.5f));
            gl.glVertex2f(SIN60-(addlen*SIN60),0-(addlen*0.5f));
            gl.glVertex2f(2*SIN60+(addlen*SIN60),0.5f+(addlen*0.5f));
            gl.glVertex2f(2*SIN60,0.5f-addlen);
            gl.glVertex2f(2*SIN60,1.5f+addlen);
            gl.glVertex2f(2*SIN60+(addlen*SIN60),1.5f-(addlen*0.5f));
            gl.glVertex2f(SIN60-(addlen*SIN60),2f+(addlen*0.5f));
            gl.glVertex2f(SIN60+(addlen*SIN60),2f+(addlen*0.5f));
            gl.glVertex2f(0-(addlen*SIN60),1.5f-(addlen*0.5f));
            gl.glVertex2f(0,1.5f+addlen);
            gl.glVertex2f(0,0.5f-addlen);        
        gl.glEnd();
    }
    
    public void drawFilledHex(GL2 gl, float x, float y, float sidelength){
        gl.glColor3f(0f,0f,0f);
        gl.glLoadIdentity();
        gl.glTranslatef(x,y,0);
        gl.glScalef(sidelength, sidelength, 1);
        
        gl.glBegin(GL2.GL_POLYGON);
            gl.glVertex2f(0,0.5f);
            gl.glVertex2f(SIN60,0);
            gl.glVertex2f(2*SIN60,0.5f);
            gl.glVertex2f(2*SIN60,1.5f);
            gl.glVertex2f(SIN60,2f);
            gl.glVertex2f(0,1.5f);        
        gl.glEnd();
    }
    
    // updates the objects every frame
    private void update() {
        if (!prev.isEmpty()){
            for (ArrayList<MovingNote> i : prev){
                for (MovingNote h :  i){
                    h.update();
                }
            }
        }
        // calculates to which vertex the mouse is closest
        float newx = (mouseposx-10)/HEXSIZE;
        float newy = (mouseposy+10)/HEXSIZE-1;
        newx = (newx*(SIN60*2/3)-newy/3);
        newy = (newy*2/3);
        
        int x = (int)Math.floor(newx);
        int y = (int)Math.floor(newy);
        boolean up = false;
        
        // add the lit up hexes to the lit up set
        litup.clear();
        litup.add(new OrdIntPair(x,y+1));
        litup.add(new OrdIntPair(x+1,y));
        if (mod(newx,1)+(mod(newy,1))-1 > 0){
            litup.add(new OrdIntPair(x+1,y+1));
        }
        else{
            litup.add(new OrdIntPair(x,y));
            up = true;
        }
        
        if (cooldown != 0) cooldown++;
        
        // change chords if a new vertex is detected
        if (x != gridx || y != gridy || up != gridup){
        	cooldown = 1;
        }
        
        if (cooldown == COOLDOWN_WAIT){
            cooldown = 0;
        	
        	ArrayList<MovingNote> preceding=null;
            int bufferpos;
            
            if (prev.size()<4) prev.add(new ArrayList<MovingNote>());
            else{
                for (int i = 0; i < prev.size()-1; i++){
                    for (int h =0; h < prev.get(0).size(); h++){
                        prev.get(i).get(h).setpitch(prev.get(i+1).get(h),-100);
                    }
                }
            }
            
            if (prev.size() > 1){
                preceding = prev.get(prev.size()-2);    
            }
            
            bufferpos = prev.size()-1;
            
            if (up){
                MovingNote.set(prev,bufferpos, ChordProgression.addchord((byte)mod((y+4)*4+x*7,12), ChordProgression.MAJOR, 0, MovingNote.ArrayToByteArray(preceding)),false);
                seq.playpitches(MovingNote.ArrayToByteArray(prev.get(bufferpos)), 5000);
            }
            else {
                MovingNote.set(prev,bufferpos, ChordProgression.addchord((byte)mod((y+5)*4+x*7,12), ChordProgression.MINOR, 0, MovingNote.ArrayToByteArray(preceding)),true);
                seq.playpitches(MovingNote.ArrayToByteArray(prev.get(bufferpos)), 5000);
            }
        }
        
        gridx = x;
        gridy = y;
        gridup = up;
    }
 
    @Override
    public void dispose(GLAutoDrawable drawable) {
        seq.cleanup();
    }

    public GLU getGlu() {
        return glu;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
            new Thread() {
               @Override
               public void run() {
                  window.destroy();
               }
            }.start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseposx = e.getX();
        mouseposy = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseposx = e.getX();
        mouseposy = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
    }
    
    // ordered integer pair
   class OrdIntPair{
        @Override
        public int hashCode() {
            int hash = mod((this.y)*4+this.x*7,12);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OrdIntPair other = (OrdIntPair) obj;
            if (mod((this.y)*4+this.x*7,12) != mod((other.y)*4+other.x*7,12)) {
                return false;
            }
            return true;
        }
       int x;
       int y;
       
       public OrdIntPair(int x, int y){
           this.x = x;
           this.y = y;
       }
   }
    
    private static String pitchtostring(int pitch){
        String out;
        
        switch(mod(pitch,12)){
            case 0:
                out = "C";
                break;
            case 1:
                out = "C#";
                break;
            case 2:
                out = "D";
                break;
            case 3:
                out = "Eb";
                break;
            case 4:
                out = "E";
                break;
            case 5:
                out = "F";
                break;
            case 6:
                out = "F#";
                break;
            case 7:
                out = "G";
                break;
            case 8:
                out = "Ab";
                break;
            case 9:
                out = "A";
                break;
            case 10:
                out = "Bb";
                break;
            default:
                out = "B";
        }
        
        return out;
    }
    
    static public float mod(float input, float mod){
        while (input >= mod) input -= mod;
        while (input < 0) input += mod;
        return input;
    }
    
    static public int mod(int input, int mod){
        while (input >= mod) input -= mod;
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