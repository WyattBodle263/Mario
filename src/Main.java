
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

class MapManager {

     Map1 map;

     MapManager() {}

     void updateLocations() {
        if (map == null)
            return;

        map.updateLocations();
    }

     void resetCurrentMap(GameEngine engine) {
        Mario mario = getMario();
        mario.resetLocation();
        engine.resetCamera();
        createMap(engine.getImageLoader(), map.getPath());
        map.setMario(mario);
    }

     boolean createMap(ImageLoader loader, String path) {
        MapCreator mapCreator = new MapCreator(loader);
        map = mapCreator.createMap("/maps/" + path, 400);

        return map != null;
    }

     void acquirePoints(int point) {
        map.getMario().acquirePoints(point);
    }

    public Mario getMario() {
        return map.getMario();
    }


    public boolean isGameOver() {
        return getMario().getRemainingLives() == 0 || map.isTimeOver();
    }

    public int getScore() {
        return getMario().getPoints();
    }

    public int getRemainingLives() {
        return getMario().getRemainingLives();
    }

    public int getCoins() {
        return getMario().getCoins();
    }

    public void drawMap(Graphics2D g2) {
        map.drawMap(g2);
    }

    public int passMission() {
        if(getMario().getX() >= map.getEndPoint().getX() && !map.getEndPoint().isTouched()){
            map.getEndPoint().setTouched(true);
            int height = (int)getMario().getY();
            return height * 2;
        }
        else
            return -1;
    }

    public boolean endLevel(){
        return getMario().getX() >= map.getEndPoint().getX() + 320;
    }

    public void checkCollisions(GameEngine engine) {
        if (map == null) {
            return;
        }

        checkBottomCollisions(engine);
        checkTopCollisions(engine);
        checkMarioHorizontalCollision(engine);
        checkEnemyCollisions();
        checkFireballContact();
    }

    private void checkBottomCollisions(GameEngine engine) {
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();

        Rectangle marioBottomBounds = mario.getBottomBounds();

        if (!mario.isJumping())
            mario.setFalling(true);

        for (Brick brick : bricks) {
            Rectangle brickTopBounds = brick.getTopBounds();
            if (marioBottomBounds.intersects(brickTopBounds)) {
                mario.setY(brick.getY() - mario.getDimension().height + 1);
                mario.setFalling(false);
                mario.setVelY(0);
            }
        }

        for (Enemy enemy : enemies) {
            Rectangle enemyTopBounds = enemy.getTopBounds();
            if (marioBottomBounds.intersects(enemyTopBounds)) {
                mario.acquirePoints(100);
                toBeRemoved.add(enemy);
                engine.playStomp();
            }
        }

        if (mario.getY() + mario.getDimension().height >= map.getBottomBorder()) {
            mario.setY(map.getBottomBorder() - mario.getDimension().height);
            mario.setFalling(false);
            mario.setVelY(0);
        }

        removeObjects(toBeRemoved);
    }

    private void checkTopCollisions(GameEngine engine) {
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();

        Rectangle marioTopBounds = mario.getTopBounds();
        for (Brick brick : bricks) {
            Rectangle brickBottomBounds = brick.getBottomBounds();
            if (marioTopBounds.intersects(brickBottomBounds)) {
                mario.setVelY(0);
                mario.setY(brick.getY() + brick.getDimension().height);
            }
        }
    }

    private void checkMarioHorizontalCollision(GameEngine engine){
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();

        boolean marioDies = false;
        boolean toRight = mario.getToRight();

        Rectangle marioBounds = toRight ? mario.getRightBounds() : mario.getLeftBounds();

        for (Brick brick : bricks) {
            Rectangle brickBounds = !toRight ? brick.getRightBounds() : brick.getLeftBounds();
            if (marioBounds.intersects(brickBounds)) {
                mario.setVelX(0);
                if(toRight)
                    mario.setX(brick.getX() - mario.getDimension().width);
                else
                    mario.setX(brick.getX() + brick.getDimension().width);
            }
        }

        for(Enemy enemy : enemies){
            Rectangle enemyBounds = !toRight ? enemy.getRightBounds() : enemy.getLeftBounds();
            if (marioBounds.intersects(enemyBounds)) {
                marioDies = mario.onTouchEnemy(engine);
                toBeRemoved.add(enemy);
            }
        }
        removeObjects(toBeRemoved);


        if (mario.getX() <= engine.getCameraLocation().getX() && mario.getVelX() < 0) {
            mario.setVelX(0);
            mario.setX(engine.getCameraLocation().getX());
        }

        if(marioDies) {
            resetCurrentMap(engine);
        }
    }

    private void checkEnemyCollisions() {
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();

        for (Enemy enemy : enemies) {
            boolean standsOnBrick = false;

            for (Brick brick : bricks) {
                Rectangle enemyBounds = enemy.getLeftBounds();
                Rectangle brickBounds = brick.getRightBounds();

                Rectangle enemyBottomBounds = enemy.getBottomBounds();
                Rectangle brickTopBounds = brick.getTopBounds();

                if (enemy.getVelX() > 0) {
                    enemyBounds = enemy.getRightBounds();
                    brickBounds = brick.getLeftBounds();
                }

                if (enemyBounds.intersects(brickBounds)) {
                    enemy.setVelX(-enemy.getVelX());
                }

                if (enemyBottomBounds.intersects(brickTopBounds)){
                    enemy.setFalling(false);
                    enemy.setVelY(0);
                    enemy.setY(brick.getY()-enemy.getDimension().height);
                    standsOnBrick = true;
                }
            }

            if(enemy.getY() + enemy.getDimension().height > map.getBottomBorder()){
                enemy.setFalling(false);
                enemy.setVelY(0);
                enemy.setY(map.getBottomBorder()-enemy.getDimension().height);
            }

            if (!standsOnBrick && enemy.getY() < map.getBottomBorder()){
                enemy.setFalling(true);
            }
        }
    }


    private void checkFireballContact() {
        ArrayList<Enemy> enemies = map.getEnemies();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();


        removeObjects(toBeRemoved);
    }

