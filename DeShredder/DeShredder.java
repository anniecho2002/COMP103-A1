/* Code for COMP103 - 2021T2, Assignment 1
 * Name: Annie Cho
 * Username: choanni
 * ID: 300575457
 */

import ecs100.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * DeShredder allows a user to sort fragments of a shredded document ("shreds") into strips, and
 * then sort the strips into the original document.
 * The program shows
 *   - a list of all the shreds along the top of the window, 
 *   - the working strip (which the user is constructing) just below it.
 *   - the list of completed strips below the working strip.
 * The "rotate" button moves the first shred on the list to the end of the list to let the
 *  user see the shreds that have disappeared over the edge of the window.
 * The "shuffle" button reorders the shreds in the list randomly
 * The user can use the mouse to drag shreds between the list at the top and the working strip,
 *  and move shreds around in the working strip to get them in order.
 * When the user has the working strip complete, they can move
 *  the working strip down into the list of completed strips, and reorder the completed strips
 *
 */
public class DeShredder {
    // Fields to store the lists of Shreds and strips.  These should never be null.
    private List<Shred> allShreds = new ArrayList<Shred>();    //  List of all shreds
    private List<Shred> workingStrip = new ArrayList<Shred>(); // Current strip of shreds
    private List<List<Shred>> completedStrips = new ArrayList<List<Shred>>();

    // Constants for the display and the mouse
    public static final double LEFT = 20;       // left side of the display
    public static final double TOP_ALL = 20;    // top of list of all shreds 
    public static final double GAP = 5;         // gap between strips
    public static final double SIZE = Shred.SIZE; // size of the shreds

    public static final double TOP_WORKING = TOP_ALL+SIZE+GAP;
    public static final double TOP_STRIPS = TOP_WORKING+(SIZE+GAP);

    //Fields for recording where the mouse was pressed  (which list/strip and position in list)
    // note, the position may be past the end of the list!
    private List<Shred> fromStrip;   // The strip (List of Shreds) that the user pressed on
    private int fromPosition = -1;   // index of shred in the strip

    /**
     * Initialises the UI window, and sets up the buttons. 
     */
    public void setupGUI() {
        UI.addButton("Load library",   this::loadLibrary);
        UI.addButton("Rotate",         this::rotateList);
        UI.addButton("Shuffle",        this::shuffleList);
        UI.addButton("Complete Strip", this::completeStrip);
        //UI.addButton("Save Image",     this::saveImage);
        UI.addButton("Quit",           UI::quit);

        UI.setMouseListener(this::doMouse);
        UI.setWindowSize(1000,800);
        UI.setDivider(0);
    }

    /**
     * Asks user for a library of shreds, loads it, and redisplays.
     * Uses UIFileChooser to let user select library
     * and finds out how many images are in the library
     * Calls load(...) to construct the List of all the Shreds
     */
    public void loadLibrary(){
        Path filePath = Path.of(UIFileChooser.open("Choose first shred in directory"));
        Path directory = filePath.getParent(); //subPath(0, filePath.getNameCount()-1);
        int count=1;
        while(Files.exists(directory.resolve(count+".png"))){ count++; }
        //loop stops when count.png doesn't exist
        count = count-1;
        load(directory, count);   // YOU HAVE TO COMPLETE THE load METHOD
        display();
    }

    /**
     * Empties out all the current lists (the list of all shreds,
     *  the working strip, and the completed strips).
     * Loads the library of shreds into the allShreds list.
     * Parameters are the directory containing the shred images and the number of shreds.
     * Each new Shred needs the directory and the number/id of the shred.
     */
    public void load(Path dir, int count) {
        workingStrip.clear();
        allShreds.clear();
        completedStrips.clear();
        for (int i=0; i<count; i++){
            Shred placeholder = new Shred(dir, i+1);
            allShreds.add(placeholder);
        }
    }

    /**
     * Rotate the list of all shreds by one step to the left
     * and redisplay;
     * Should not have an error if the list is empty
     * (Called by the "Rotate" button)
     */
    public void rotateList(){
        if (allShreds.size() == 0){
            UI.println("Need to load strips.");
        }
        else{
            Shred placeholder = allShreds.get(0);
            allShreds.remove(0);
            allShreds.add(placeholder);
            display();
        }
    }

    /**
     * Shuffle the list of all shreds into a random order
     * and redisplay;
     */
    public void shuffleList(){
        Collections.shuffle(allShreds);
        display();
    }

    /**
     * Move the current working strip to the end of the list of completed strips.
     * (Called by the "Complete Strip" button)
     */
    public void completeStrip(){
        completedStrips.add(workingStrip);
        workingStrip = new ArrayList<Shred>();
        display();
    }

