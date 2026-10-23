package com.mojang.authlib.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientException.ErrorType;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.services.response.discovery.DiscoveryResponse;
import com.mojang.authlib.services.response.discovery.Endpoint;
import com.mojang.authlib.services.response.discovery.Endpoints;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftServicesDiscoveryService extends HttpDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServicesDiscoveryService.class);

    public static final ScheduledExecutorService DISCOVERY_EXECUTOR = Executors.newScheduledThreadPool(
        1, new ThreadFactoryBuilder()
            .setNameFormat("Minecraft Services Discovery")
            .setDaemon(true)
            .build()
    );

    private final boolean servicesKeySetEnabled;
    private final Supplier<DiscoveryResponse> discoverySupplier;

    protected MinecraftServicesDiscoveryService( // Paper - private->protected
        final Proxy proxy,
        final boolean servicesKeySetEnabled,
        final Supplier<DiscoveryResponse> discoverySupplier
    ) {
        super(proxy);
        this.servicesKeySetEnabled = servicesKeySetEnabled;
        this.discoverySupplier = discoverySupplier;
    }

    public static MinecraftServicesDiscoveryService create(final Proxy proxy) {
        return create(proxy, true, determineEnvironment());
    }

    public static MinecraftServicesDiscoveryService create(final Proxy proxy, final boolean servicesKeySetEnabled) {
        return create(proxy, servicesKeySetEnabled, determineEnvironment());
    }

    public static MinecraftServicesDiscoveryService create(
        final Proxy proxy,
        final boolean servicesKeySetEnabled,
        final Environment environment
    ) {
        LOGGER.info("Environment: {}", environment);
        return new MinecraftServicesDiscoveryService(
            proxy,
            servicesKeySetEnabled,
            createDiscoverySupplier(proxy, environment)
        );
    }

    protected static Supplier<DiscoveryResponse> createDiscoverySupplier(final Proxy proxy, final Environment environment) { // Paper - private->protected
        final MinecraftClient client = MinecraftClient.unauthenticated(proxy);
        return RetryableFetch.fetch(
            () -> HttpDiscoveryService.constantURL(environment.discoveryUrl()),
            DISCOVERY_EXECUTOR,
            (url) -> Optional.ofNullable(client.get(url, DiscoveryResponse.class)),
            DiscoveryResponse.offline(),
            RetryableFetch.REFRESH_INTERVAL_HOURS,
            1
        );
    }

    public static MinecraftServicesDiscoveryService createOffline(final Proxy proxy) {
        return new MinecraftServicesDiscoveryService(proxy, false, DiscoveryResponse::offline);
    }

    protected static Environment determineEnvironment() { // Paper - private->protected
        return EnvironmentParser
            .getEnvironmentFromProperties()
            .orElse(MinecraftServicesEnvironment.PROD.getEnvironment());
    }

    @Override
    public SessionService createMinecraftSessionService() {
        return new MinecraftServicesSessionService(getServicesKeySet(), getProxy(), this);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new MinecraftServicesProfileRepository(getProxy(), this);
    }

    public UserApiService createUserApiService(final String accessToken) {
        return new MinecraftServicesUserApiService(accessToken, this, getProxy());
    }

    public FriendsService createFriendsService(final String accessToken) {
        return new MinecraftServicesFriendsService(accessToken, getProxy(), this);
    }

    public ServicesKeySet getServicesKeySet() {
        if (!servicesKeySetEnabled) {
            return ServicesKeySet.EMPTY;
        }
        final MinecraftClient client = MinecraftClient.unauthenticated(getProxy());
        return MinecraftServicesKeyInfo.get(
            () -> HttpDiscoveryService.constantURL(getUrl(
                Service.AUTHENTICATION,
                "getPublicKeys"
            )), client
        );
    }

    public String getUrl(final Service service, final String endpointKey) {
        final Endpoint endpoint = getEndpoint(service, endpointKey);
        return endpoint.uri();
    }

    public List<String> getValidUris(final Service service, final String endpointKey) {
        final Endpoint endpoint = getEndpoint(service, endpointKey);
        final List<String> validUris = endpoint.validUris();
        return validUris != null ? validUris : List.of();
    }

    private Endpoint getEndpoint(final Service service, final String endpointKey) {
        final DiscoveryResponse discovery = discoverySupplier.get();
        if (discovery.isOffline()) {
            LOGGER.warn("Services are unavailable, unable to fetch uri for {}/{}", service, endpointKey);
            throw new MinecraftClientException(ErrorType.SERVICE_UNAVAILABLE, "Services are unavailable");
        }
        final Endpoints endpoints = discovery.discovery().mapEndpoints(service);
        final Endpoint endpoint = endpoints.endpoints().get(endpointKey);
        if (endpoint == null) {
            LOGGER.warn("Services are available, but unable to find uri for {}/{}", service, endpointKey);
            throw new MinecraftClientException(ErrorType.SERVICE_UNAVAILABLE, "Services are unavailable");
        }
        return endpoint;
    }

    public boolean isAllowedTextureDomain(final String url) {
        final List<String> validUris = getValidUris(Service.PROFILES, "getTexture");
        for (final String validUri : validUris) {
            if (url.startsWith(validUri.replace("{textureId}", ""))) {
                return true;
            }
        }
        return false;
    }
}