    private void removeObjects(ArrayList<GameObject> list){
        if(list == null)
            return;
    }

    public void addRevealedBrick(OrdinaryBrick ordinaryBrick) {
        map.addRevealedBrick(ordinaryBrick);
    }

    public void updateTime(){
        if(map != null)
            map.updateTime(1);
    }

    public int getRemainingTime() {
        return (int)map.getRemainingTime();
    }
}


  class GameEngine implements Runnable {

    private final static int WIDTH = 1268, HEIGHT = 708;

    private MapManager mapManager;
    private UIManager uiManager;
    private SoundManager soundManager;
    private GameStatus gameStatus;
    private boolean isRunning;
    private Camera camera;
    private ImageLoader imageLoader;
    private Thread thread;
    private StartScreenSelection startScreenSelection = StartScreenSelection.START_GAME;
    private int selectedMap = 0;

    private GameEngine() {
        init();
    }

    private void init() {
        imageLoader = new ImageLoader();
        InputManager inputManager = new InputManager(this);
        gameStatus = GameStatus.MAP_SELECTION;
        camera = new Camera();
        uiManager = new UIManager(this, WIDTH, HEIGHT);
        soundManager = new SoundManager();
        mapManager = new MapManager();

        JFrame frame = new JFrame("Super Mario Bros.");
        frame.add(uiManager);
        frame.addKeyListener(inputManager);
        frame.addMouseListener(inputManager);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        start();
    }

    private synchronized void start() {
        if (isRunning)
            return;

        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    private void reset(){
        resetCamera();
        setGameStatus(GameStatus.START_SCREEN);
    }

      void resetCamera(){
        camera = new Camera();
        soundManager.restartBackground();
    }

      void selectMapViaMouse() {
        String path = uiManager.selectMapViaMouse(uiManager.getMousePosition());
        if (path != null) {
            createMap(path);
        }
    }

      void selectMapViaKeyboard(){
        String path = uiManager.selectMapViaKeyboard(selectedMap);
        if (path != null) {
            createMap(path);
        }
    }

      void changeSelectedMap(boolean up){
        selectedMap = uiManager.changeSelectedMap(selectedMap, up);
    }

    private void createMap(String path) {
        boolean loaded = mapManager.createMap(imageLoader, path);
        if(loaded){
            setGameStatus(GameStatus.RUNNING);
            soundManager.restartBackground();
        }

        else
            setGameStatus(GameStatus.START_SCREEN);
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();

        while (isRunning && !thread.isInterrupted()) {

            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                if (gameStatus == GameStatus.RUNNING) {
                    gameLoop();
                }
                delta--;
            }
            render();

            if(gameStatus != GameStatus.RUNNING) {
                timer = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                mapManager.updateTime();
            }
        }
    }

    private void render() {
        uiManager.repaint();
    }

    private void gameLoop() {
        updateLocations();
        checkCollisions();
        updateCamera();

        if (isGameOver()) {
            setGameStatus(GameStatus.GAME_OVER);
        }

        int missionPassed = passMission();
        if(missionPassed > -1){
            mapManager.acquirePoints(missionPassed);
            //setGameStatus(GameStatus.MISSION_PASSED);
        } else if(mapManager.endLevel())
            setGameStatus(GameStatus.MISSION_PASSED);
    }

    private void updateCamera() {
        Mario mario = mapManager.getMario();
        double marioVelocityX = mario.getVelX();
        double shiftAmount = 0;

        if (marioVelocityX > 0 && mario.getX() - 600 > camera.getX()) {
            shiftAmount = marioVelocityX;
        }

        camera.moveCam(shiftAmount, 0);
    }

    private void updateLocations() {
        mapManager.updateLocations();
    }

    private void checkCollisions() {
        mapManager.checkCollisions(this);
    }

      void receiveInput(ButtonAction input) {

        if (gameStatus == GameStatus.START_SCREEN) {
            if (input == ButtonAction.SELECT && startScreenSelection == StartScreenSelection.START_GAME) {
                startGame();
            } else if (input == ButtonAction.SELECT && startScreenSelection == StartScreenSelection.VIEW_ABOUT) {
                setGameStatus(GameStatus.ABOUT_SCREEN);
            } else if (input == ButtonAction.SELECT && startScreenSelection == StartScreenSelection.VIEW_HELP) {
                setGameStatus(GameStatus.HELP_SCREEN);
            } else if (input == ButtonAction.GO_UP) {
                selectOption(true);
            } else if (input == ButtonAction.GO_DOWN) {
                selectOption(false);
            }
        }
        else if(gameStatus == GameStatus.MAP_SELECTION){
            if(input == ButtonAction.SELECT){
                selectMapViaKeyboard();
            }
            else if(input == ButtonAction.GO_UP){
                changeSelectedMap(true);
            }
            else if(input == ButtonAction.GO_DOWN){
                changeSelectedMap(false);
            }
        } else if (gameStatus == GameStatus.RUNNING) {
            Mario mario = mapManager.getMario();
            if (input == ButtonAction.JUMP) {
                mario.jump(this);
            } else if (input == ButtonAction.M_RIGHT) {
                mario.move(true, camera);
            } else if (input == ButtonAction.M_LEFT) {
                mario.move(false, camera);
            } else if (input == ButtonAction.ACTION_COMPLETED) {
                mario.setVelX(0);
            } else if (input == ButtonAction.FIRE) {
            } else if (input == ButtonAction.PAUSE_RESUME) {
                pauseGame();
            }
        } else if (gameStatus == GameStatus.PAUSED) {
            if (input == ButtonAction.PAUSE_RESUME) {
                pauseGame();
            }
        } else if(gameStatus == GameStatus.GAME_OVER && input == ButtonAction.GO_TO_START_SCREEN){
            reset();
        } else if(gameStatus == GameStatus.MISSION_PASSED && input == ButtonAction.GO_TO_START_SCREEN){
            reset();
        }

        if(input == ButtonAction.GO_TO_START_SCREEN){
            setGameStatus(GameStatus.START_SCREEN);
        }
    }

    private void selectOption(boolean selectUp) {
        startScreenSelection = startScreenSelection.select(selectUp);
    }

    private void startGame() {
        if (gameStatus != GameStatus.GAME_OVER) {
            setGameStatus(GameStatus.MAP_SELECTION);
        }
    }

    private void pauseGame() {
        if (gameStatus == GameStatus.RUNNING) {
            setGameStatus(GameStatus.PAUSED);
            soundManager.pauseBackground();
        } else if (gameStatus == GameStatus.PAUSED) {
            setGameStatus(GameStatus.RUNNING);
            soundManager.resumeBackground();
        }
    }

      void shakeCamera(){
        camera.shakeCamera();
    }

    private boolean isGameOver() {
        if(gameStatus == GameStatus.RUNNING)
            return mapManager.isGameOver();
        return false;
    }

      ImageLoader getImageLoader() {
        return imageLoader;
    }

      GameStatus getGameStatus() {
        return gameStatus;
    }

      StartScreenSelection getStartScreenSelection() {
        return startScreenSelection;
    }

      void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

      int getScore() {
        return mapManager.getScore();
    }

      int getRemainingLives() {
        return mapManager.getRemainingLives();
    }

      int getCoins() {
        return mapManager.getCoins();
    }

      int getSelectedMap() {
        return selectedMap;
    }

      void drawMap(Graphics2D g2) {
        mapManager.drawMap(g2);
    }

      Point getCameraLocation() {
        return new Point((int)camera.getX(), (int)camera.getY());
    }

    private int passMission(){
        return mapManager.passMission();
    }

      void playCoin() {
        soundManager.playCoin();
    }

      void playOneUp() {
        soundManager.playOneUp();
    }

      void playSuperMushroom() {
        soundManager.playSuperMushroom();
    }

      void playMarioDies() {
        soundManager.playMarioDies();
    }

      void playJump() {
        soundManager.playJump();
    }

      void playFireFlower() {
        soundManager.playFireFlower();
    }

      void playFireball() {
        soundManager.playFireball();
    }

      void playStomp() {
        soundManager.playStomp();
    }

      MapManager getMapManager() {
        return mapManager;
    }

      public static void main(String[] args) {
        new GameEngine();
    }

      int getRemainingTime() {
        return mapManager.getRemainingTime();
    }
}




