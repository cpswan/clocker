/*
 * Copyright 2014-2015 by Cloudsoft Corporation Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package brooklyn.entity.container.docker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicStartable;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.DynamicGroup;
import brooklyn.entity.container.DockerAttributes;
import brooklyn.entity.container.DockerUtils;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.group.DynamicMultiGroup;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Resizable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.AttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.location.docker.DockerLocation;
import brooklyn.location.docker.strategy.DepthFirstPlacementStrategy;
import brooklyn.location.docker.strategy.DockerAwarePlacementStrategy;
import brooklyn.location.docker.strategy.affinity.AffinityRules;
import brooklyn.location.dynamic.LocationOwner;
import brooklyn.networking.sdn.SdnAttributes;
import brooklyn.util.collections.MutableList;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.time.Duration;

import com.google.common.reflect.TypeToken;

/**
 * A collection of machines running Docker.
 */
@Catalog(name = "Docker Infrastructure",
        description = "Docker is an open-source engine to easily create lightweight, portable, self-sufficient containers from any application.",
        iconUrl = "classpath:///docker-logo.png")
@ImplementedBy(DockerInfrastructureImpl.class)
public interface DockerInfrastructure extends BasicStartable, Resizable, LocationOwner<DockerLocation, DockerInfrastructure> {

    @SetFromFlag("dockerVersion")
    ConfigKey<String> DOCKER_VERSION = ConfigKeys.newStringConfigKey("docker.version", "The Docker Engine version number", "1.4.1");

    @SetFromFlag("securityGroup")
    ConfigKey<String> SECURITY_GROUP = ConfigKeys.newStringConfigKey(
            "docker.host.securityGroup", "Set a network security group for cloud servers to use; (null to use default configuration)");

    @SetFromFlag("minHost")
    ConfigKey<Integer> DOCKER_HOST_CLUSTER_MIN_SIZE = ConfigKeys.newConfigKeyWithPrefix("docker.host.", DynamicCluster.INITIAL_SIZE);

    @SetFromFlag("strategies")
    ConfigKey<List<DockerAwarePlacementStrategy>> PLACEMENT_STRATEGIES = ConfigKeys.newConfigKeyWithDefault(DockerAttributes.PLACEMENT_STRATEGIES,
            MutableList.<DockerAwarePlacementStrategy>of(new DepthFirstPlacementStrategy()));

    @SetFromFlag("highAvailabilty")
    ConfigKey<Boolean> HA_POLICY_ENABLE = ConfigKeys.newBooleanConfigKey("docker.policy.ha.enable",
            "Enable high-availability and resilience/restart policies", Boolean.FALSE);

    @SetFromFlag("removeEmptyHosts")
    ConfigKey<Boolean> REMOVE_EMPTY_DOCKER_HOSTS = ConfigKeys.newBooleanConfigKey("docker.host.removeEmpty",
            "Remove empty Docker Hosts with no containers", Boolean.FALSE);

    @SetFromFlag("enableSdn")
    ConfigKey<Boolean> SDN_ENABLE = SdnAttributes.SDN_ENABLE;

    @SetFromFlag("sdnProviderSpec")
    ConfigKey<EntitySpec> SDN_PROVIDER_SPEC = ConfigKeys.newConfigKey(EntitySpec.class, "sdn.provider.spec", "SDN provider entity specification");

    @SetFromFlag("hostSpec")
    AttributeSensorAndConfigKey<EntitySpec, EntitySpec> DOCKER_HOST_SPEC = ConfigKeys.newSensorAndConfigKey(
            EntitySpec.class, "docker.host.spec", "Specification to use when creating child Docker Hosts",
            EntitySpec.create(DockerHost.class));

    @SetFromFlag("tlsCertificate")
    ConfigKey<String> DOCKER_CERTIFICATE_PATH = ConfigKeys.newStringConfigKey("docker.tls.certificate", "The Docker Engine TLS certificate PEM file path", "conf/server-cert.pem");

    @SetFromFlag("tlsKey")
    ConfigKey<String> DOCKER_KEY_PATH = ConfigKeys.newStringConfigKey("docker.tls.key", "The Docker Engine TLS key PEM file path", "conf/server-key.pem");

    @SetFromFlag("dockerfileUrl")
    ConfigKey<String> DOCKERFILE_URL = ConfigKeys.newConfigKeyWithDefault(DockerAttributes.DOCKERFILE_URL, DockerUtils.UBUNTU_DOCKERFILE);

    @SetFromFlag("dockerfileName")
    ConfigKey<String> DOCKERFILE_NAME = ConfigKeys.newConfigKeyWithDefault(DockerAttributes.DOCKERFILE_NAME, "ubuntu");

    @SetFromFlag("imageId")
    ConfigKey<String> DOCKER_IMAGE_ID = DockerAttributes.DOCKER_IMAGE_ID.getConfigKey();

    @SetFromFlag("hardwareId")
    ConfigKey<String> DOCKER_HARDWARE_ID = DockerAttributes.DOCKER_HARDWARE_ID.getConfigKey();

    @SetFromFlag("affinityRules")
    ConfigKey<List<String>> DOCKER_HOST_AFFINITY_RULES = AffinityRules.AFFINITY_RULES;

    @SetFromFlag("shutdownTimeout")
    ConfigKey<Duration> SHUTDOWN_TIMEOUT = ConfigKeys.newDurationConfigKey("docker.timeout.shutdown", "Timeout to wait for children when shutting down", Duration.FIVE_MINUTES);

    @SetFromFlag("substitutions")
    ConfigKey<Map<String, Object>> DOCKERFILE_SUBSTITUTIONS = ConfigKeys.newConfigKey(
            new TypeToken<Map<String, Object>>() { },
            "docker.dockerfile.substitutions", "Dockerfile template substitutions", MutableMap.<String, Object>of());

    AttributeSensor<DynamicCluster> DOCKER_HOST_CLUSTER = Sensors.newSensor(DynamicCluster.class, "docker.hosts", "Docker host cluster");
    AttributeSensor<DynamicGroup> DOCKER_CONTAINER_FABRIC = Sensors.newSensor(DynamicGroup.class, "docker.fabric", "Docker container fabric");
    AttributeSensor<DynamicMultiGroup> DOCKER_APPLICATIONS = Sensors.newSensor(DynamicMultiGroup.class, "docker.buckets", "Docker applications");
    AttributeSensor<Entity> SDN_PROVIDER = Sensors.newSensor(Entity.class, "sdn.provider.network", "SDN provider network entity");

    AttributeSensor<AtomicInteger> DOCKER_HOST_COUNTER = Sensors.newSensor(AtomicInteger.class, "docker.hosts.counter", "Docker host counter");
    AttributeSensor<AtomicInteger> DOCKER_CONTAINER_COUNTER = Sensors.newSensor(AtomicInteger.class, "docker.containers.counter", "Docker container counter");;

    AttributeSensor<Integer> DOCKER_HOST_COUNT = DockerAttributes.DOCKER_HOST_COUNT;
    AttributeSensor<Integer> DOCKER_CONTAINER_COUNT = DockerAttributes.DOCKER_CONTAINER_COUNT;

    List<Entity> getDockerHostList();

    DynamicCluster getDockerHostCluster();

    List<Entity> getDockerContainerList();

    DynamicGroup getContainerFabric();
}
