package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.Utils;
import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.model.mapper.RCConfigMapper;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.ControllerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class ControllerService {
    private final ControllerRepository controllerRepository;
    private static final Logger logger = LoggerFactory.getLogger(ControllerService.class);
    private final RCConfigMapper rcConfigMapper;
    public ControllerService(ControllerRepository controllerRepository, RCConfigMapper rcConfigMapper) {
        this.controllerRepository = controllerRepository;
        this.rcConfigMapper = rcConfigMapper;
    }

    public boolean isControllerLinked(String mac) {
        // TODO : проверить по всем видам?
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        return !(c == null || c.getUser() == null);
    }

    public User findControllerOwner(String mac) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c == null)
            return null;
        return c.getUser();
    }

    public String linkController(String mac, User user) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            c.setUser(user);
            c.setLinkDate(new Date());
            controllerRepository.save(c);
            return "OK";
        }
        return "NOT_FOUND";
    }

    public String unlinkController(String mac, User user) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            c.setUser(null);
            controllerRepository.save(c);
            return "OK";
        }
        return "NOT_FOUND";
    }

    public Controller findController(String mac) {
        return controllerRepository.findByMac(mac.toUpperCase());
    }

    public User getUserByController(String mac) {
        Controller controller = controllerRepository.findByMac(mac);
        if (controller != null)
            return controller.getUser();
        return null;
    }

    public List<Controller> getUserControllers(User user) {
        return controllerRepository.findByUser(user);
    }

    public List<Controller> getAllControllers() {
        return controllerRepository.findAll();
    }

    public void setControllerInfo(Info info) {
        Controller c = controllerRepository.findByMac(info.getMac().toUpperCase());
        if (c != null) {
            try {
                c.setUptime(info.getUptime());
                c.setUptimeRaw(info.getUptimeRaw());
                c.setFreeMemory(info.getFreeMemory());
                c.setVersion(info.getVersion());
                c.setEthIP(info.getEthIP());
                c.setWifiIP(info.getWifiIP());
//                c.setName(info.getName());
//                c.setDescription(info.getDescription());
                c.setWifiRSSI(info.getWifiRSSI());
                c.setModel(info.getModel());
                c.setResetReason(info.getResetReason());
                c.setStatus("online");
                controllerRepository.save(c);
            }
            catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        }
    }

    private void setControllerStatus(String mac, String status) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            c.setStatus(status);
            //c.setLastSeen(new Date());
            controllerRepository.save(c);
        }
    }

    public void setControllerOnline(String mac) {
        setControllerStatus(mac, "online");
    }

    public void setControllerOffline(String mac) {
        setControllerStatus(mac, "offline");
    }

    public void setInactiveOffline() {
        controllerRepository.setOffline();
    }

    public void updateLastSeen(UUID uuid) {
        Controller c = controllerRepository.findById(uuid).orElse(null);
        if (c != null) {
            c.setLastSeen(new Date());
            controllerRepository.save(c);
        }
    }

    public boolean isControllerBelongs(UUID uuid, User user) {
        Optional<Controller> c = controllerRepository.findById(uuid);
        return c.map(controller -> controller.getUser().equals(user)).orElse(false);
    }

    @Transactional
    public String deleteController(String mac) {
        Controller c = controllerRepository.findByMac(mac);
        if (c == null)
            return "No controller found";
        try {
            controllerRepository.delete(c);
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
        return "OK";
    }


    @Transactional
    public List<Object> getUserDevices(User user) {
        if (user == null)
            return null;

        List<Controller> userControllers;
        List<Object> controllers = new ArrayList<>();
        if (user.isAdmin()) {
            userControllers = getAllControllers();
        } else {
            userControllers = getUserControllers(user);
        }
        for (Controller controller : userControllers) {
            if ("relayController".equalsIgnoreCase(controller.getType()) && controller instanceof RelayController rc) {
                controllers.add(rcConfigMapper.RCtoDto(rc));
            }
        }

        return controllers;
    }

}