class Camera {

    private double x, y;
    private int frameNumber;
    private boolean shaking;

      Camera(){
        this.x = 0;
        this.y = 0;
        this.frameNumber = 25;
        this.shaking = false;
    }

      double getX() {
        return x;
    }

      void setX(double x) {
        this.x = x;
    }

      double getY() {
        return y;
    }

      void setY(double y) {
        this.y = y;
    }

      void shakeCamera() {
        shaking = true;
        frameNumber = 60;
    }

      void moveCam(double xAmount, double yAmount){
        if(shaking && frameNumber > 0){
            int direction = (frameNumber%2 == 0)? 1 : -1;
            x = x + 4 * direction;
            frameNumber--;
        }
        else{
            x = x + xAmount;
            y = y + yAmount;
        }

        if(frameNumber < 0)
            shaking = false;
    }
}


  enum GameStatus {
    GAME_OVER,
    PAUSED,
    RUNNING,
    START_SCREEN,
    MAP_SELECTION,
    HELP_SCREEN,
    MISSION_PASSED,
    ABOUT_SCREEN
}
  class InputManager implements KeyListener, MouseListener{

    private GameEngine engine;

    InputManager(GameEngine engine) {
        this.engine = engine; }

    @Override
    public void keyPressed(KeyEvent event) {
        int keyCode = event.getKeyCode();
        GameStatus status = engine.getGameStatus();
        ButtonAction currentAction = ButtonAction.NO_ACTION;

        if (keyCode == KeyEvent.VK_UP) {
            if(status == GameStatus.START_SCREEN || status == GameStatus.MAP_SELECTION)
                currentAction = ButtonAction.GO_UP;
            else
                currentAction = ButtonAction.JUMP;
        }
        else if(keyCode == KeyEvent.VK_DOWN){
            if(status == GameStatus.START_SCREEN || status == GameStatus.MAP_SELECTION)
                currentAction = ButtonAction.GO_DOWN;
        }
        else if (keyCode == KeyEvent.VK_RIGHT) {
            currentAction = ButtonAction.M_RIGHT;
        }
        else if (keyCode == KeyEvent.VK_LEFT) {
            currentAction = ButtonAction.M_LEFT;
        }
        else if (keyCode == KeyEvent.VK_ENTER) {
            currentAction = ButtonAction.SELECT;
        }
        else if (keyCode == KeyEvent.VK_ESCAPE) {
            if(status == GameStatus.RUNNING || status == GameStatus.PAUSED )
                currentAction = ButtonAction.PAUSE_RESUME;
            else
                currentAction = ButtonAction.GO_TO_START_SCREEN;

        }
        else if (keyCode == KeyEvent.VK_SPACE){
            currentAction = ButtonAction.FIRE;
        }


        notifyInput(currentAction);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(engine.getGameStatus() == GameStatus.MAP_SELECTION){
            engine.selectMapViaMouse();
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.VK_RIGHT || event.getKeyCode() == KeyEvent.VK_LEFT)
            notifyInput(ButtonAction.ACTION_COMPLETED);
    }

    private void notifyInput(ButtonAction action) {
        if(action != ButtonAction.NO_ACTION)
            engine.receiveInput(action);
    }

    @Override
    public void keyTyped(KeyEvent arg0) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

class MapCreator {

    private ImageLoader imageLoader;

    private BufferedImage backgroundImage;
    private BufferedImage superMushroom, oneUpMushroom, fireFlower, coin;
    private BufferedImage ordinaryBrick, surpriseBrick, groundBrick, pipe;
    private BufferedImage goombaLeft, goombaRight, koopaLeft, koopaRight, endFlag;


    MapCreator(ImageLoader imageLoader) {

        this.imageLoader = imageLoader;
        BufferedImage sprite = imageLoader.loadImage("/sprite.png");

        this.backgroundImage = imageLoader.loadImage("/background.png");
        this.superMushroom = imageLoader.getSubImage(sprite, 2, 5, 48, 48);
        this.oneUpMushroom= imageLoader.getSubImage(sprite, 3, 5, 48, 48);
        this.fireFlower= imageLoader.getSubImage(sprite, 4, 5, 48, 48);
        this.coin = imageLoader.getSubImage(sprite, 1, 5, 48, 48);
        this.ordinaryBrick = imageLoader.getSubImage(sprite, 1, 1, 48, 48);
        this.surpriseBrick = imageLoader.getSubImage(sprite, 2, 1, 48, 48);
        this.groundBrick = imageLoader.getSubImage(sprite, 2, 2, 48, 48);
        this.pipe = imageLoader.getSubImage(sprite, 3, 1, 96, 96);
        this.goombaLeft = imageLoader.getSubImage(sprite, 2, 4, 48, 48);
        this.goombaRight = imageLoader.getSubImage(sprite, 5, 4, 48, 48);
        this.koopaLeft = imageLoader.getSubImage(sprite, 1, 3, 48, 64);
        this.koopaRight = imageLoader.getSubImage(sprite, 4, 3, 48, 64);
        this.endFlag = imageLoader.getSubImage(sprite, 5, 1, 48, 48);

    }

    Map1 createMap(String mapPath, double timeLimit) {
        BufferedImage mapImage = imageLoader.loadImage(mapPath);

        if (mapImage == null) {
            System.out.println("Given path is invalid...");
            return null;
        }

        Map1 createdMap = new Map1(timeLimit, backgroundImage);
        String[] paths = mapPath.split("/");
        createdMap.setPath(paths[paths.length-1]);

        int pixelMultiplier = 48;

        int mario = new Color(160, 160, 160).getRGB();
        int ordinaryBrick = new Color(0, 0, 255).getRGB();
        int surpriseBrick = new Color(255, 255, 0).getRGB();
        int groundBrick = new Color(255, 0, 0).getRGB();
        int pipe = new Color(0, 255, 0).getRGB();
        int goomba = new Color(0, 255, 255).getRGB();
        int koopa = new Color(255, 0, 255).getRGB();
        int end = new Color(160, 0, 160).getRGB();

        for (int x = 0; x < mapImage.getWidth(); x++) {
            for (int y = 0; y < mapImage.getHeight(); y++) {

                int currentPixel = mapImage.getRGB(x, y);
                int xLocation = x*pixelMultiplier;
                int yLocation = y*pixelMultiplier;

                if (currentPixel == ordinaryBrick) {
                    Brick brick = new OrdinaryBrick(xLocation, yLocation, this.ordinaryBrick);
                    createdMap.addBrick(brick);
                }
                else if (currentPixel == surpriseBrick) {
                }
                else if (currentPixel == pipe) {
                    Brick brick = new Pipe(xLocation, yLocation, this.pipe);
                    createdMap.addGroundBrick(brick);
                }
                else if (currentPixel == groundBrick) {
                    Brick brick = new GroundBrick(xLocation, yLocation, this.groundBrick);
                    createdMap.addGroundBrick(brick);
                }
                else if (currentPixel == goomba) {
                    Enemy enemy = new Goomba(xLocation, yLocation, this.goombaLeft);
                    ((Goomba)enemy).setRightImage(goombaRight);
                    createdMap.addEnemy(enemy);
                }
                else if (currentPixel == mario) {
                    Mario marioObject = new Mario(xLocation, yLocation);
                    createdMap.setMario(marioObject);
                }
                else if(currentPixel == end){
                    EndFlag endPoint= new EndFlag(xLocation+24, yLocation, endFlag);
                    createdMap.setEndPoint(endPoint);
                }
            }
        }

        System.out.println("Map is created..");
        return createdMap;
    }



}

  class SoundManager {

    private Clip background;
    private long clipTime = 0;

      SoundManager() {
        background = getClip(loadAudio("background"));
    }

    private AudioInputStream loadAudio(String url) {
        try {
            InputStream audioSrc = getClass().getResourceAsStream("/media/audio/" + url + ".wav");
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            return AudioSystem.getAudioInputStream(bufferedIn);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    private Clip getClip(AudioInputStream stream) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

      void resumeBackground(){
        background.setMicrosecondPosition(clipTime);
        background.start();
    }

      void pauseBackground(){
        clipTime = background.getMicrosecondPosition();
        background.stop();
    }

      void restartBackground() {
        clipTime = 0;
        resumeBackground();
    }

      void playJump() {
        Clip clip = getClip(loadAudio("jump"));
        clip.start();

    }

      void playCoin() {
        Clip clip = getClip(loadAudio("coin"));
        clip.start();

    }

      void playFireball() {
        Clip clip = getClip(loadAudio("fireball"));
        clip.start();

    }

      void playGameOver() {
        Clip clip = getClip(loadAudio("gameOver"));
        clip.start();

    }

      void playStomp() {
        Clip clip = getClip(loadAudio("stomp"));
        clip.start();

    }

      void playOneUp() {
        Clip clip = getClip(loadAudio("oneUp"));
        clip.start();

    }

      void playSuperMushroom() {

        Clip clip = getClip(loadAudio("superMushroom"));
        clip.start();

    }

      void playMarioDies() {

        Clip clip = getClip(loadAudio("marioDies"));
        clip.start();

    }

      void playFireFlower() {

    }
}
  abstract class Brick extends GameObject{

    private boolean breakable;

    private boolean empty;

      Brick(double x, double y, BufferedImage style){
        super(x, y, style);
        setDimension(48, 48);
    }

      boolean isBreakable() {
        return breakable;
    }

      void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

      boolean isEmpty() {
        return empty;
    }

      void setEmpty(boolean empty) {
        this.empty = empty;
    }

}
  class GroundBrick extends Brick{

      GroundBrick(double x, double y, BufferedImage style){
        super(x, y, style);
        setBreakable(false);
        setEmpty(true);
    }

}
  class OrdinaryBrick extends Brick {

    private Animation animation;
    private boolean breaking;
    private int frames;

      OrdinaryBrick(double x, double y, BufferedImage style){
        super(x, y, style);
        setBreakable(true);
        setEmpty(true);

        setAnimation();
        breaking = false;
        frames = animation.getLeftFrames().length;
    }

    private void setAnimation(){
        ImageLoader imageLoader = new ImageLoader();
        BufferedImage[] leftFrames = imageLoader.getBrickFrames();

        animation = new Animation(leftFrames, leftFrames);
    }



      int getFrames(){
        return frames;
    }

      void animate(){
        if(breaking){
            setStyle(animation.animate(3, true));
            frames--;
        }
    }
}

  class Pipe extends Brick{

      Pipe(double x, double y, BufferedImage style){
        super(x, y, style);
        setBreakable(false);
        setEmpty(true);
        setDimension(96, 96);
    }
}




  abstract class Enemy extends GameObject{

      Enemy(double x, double y, BufferedImage style) {
        super(x, y, style);
        setFalling(false);
        setJumping(false);
    }
}



  class Goomba extends Enemy{

    private BufferedImage rightImage;

      Goomba(double x, double y, BufferedImage style) {
        super(x, y, style);
        setVelX(3);
    }

    @Override
      void draw(Graphics g){
        if(getVelX() > 0){
            g.drawImage(rightImage, (int)getX(), (int)getY(), null);
        }
        else
            super.draw(g);
    }

      void setRightImage(BufferedImage rightImage) {
        this.rightImage = rightImage;
    }
}

  class Mario extends GameObject{

    private int remainingLives;
    private int coins;
    private int points;
    private double invincibilityTimer;
    private MarioForm marioForm;
    private boolean toRight = true;

      Mario(double x, double y){
        super(x, y, null);
        setDimension(48,48);

        remainingLives = 3;
        points = 0;
        coins = 0;
        invincibilityTimer = 0;

        ImageLoader imageLoader = new ImageLoader();
        BufferedImage[] leftFrames = imageLoader.getLeftFrames(MarioForm.SMALL);
        BufferedImage[] rightFrames = imageLoader.getRightFrames(MarioForm.SMALL);

        Animation animation = new Animation(leftFrames, rightFrames);
        marioForm = new MarioForm(animation, false, false);
        setStyle(marioForm.getCurrentStyle(toRight, false, false));
    }

    @Override
      void draw(Graphics g){
        boolean movingInX = (getVelX() != 0);
        boolean movingInY = (getVelY() != 0);

        setStyle(marioForm.getCurrentStyle(toRight, movingInX, movingInY));

        super.draw(g);
    }

      void jump(GameEngine engine) {
        if(!isJumping() && !isFalling()){
            setJumping(true);
            setVelY(10);
            engine.playJump();
        }
    }

      void move(boolean toRight, Camera camera) {
        if(toRight){
            setVelX(5);
        }
        else if(camera.getX() < getX()){
            setVelX(-5);
        }

        this.toRight = toRight;
    }

      boolean onTouchEnemy(GameEngine engine){

        if(!marioForm.isSuper() && !marioForm.isFire()){
            remainingLives--;
            engine.playMarioDies();
            return true;
        }
        else{
            engine.shakeCamera();
            marioForm = marioForm.onTouchEnemy(engine.getImageLoader());
            setDimension(48, 48);
            return false;
        }
    }

      void acquireCoin() {
        coins++;
    }

      void acquirePoints(int point){
        points = points + point;
    }

      int getRemainingLives() {
        return remainingLives;
    }

      void setRemainingLives(int remainingLives) {
        this.remainingLives = remainingLives;
    }

      int getPoints() {
        return points;
    }

      int getCoins() {
        return coins;
    }

      MarioForm getMarioForm() {
        return marioForm;
    }

      void setMarioForm(MarioForm marioForm) {
        this.marioForm = marioForm;
    }

      boolean isSuper() {
        return marioForm.isSuper();
    }

      boolean getToRight() {
        return toRight;
    }

      void resetLocation() {
        setVelX(0);
        setVelY(0);
        setX(50);
        setJumping(false);
        setFalling(true);
    }
}
  class MarioForm {

      static final int SMALL = 0, SUPER = 1, FIRE = 2;

    private Animation animation;
    private boolean isSuper, isFire; //note: fire form has priority over super form
    private BufferedImage fireballStyle;

      MarioForm(Animation animation, boolean isSuper, boolean isFire){
        this.animation = animation;
        this.isSuper = isSuper;
        this.isFire = isFire;

        ImageLoader imageLoader = new ImageLoader();
        BufferedImage fireball = imageLoader.loadImage("/sprite.png");
        fireballStyle = imageLoader.getSubImage(fireball, 3, 4, 24, 24);
    }

      BufferedImage getCurrentStyle(boolean toRight, boolean movingInX, boolean movingInY){

        BufferedImage style;

        if(movingInY && toRight){
            style = animation.getRightFrames()[0];
        }
        else if(movingInY){
            style = animation.getLeftFrames()[0];
        }
        else if(movingInX){
            style = animation.animate(5, toRight);
        }
        else {
            if(toRight){
                style = animation.getRightFrames()[1];
            }
            else
                style = animation.getLeftFrames()[1];
        }

        return style;
    }

      MarioForm onTouchEnemy(ImageLoader imageLoader) {
        BufferedImage[] leftFrames = imageLoader.getLeftFrames(0);
        BufferedImage[] rightFrames= imageLoader.getRightFrames(0);

        Animation newAnimation = new Animation(leftFrames, rightFrames);

        return new MarioForm(newAnimation, false, false);
    }

      boolean isSuper() {
        return isSuper;
    }

      void setSuper(boolean aSuper) {
        isSuper = aSuper;
    }

      boolean isFire() {
        return isFire;
    }
}
  class EndFlag extends GameObject{

    private boolean touched = false;

      EndFlag(double x, double y, BufferedImage style) {
        super(x, y, style);
    }

    @Override
      void updateLocation() {
        if(touched){
            if(getY() + getDimension().getHeight() >= 576){
                setFalling(false);
                setVelY(0);
                setY(576 - getDimension().getHeight());
            }
            super.updateLocation();
        }
    }

      boolean isTouched() {
        return touched;
    }

      void setTouched(boolean touched) {
        this.touched = touched;
    }
}

  abstract class GameObject {

    private double x, y;

    private double velX, velY;

    private Dimension dimension;

    private BufferedImage style;

    private double gravityAcc;

    private boolean falling, jumping;

      GameObject(double x, double y, BufferedImage style){
        setLocation(x, y);
        setStyle(style);

        if(style != null){
            setDimension(style.getWidth(), style.getHeight());
        }

        setVelX(0);
        setVelY(0);
        setGravityAcc(0.38);
        jumping = false;
        falling = true;
    }

      void draw(Graphics g) {
        BufferedImage style = getStyle();

        if(style != null){
            g.drawImage(style, (int)x, (int)y, null);
        }

        //for debugging
        /*Graphics2D g2 = (Graphics2D)g;
        g2.setColor(Color.WHITE);

        g2.draw(getTopBounds());
        g2.draw(getBottomBounds());
        g2.draw(getRightBounds());
        g2.draw(getLeftBounds());*/
    }

      void updateLocation() {
        if(jumping && velY <= 0){
            jumping = false;
            falling = true;
        }
        else if(jumping){
            velY = velY - gravityAcc;
            y = y - velY;
        }

        if(falling){
            y = y + velY;
            velY = velY + gravityAcc;
        }

        x = x + velX;
    }

      void setLocation(double x, double y) {
        setX(x);
        setY(y);
    }

      double getX() {
        return x;
    }

      void setX(double x) {
        this.x = x;
    }

      double getY() {
        return y;
    }

      void setY(double y) {
        this.y = y;
    }

      Dimension getDimension(){
        return dimension;
    }

      void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

      void setDimension(int width, int height){ this.dimension =  new Dimension(width, height); }

      BufferedImage getStyle() {
        return style;
    }

      void setStyle(BufferedImage style) {
        this.style = style;
    }

      double getVelX() {
        return velX;
    }

      void setVelX(double velX) {
        this.velX = velX;
    }

      double getVelY() {
        return velY;
    }

      void setVelY(double velY) {
        this.velY = velY;
    }

      double getGravityAcc() {
        return gravityAcc;
    }

      void setGravityAcc(double gravityAcc) {
        this.gravityAcc = gravityAcc;
    }

      Rectangle getTopBounds(){
        return new Rectangle((int)x+dimension.width/6, (int)y, 2*dimension.width/3, dimension.height/2);
    }

      Rectangle getBottomBounds(){
        return new Rectangle((int)x+dimension.width/6, (int)y + dimension.height/2, 2*dimension.width/3, dimension.height/2);
    }

      Rectangle getLeftBounds(){
        return new Rectangle((int)x, (int)y + dimension.height/4, dimension.width/4, dimension.height/2);
    }

      Rectangle getRightBounds(){
        return new Rectangle((int)x + 3*dimension.width/4, (int)y + dimension.height/4, dimension.width/4, dimension.height/2);
    }

      Rectangle getBounds(){
        return new Rectangle((int)x, (int)y, dimension.width, dimension.height);
    }

      boolean isFalling() {
        return falling;
    }

      void setFalling(boolean falling) {
        this.falling = falling;
    }

      boolean isJumping() {
        return jumping;
    }

      void setJumping(boolean jumping) {
        this.jumping = jumping;
    }
}
 enum ButtonAction {
    JUMP,
    M_RIGHT,
    M_LEFT,
    CROUCH,
    FIRE,
    START,
    PAUSE_RESUME,
    ACTION_COMPLETED,
    SELECT,
    GO_UP,
    GO_DOWN,
    GO_TO_START_SCREEN,
    NO_ACTION
}


  class Animation {

    private int index = 0, count = 0;
    private BufferedImage[] leftFrames, rightFrames;
    private BufferedImage currentFrame;

      Animation(BufferedImage[] leftFrames, BufferedImage[] rightFrames){
        this.leftFrames = leftFrames;
        this.rightFrames = rightFrames;

        currentFrame = rightFrames[1];
    }

      BufferedImage animate(int speed, boolean toRight){
        count++;
        BufferedImage[] frames = toRight ? rightFrames : leftFrames;

        if(count > speed){
            nextFrame(frames);
            count = 0;
        }

        return currentFrame;
    }

    private void nextFrame(BufferedImage[] frames) {
        if(index + 3 > frames.length)
            index = 0;

        currentFrame = frames[index];
        index++;
    }

      BufferedImage[] getLeftFrames() {
        return leftFrames;
    }

      BufferedImage[] getRightFrames() {
        return rightFrames;
    }

}
  class ImageLoader {

    private BufferedImage marioForms;
    private BufferedImage brickAnimation;

      ImageLoader(){
        marioForms = loadImage("/mario-forms.png");
        brickAnimation = loadImage("/brick-animation.png");
    }

      BufferedImage loadImage(String path){
        BufferedImage imageToReturn = null;

        try {
            imageToReturn = ImageIO.read(getClass().getResource("/media" + path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageToReturn;
    }

      BufferedImage loadImage(File file){
        BufferedImage imageToReturn = null;

        try {
            imageToReturn = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageToReturn;
    }

      BufferedImage getSubImage(BufferedImage image, int col, int row, int w, int h){
        if((col == 1 || col == 4) && row == 3){ //koopa
            return image.getSubimage((col-1)*48, 128, w, h);
        }
        return image.getSubimage((col-1)*48, (row-1)*48, w, h);
    }

      BufferedImage[] getLeftFrames(int marioForm){
        BufferedImage[] leftFrames = new BufferedImage[5];
        int col = 1;
        int width = 52, height = 48;

        if(marioForm == 1) { //super mario
            col = 4;
            width = 48;
            height = 96;
        }
        else if(marioForm == 2){ //fire mario
            col = 7;
            width = 48;
            height = 96;
        }

        for(int i = 0; i < 5; i++){
            leftFrames[i] = marioForms.getSubimage((col-1)*width, (i)*height, width, height);
        }
        return leftFrames;
    }

      BufferedImage[] getRightFrames(int marioForm){
        BufferedImage[] rightFrames = new BufferedImage[5];
        int col = 2;
        int width = 52, height = 48;

        if(marioForm == 1) { //super mario
            col = 5;
            width = 48;
            height = 96;
        }
        else if(marioForm == 2){ //fire mario
            col = 8;
            width = 48;
            height = 96;
        }

        for(int i = 0; i < 5; i++){
            rightFrames[i] = marioForms.getSubimage((col-1)*width, (i)*height, width, height);
        }
        return rightFrames;
    }

      BufferedImage[] getBrickFrames() {
        BufferedImage[] frames = new BufferedImage[4];
        for(int i = 0; i < 4; i++){
            frames[i] = brickAnimation.getSubimage(i*105, 0, 105, 105);
        }
        return frames;
    }
}

  class MapSelection {

    private ArrayList<String> maps = new ArrayList<>();
    private MapSelectionItem[] mapSelectionItems;

      MapSelection(){
        getMaps();
        this.mapSelectionItems = createItems(this.maps);
    }

      void draw(Graphics g){
        g.setColor(Color.BLACK);
        g.fillRect(0,0, 1280, 720);

        if(mapSelectionItems == null){
            System.out.println(1);
            return;
        }

        for(MapSelectionItem item : mapSelectionItems){
            g.setColor(Color.WHITE);
            int width = g.getFontMetrics().stringWidth(item.getName().split("[.]")[0]);
            int height = g.getFontMetrics().getHeight();
            item.setDimension( new Dimension(width, height));
            item.setLocation( new Point((1280-width)/2, item.getLocation().y));
            g.drawString(item.getName().split("[.]")[0], item.getLocation().x + 30, item.getLocation().y);
        }
    }

    private void getMaps(){
        //TODO: read from file
        maps.add("Start Game.png");
    }

    private MapSelectionItem[] createItems(ArrayList<String> maps){
        if(maps == null)
            return null;

        int defaultGridSize = 100;
        MapSelectionItem[] items = new MapSelectionItem[maps.size()];
        for (int i = 0; i < items.length; i++) {
            Point location = new Point(0, (i+1)*defaultGridSize+200);
            items[i] = new MapSelectionItem(maps.get(i), location);
        }

        return items;
    }

      String selectMap(Point mouseLocation) {
        for(MapSelectionItem item : mapSelectionItems) {
            Dimension dimension = item.getDimension();
            Point location = item.getLocation();
            boolean inX = location.x <= mouseLocation.x && location.x + dimension.width >= mouseLocation.x;
            boolean inY = location.y >= mouseLocation.y && location.y - dimension.height <= mouseLocation.y;
            if(inX && inY){
                return item.getName();
            }
        }
        return null;
    }

      String selectMap(int index){
        if(index < mapSelectionItems.length && index > -1)
            return mapSelectionItems[index].getName();
        return null;
    }

      int changeSelectedMap(int index, boolean up) {
        if(up){
            if(index <= 0)
                return mapSelectionItems.length - 1;
            else
                return index - 1;
        }
        else{
            if(index >= mapSelectionItems.length - 1)
                return 0;
            else
                return index + 1;
        }
    }
}

  class MapSelectionItem {

    private BufferedImage image;
    private String name;
    private Point location;
    private Dimension dimension;

      MapSelectionItem(String map, Point location){
        this.location = location;
        this.name = map;

        ImageLoader loader = new ImageLoader();
        this.image = loader.loadImage("/maps/" + "Start Game.png");

        this.dimension = new Dimension();
    }


      String getName() {
        return name;
    }

      Point getLocation() {
        return location;
    }

      Dimension getDimension() {
        return dimension;
    }

      void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

      void setLocation(Point location) {
        this.location = location;
    }
}



  enum StartScreenSelection {
    START_GAME(0),
    VIEW_HELP(1),
    VIEW_ABOUT(2);

    private final int lineNumber;
    StartScreenSelection(int lineNumber){ this.lineNumber = lineNumber; }

      StartScreenSelection getSelection(int number){
        if(number == 0)
            return START_GAME;
        else if(number == 1)
            return VIEW_HELP;
        else if(number == 2)
            return VIEW_ABOUT;
        else return null;
    }

      StartScreenSelection select(boolean toUp){
        int selection;

        if(lineNumber > -1 && lineNumber < 3){
            selection = lineNumber - (toUp ? 1 : -1);
            if(selection == -1)
                selection = 2;
            else if(selection == 3)
                selection = 0;
            return getSelection(selection);
        }

        return null;
    }

      int getLineNumber() {
        return lineNumber;
    }
}


  class UIManager extends JPanel{

    private GameEngine engine;
    private Font gameFont;
    private BufferedImage startScreenImage, aboutScreenImage, helpScreenImage, gameOverScreen;
    private BufferedImage heartIcon;
    private BufferedImage coinIcon;
    private BufferedImage selectIcon;
    private MapSelection mapSelection;

      UIManager(GameEngine engine, int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));

        this.engine = engine;
        ImageLoader loader = engine.getImageLoader();

        mapSelection = new MapSelection();

        BufferedImage sprite = loader.loadImage("/sprite.png");
        this.heartIcon = loader.loadImage("/heart-icon.png");
        this.coinIcon = loader.getSubImage(sprite, 1, 5, 48, 48);
        this.selectIcon = loader.loadImage("/select-icon.png");
        this.startScreenImage = loader.loadImage("/start-screen.png");
        this.helpScreenImage = loader.loadImage("/help-screen.png");
        this.aboutScreenImage = loader.loadImage("/about-screen.png");
        this.gameOverScreen = loader.loadImage("/game-over.png");

        try {
            InputStream in = getClass().getResourceAsStream("/media/font/mario-font.ttf");
            gameFont = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (FontFormatException | IOException e) {
            gameFont = new Font("Verdana", Font.PLAIN, 12);
            e.printStackTrace();
        }
    }

    @Override
      public void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        GameStatus gameStatus = engine.getGameStatus();

        if(gameStatus == GameStatus.START_SCREEN){
            drawStartScreen(g2);
        }
        else if(gameStatus == GameStatus.MAP_SELECTION){
            drawMapSelectionScreen(g2);
        }
        else if(gameStatus == GameStatus.ABOUT_SCREEN){
            drawAboutScreen(g2);
        }
        else if(gameStatus == GameStatus.HELP_SCREEN){
            drawHelpScreen(g2);
        }
        else if(gameStatus == GameStatus.GAME_OVER){
            drawGameOverScreen(g2);
        }
        else {
            Point camLocation = engine.getCameraLocation();
            g2.translate(-camLocation.x, -camLocation.y);
            engine.drawMap(g2);
            g2.translate(camLocation.x, camLocation.y);

            drawPoints(g2);
            drawRemainingLives(g2);
            drawAcquiredCoins(g2);
            drawRemainingTime(g2);

            if(gameStatus == GameStatus.PAUSED){
                drawPauseScreen(g2);
            }
            else if(gameStatus == GameStatus.MISSION_PASSED){
                drawVictoryScreen(g2);
            }
        }

        g2.dispose();
    }

    private void drawRemainingTime(Graphics2D g2) {

    }

    private void drawVictoryScreen(Graphics2D g2) {

    }

    private void drawHelpScreen(Graphics2D g2) {
        g2.drawImage(helpScreenImage, 0, 0, null);
    }

    private void drawAboutScreen(Graphics2D g2) {
        g2.drawImage(aboutScreenImage, 0, 0, null);
    }

    private void drawGameOverScreen(Graphics2D g2) {

    }

    private void drawPauseScreen(Graphics2D g2) {

    }

    private void drawAcquiredCoins(Graphics2D g2) {

    }

    private void drawRemainingLives(Graphics2D g2) {

    }

    private void drawPoints(Graphics2D g2){

    }

    private void drawStartScreen(Graphics2D g2){

    }

    private void drawMapSelectionScreen(Graphics2D g2){
        g2.setFont(gameFont.deriveFont(50f));
        g2.setColor(Color.WHITE);
        mapSelection.draw(g2);
        int row = engine.getSelectedMap();
        int y_location = row*100+300-selectIcon.getHeight();
        g2.drawImage(selectIcon, 375, y_location, null);
    }

      String selectMapViaMouse(Point mouseLocation) {
        return mapSelection.selectMap(mouseLocation);
    }

      String selectMapViaKeyboard(int index){
        return mapSelection.selectMap(index);
    }

      int changeSelectedMap(int index, boolean up){
        return mapSelection.changeSelectedMap(index, up);
    }
}
class Map1 {

    private double remainingTime;
    private Mario mario;
    private ArrayList<Brick> bricks = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Brick> groundBricks = new ArrayList<>();
    private ArrayList<Brick> revealedBricks = new ArrayList<>();
    private EndFlag endPoint;
    private BufferedImage backgroundImage;
    private double bottomBorder = 720 - 96;
    private String path;


    public Map1(double remainingTime, BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
        this.remainingTime = remainingTime;
    }


    public Mario getMario() {
        return mario;
    }

    public void setMario(Mario mario) {
        this.mario = mario;
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }


    public ArrayList<Brick> getAllBricks() {
        ArrayList<Brick> allBricks = new ArrayList<>();

        allBricks.addAll(bricks);
        allBricks.addAll(groundBricks);

        return allBricks;
    }

    public void addBrick(Brick brick) {
        this.bricks.add(brick);
    }

    public void addGroundBrick(Brick brick) {
        this.groundBricks.add(brick);
    }

    public void addEnemy(Enemy enemy) {
        this.enemies.add(enemy);
    }

    public void drawMap(Graphics2D g2){
        drawBackground(g2);
        drawBricks(g2);
        drawEnemies(g2);
        drawMario(g2);
        endPoint.draw(g2);
    }

    private void drawBackground(Graphics2D g2){
        g2.drawImage(backgroundImage, 0, 0, null);
    }

    private void drawBricks(Graphics2D g2) {
        for(Brick brick : bricks){
            if(brick != null)
                brick.draw(g2);
        }

        for(Brick brick : groundBricks){
            brick.draw(g2);
        }
    }

    private void drawEnemies(Graphics2D g2) {
        for(Enemy enemy : enemies){
            if(enemy != null)
                enemy.draw(g2);
        }
    }

    private void drawMario(Graphics2D g2) {
        mario.draw(g2);
    }

    public void updateLocations() {
        mario.updateLocation();
        for(Enemy enemy : enemies){
            enemy.updateLocation();
        }

        for(Iterator<Brick> brickIterator = revealedBricks.iterator(); brickIterator.hasNext();){
            OrdinaryBrick brick = (OrdinaryBrick)brickIterator.next();
            brick.animate();
            if(brick.getFrames() < 0){
                bricks.remove(brick);
                brickIterator.remove();
            }
        }

        endPoint.updateLocation();
    }

    public double getBottomBorder() {
        return bottomBorder;
    }



    public void setEndPoint(EndFlag endPoint) {
        this.endPoint = endPoint;
    }

    public EndFlag getEndPoint() {
        return endPoint;
    }

    public void addRevealedBrick(OrdinaryBrick ordinaryBrick) {
        revealedBricks.add(ordinaryBrick);
    }

    public void removeEnemy(Enemy object) {
        enemies.remove(object);
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void updateTime(double passed){
        remainingTime = remainingTime - passed;
    }

    public boolean isTimeOver(){
        return remainingTime <= 0;
    }

    public double getRemainingTime() {
        return remainingTime;
    }
}
