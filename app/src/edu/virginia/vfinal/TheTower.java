package edu.virginia.vfinal;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

import edu.virginia.engine.display.DisplayObject;
import edu.virginia.engine.display.DisplayObjectContainer;
import edu.virginia.engine.display.Game;
import edu.virginia.engine.display.Note;
import edu.virginia.engine.display.PhysicsSprite;
import edu.virginia.engine.display.Sprite;
import edu.virginia.engine.display.Tween;
import edu.virginia.engine.display.TweenJuggler;
import edu.virginia.engine.display.TweenableParams;
import edu.virginia.engine.util.Event;
import edu.virginia.engine.util.GameClock;
import edu.virginia.engine.util.GamePad;
import edu.virginia.engine.util.SoundManager;
import edu.virginia.engine.util.listeners.CollisionEvent;
import edu.virginia.engine.util.listeners.HookListener;
import edu.virginia.engine.util.listeners.PlayerListener;

public class TheTower extends Game{
	/* Player variables */
	PhysicsSprite player = new PhysicsSprite("Player", "Player.png");
	Sprite crosshairs = new Sprite("CrosshairsRed", "crosshairsRed.png");
	Sprite hookshot = new Sprite("Hookshot", "hookshot.png"); 
	Sprite staff = new Sprite("Staff", "staff.png");
	Sprite lossScreen = new Sprite ("Loss Screen", "End Screen Lost.jpg");
	Sprite winScreen = new Sprite ("Win Screen", "End Screen Won.jpg");
	Sprite deathBar = new Sprite("Death Bar", "death bar.png");
	ArrayList<Note> collected = new ArrayList<Note>();
	/* Sprite collections */
	DisplayObjectContainer playerSprites = new DisplayObjectContainer("Player Sprites");
	ArrayList<String> backgroundFilePaths = new ArrayList<String>();
	DisplayObjectContainer backgroundCollection = new DisplayObjectContainer("Background Collection");
	int backgroundFallRate = 200;
	DisplayObjectContainer foregroundCollection = new DisplayObjectContainer("Foreground Collection");
	int foregroundFallRate = 20; //how often platforms should move
	DisplayObjectContainer platformCollection = new DisplayObjectContainer("Platform Collection");
	DisplayObjectContainer notesCollection = new DisplayObjectContainer("Notes Collection");
	/* Game variables */
	int livesLeft = 3;
	boolean gameEnded=false;
	boolean gameLoss=false;
	boolean inPlay = true;
	String noteLength="";
	TweenJuggler tInstance;
	double startTime=System.currentTimeMillis();
	double timeElapsed=0;
	GameClock backgroundInterlude = new GameClock();
	GameClock foregroundInterlude = new GameClock();
	/* HUD variables */
	int notesCollected = 0;
	int currentLevel = 0;
	int notesToNextLevel = 2;
	int notesPerLevel = 2;
	int notesToBeatStage = 10;
	int missedNotes=0;
	int totalNotes=10;
	private long gameEndedTime;
	private boolean toPlay;
	/* Audio assets */
	SoundManager soundManager = new SoundManager();
	GameClock timeToSwapBGM = new GameClock();
	
	/* Set up game */
	public TheTower() throws IOException {
		super("The Tower", 1600, 768);
		/* Set TweenJuggler */
		if (TweenJuggler.getInstance() == null){tInstance = new TweenJuggler();}
		else {tInstance = TweenJuggler.getInstance();}
		/* Set Player Sprite positions */
		player.setPosition(new Point(100, 500));
		player.setCrosshairsAngle(270);
		crosshairs.setPivotPoint(new Point(-26, -26));
		winScreen.setVisible(false);
		drawCrosshairsKeyboard(crosshairs, player, hookshot);
		/* Set up backgrounds */
		this.addChild(backgroundCollection);
		setupBackgrounds();
		/* Set up sprites that will fall in the foreground */
		this.addChild(foregroundCollection);
		// Set up platforms and notes
		setupPlatformsAndNotes();
		/* Set up player sprites as children of Game */
		setupPlayerSprites(playerSprites,
				    	   crosshairs, 
				    	   player, 
				    	   hookshot);
		foregroundCollection.addChild(playerSprites);
		/* Set up hookshot */
		HookListener hookListener = new HookListener(player, tInstance, hookshot);
		PlayerListener playerListener = new PlayerListener(player, tInstance, soundManager);
		this.setupHookshot(player, playerListener, hookListener, hookshot);
		/* Set up audio */
		setupAudio();
		deathBar.setPosition(new Point(0, 900));
		staff.setPosition(new Point(200, 250));
		staff.setVisible(false);
	}

