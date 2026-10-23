package com.mojang.authlib.services;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.services.response.NameAndId;
import com.mojang.authlib.services.response.ProfileSearchResultsResponse;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftServicesProfileRepository implements GameProfileRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServicesProfileRepository.class);
    private static final int ENTRIES_PER_PAGE = 10;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final MinecraftClient client;
    private final MinecraftServicesDiscoveryService discoveryService;

    public MinecraftServicesProfileRepository(final Proxy proxy, final MinecraftServicesDiscoveryService discoveryService) {
        this.client = MinecraftClient.unauthenticated(proxy);
        this.discoveryService = discoveryService;
    }

    @Override
    public void findProfilesByNames(final String[] names, final ProfileLookupCallback callback) {
        final Set<String> criteria = Arrays.stream(names)
            .filter(name -> !Strings.isNullOrEmpty(name))
            .collect(Collectors.toSet());

        boolean hasRequested = false; // Paper - Don't sleep after profile lookups if not needed

        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            final List<String> normalizedRequest = request.stream().map(MinecraftServicesProfileRepository::normalizeName).toList();

            int failCount = 0;
            boolean failed;

            do {
                failed = false;

                try {
                    final ProfileSearchResultsResponse response = client.post(MinecraftServicesDiscoveryService.constantURL(discoveryService.getUrl(Service.PROFILES, "getManyByName")), normalizedRequest, ProfileSearchResultsResponse.class);
                    final List<NameAndId> results = response != null ? response.profiles() : List.of();
                    failCount = 0;

                    LOGGER.debug("{} results returned, parsing", results.size());

                    final Set<String> received = new HashSet<>(results.size());
                    for (final NameAndId profile : results) {
                        LOGGER.debug("Successfully looked up profile {}", profile);
                        received.add(normalizeName(profile.name()));
                        callback.onProfileLookupSucceeded(profile.name(), profile.id());
                    }

                    for (final String name : request) {
                        if (received.contains(normalizeName(name))) {
                            continue;
                        }
                        LOGGER.debug("Couldn't find profile {}", name);
                        callback.onProfileLookupFailed(name, new ProfileNotFoundException("Server did not find the requested profile"));
                    }

                    // Paper start - Don't sleep after profile lookups if not needed
                    if (!hasRequested) {
                        hasRequested = true;
                        continue;
                    }
                    // Paper end - Don't sleep after profile lookups if not needed

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (final InterruptedException ignored) {
                    }
                } catch (final MinecraftClientException e) {
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT) {
                        for (final String name : request) {
                            LOGGER.debug("Couldn't find profile {} because of a server error", name);
                            callback.onProfileLookupFailed(name, e.toAuthenticationException());
                        }
                    } else {
                        try {
                            Thread.sleep(DELAY_BETWEEN_FAILURES);
                        } catch (final InterruptedException ignored) {
                        }
                        failed = true;
                    }
                }
            } while (failed);
        }
    }

    @Override
    public Optional<NameAndId> findProfileByName(final String name) {
        try {
            return Optional.ofNullable(client.get(HttpDiscoveryService.constantURL(discoveryService.getUrl(Service.PROFILES, "getByName").replace("{name}", normalizeName(name))), NameAndId.class));
        } catch (final MinecraftClientException e) {
            // Paper start - only log details when debug is enabled
            final net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
            final boolean debugging = server != null && server.isDebugging();
            if (debugging && e instanceof com.mojang.authlib.exceptions.MinecraftClientHttpException httpException) {
                LOGGER.warn("Couldn't find profile with name: {}. Received status {} - {}", name, httpException.getStatus(), httpException.getMessage(), e);
            } else if (debugging) {
                LOGGER.warn("Couldn't find profile with name: {}", name, e);
            } else {
                LOGGER.warn("Couldn't find profile with name: {}", name);
            }
            // Paper end - only log details when debug is enabled
            return Optional.empty();
        }
    }

    private static String normalizeName(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