    /**
     * Simple Mouse actions to move shreds and strips
     *  User can
     *  - move a Shred from allShreds to a position in the working strip
     *  - move a Shred from the working strip back into allShreds
     *  - move a Shred around within the working strip.
     *  - move a completed Strip around within the list of completed strips
     *  - move a completed Strip back to become the working strip
     *    (but only if the working strip is currently empty)
     * Moving a shred to a position past the end of a List should put it at the end.
     * You should create additional methods to do the different actions - do not attempt
     *  to put all the code inside the doMouse method - you will lose style points for this.
     * Attempting an invalid action should have no effect.
     * Note: doMouse uses getStrip and getColumn, which are written for you (at the end).
     * You should not change them.
     */
    public void doMouse(String action, double x, double y){
        if (action.equals("pressed")){
            fromStrip = getStrip(y);      // the List of shreds to move from (possibly null)
            fromPosition = getColumn(x);  // the index of the shred to move (may be off the end)
        }
        if (action.equals("released")){
            List<Shred> toStrip = getStrip(y); // the List of shreds to move to (possibly null)
            int toPosition = getColumn(x);     // the index to move the shred to (may be off the end)
            int toRow = getRow(y);             // the row that the mouse finishes on
            if (fromPosition > fromStrip.size()-1 || toStrip==null || fromStrip==null || 
               (fromStrip.equals(allShreds) && toRow>1) || (fromStrip.equals(workingStrip) && toRow>1)){
                UI.println("Try again please.");
            }
            else if (fromStrip.equals(workingStrip) || (fromStrip.equals(allShreds))){
                move(toStrip, toPosition);
            }
            else {  // it is moving to a completed strip
                completed(toStrip, toPosition, toRow);
            }
            display();
            highlightWorking();
        }
    }
    
    /*
     * saves the completed strips into an image 
     * didn't work lol
     */
    public void saveImage(){
        int pixelRow = completedStrips.size() * 40;              // number of pixels all the way down
        int pixelCol = 0;                                        // initialized to 0, to be changed to max num of columns 
        for (int i=0; i<completedStrips.size(); i++){
            if (completedStrips.get(i).size() > pixelCol){
                pixelCol = completedStrips.get(i).size();
            }
        }
        pixelRow = pixelRow * 40;
        Color[][] allShredCol = new Color[pixelRow][pixelCol];   // the full 2d array of all the pixels
        
        UI.println("For each completed strip: " + completedStrips.size());
        UI.println("For each shred in the completed strip: " + completedStrips.get(0).size());
        
        for (int i=0; i<completedStrips.size(); i++){            // for each completed strip
            for (int j=0; j<completedStrips.get(i).size(); j++){ // for each Shred in the completed strip
                Shred tempShred = completedStrips.get(i).get(j);
                Color[][] tempCol;
                tempCol = loadImage(tempShred.filename());       // return a 2d array filled with all the colors in that shred
                
                // put that 2d array into the big array in the right place.... ugh but how
                for (int row=0; row<39; row++){
                    for (int col=0; col<39; col++){
                        allShredCol[i*40+row][j*40+row] = tempCol[row][col];
                    }
            }
        }
        
        // goes through every value in the big array
        /*for (int i=0; i<pixelRow; i++){
            for (int j=0; j<pixelCol; j++){
                if (allShredCol[i][j] == null){
                    UI.println("Null value found.");
                }
            }
        } */
        
        //saveImage(allShredCol, "save");
        }  
    }
    
    // Additional methods to perform the different actions, called by doMouse
    
    /*
     * moving around from allShreds and workingStrip
     */
    public void move(List<Shred> toStrip, int toPosition){
        Shred placeholder = fromStrip.get(fromPosition); // grabs the selected shred
        fromStrip.remove(fromPosition); // removes it the strip
        if (toStrip.size() == 0){ // if the to strip is empty
            toStrip.add(0, placeholder);
        }
        else if (toPosition > toStrip.size()){
            toStrip.add(toStrip.size(), placeholder); // moving the shred to past the end of a list
        }
        else{
            toStrip.add(toPosition, placeholder); // otherwise adds to working strip, in the right place
        }
    }
    
    /*
     * moving around with completed strips
     */
    public void completed(List<Shred> toStrip, int toPosition, int row){
        if(row==1 && workingStrip.size()==0){ //moving into the working strip
            workingStrip = fromStrip;
            completedStrips.remove(fromStrip);
        }
        else if(row>1){ //moving around in completed strips
            List<Shred> completedPlaceholder = fromStrip;
            completedStrips.remove(fromStrip); // removes it from the big list (hopefully)
            completedStrips.add(row-2, completedPlaceholder);
        }
    }
    
    /*
     * returns what row mouse is on
     */
    public int getRow(double y){
        int row = (int) ((y-TOP_ALL)/(SIZE+GAP));
        if (row<=0){
            return 0;
        }
        else{
            return row;
        }
    }
    