	public static void main(String[] args) throws IOException {
		TheTower game = new TheTower();
		game.start();
	}
	
	@Override
	public void draw(Graphics g){
			/* Draw all assets */
			super.draw(g);
			/* Draw HUD */
			g.setColor(Color.WHITE);
			if (gameLoss){
				lossScreen.draw(g);
				g.setColor(Color.MAGENTA);
				g.setFont(new Font("KinoMT", Font.PLAIN, 40));
				//g.drawString("You have missed too many notes!", 400, 150);
				g.drawString("1: Restart    2: Menu", 1175, 710);
				
				if (tInstance.getSize()==0){
					//this.stop();
				}			
				
			}
			if (gameEnded==true && gameLoss==false){
				// TODO: Replace with victory splash screen
				g.setFont(new Font("KinoMT", Font.PLAIN, 80));
				winScreen.draw(g);
				winScreen.setVisible(true);
				if(staff != null){
					staff.draw(g);
				}
				for (Note note : collected){
					note.setAlpha(1);
					note.draw(g);
				}
				//strokeText("Sound Restored!", g, 450, 100);
				//if (tInstance.getSize()==0){
				//	this.stop();
				//}
				g.setFont(new Font("KinoMT", Font.PLAIN, 30));
				g.setColor(Color.BLACK);
				if (collected.size()==0) g.drawString("1: Restart   2: Menu   3: Next Level", 40, 710);
			}
			g.setFont(new Font("KinoMT", Font.BOLD, 30));
			String strNotesCollected = "Notes Collected: " + notesCollected + "/" + notesToBeatStage;
			String strNotesToNextLevel = "Notes To Next Level: " + notesToNextLevel;
			String strNotesMissed = "Note Lives Left: " + (1 - missedNotes);
			//g.drawString(strNotesCollected, 1260, 50);
			strokeText(strNotesCollected, g, 1260, 50);
			strokeText(strNotesToNextLevel,g, 1200, 90);
			strokeText(strNotesMissed, g, 10, 50);
			if (missedNotes >= 1) {
				g.setColor(Color.RED);
				g.drawString("Note Lives Left: 0", 10, 50);
				
			}
	}
		
