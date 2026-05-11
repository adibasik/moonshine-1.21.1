package moonshine.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import moonshine.modules.impl.combat.*;
import moonshine.modules.impl.combat.NoInteract;
import moonshine.modules.impl.combat.AutoTotem;
import moonshine.modules.impl.misc.*;
import moonshine.modules.impl.misc.autoparser.AutoParser;
import moonshine.modules.impl.movement.*;
import moonshine.modules.impl.player.*;
import moonshine.modules.impl.render.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();
    List<ModuleStructure> hiddenModules = new ArrayList<>();
    Set<Class<? extends ModuleStructure>> registeredClasses = new HashSet<>();

    public void setup() {
        builder()
                .add(new Hud())
                .add(new Aura())
                .add(new HitEffect())
                .add(new Esp())
                .add(new BlockESP())
                .add(new AutoTool())
                .add(new RegionExploit())
                .add(new WorldParticles())
                .add(new Arrows())
                .add(new Particles())
                .add(new AuctionHelper())
                .add(new GlassHands())
                .add(new ChunkAnimator())
                .add(new MaceTarget())
                .add(new TriggerBot())
                .add(new BowSpammer())
                .add(new Ambience())
                .add(new AutoTotem())
                .add(new TapeMouse())
                .add(new ElytraHelper())
                .add(new ChinaHat())
                .add(new AutoPotion())
                .add(new Jesus())
                .add(new ClientSounds())
                .add(new AutoGApple())
                .add(new ServerHelper())
                .add(new WindJump())
                .add(new TargetESP())
                .add(new BlockOverlay())
                .add(new HitSound())
                .add(new ClickPearl())
                .add(new JumpCircle())
                .add(new ItemScroller())
                .add(new TargetStrafe())
                .add(new AutoLeave())
                .add(new Strafe())
                .add(new AutoDuel())
                .add(new NoWeb())
                .add(new AutoTpAccept())
                .add(new Spider())
                .add(new ClickFriend())
                .add(new FreeLook())
                .add(new Fly())
                .add(new ElytraMotion())
                .add(new FullBright())
                .add(new CameraSettings())
                .add(new ItemPhysic())
                .add(new NoDelay())
                .add(new ServerRPSpoofer())
                .add(new SeeInvisible())
                .add(new AutoPilot())
                .add(new NoFallDamage())
                .add(new NoRender())
                .add(new ShiftTap())
                .add(new HitBoxModule())
                .add(new WaterSpeed())
                .add(new NameProtect())
                .add(new NoFriendDamage())
                .add(new ProjectileHelper())
                .add(new InventoryMove())
                .add(new ChestStealer())
                .add(new NoInteract())
                .add(new AntiBot())
                .add(new ViewModel())
                .add(new SuperFireWork())
                .add(new RwJoiner())
                .add(new LongJump())
                .add(new ElytraTarget())
                .add(new FreeCam())
                .add(new Speed())
                .add(new AutoLes())
                .add(new NoEntityTrace())
                .add(new AutoMessage())
                .add(new AutoRespawn())
                .add(new AutoSwap())
                .add(new NoPush())
                .add(new CrystalAura())
                .add(new AutoCrystal())
                .add(new Shadow())
                .add(new NoSlow())
                .add(new AutoSnow())
                .add(new Velocity())
                .add(new SwingAnimation())
                .add(new ScoreboardHealth())
                .add(new GrimGlide())
                .add(new CordsDropper())
                .add(new AutoSprint())
                .add(new AutoBuy())
                .add(new SelfDestruct())
                .hidden(new AutoParser());
    }

    public ModuleBuilder builder() {
        return new ModuleBuilder(this);
    }

    void registerModule(ModuleStructure module, boolean hidden) {
        Class<? extends ModuleStructure> clazz = module.getClass();
        if (registeredClasses.contains(clazz)) {
            throw new DuplicateModuleException(clazz.getSimpleName());
        }
        registeredClasses.add(clazz);
        if (hidden) {
            hiddenModules.add(module);
            module.setState(true);
        } else {
            moduleStructures.add(module);
        }
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }

    public List<ModuleStructure> hiddenModules() {
        return hiddenModules;
    }

    public List<ModuleStructure> allModules() {
        List<ModuleStructure> all = new ArrayList<>(moduleStructures);
        all.addAll(hiddenModules);
        return all;
    }
}