    /*
     * finds good colour matches and highlights the working strip
     */
    public void highlightWorking(){
        if (workingStrip.size() > 0){   // makes sure it isn't empty
            Color[][] lastShredCol;
            Shred lastShred = workingStrip.get(workingStrip.size()-1);    // grabs last value in working strip
            lastShredCol = loadImage(lastShred.filename());               // returns the 2d array, each pixel of the shred -> pixels color
            ArrayList<Color> leftCol = new ArrayList<Color>();      
            int matchPosition = 0;                                        // stores the i position of good match
            Shred matchShred = lastShred;                                 // initializing match to be the same as lastshred
            for (int i=0; i<40; i++){
                if (!leftCol.contains(lastShredCol[i][0])){leftCol.add(lastShredCol[i][0]);}
            }
            
            for (int i=0; i<workingStrip.size()-1; i++){ //each shred in working strip
                ArrayList<Color> rightCol = new ArrayList<Color>();       
                Shred compareShred = workingStrip.get(i);
                Color[][] compareShredCol;
                compareShredCol = loadImage(compareShred.filename());
                
                for (int j=0; j<40; j++){ 
                    if (!rightCol.contains(compareShredCol[j][39])){rightCol.add(compareShredCol[j][39]);}
                }
            
                leftCol.retainAll(rightCol);
                if (leftCol.size() > 2){  // more than two matching colours
                    matchPosition = i;
                    matchShred = workingStrip.get(i);
                }
            }
            
            // highlights it if the match has been found
            if (!matchShred.equals(lastShred)){
                matchShred.highlight(LEFT + SIZE*matchPosition, TOP_WORKING);
            }
        }
    }

    //=============================================================================
    // Completed for you. Do not change.
    // loadImage and saveImage may be useful for the challenge.

    /**
     * Displays the remaining Shreds, the working strip, and all completed strips
     */
    public void display(){
        UI.clearGraphics();

        // list of all the remaining shreds that haven't been added to a strip
        double x=LEFT;
        for (Shred shred : allShreds){
            shred.drawWithBorder(x, TOP_ALL);
            x+=SIZE;
        }

        //working strip (the one the user is workingly working on)
        x=LEFT;
        for (Shred shred : workingStrip){
            shred.draw(x, TOP_WORKING);
            x+=SIZE;
        }
        UI.setColor(Color.red);
        UI.drawRect(LEFT-1, TOP_WORKING-1, SIZE*workingStrip.size()+2, SIZE+2);
        UI.setColor(Color.black);

        //completed strips
        double y = TOP_STRIPS;
        for (List<Shred> strip : completedStrips){
            x = LEFT;
            for (Shred shred : strip){
                shred.draw(x, y);
                x+=SIZE;
            }
            UI.drawRect(LEFT-1, y-1, SIZE*strip.size()+2, SIZE+2);
            y+=SIZE+GAP;
        }
    }

    /**
     * Returns which column the mouse position is on.
     * This will be the index in the list of the shred that the mouse is on, 
     * (or the index of the shred that the mouse would be on if the list were long enough)
     */
    public int getColumn(double x){
        return (int) ((x-LEFT)/(SIZE));
    }

    /**
     * Returns the strip that the mouse position is on.
     * This may be the list of all remaining shreds, the working strip, or
     *  one of the completed strips.
     * If it is not on any strip, then it returns null.
     */
    public List<Shred> getStrip(double y){
        int row = (int) ((y-TOP_ALL)/(SIZE+GAP));
        if (row<=0){
            return allShreds;
        }
        else if (row==1){
            return workingStrip;
        }
        else if (row-2<completedStrips.size()){
            return completedStrips.get(row-2);
        }
        else {
            return null;
        }
    }

    public static void main(String[] args) {
        DeShredder ds =new DeShredder();
        ds.setupGUI();
    }


    /**
     * Load an image from a file and return as a two-dimensional array of Color.
     * From COMP 102 assignment 8&9.
     * Maybe useful for the challenge. Not required for the core or completion.
     */
    public Color[][] loadImage(String imageFileName) {
        if (imageFileName==null || !Files.exists(Path.of(imageFileName))){
            UI.println("File name is invalid.");   // for testing
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(Files.newInputStream(Path.of(imageFileName)));
            int rows = img.getHeight();
            int cols = img.getWidth();
            Color[][] ans = new Color[rows][cols];
            for (int row = 0; row < rows; row++){
                for (int col = 0; col < cols; col++){                 
                    Color c = new Color(img.getRGB(col, row));
                    ans[row][col] = c;
                }
            }
            return ans;
        } catch(IOException e){UI.println("Reading Image from "+imageFileName+" failed: "+e);}
        return null;
    }

    /**
     * Save a 2D array of Color as an image file
     * From COMP 102 assignment 8&9.
     * Maybe useful for the challenge. Not required for the core or completion.
     */
    public  void saveImage(Color[][] imageArray, String imageFileName) {
        int rows = imageArray.length;
        int cols = imageArray[0].length;
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Color c =imageArray[row][col];
                img.setRGB(col, row, c.getRGB());
            }
        }
        try {
            if (imageFileName==null) { return;}
            ImageIO.write(img, "png", Files.newOutputStream(Path.of(imageFileName)));
        } catch(IOException e){UI.println("Image reading failed: "+e);}

    }

}
