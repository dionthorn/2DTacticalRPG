package org.dionthorn;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * The GameState class will be responsible for the master references to game objects via its .entities list
 * as well as all maps via its .maps list
 * it will also manage the 'state' of the game or different 'screens' of the game
 */
public class GameState {

    enum STATE {MAIN_MENU, BATTLE, GAME, GAME_OVER, LEVEL_SELECTION, CHARACTER_STATUS, CHARACTER_CREATION}

    private STATE currentState;
    private ArrayList<Map> maps;
    private ArrayList<Entity> entities;
    private Map currentMap;
    private ArrayList<Entity> playerTeam;
    private ArrayList<Entity> enemyTeam;
    private boolean nextTurn;
    private Character attacker;
    private Character defender;

    /**
     * Default GameState Constructor will generate the base state need for the game to initiate.
     * @param initialMap the default map to load for seamless initial game flow
     */
    public GameState(Map initialMap) {
        maps = new ArrayList<>();
        maps.add(initialMap);
        currentMap = initialMap;
        currentState = STATE.MAIN_MENU;
        entities = new ArrayList<>();
        playerTeam = new ArrayList<>();
        enemyTeam = new ArrayList<>();
        attacker = null;
        defender = null;
        Entity.GEN_COUNT = 0;
    }

    /**
     * Will begin a battle between the characters provided
     * @param attacker the assaulting character
     * @param defender the defending character
     */
    public void startBattle(Character attacker, Character defender) {
        Run.programLogger.log(Level.INFO, "Battle Started");
        this.currentState = STATE.BATTLE;
        this.attacker = attacker;
        this.defender = defender;
        this.attacker.setBattleTurn(true);
    }

    // Getters and Setters
    /**
     * Returns the boolean value of nextTurn flag
     * @return the boolean value of the next turn flag
     */
    public boolean getNextTurn() { return nextTurn; }

    /**
     * Will set the nextTurn boolean flag.
     * @param value the value to assign to next turn flag
     */
    public void setNextTurn(boolean value) { nextTurn = value; }

    /**
     * Returns the playerTeam entity ArrayList.
     * @return the player team entity array list
     */
    public ArrayList<Entity> getPlayerTeam() { return playerTeam; }

    /**
     * Returns the enemyTeam entity ArrayList.
     * @return the enemy team entity array list
     */
    public ArrayList<Entity> getEnemyTeam() { return enemyTeam; }

    /**
     * will assign the entity with UID to take their move turn.
     * @param UID the integer id of the entity to advance to their move turn
     */
    public void nextTurn(int UID) {
        for(Entity e: entities) {
            if(e.getUID() == UID) {
                ((Character) e).setMoveTurn(true);
            }
        }
    }

    /**
     * Returns the GameState map ArrayList.
     * @return the game state map array list
     */
    public ArrayList<Map> getMaps() { return maps; }

    /**
     * Returns the GameState entities ArrayList.
     * @return the game state entities array list
     */
    public ArrayList<Entity> getEntities() { return entities; }

    /**
     * Returns the GameState current map object.
     * @return the game state current map
     */
    public Map getCurrentMap() { return currentMap; }

    /**
     * Will set the GameState currentMap to the provided newMap
     * @param newMap set the game state current map to this new map
     */
    public void setCurrentMap(Map newMap) {
        currentMap = newMap;
        String[] data;
        if(newMap.getPATH().contains("RANDOM")) {
            newMap.saveData();
        }
        data = FileOps.getFileLines(currentMap.getMetaPATH());
        ArrayList<ArrayList<Integer>> mapDataTileMetaIDs = new ArrayList<>();
        for(int i=0; i< MapTile.TileType.values().length; i++) {
            mapDataTileMetaIDs.add(new ArrayList<>());
        }
        for(String line: data) {
            if(line.contains("FIRE")) {
                String[] tileIdsFire = line.split(":")[0].split(",");
                for (String s : tileIdsFire) {
                    if (!s.equals("")) {
                        int fireTag = Integer.parseInt(s);
                        mapDataTileMetaIDs.get(MapTile.TileType.FIRE.ordinal()).add(fireTag);
                    }
                }
            } else if(line.contains("IMPASSABLE")) {
                String[] tileIdsImpassable = line.split(":")[0].split(",");
                for (String s : tileIdsImpassable) {
                    if (!s.equals("")) {
                        int impassableTag = Integer.parseInt(s);
                        mapDataTileMetaIDs.get(MapTile.TileType.IMPASSABLE.ordinal()).add(impassableTag);
                    }
                }
            }
        }
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
        String[] loc = new String[2];
        for(String line: data) {
            if(line.contains("STARTLOC")) {
                loc = line.split(":")[0].split(",");
            }
        }
        playerTeam.clear();
        enemyTeam.clear();
        Entity tempPlayer = getPlayerEntity();
        entities.clear();
        entities.add(tempPlayer);
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
                NonPlayerCharacter tempChar = new NonPlayerCharacter(currentMap,
                        tempClass.getDefaultSpriteAlly(), name, x, y, tempClass
                );
                entities.add(tempChar);
                playerTeam.add(tempChar);
            }
        }
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
                assert tempClass != null;
                NonPlayerCharacter tempChar = new NonPlayerCharacter(currentMap,
                        tempClass.getDefaultSpriteEnemy(), name, x, y, tempClass
                );
                entities.add(tempChar);
                enemyTeam.add(tempChar);
            }
        }
        getPlayerEntity().setCurrentMap(currentMap, Integer.parseInt(loc[0]), Integer.parseInt(loc[1]));
        playerTeam.trimToSize();
        enemyTeam.trimToSize();
        entities.trimToSize();
    }

    /**
     * Returns the currentState of the GameState.
     * @return the current state of the game state
     */
    public STATE getCurrentState() { return currentState; }

    /**
     * Will set the currentState of the GameState to the provided newState
     * @param newState the target new state to place the game state into
     */
    public void setState(STATE newState) { currentState = newState; }

    /**
     * Returns the current Player Character Object. Player object should always exist from Run.newGame()
     * @return the current player character
     */
    public Player getPlayerEntity() {
        for(Entity e: entities) {
            if(e instanceof Player) {
                return (Player) e;
            }
        }
        return null;
    }

    /**
     * Returns the current attacker Character object.
     * @return the current attacker character
     */
    public Character getAttacker() { return attacker; }

    /**
     * Returns the current defender Character object.
     * @return the current defender character
     */
    public Character getDefender() { return defender; }
}

