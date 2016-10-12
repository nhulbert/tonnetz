package tonnetz;

import java.util.HashMap;
import com.jogamp.openal.*;
import com.jogamp.openal.util.ALut;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Set;

// plays each individual pitch with OpenAL

public class SoundPlayer {
	// the maps for buffers and sources
    private HashMap<String, int[]> buffersMap;
    private HashMap<String, int[]> sourcesMap;
    
    // the main al object
    private AL al;
    
    private float xLis, yLis, zLis;
    private float[] oriLis;
    
    private final static String SOUND_DIR = "Sounds/";
    
    public SoundPlayer(){
        buffersMap = new HashMap<String, int[]>();
        sourcesMap = new HashMap<String, int[]>();
        
        initOpenAL();
        initListener();
    }
    
    private void initOpenAL(){
        try {
            ALut.alutInit();
            al = ALFactory.getAL();
            al.alGetError();
        }
        catch (ALException e){
            System.out.print("Sound Problems");
        }
        
        al.alDistanceModel(AL.AL_LINEAR_DISTANCE_CLAMPED);
    }
    
    // initialize the virtual "microphone" with a position and a velocity  
    private void initListener(){
        xLis = 640f; yLis = 480f; zLis = -100f;
        al.alListener3f(AL.AL_POSITION, xLis, yLis, zLis);
        al.alListener3i(AL.AL_VELOCITY, 0, 0, 0);
        oriLis = new float[] {0f, 0f, 1f, 0f, -1f, 0f};
        al.alListenerfv(AL.AL_ORIENTATION, oriLis, 0);
    }
    
    public void cleanUp(){
        Set<String> keys = sourcesMap.keySet();
        Iterator<String> iter = keys.iterator();
        String nm;
        int[] buffer, source;
        while(iter.hasNext()){
            nm = iter.next();
            source = sourcesMap.get(nm);
            System.out.println("Stopping " + nm);
            al.alSourceStop(source[0]);
            al.alDeleteSources(1, source, 0);

        }
        keys = buffersMap.keySet();
        iter = keys.iterator();
        while(iter.hasNext()){
            nm = iter.next();
            buffer = buffersMap.get(nm);
            al.alDeleteBuffers(1, buffer, 0);
        }
        ALut.alutExit();
    }
    
    public boolean isloaded(String nm){
        return (sourcesMap.get(nm) != null);
    }
    
    public boolean load(String nm, String file, boolean toLoop){
        int[] buffer = buffersMap.get(file);
        if (buffer == null)
            buffer = initBuffer(file);
        if (buffer == null) return false;
        
        int[] source = initSource(nm, buffer, toLoop);
        if (source == null) {
            al.alDeleteBuffers(1, buffer, 0);
            return false;
        }
        if (toLoop)
            System.out.println("Looping source created for " + nm);
        else
            System.out.println("Source created for " + nm);
        buffersMap.put(file, buffer);
        sourcesMap.put(nm, source);
        return true;
    }
    
    public boolean load(String nm, String file, float x, float y, float z, boolean toLoop){
        if (load(nm, file, toLoop))
            return setPos(nm, x, y, z);
        else
            return false;
    }
    
    private int[] initBuffer(String file){
        int[] format = new int[1];
        ByteBuffer[] data = new ByteBuffer[1];
        int[] size = new int[1];
        int[] freq = new int[1];
        int[] loop = new int[1];
        // load WAV file into the data arrays
        String fnm = SOUND_DIR + file + ".wav";
        try {
            ALut.alutLoadWAVFile(this.getClass().getResourceAsStream(fnm), format, data, size, freq, loop);
        }
        catch(ALException e) {
            System.out.println("Error loading WAV file: " + fnm);
            return null;
        }
        // System.out.println("Sound size = " + size[0]);
        // System.out.println("Sound freq = " + freq[0]);
        // create an empty buffer to hold the sound data
        int[] buffer = new int[1];
        al.alGenBuffers(1, buffer, 0);
        if (al.alGetError() != AL.AL_NO_ERROR) {
            System.out.println("Could not create a buffer for " + file);
            return null;
        }
        // store data in the buffer
        al.alBufferData(buffer[0], format[0], data[0], size[0], freq[0]);
        // ALut.alutUnloadWAV(format[0], data[0], size[0], freq[0]);
        // not in API anymore
        return buffer;
    }
    
    // initializes the sources of audio in the virtual world
    private int[] initSource(String nm, int[] buf, boolean toLoop){
        int[] source = new int[1];
        al.alGenSources(1, source, 0);
        if (al.alGetError() != AL.AL_NO_ERROR) {
            System.out.println("Error creating source for " + nm);
            return null;
        }
        al.alSourcei(source[0], AL.AL_BUFFER, buf[0]); // bind buffer
        al.alSourcef(source[0], AL.AL_PITCH, 1.0f);
        al.alSourcef(source[0], AL.AL_GAIN, 1.0f);
        al.alSourcei(source[0], AL.AL_REFERENCE_DISTANCE, 100);
        al.alSourcei(source[0], AL.AL_MAX_DISTANCE, 3000);
        al.alSource3f(source[0], AL.AL_POSITION, 640f, 480f, 0.0f);
        
        // position the source
        al.alSource3i(source[0], AL.AL_VELOCITY, 0, 0, 0); // no velocity
        if (toLoop)
            al.alSourcei(source[0], AL.AL_LOOPING, AL.AL_TRUE); // looping
        else
            al.alSourcei(source[0], AL.AL_LOOPING, AL.AL_FALSE); //play once
        if (al.alGetError() != AL.AL_NO_ERROR) {
            System.out.println("Error configuring source for " + nm);
            return null;
        }
        return source;
    }
    
    public boolean setPos(String nm, float x, float y, float z)
    {
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return false;
        }
        al.alSource3f(source[0], AL.AL_POSITION, x, y, z);
        return true;
    }
    
    public boolean play(String nm){
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return false;
        }
        al.alSourcePlay(source[0]);
        return true; 
    }
    
    public boolean stop(String nm){
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return false;
        }
        al.alSourceStop(source[0]);
        return true; 
    }
    
    public boolean stopall(){
        boolean stopped = true;
        for (String nm : sourcesMap.keySet()){
            stopped = (stop(nm) && stopped);
        }
        return stopped;
    }
    
    public boolean isPlaying(String nm){
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return false;
        }
        IntBuffer state = IntBuffer.allocate(1);
        al.alGetSourcei(source[0],AL.AL_SOURCE_STATE, state);
        if (state.get(0) == AL.AL_PLAYING) return true;
        else return false;
    }
    
    public float playposition(String nm){
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return 0;
        }
        FloatBuffer state = FloatBuffer.allocate(1);
        al.alGetSourcef(source[0],AL.AL_SEC_OFFSET, state);
        return state.get(0);
    }
            
    public boolean setVolume(String nm, float volume){
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return false;
        }
        al.alSourcef(source[0], AL.AL_GAIN, volume);
        return true;
    }
    
    public boolean setPitch(String nm, float pitch){
        int[] source = (int[]) sourcesMap.get(nm);
        if (source == null) {
            System.out.println("No source found for " + nm);
            return false;
        }
        al.alSourcef(source[0], AL.AL_PITCH, pitch);
        return true;
    }
}
