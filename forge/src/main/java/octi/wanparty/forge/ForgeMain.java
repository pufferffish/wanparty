/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package octi.wanparty.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import octi.wanparty.common.WANParty;

@Mod(WANParty.MOD_ID)
public class ForgeMain {
    public ForgeMain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initClient);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initDedicated);
    }

    private void initClient(final FMLClientSetupEvent event) {
        WANParty.initClient();
    }

    private void initDedicated(final FMLDedicatedServerSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStartup);
    }

    public void onServerStartup(ServerStartedEvent event) {
        WANParty.initServer(event.getServer().getPort());
    }

}
