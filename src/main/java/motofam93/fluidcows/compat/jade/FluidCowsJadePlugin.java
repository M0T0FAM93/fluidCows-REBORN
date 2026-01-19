package motofam93.fluidcows.compat.jade;

import motofam93.fluidcows.entity.FluidCowEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FluidCowsJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // send server data (cooldown ticks) to client
        registration.registerEntityDataProvider(FluidCowJadeProvider.INSTANCE, FluidCowEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // render a line in the tooltip
        registration.registerEntityComponent(FluidCowJadeProvider.INSTANCE, FluidCowEntity.class);
    }
}
