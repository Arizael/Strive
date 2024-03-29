package de.arizael.strive;

import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.arizael.strive.entities.CustomCreatureMapObjectLoader;
import de.arizael.strive.entities.DecorMob;
import de.arizael.strive.entities.Enemy;
import de.arizael.strive.entities.Gatekeeper;
import de.arizael.strive.entities.HealthPot;
import de.arizael.strive.entities.Player;
import de.arizael.strive.entities.Slave;
import de.arizael.strive.entities.Player.PlayerState;
import de.arizael.strive.ui.IngameScreen;
import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.entities.Spawnpoint;
import de.gurkenlabs.litiengine.environment.CreatureMapObjectLoader;
import de.gurkenlabs.litiengine.environment.Environment;
import de.gurkenlabs.litiengine.environment.PropMapObjectLoader;
import de.gurkenlabs.litiengine.environment.tilemap.MapProperty;
import de.gurkenlabs.litiengine.graphics.Camera;
import de.gurkenlabs.litiengine.graphics.PositionLockCamera;
import de.gurkenlabs.litiengine.gui.GuiProperties;
import de.gurkenlabs.litiengine.gui.SpeechBubbleAppearance;
import de.gurkenlabs.litiengine.resources.Resources;

public final class GameManager {
  public enum GameState {
    INGAME,
    MENU,
    INGAME_MENU,
    SLAVES_DEAD
  }

  public static final Font GUI_FONT = Resources.fonts().get("fsex300.ttf").deriveFont(32f);
  public static final Font SPEECH_BUBBLE_FONT = GUI_FONT.deriveFont(4f);
  public static final Font MENU_FONT = Resources.fonts().get("caesar.ttf").deriveFont(40f);
  public static final Font GUI_FONT_ALT = Resources.fonts().get("roman.ttf").deriveFont(40f);
  public static String START_LEVEL = "level0";

  public static final SpeechBubbleAppearance SPEECH_BUBBLE_APPEARANCE = new SpeechBubbleAppearance(new Color(16, 20, 19), new Color(255, 255, 255, 150), new Color(16, 20, 19), 5);

  private static final Map<String, Runnable> startups = new ConcurrentHashMap<>();
  static {
    SPEECH_BUBBLE_APPEARANCE.setBackgroundColor2(new Color(255, 255, 255, 220));

    startups.put("level0", () -> {
      Camera cam = new Camera();
      cam.setFocus(Game.world().environment().getCenter());
      Game.world().setCamera(cam);
    });

    startups.put("level1", () -> {
      Camera camera = new PositionLockCamera(Player.instance());
      camera.setClampToMap(true);
      Game.world().setCamera(camera);
    });
  }

  private static GameState state = GameState.MENU;

  private GameManager() {
  }

  public static void init() {
    GuiProperties.setDefaultFont(GUI_FONT);

    Environment.registerMapObjectLoader(new CustomCreatureMapObjectLoader());

    CreatureMapObjectLoader.registerCustomCreatureType(DecorMob.class);

    CreatureMapObjectLoader.registerCustomCreatureType(Slave.class);
    CreatureMapObjectLoader.registerCustomCreatureType(Enemy.class);
    CreatureMapObjectLoader.registerCustomCreatureType(Gatekeeper.class);

    PropMapObjectLoader.registerCustomPropType(HealthPot.class);

    Camera camera = new PositionLockCamera(Player.instance());
    camera.setClampToMap(true);
    Game.world().setCamera(camera);

    // add default game logic for when a level was loaded

    Game.world().addLoadedListener(e -> {
      Game.loop().perform(500, () -> Game.window().getRenderComponent().fadeIn(500));

      if (startups.containsKey(e.getMap().getName())) {
        startups.get(e.getMap().getName()).run();
      }
      setState(GameState.INGAME);
      Player.instance().getHitPoints().setToMaxValue();
      Player.instance().setIndestructible(false);
      Player.instance().setCollision(true);
      // spawn the player instance on the spawn point with the name "enter"
      Spawnpoint enter = e.getSpawnpoint("enter");
      if (enter != null) {
        enter.spawn(Player.instance());
      }
    });
  }

  public static int getOwnSlaveCount() {
    return (int) Game.world().environment().getByType(Slave.class).stream().filter(x -> !x.isDead() && x.getOwner() == null).count();
  }

  public static int getAliveSlaveCount() {
    return (int) Game.world().environment().getByType(Slave.class).stream().filter(x -> !x.isDead()).count();
  }

  public static Gatekeeper getGateKeeper() {
    return Game.world().environment().get(Gatekeeper.class, "keeper");
  }

  public static int getRequiredSlaveCount() {
    Gatekeeper keeper = getGateKeeper();
    if (keeper == null) {
      return 0;
    }

    return keeper.getRequiredSlaves();
  }

  public static String getCity(String levelName) {
    return Game.world().getEnvironment(levelName).getMap().getStringValue(MapProperty.MAP_TITLE);
  }

  public static String getCurrentCity() {
    return getCity(Game.world().environment().getMap().getName());
  }

  public static GameState getState() {
    return state;
  }

  public static void setState(GameState state) {
    GameManager.state = state;

    if (getState() == GameState.INGAME_MENU) {
      Game.loop().setTimeScale(0);
      IngameScreen.ingameMenu.setVisible(true);
    } else {
      Game.loop().setTimeScale(1);
      IngameScreen.ingameMenu.setVisible(false);
    }

    if (getState() == GameState.SLAVES_DEAD) {
      Game.audio().playSound("fail.ogg");
      Player.instance().setState(PlayerState.LOCKED);
      Player.instance().setIndestructible(true);
    }
  }
}
