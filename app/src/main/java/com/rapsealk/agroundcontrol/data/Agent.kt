package com.rapsealk.agroundcontrol.data

import com.google.android.gms.maps.model.LatLng

class Agent(val id: String, val leader: Boolean = false) {

    companion object {
        private val FLOCK_BOUNDARY = Double.POSITIVE_INFINITY // 25.0
    }

    var position = Vector() // GlobalPosition
    var velocity = Vector()

    /*
    public fun formationFlight(flockmates: List<Agent>, target: LatLng, mode: String = "row") {
        val localized = position.toLocalized()

        val visibleFlockMates = flockmates.filter { Vector.distnace3D(position, it.position) < FLOCK_BOUNDARY }

        var factorCollisionAvoidance    = Vector() // Separation
        var factorVelocityMatching      = Vector() // Alignment
        var factorFormationControl      = Vector() // Cohesion

        var acceleration = Vector()

        if (this.leader) {
            acceleration = WaypointFlight()
            this.velocity = this.velocity + acceleration
        } else {
            factorCollisionAvoidance = collisionAvoidance(visibleFlockMates)
            factorVelocityMatching = velocityMatching(visibleFlockMates)
            factorFormationControl = formationControl(visibleFlockMates, mode)
            acceleration = factorCollisionAvoidance + factorVelocityMatching + factorFormationControl
            self._velocity
        }


    }
    */