	@Override
	public void update(ArrayList<Integer> pressedKeys, ArrayList<GamePad> controllers){
		// Hold 'P' to pause (for debugging)
		if ((pressedKeys.contains(80))){
			inPlay = false;
		}
		else{
			inPlay = true;
		}
		if (missedNotes>totalNotes-notesToBeatStage || (gameEnded==true && collected.size()==0) || gameLoss){
			if (pressedKeys.contains(49)){
				String[] args={"0"};
				try {
					TheTower.main(args);
					soundManager.StopBGM();
					this.exitGame();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (pressedKeys.contains(50)){
				String[] args={"0"};
				try {
					Menu.main(args);
					soundManager.StopBGM();
					this.exitGame();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (gameEnded==true && collected.size()==0){
				if (pressedKeys.contains(51)){
					String[] args={"0"};
					try {
						LisaRiccia.main(args);
						soundManager.StopBGM();
						this.exitGame();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		if (inPlay){
			if (gameEnded==true){
				timeElapsed=Math.min(System.currentTimeMillis()-startTime,500);
			}
			if (tInstance!=null){tInstance.nextFrame();}
			
			/* Update methods */
			super.update(pressedKeys, controllers);
			player.update(pressedKeys, controllers);
			
			/* Update hook status */
			drawCrosshairsKeyboard(crosshairs, player, hookshot);
			platformCollection.hookablePlatform(crosshairs, player);
		
			/* Have foreground and background elements fall */
			updateBackgroundCollection(backgroundCollection, backgroundInterlude);
			updateForegroundCollection(foregroundCollection, foregroundInterlude);
		
			/* Collisions only occur if player can be interacted with */
			if (player.isInPlay()){
				/* Player colliding with platform */
				if (platformCollection.collidesWithPlatform(player)){
					// If true, player will send collision event to all listeners.
				}
				/* Player is in the Air*/
				else {
					player.dispatchEvent(new Event(CollisionEvent.INAIR, null));
				}
				// Player collides with note
				if (notesCollection.collidesWithNoteSound(player, tInstance)){
					Note note = notesCollection.getCollidedNote();
					collected.add(note);
					note.setVisible(true);
					note.setInPlay(true);
					int height=300;
					if (note.getId().equals("c4") || note.getId().equals("c4q")) height=384;
					else if (note.getId().equals("d4") || note.getId().equals("d4q")) height=370;
					else if (note.getId().equals("e4") || note.getId().equals("e4q")) height=356;
					else if (note.getId().equals("f4") || note.getId().equals("f4q")) height=342;
					else if (note.getId().equals("g4") || note.getId().equals("g4q")) height=328;
					else if (note.getId().equals("a5") || note.getId().equals("a5q")) height=314;
					else if (note.getId().equals("b5") || note.getId().equals("b5q")) height=300;
					
				    note.setPosition(new Point((250+(collected.size()+1)*1100/5), height));
					note.setScaleX(0.4);
					note.setScaleY(0.4);
					notesCollection.removeChild(notesCollection.getCollidedNote());
					updateHUD(true);
				}
				
				if (notesCollection.collidesWithNoteSound(deathBar, tInstance)){
					Note note = notesCollection.getCollidedNote();
					note.setVisible(false);
					updateHUD(false);
				}
				
				/* If player falls to the bottom of the screen, lose */
				if((!gameEnded && player.getPosition().getY()>750) || missedNotes>totalNotes-notesToBeatStage){
					gameEnded = true;
					gameLoss = true;
					soundManager.StopBGM();
					soundManager.PlaySoundEffect("Game Over");
					player.setVisible(false);
					crosshairs.setVisible(false);
				}
				if (gameEnded==true && gameLoss==false){
					notesCollection.removeChildren();
					platformCollection.removeChildren();
					this.removeChild(foregroundCollection);
					if (tInstance.getSize()==0){
						if (collected.size()>0 ){
							Note note =collected.get(collected.size()-1);
							note.setVisible(true);
							note.setAlpha(1);
							int height=300;
							if (note.getId().equals("c4") || note.getId().equals("c4q")) height=384;
							else if (note.getId().equals("d4") || note.getId().equals("d4q")) height=370;
							else if (note.getId().equals("e4") || note.getId().equals("e4q")) height=356;
							else if (note.getId().equals("f4") || note.getId().equals("f4q")) height=342;
							else if (note.getId().equals("g4") || note.getId().equals("g4q")) height=328;
							else if (note.getId().equals("a5") || note.getId().equals("a5q")) height=314;
							else if (note.getId().equals("b5") || note.getId().equals("b5q")) height=300;
							//note.setPosition(new Point((250+(totalNotes-missedNotes-1) * 1100/5), height));
							note.setPosition(new Point((int)(note.getPosition().getX()), height));
						}
					}
					if (collected.isEmpty()){
						//System.out.println("Collected is empty");
						//this.stop();
					}
					else {
						if (toPlay==true){
							try {
								soundManager.LoadSoundEffect(collected.get(0).getSound(), collected.get(0).getSound());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							soundManager.PlaySoundEffect(collected.get(0).getSound());
							noteLength=collected.get(0).getLength();
							collected.remove(0);
							toPlay=false;
							//System.out.println(toPlay);
						}	
						else{
							//System.out.println("um2");
							//System.out.println(System.currentTimeMillis()-gameEndedTime);
							int t=500;
							if (noteLength.equals("quarter")) t=1000; 
							if (System.currentTimeMillis()-gameEndedTime>t){
								//System.out.println(System.currentTimeMillis()-gameEndedTime);
								gameEndedTime=System.currentTimeMillis();
								toPlay=true;
								}
							}
					}}
			}
		}
	}
	
	public void updateHUD(boolean hit){
		if (hit==true){
			notesCollected += 1; 
			notesToNextLevel -= 1;
			}
			else {
				missedNotes++;
			}
		// Increase threshold for achieve next level
		if (notesToNextLevel <= 0){
			currentLevel += 1;
			swapBackground(currentLevel, backgroundFilePaths, 0);
			notesToNextLevel = notesPerLevel;
			// Player has collected all notes 
			if ((notesCollected+missedNotes) >= totalNotes){
				notesToNextLevel = notesToBeatStage;
				gameEnded = true;
				toPlay=false;
				gameEndedTime=System.currentTimeMillis();
				staff.setVisible(true);
				int height=300;
				this.removeChild(foregroundCollection);
				for (int i=0; i<collected.size(); i++){
					if (collected.get(i).getId().equals("c4") || collected.get(i).getId().equals("c4q")) height=384;
					else if (collected.get(i).getId().equals("d4") || collected.get(i).getId().equals("d4q")) height=370;
					else if (collected.get(i).getId().equals("e4") || collected.get(i).getId().equals("e4q")) height=356;
					else if (collected.get(i).getId().equals("f4") || collected.get(i).getId().equals("f4q")) height=342;
					else if (collected.get(i).getId().equals("g4") || collected.get(i).getId().equals("g4q")) height=328;
					else if (collected.get(i).getId().equals("a5") || collected.get(i).getId().equals("a5q")) height=314;
					else if (collected.get(i).getId().equals("b5") || collected.get(i).getId().equals("b5q")) height=300;
					else if (collected.get(i).getId().equals("g3") || collected.get(i).getId().equals("g3q")) height=440;
				
					collected.get(i).setPosition(new Point((250+(i+1)*1100/collected.size()), height));
					collected.get(i).setScaleX(0.4);
					collected.get(i).setScaleY(0.4);
			}
			
		}
		
		}	
	}
	
	public void updateBackgroundCollection(DisplayObjectContainer backgroundRoot, GameClock timeSinceFall){
		/** Change how often the background will update **/
		if (timeSinceFall.getElapsedTime() > backgroundFallRate){
			/* Have your children fall */
			for (int i = 0; i < backgroundRoot.numberOfChildren(); i++){
				DisplayObject toFallNext = backgroundRoot.getChildAtIndex(i);
				int x = (int) toFallNext.getPosition().getX();
				int y = (int) toFallNext.getPosition().getY();
				/** Change how much the background will fall **/
				int newY = y + 1;
				toFallNext.setPosition(new Point(x,newY));
			}
			/* Reset timer */
			timeSinceFall.resetGameClock();
		}
	}
	
	public void swapBackground(int currLevel, ArrayList<String> bfp, long timePassed){
		// Swap the background to the next image
		DisplayObject currBackground = backgroundCollection.getChildAtIndex(0);
		currBackground.setImage(bfp.get(currLevel));
		currBackground.setAlpha(0);
		// Tween the backgrounds
		Tween fadeIn = new Tween (currBackground, null, null);
		tInstance.add(fadeIn);
		int timeTweening = 500;
		fadeIn.animate(TweenableParams.ALPHA, 0, 1, timeTweening);
		//Update the BGM
		soundManager.updateBGM(currLevel, timeToSwapBGM.getElapsedTime());
		/* Have the previous background fade out 
		int prevLevel = currLevel - 1;
		DisplayObject prevBackground = backgroundCollection.getChildAtIndex(0);
		prevBackground.setImage(bfp.get(prevLevel));
		prevBackground.setAlpha(1);
		prevBackground.setVisible(false);
		Tween fadeOut = new Tween (prevBackground, null, null);
		tInstance.add(fadeOut);
		fadeOut.animate(TweenableParams.ALPHA, 1, 0, timeTweening);
		*/
	}
	
	public void updateForegroundCollection(DisplayObjectContainer foregroundRoot, GameClock timeSinceFall){
		/** Change how often the foreground and background will update **/
		if (timeSinceFall.getElapsedTime() > foregroundFallRate){
			// Have foreground elements fall 
			for (int i = 0; i < foregroundRoot.numberOfChildren(); i++){
				DisplayObject toFallNext = foregroundRoot.getChildAtIndex(i);
				foregroundFallSpeed1((DisplayObjectContainer)toFallNext);
			}
			// Reset timer 
			timeSinceFall.resetGameClock();
		}
	}
	
	public void foregroundFallSpeed1(DisplayObjectContainer toFallCurrent){
		/** Change how much the foreground will fall **/
		int x = (int) toFallCurrent.getPosition().getX();
		int y = (int) toFallCurrent.getPosition().getY();
		int newY = y + 1;
		toFallCurrent.setPosition(new Point(x,newY));
		for (int i = 0; i < toFallCurrent.numberOfChildren(); i++){
			DisplayObject toFallNext = toFallCurrent.getChildAtIndex(i);
			foregroundFallSpeed1((DisplayObjectContainer)toFallNext);
		}	
	}	
	
	/** Set backgrounds here! **/
	public void setupBackgrounds(){
		// Level 0
		backgroundFilePaths.add("TheTowerLevel0.jpg");
		// Level 1
		backgroundFilePaths.add("TheTowerLevel1.jpg");
		// Level 2
		backgroundFilePaths.add("TheTowerLevel2.jpg");
		// Level 3
		backgroundFilePaths.add("TheTowerLevel3.jpg");
		// Level 4
		backgroundFilePaths.add("TheTowerLevel4.jpg");
		// Level 5
		backgroundFilePaths.add("TheTowerLevel5.jpg");
		/* Previous Background
		backgroundCollection.addChild(new Sprite ("Previous Background", "TempleOfTimeLevel1.png", 0, -200));*/
		// Current Background
		backgroundCollection.addChild(new Sprite ("Current Background", "TheTowerLevel0.jpg", -200, -400));
	}
	
	/** Create platforms here! 
	 * @throws IOException **/
	public void setupPlatformsAndNotes() throws IOException{
		/* Set up display hierarchy and platforms */
		foregroundCollection.addChild(platformCollection);
		foregroundCollection.addChild(notesCollection);
		/* Base platform */
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 0, 600));
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 250, 600));
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 500, 600));
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 750, 600));
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 1000, 600));
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 1250, 600));
		platformCollection.addChild(new Sprite ("PlatformBase", "platformBlue.png", 1500, 600));
		/* Level platforms */
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1300, 400));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 100, 200));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 800, 200));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 100, -100));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 100, -400));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 600, -400));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1100, -400));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1450, -100));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1300, -650));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 800, -900));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 300, -1150));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 800, -1150));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1300, -1150));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 100, -1450));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 600, -1500));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1000, -1750));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 1400, -2000));
		platformCollection.addChild(new Sprite ("Platform", "platformBlue.png", 600, -2000));
		
		/* Collectable notes */
		notesCollection.addChild(new Note ("a4", "a.png", "a4.wav", "eighth", 750, -250)); //First Pair 
		notesCollection.addChild(new Note ("d4", "d.png", "d5.wav", "eighth", 500, -250)); 
		notesCollection.addChild(new Note ("e4", "e.png", "e4.wav", "eighth", 1450, -600)); //Second Pair
		notesCollection.addChild(new Note ("f4", "f.png", "f4.wav", "eighth", 1450, -300));
		notesCollection.addChild(new Note ("g4", "g.png", "g4.wav", "eighth", 400, -1000)); //Third Pair
		notesCollection.addChild(new Note ("a4", "a.png", "a5.wav", "eighth", 500, -850));
		notesCollection.addChild(new Note ("d4", "d.png", "d4.wav", "eighth", 1400, -1400)); // Horizontal Cross
		notesCollection.addChild(new Note ("e4", "e.png", "e4.wav", "eighth", 300, -2000));  // Random note 		
		notesCollection.addChild(new Note ("f4", "f.png", "f4.wav", "eighth", 1450, -1850)); // Second to last
		notesCollection.addChild(new Note ("g4", "g.png", "g4.wav", "eighth", 300, -2250)); //Last
		//TODO: Remove debug platform
		//platformCollection.addChild(new Sprite ("Platform", "platformPink.png", 300, 400));
	}
	
	public void setupAudio() throws IOException{
		/* Loads into Hash map, integer is level when music is played */
		soundManager.LoadSoundEffect("Game Over", "GameOver.wav");
		soundManager.LoadBGM(0, "TheTowerLevel0.wav");
		soundManager.LoadBGM(1, "TheTowerLevel1.wav");
		soundManager.LoadBGM(2, "TheTowerLevel2.wav");
		soundManager.LoadBGM(3, "TheTowerLevel3.wav");
		soundManager.LoadBGM(4, "TheTowerLevel4.wav");
		soundManager.LoadBGM(5, "TheTowerLevel5.wav");
		soundManager.PlayBGM(0, 0);
	}
	
}