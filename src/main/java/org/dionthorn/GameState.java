package org.dionthorn;

import java.util.ArrayList;
import java.util.Arrays;

public class GameState {
    // Simply used as flags to determine the 'state' of the game
    enum STATE {MAIN_MENU, BATTLE, GAME, GAME_OVER, LEVEL_SELECTION, CHARACTER_STATUS, CHARACTER_CREATION}
    // The currently assigned 'state'
    private STATE currentState;
    // Needed references to game objects
    private ArrayList<Map> maps; // List of all created maps
    private ArrayList<Entity> entities; // List of all created entities
    private Map currentMap; // The currently selected map, which is to be displayed
    // Entity list of player/enemies team used for map level
    private ArrayList<Entity> playerTeam;
    private ArrayList<Entity> enemyTeam;
    private boolean nextTurn; // Flag to determine turn transitions
    // Battle Scene references to an attacker and defender
    private Character attacker;
    private Character defender;

    public GameState(Map initialMap) {
        maps = new ArrayList<>();
        maps.add(initialMap); // Load initial map on startup
        currentMap = initialMap;
        currentState = STATE.MAIN_MENU; // Start at the main menu screen
        entities = new ArrayList<>();
        playerTeam = new ArrayList<>();
        enemyTeam = new ArrayList<>();
        attacker = null;
        defender = null;
        Entity.GEN_COUNT = 0;
    }

    public void createPlayerTeam(Entity... toAddList) {
        // Add an arbitrary list of Entity objects to the playerTeam ArrayList
        // Will clear any previous assignment!
        playerTeam.clear();
        playerTeam.addAll(Arrays.asList(toAddList));
        playerTeam.trimToSize();
    }

    public void createEnemyTeam(Entity... toAddList) {
        // Add an arbitrary list of Entity objects to the enemyTeam ArrayList
        // Will clear any previous assignment!
        enemyTeam.clear();
        enemyTeam.addAll(Arrays.asList(toAddList));
        enemyTeam.trimToSize();
    }

    public void startBattle(Character attacker, Character defender) {
        if(Run.DEBUG_OUTPUT) {
            System.out.println("Battle Started");
        }
        // Set attacker/defender and start battle
        this.currentState = STATE.BATTLE;
        this.attacker = attacker;
        this.defender = defender;
        this.attacker.setBattleTurn(true);
    }
    // Getters and Setters
    public boolean getNextTurn() { return nextTurn; }

    public void setNextTurn(boolean value) { nextTurn = value; }

    public ArrayList<Entity> getPlayerTeam() { return playerTeam; }

    public ArrayList<Entity> getEnemyTeam() { return enemyTeam; }

    public void nextTurn(int UID) {
        for(Entity e: entities) {
            if(e.getUID() == UID) {
                ((Character) e).setMoveTurn(true);
            }
        }
    }

    public ArrayList<Map> getMaps() { return maps; }

    public ArrayList<Entity> getEntities() { return entities; }

    public Map getCurrentMap() { return currentMap; }

    public void setCurrentMap(Map newMap) {
        // Have a load of map also reset player team and enemy team x,y,etc based on .meta file
        currentMap = newMap;
        String[] data = Map.getFileLines(currentMap.getMetaPATH());
        String[] enemies = new String[0];
        for(String line: data) {
            if(line.contains("ENEMIES")) {
                enemies = line.split(":")[0].split("/");
            }
        }
        String[] allies = new String[0];
        for(String line: data) {
            if(line.contains("ALLIES")) {
                allies = line.split(":")[0].split("/");
            }
        }
        // clear stuff
        playerTeam.clear();
        enemyTeam.clear();
        Entity tempPlayer = getPlayerEntity();
        entities.clear();
        entities.add(tempPlayer);
        // add Player and ally NPCs
        playerTeam.add(getPlayerEntity());
        CharacterClass tempClass = null;
        for(String ally: allies) {
            if(!ally.equals("")) {
                String[] temp = ally.split(",");
                String name = temp[0];
                int x = Integer.parseInt(temp[1]);
                int y = Integer.parseInt(temp[2]);
                if(temp[3].equals("magic")) {
                    tempClass = new MagicClass();
                } else if(temp[3].equals("martial")) {
                    tempClass = new MartialClass();
                }
                assert tempClass != null;
                NonPlayerCharacter tempChar = new NonPlayerCharacter(currentMap, tempClass.getDefaultSpriteAlly(), name, x, y, tempClass);
                entities.add(tempChar);
                playerTeam.add(tempChar);
            }
        }
        // add enemy NPCs
        for(String enemy: enemies) {
            if(!enemy.equals("")) {
                String[] temp = enemy.split(",");
                String name = temp[0];
                int x = Integer.parseInt(temp[1]);
                int y = Integer.parseInt(temp[2]);
                if(temp[3].equals("magic")) {
                    tempClass = new MagicClass();
                } else if(temp[3].equals("martial")) {
                    tempClass = new MartialClass();
                }
                NonPlayerCharacter tempChar = new NonPlayerCharacter(currentMap, tempClass.getDefaultSpriteEnemy(), name, x, y, tempClass);
                entities.add(tempChar);
                enemyTeam.add(tempChar);
            }
        }
        playerTeam.trimToSize();
        enemyTeam.trimToSize();
        entities.trimToSize();
    }

    public STATE getCurrentState() { return currentState; }

    public void setState(STATE newState) { currentState = newState; }

    public Player getPlayerEntity() {
        // Will either return the player if they exist or null (player should always exist)
        for(Entity e: entities) {
            if(e instanceof Player) {
                return (Player) e;
            }
        }
        return null;
    }

    public Character getAttacker() { return attacker; }

    public Character getDefender() { return defender; }
}