    /*
    private fun collisionAvoidance(flockmates: List<Agent>): Vector {

    }

    private fun velocityMatching(flockmates: List<Agent>): Vector {

    }

    private fun formationControl(flockmates: List<Agent>, mode: String = "row"): Vector {

    }

    private fun makeSteer(steer: Vector): Vector {

    }
    */
}
/*
def formation_flight(self, boundary=FLOCK_BOUNDARY, weights=[1, 1, 1], mode=FlightMode.COLUMN):

col_avo = Vector()
vel_mat = Vector()
for_con = Vector()

if self._leader:
acceleration = self.waypoint_flight(boundary=boundary)
self._velocity += acceleration
else:
#self._maxspeed = self._weights[self._wp_idx] * self._maxspeed_weight
col_avo = self.collision_avoidance(weight=weights[0], drones=neighbors)
vel_mat = self.velocity_matching(weight=weights[1], drones=neighbors)
for_con = self.formation_control(weight=weights[2], drones=neighbors, mode=mode)
self._log.write("[%f] collision avoidance: %s\n" % (time.time(), col_avo))
self._log.write("[%f] velocity matching  : %s\n" % (time.time(), vel_mat))
self._log.write("[%f] formation control  : %s\n" % (time.time(), for_con))
acceleration = (col_avo + vel_mat + for_con)
steer = self._velocity + acceleration
self._velocity = steer.make_steer(self._maxspeed)

message = "[%f] formation::velocity: %s\n" % (time.time(), self._velocity)
rospy.loginfo(message)
self._log.write(message)

gp_coord = (self._global_position.latitude, self._global_position.longitude)
velocity = (self._velocity.x_val, self._velocity.y_val)
( self._global_position_target.latitude,
self._global_position_target.longitude ) = haversine.InverseHaversine2D(gp_coord, velocity).getCoord()

#self._posestamped.position.x += steer.x_val
#self._posestamped.position.y += steer.y_val

col_avo = haversine.InverseHaversine2D(gp_coord, (col_avo.x_val, col_avo.y_val)).getCoord()
vel_mat = haversine.InverseHaversine2D(gp_coord, (vel_mat.x_val, vel_mat.y_val)).getCoord()
for_con = haversine.InverseHaversine2D(gp_coord, (for_con.x_val, for_con.y_val)).getCoord()
steer = haversine.InverseHaversine2D(gp_coord, (self._velocity.x_val, self._velocity.y_val)).getCoord()
(self._collision_avoidance,
self._velocity_matching,
self._flocking_center,
self._formation_control,
self._steer,
self._target) = (Vector(col_avo[0], col_avo[1]),
Vector(vel_mat[0], vel_mat[1]),
Vector(),
Vector(for_con[0], for_con[1]),
Vector(steer[0], steer[1]),
Vector.from_global_position(self._global_position_target))
"""
        self._collision_avoidance = Vector(col_avo[0], col_avo[1])
        self._velocity_matching = Vector(vel_mat[0], vel_mat[1])
        self._flocking_center = Vector()
        self._formation_control = Vector(for_con[0], for_con[1])
        self._steer = Vector(steer[0], steer[1])
        self._target = Vector.from_global_position(self._global_position_target)
        """

self._log.write("[%f] formation :: position: %s\n" % (time.time(), gp_coord))
self._log.write("[%f] formation :: target: %s\n" % (time.time(), (self._global_position_target.latitude, self._global_position_target.longitude)))
self._log.write("[%f] formation :: diff: %s\n" % (time.time(), (self._global_position_target.latitude - gp_coord[0], self._global_position_target.longitude - gp_coord[1])))

def formation_control(self, weight=1, drones=[], mode=FlightMode.COLUMN, swarm_distance=10):
if mode == FlightMode.COLUMN:
return self.column_formation_control(weight=weight, drones=drones, swarm_distance=swarm_distance)
elif mode == FlightMode.ROW:
return self.row_formation_control(weight=weight, drones=drones, swarm_distance=swarm_distance)
else:
return Vector()

def column_formation_control(self, weight=1, drones=[], swarm_distance=3):
# Preprocessing
column = { "left": [], "right": [] }

for drone in drones:
drone["distance"] = float("inf")

# find leader location / direction / velocity
leader = { "location": Vector(), "direction": Vector() }
for drone in drones:
if drone["leader"]:
leader["location"]  = drone["location"]
leader["direction"] = self._waypoints[self._wp_idx] - drone["location"]
leader["direction"].normalize2D()

# y = ax + b, a = north/east, north: x_val, east: y_val
if leader["direction"].y_val == 0:
a = float("inf") * leader["direction"].x_val
else:
a = leader["direction"].x_val / leader["direction"].y_val
b = leader["location"].x_val - a * leader["location"].y_val

# Assign left or right
for drone in drones:
drone["distance"] = localmap.distance3Dv(loc3d1=drone["location"], loc3d2=leader["location"])
if not drone["leader"]:
if leader["direction"].y_val > 0:
if drone["location"].x_val - a * drone["location"].y_val >= b:
column["left"].append(drone)
else:
column["right"].append(drone)
else:
if drone["location"].x_val - a * drone["location"].y_val <= b:
column["left"].append(drone)
else:
column["right"].append(drone)

column["left"] = self.distance_qsort(column["left"])
column["right"] = self.distance_qsort(column["right"])

while len(column["left"]) > len(drones) / 2:
column["right"].insert(0, column["left"].pop(0))
while len(column["right"]) > len(drones) / 2:
column["left"].insert(0, column["right"].pop(0))

distribute = Vector()
for idx, drone in enumerate(column["left"]):
if drone["hostname"] == self._socket._id:
distribute = leader["direction"].turn_left() * (idx + 1) * swarm_distance
break
for idx, drone in enumerate(column["right"]):
if drone["hostname"] == self._socket._id:
distribute = leader["direction"].turn_right() * (idx + 1) * swarm_distance
break
desired = distribute + leader["location"] - self._location

# Calculate maxspeed weight
target_distance = desired.size()
self._maxspeed_weight = math.log1p(target_distance) / 10.0 + 1

desired = desired.normalize() * self._maxspeed
steer = desired - self._velocity
steer = steer.make_steer(self._maxforce)

return steer * weight


def row_formation_control(self, weight=1, drones=[], swarm_distance=2):
for drone in drones:
drone["distance"] = float("inf")

leader = { "location": Vector(), "direction": Vector() }
for drone in drones:
if drone["leader"]:
leader["location"] = drone["location"]
leader["direction"] = self._waypoints[self._wp_idx] - drone["location"]
leader["direction"].normalize2D()

# Assign left or right
for drone in drones:
drone["distance"] = localmap.distance3Dv(loc3d1=drone["location"], loc3d2=leader["location"])
drones = self.distance_qsort(drones)

distribute = Vector()
for idx, drone in enumerate(drones):
if drone["hostname"] == self._socket._id:
distribute = leader["direction"].turn_around() * (idx + 1) * swarm_distance
break
desired = distribute + leader["location"] - self._location

# Calculate maxspeed weight
target_distance = desired.size()
self._maxspeed_weight = math.log1p(target_distance) / 10.0 + 1

desired = desired.normalize() * self._maxspeed
steer = desired - self._velocity
steer = steer.make_steer(self._maxforce)

return steer * weight
*/