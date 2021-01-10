package examplefuncsplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runSlanderer() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

// -----> Communication between the Enlightment Center (the main controller of robots) and the other friendly robots on the same team
// this is for the robot to detect its surrounding location using pathfinding algorithm
static void sendLocation() throws GameActionException {
    MapLocation location = rc.getLocation();
    int x = location.x, y = location.y;
    int encodedlocation = (x % 128) * 128 + (y % 128); //using 128 instead of 64 for more precise and more accurate location 
    if (rc.canSetFlag(encodedlocation)) {
        rc.setFlag(encodedlocation);
    }
}


static void sendLocation(int extraInformation) throws GameActionException {
     MapLocation location = rc.getLocation();
    int x = location.x, y = location.y;
    int encodedlocation = (x % 128) * 128 + (y % 128) + extraInformation * 128 * 128; //This is used for the the extra 10 bits remaining for any extra information for the robot
    if (rc.canSetFlag(encodedlocation)) {
        rc.setFlag(encodedlocation);
    }
}

static MapLocation getLocationFromFlag(int flag){
        int y = flag % 128;
        int x = (flag / 128) % 128;
        int extraInformation = flag / 128 / 128;

        MapLocation currentLocation =  rc.getLocation(); // to get the current location of our robot's position on the map
        int offsetX128 = currentLocation.x / 128; 
        int offsetY128 =  currentLocation.y / 128;
        MapLocation actualLocation = new MapLocation(offsetX128 * 128 + x, offsetY128 * 128 + y);

        MapLocation alternative = actualLocation.translate(-128, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }

        alternative = actualLocation.translate(128, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }

        alternative = actualLocation.translate(0, -128);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }

        alternative = actualLocation.translate(0, 128);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)){
            actualLocation = alternative;
        }

        return actualLocation;
    }

// ------> Pathfinding logic
        public static void run(){
            while(true){
                MapLocation target = getLocationFromFlag();
                try{
                    basicBug();
                } catch (GameActionException e){
                    //--------
                }
                Clock.yield(); // to end turn
            }
        }

        static final double passabilityThreshold = 0.7; //This is to check if the square near the robot's current location has a minimum passability of 0.7 out of 1
        static Direction bugDirection = null;

        static void basicBug(MapLocation target) throws GameActionException{
    
                Direction d = rc.getLocation().directionTo(target);
                if (rc.getLocation().equals(target)){
                    // -----------------
                } else if (rc.isReady()){
                  if(rc.canMove(d) && rc.sensePassability(rc.getLocation().add(d)) >= passabilityThreshold){
                      rc.move(d);
                      bugDirection = null;
                  } else {
                      if (bugDirection == null){
                          bugDirection = d.rotateRight();
                      }
                      for (int i = 0; i < 8; ++i) {
                          if (rc.canMove(bugDirection) && rc.sensePassability(rc.getLocation().add(bugDirection)) >= passabilityThreshold){
                              rc.move(bugDirection);
                              break;
                          }
                          bugDirection = bugDirection.rotateRight(); //act as a right-handed basic bug
                      }

                      bugDirection = bugDirection.rotateLeft(); //act as a left-handed basic bug
                  }
              }
        
        }

}
