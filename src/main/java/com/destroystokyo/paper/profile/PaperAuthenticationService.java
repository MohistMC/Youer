package com.destroystokyo.paper.profile;

import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;

import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import java.net.Proxy;

public class PaperAuthenticationService extends MinecraftServicesDiscoveryService {

    private final Environment environment;

    public PaperAuthenticationService(Proxy proxy) {
        super(proxy);
        this.environment = EnvironmentParser.getEnvironmentFromProperties().orElse(YggdrasilEnvironment.PROD.getEnvironment());
    }

    @Override
    public SessionService createMinecraftSessionService() {
        return new PaperMinecraftSessionService(this.getServicesKeySet(), this.getProxy(), this.environment);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new PaperGameProfileRepository(this.getProxy(), this.environment);
    }
}
