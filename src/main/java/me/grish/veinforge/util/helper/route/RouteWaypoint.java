package me.grish.veinforge.util.helper.route;

import com.google.gson.annotations.Expose;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class RouteWaypoint {

    @Expose
    private int x;
    @Expose
    private int y;
    @Expose
    private int z;
    @Expose
    private WaypointType transportMethod;

    public RouteWaypoint() {
    }

    public RouteWaypoint(int x, int y, int z, WaypointType transportMethod) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.transportMethod = transportMethod;
    }

    public RouteWaypoint(BlockPos pos, WaypointType transportMethod) {
        this(pos.getX(), pos.getY(), pos.getZ(), transportMethod);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public WaypointType getTransportMethod() {
        return transportMethod;
    }

    public void setTransportMethod(WaypointType transportMethod) {
        this.transportMethod = transportMethod;
    }

    public boolean isWithinRange(BlockPos pos, int range) {
        return Math.abs(pos.getX() - x) <= range && Math.abs(pos.getY() - y) <= range && Math.abs(pos.getZ() - z) <= range;
    }

    public int distanceTo(BlockPos pos) {
        return (int) Math.sqrt(Math.pow(pos.getX() - x, 2) + Math.pow(pos.getY() - y, 2) + Math.pow(pos.getZ() - z, 2));
    }

    public Vec3 toVec3d() {
        return new Vec3(this.x, this.y, this.z);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z + "," + (transportMethod == null ? "null" : transportMethod.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteWaypoint that = (RouteWaypoint) o;
        return x == that.x && y == that.y && z == that.z && transportMethod == that.transportMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, transportMethod);
    }
}
