package map.road.speed_limit.base.response;

import java.util.List;

public class SnapToRoadResponse {
    public List<ResourceSet> resourceSets;

    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    public void setResourceSets(List<ResourceSet> resourceSets) {
        this.resourceSets = resourceSets;
    }

    public class ResourceSet {
        public List<Resource> resources;

        public List<Resource> getResources() {
            return resources;
        }

        public void setResources(List<Resource> resources) {
            this.resources = resources;
        }

        public class Resource {
            public List<SnappedPoint> getSnappedPoints() {
                return snappedPoints;
            }

            public void setSnappedPoints(List<SnappedPoint> snappedPoints) {
                this.snappedPoints = snappedPoints;
            }

            public List<SnappedPoint> snappedPoints;

            public class SnappedPoint {
                public String name;
                public int speedLimit;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public int getSpeedLimit() {
                    return speedLimit;
                }

                public void setSpeedLimit(int speedLimit) {
                    this.speedLimit = speedLimit;
                }
            }
        }
    }
}
